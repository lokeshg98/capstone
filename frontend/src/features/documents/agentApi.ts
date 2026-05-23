import axios from 'axios';
import { useAuthStore } from '@/features/auth/useAuthStore';
import { api } from '@/lib/api';

/** Mirrors backend AgentStreamEvent.AgentAnswerPayload */
export interface AgentCitationPayload {
  documentTitle: string;
  chunkText: string;
  chunkIndex: number;
}

export interface AgentStepPayload {
  kind: string;
  detail: string;
}

export interface ProposalPayload {
  id: string;
  actionType: string;
  summary: string;
}

export interface AgentAnswerPayload {
  answer: string | null;
  sourceChunks: number;
  citations: AgentCitationPayload[];
  steps: AgentStepPayload[];
  proposedAction: ProposalPayload | null;
}

export type AgentStreamEventType =
  | 'thinking'
  | 'tool_start'
  | 'tool_result'
  | 'token'
  | 'action_proposal'
  | 'final';

/** Mirrors backend AgentStreamEvent */
export interface AgentStreamEvent {
  type: AgentStreamEventType;
  message?: string | null;
  toolName?: string | null;
  toolArgsSummary?: string | null;
  toolResultSummary?: string | null;
  token?: string | null;
  payload?: AgentAnswerPayload | null;
}

async function refreshAccessToken(): Promise<string> {
  const { data } = await axios.post<{ ok: boolean; data: { accessToken: string } }>(
    '/api/auth/refresh',
    {},
    { withCredentials: true },
  );
  const newToken = data.data.accessToken;
  useAuthStore.getState().setAccessToken(newToken);
  return newToken;
}

async function authorizedFetch(input: RequestInfo | URL, init: RequestInit, retried = false): Promise<Response> {
  let token = useAuthStore.getState().accessToken;
  const headers = new Headers(init.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  headers.set('Accept', 'text/event-stream');
  if (!headers.has('Content-Type') && init.body) headers.set('Content-Type', 'application/json');

  const res = await fetch(input, { ...init, headers });

  if (res.status === 401 && !retried) {
    try {
      await refreshAccessToken();
      return authorizedFetch(input, init, true);
    } catch {
      useAuthStore.getState().clear();
      window.location.href = '/login';
      throw new Error('Session expired');
    }
  }

  return res;
}

function parseSseBlock(raw: string): { event?: string; data: string } {
  let event: string | undefined;
  const dataLines: string[] = [];
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart());
  }
  return { event, data: dataLines.join('\n') };
}

/**
 * POST /api/workspaces/:id/agent/ask/stream — consumes SSE `event: agent` with JSON payloads.
 */
export async function streamAgentAsk(
  workspaceId: string,
  question: string,
  conversationId: string,
  onEvent: (ev: AgentStreamEvent) => void,
  options?: { signal?: AbortSignal },
): Promise<void> {
  const res = await authorizedFetch(`/api/workspaces/${workspaceId}/agent/ask/stream`, {
    method: 'POST',
    body: JSON.stringify({ question, conversationId }),
    signal: options?.signal,
  });

  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try {
      const j = (await res.json()) as { error?: string };
      if (j.error) msg = j.error;
    } catch {
      /* ignore */
    }
    throw new Error(msg);
  }

  const reader = res.body?.getReader();
  if (!reader) throw new Error('No response body');

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    const chunks = buffer.split('\n\n');
    buffer = chunks.pop() ?? '';

    for (const block of chunks) {
      if (!block.trim()) continue;
      const { event, data } = parseSseBlock(block);
      if (event !== 'agent' || !data) continue;
      try {
        onEvent(JSON.parse(data) as AgentStreamEvent);
      } catch {
        /* skip malformed */
      }
    }
  }

  if (buffer.trim()) {
    const { event, data } = parseSseBlock(buffer);
    if (event === 'agent' && data) {
      try {
        onEvent(JSON.parse(data) as AgentStreamEvent);
      } catch {
        /* skip */
      }
    }
  }
}

export async function confirmAgentAction(workspaceId: string, actionId: string): Promise<void> {
  await api.post(`/workspaces/${workspaceId}/agent/actions/${actionId}/confirm`);
}

export async function declineAgentAction(workspaceId: string, actionId: string): Promise<void> {
  await api.post(`/workspaces/${workspaceId}/agent/actions/${actionId}/decline`);
}
