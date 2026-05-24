import { useState, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Bot, Send, FileText, Upload, ChevronDown, ChevronUp, Trash2 } from 'lucide-react';
import {
  streamAgentAsk,
  confirmAgentAction,
  declineAgentAction,
  type AgentCitationPayload,
  type ProposalPayload,
} from './agentApi';
import { fetchDocuments, ingestDocument, deleteDocument } from './documentApi';
import { uploadAttachment } from '@/features/messages/attachmentApi';
import { cn } from '@/lib/utils';
import { AgentTimeline, type ToolCallEntry } from './AgentTimeline';

interface Props {
  workspaceId: string;
}

interface ChatTurn {
  id:             string;
  question:       string;
  answer:         string;
  sourceChunks:   number;
  thinking:       string | null;
  toolCalls:      ToolCallEntry[];
  citations:      AgentCitationPayload[];
  proposedAction: ProposalPayload | null;
  proposalStatus: 'pending' | 'confirmed' | 'declined' | null;
  proposalBusy:   boolean;
  loading:        boolean;
  error?:         string;
}

/**
 * Full-page Ask Bot: SSE multi-agent stream, tool timeline, citations, scheduling confirmation.
 */
export default function AskBotPanel({ workspaceId }: Props) {
  const [question, setQuestion]  = useState('');
  const [history, setHistory]   = useState<ChatTurn[]>([]);
  const [inputBusy, setInputBusy] = useState(false);
  const [showDocs, setShowDocs]   = useState(false);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const conversationIdRef = useRef(crypto.randomUUID());

  const { data: docs = [], refetch: refetchDocs } = useQuery({
    queryKey: ['documents', workspaceId],
    queryFn:  () => fetchDocuments(workspaceId),
  });

  const handleConfirmProposal = async (turnId: string) => {
    let actionId: string | null = null;
    setHistory((h) => {
      const t = h.find((x) => x.id === turnId);
      actionId = t?.proposedAction?.id ?? null;
      if (!actionId) return h;
      return h.map((x) => (x.id === turnId ? { ...x, proposalBusy: true } : x));
    });
    if (!actionId) return;
    try {
      await confirmAgentAction(workspaceId, actionId);
      setHistory((h) =>
        h.map((x) =>
          x.id === turnId ? { ...x, proposalStatus: 'confirmed', proposalBusy: false } : x,
        ),
      );
    } catch {
      setHistory((h) => h.map((x) => (x.id === turnId ? { ...x, proposalBusy: false } : x)));
    }
  };

  const handleDeclineProposal = async (turnId: string) => {
    let actionId: string | null = null;
    setHistory((h) => {
      const t = h.find((x) => x.id === turnId);
      actionId = t?.proposedAction?.id ?? null;
      if (!actionId) return h;
      return h.map((x) => (x.id === turnId ? { ...x, proposalBusy: true } : x));
    });
    if (!actionId) return;
    try {
      await declineAgentAction(workspaceId, actionId);
      setHistory((h) =>
        h.map((x) =>
          x.id === turnId ? { ...x, proposalStatus: 'declined', proposalBusy: false } : x,
        ),
      );
    } catch {
      setHistory((h) => h.map((x) => (x.id === turnId ? { ...x, proposalBusy: false } : x)));
    }
  };

  const handleAsk = async () => {
    const q = question.trim();
    if (!q || inputBusy) return;
    setQuestion('');
    const turnId = crypto.randomUUID();
    const newTurn: ChatTurn = {
      id:             turnId,
      question:       q,
      answer:         '',
      sourceChunks:   0,
      thinking:       'Planning and calling tools as needed…',
      toolCalls:      [],
      citations:      [],
      proposedAction: null,
      proposalStatus: null,
      proposalBusy:   false,
      loading:        true,
    };
    setHistory((h) => [...h, newTurn]);
    setInputBusy(true);

    try {
      await streamAgentAsk(workspaceId, q, conversationIdRef.current, (ev) => {
        setHistory((h) => {
          const idx = h.findIndex((t) => t.id === turnId);
          if (idx < 0) return h;
          const cur = h[idx];

          if (ev.type === 'thinking' && ev.message) {
            const next = [...h];
            next[idx] = { ...cur, thinking: ev.message };
            return next;
          }

          if (ev.type === 'tool_start' && ev.toolName) {
            const next = [...h];
            next[idx] = {
              ...cur,
              thinking:  null,
              toolCalls: [
                ...cur.toolCalls,
                {
                  id:     crypto.randomUUID(),
                  name:   ev.toolName,
                  args:   ev.toolArgsSummary ?? '',
                  result: undefined,
                },
              ],
            };
            return next;
          }

          if (ev.type === 'tool_result' && ev.toolName) {
            const tc = [...cur.toolCalls];
            for (let i = tc.length - 1; i >= 0; i--) {
              if (tc[i].name === ev.toolName && tc[i].result === undefined) {
                tc[i] = { ...tc[i], result: ev.toolResultSummary ?? '' };
                break;
              }
            }
            const next = [...h];
            next[idx] = { ...cur, toolCalls: tc };
            return next;
          }

          if (ev.type === 'token' && ev.token) {
            const next = [...h];
            next[idx] = { ...cur, thinking: null, answer: cur.answer + ev.token };
            return next;
          }

          if (ev.type === 'action_proposal' && ev.payload?.proposedAction) {
            const next = [...h];
            next[idx] = {
              ...cur,
              proposedAction: ev.payload.proposedAction,
              proposalStatus: 'pending',
            };
            return next;
          }

          if (ev.type === 'final' && ev.payload) {
            const p = ev.payload;
            const next = [...h];
            next[idx] = {
              ...cur,
              answer:         p.answer ?? cur.answer,
              sourceChunks:   p.sourceChunks,
              citations:      p.citations ?? [],
              loading:        false,
              thinking:       null,
              proposedAction: p.proposedAction ?? cur.proposedAction,
              proposalStatus: p.proposedAction
                ? (cur.proposalStatus ?? 'pending')
                : cur.proposalStatus,
            };
            return next;
          }

          return h;
        });
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Bot is unavailable. Please try again.';
      setHistory((h) => {
        const idx = h.findIndex((t) => t.id === turnId);
        if (idx < 0) return h;
        const next = [...h];
        next[idx] = {
          ...h[idx],
          loading: false,
          thinking: null,
          error:   `⚠️ ${msg}`,
        };
        return next;
      });
    } finally {
      setInputBusy(false);
      setHistory((h) => {
        const idx = h.findIndex((t) => t.id === turnId);
        if (idx < 0) return h;
        const t = h[idx];
        if (!t.loading) return h;
        const next = [...h];
        next[idx] = { ...t, loading: false, thinking: null };
        return next;
      });
      inputRef.current?.focus();
    }
  };

  return (
    <div className="flex-1 flex flex-col bg-white overflow-hidden">
      <div className="shrink-0 h-14 px-5 flex items-center gap-3 border-b border-gray-200">
        <div className="h-8 w-8 rounded-lg bg-brand-600 flex items-center justify-center">
          <Bot className="h-4 w-4 text-white" />
        </div>
        <div>
          <p className="font-semibold text-gray-900 text-sm leading-tight">Community Bot</p>
          <p className="text-xs text-gray-400">FAQ search · conversation memory · channels · moderation</p>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-5">
        {history.length === 0 && (
          <div className="text-center text-gray-400 pt-12">
            <Bot className="mx-auto h-12 w-12 mb-3 opacity-20" />
            <p className="text-sm font-medium">Ask anything about your community</p>
            <p className="text-xs mt-1 max-w-md mx-auto">
              Similar past questions and answers from this session and earlier Ask Bot chats are
              retrieved automatically before the bot replies.
            </p>
            <p className="text-xs mt-3 text-brand-500">
              Tip: mention <code className="bg-gray-100 px-1 rounded">@bot</code> in a channel for a short FAQ-style reply.
            </p>
          </div>
        )}

        {history.map((turn) => (
          <div key={turn.id} className="space-y-2">
            <div className="flex justify-end">
              <div className="max-w-[75%] rounded-2xl rounded-tr-sm bg-brand-600 text-white px-4 py-2.5 text-sm">
                {turn.question}
              </div>
            </div>
            <div className="flex items-start gap-2.5">
              <div className="h-7 w-7 rounded-full bg-brand-100 flex items-center justify-center shrink-0 mt-0.5">
                <Bot className="h-4 w-4 text-brand-600" />
              </div>
              <div className="max-w-[80%] rounded-2xl rounded-tl-sm bg-gray-100 px-4 py-2.5 text-sm text-gray-800">
                {turn.error && (
                  <p className="text-red-600 whitespace-pre-wrap">{turn.error}</p>
                )}
                {!turn.error && turn.answer && (
                  <div className="whitespace-pre-wrap">{turn.answer}</div>
                )}
                {!turn.error && !turn.answer && turn.loading && (
                  <div className="flex gap-1 items-center py-1">
                    <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:0ms]" />
                    <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:150ms]" />
                    <span className="h-1.5 w-1.5 rounded-full bg-gray-400 animate-bounce [animation-delay:300ms]" />
                  </div>
                )}
                {!turn.error && !turn.loading && !turn.answer && (
                  <p className="text-gray-500 text-xs">No response.</p>
                )}

                {!turn.error && turn.sourceChunks > 0 && (
                  <p className="mt-2 text-xs text-gray-400">
                    Based on {turn.sourceChunks} document excerpt{turn.sourceChunks !== 1 ? 's' : ''}
                  </p>
                )}

                {!turn.error && (
                  <AgentTimeline
                    thinking={turn.loading ? turn.thinking : null}
                    toolCalls={turn.toolCalls}
                    citations={turn.citations}
                    proposedAction={turn.proposedAction}
                    proposalStatus={turn.proposalStatus}
                    proposalBusy={turn.proposalBusy}
                    onConfirmAction={() => void handleConfirmProposal(turn.id)}
                    onDeclineAction={() => void handleDeclineProposal(turn.id)}
                  />
                )}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="shrink-0 px-4 pb-4 pt-2">
        <div
          className={cn(
            'flex items-end gap-2 rounded-xl border bg-white px-3 py-2 transition-shadow',
            'focus-within:ring-2 focus-within:ring-brand-500 focus-within:border-transparent border-gray-300',
          )}
        >
          <textarea
            ref={inputRef}
            rows={1}
            value={question}
            onChange={(e) => {
              setQuestion(e.target.value);
              const el = e.target;
              el.style.height = 'auto';
              el.style.height = `${Math.min(el.scrollHeight, 120)}px`;
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                void handleAsk();
              }
            }}
            placeholder="Ask the multi-agent assistant…"
            className="flex-1 resize-none outline-none text-sm text-gray-900 placeholder-gray-400 bg-transparent max-h-28"
            disabled={inputBusy}
          />
          <button
            type="button"
            onClick={() => void handleAsk()}
            disabled={!question.trim() || inputBusy}
            className={cn(
              'h-8 w-8 rounded-lg flex items-center justify-center shrink-0 transition-colors',
              question.trim() && !inputBusy
                ? 'bg-brand-600 text-white hover:bg-brand-700'
                : 'bg-gray-100 text-gray-400 cursor-not-allowed',
            )}
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </div>

      <div className="shrink-0 border-t border-gray-200">
        <button
          type="button"
          onClick={() => setShowDocs((s) => !s)}
          className="w-full flex items-center justify-between px-5 py-3 text-sm text-gray-600 hover:bg-gray-50 transition-colors"
        >
          <span className="flex items-center gap-2 font-medium">
            <FileText className="h-4 w-4" />
            FAQ Documents ({docs.length})
          </span>
          {showDocs ? <ChevronDown className="h-4 w-4" /> : <ChevronUp className="h-4 w-4" />}
        </button>

        {showDocs && (
          <div className="px-5 pb-4 space-y-2 max-h-64 overflow-y-auto">
            <UploadAndIngest workspaceId={workspaceId} onIngested={refetchDocs} />
            {docs.length === 0 && (
              <p className="text-xs text-gray-400 py-2">No documents yet. Upload a PDF, DOCX, TXT, or MD file to get started.</p>
            )}
            {docs.map((doc) => (
              <div key={doc.id} className="flex items-center gap-2 py-1.5 text-sm text-gray-700 group">
                <FileText className="h-3.5 w-3.5 text-gray-400 shrink-0" />
                <span className="flex-1 truncate">{doc.title}</span>
                <span className="text-xs text-gray-400 shrink-0">{doc.chunkCount} chunks</span>
                <button
                  type="button"
                  onClick={async () => {
                    try {
                      await deleteDocument(workspaceId, doc.id);
                      refetchDocs();
                    } catch { /* backend handles auth */ }
                  }}
                  className="opacity-0 group-hover:opacity-100 transition-opacity p-0.5 text-gray-400 hover:text-red-500 shrink-0"
                  title="Delete document"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function UploadAndIngest({
  workspaceId,
  onIngested,
}: {
  workspaceId: string;
  onIngested: () => void;
}) {
  const [status, setStatus] = useState<'idle' | 'uploading' | 'ingesting' | 'done' | 'error'>('idle');
  const [msg, setMsg]       = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const handle = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = '';
    setStatus('uploading');
    setMsg('');
    try {
      const att = await uploadAttachment(file);
      setStatus('ingesting');
      await ingestDocument(workspaceId, att.id);
      setStatus('done');
      setMsg(`"${file.name}" ingested successfully.`);
      onIngested();
    } catch (err: unknown) {
      setStatus('error');
      setMsg(
        (err as { response?: { data?: { error?: string } } })?.response?.data?.error
          ?? 'Ingestion failed.',
      );
    }
  };

  const label =
    status === 'uploading' ? 'Uploading…' :
    status === 'ingesting' ? 'Embedding…' :
    'Upload FAQ document';

  return (
    <div>
      <input
        ref={inputRef}
        type="file"
        accept=".pdf,.docx,.txt,.md,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain,text/markdown"
        className="hidden"
        onChange={handle}
      />
      <button
        type="button"
        onClick={() => { setStatus('idle'); setMsg(''); inputRef.current?.click(); }}
        disabled={status === 'uploading' || status === 'ingesting'}
        className={cn(
          'flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-lg border transition-colors',
          status === 'uploading' || status === 'ingesting'
            ? 'border-gray-200 text-gray-400 cursor-not-allowed'
            : 'border-brand-300 text-brand-600 hover:bg-brand-50',
        )}
      >
        <Upload className="h-3 w-3" />
        {label}
      </button>
      {msg && (
        <p className={cn('mt-1 text-xs', status === 'error' ? 'text-red-500' : 'text-green-600')}>
          {msg}
        </p>
      )}
    </div>
  );
}
