import { api } from '@/lib/api';

export interface AuthorInfo {
  id:          string;
  displayName: string | null;
  avatarUrl:   string | null;
}

export interface ReactionSummary {
  emoji:       string;
  count:       number;
  reactedByMe: boolean;
}

export interface AttachmentInfo {
  id:        string;
  filename:  string;
  mimeType:  string;
  kind:      'PDF' | 'DOCX' | 'JPEG' | 'TXT' | 'MD';
  sizeBytes: number;
}

export interface MessageResponse {
  id:           string;
  channelId:    string;
  workspaceId:  string;
  author:       AuthorInfo;
  body:         string;
  threadRootId: string | null;
  status:       'ACTIVE' | 'EDITED' | 'FLAGGED' | 'HIDDEN' | 'DELETED';
  edited:       boolean;
  editedAt:     string | null;
  createdAt:    string;
  reactions:    ReactionSummary[];
  attachment:   AttachmentInfo | null;
  pending?:     boolean;
  replyCount:   number;
}

export interface ThreadSummaryResponse {
  summaryBody:  string;
  messageCount: number;
  createdAt:    string;
  botMessageId: string | null;
}

export interface ThreadViewResponse {
  root:    MessageResponse;
  replies: MessageResponse[];
  summary: ThreadSummaryResponse | null;
}

export interface WsOutboundEvent {
  eventType: 'MESSAGE_CREATED' | 'MESSAGE_UPDATED' | 'REACTION_UPDATED' | 'TYPING';
  data:      MessageResponse | TypingPayload;
}

export interface TypingPayload {
  userId:      string;
  displayName: string | null;
}

export interface PageResponse<T> {
  content:       T[];
  page:          number;
  size:          number;
  totalElements: number;
  totalPages:    number;
  last:          boolean;
}

export async function fetchMessages(
  channelId: string,
  page = 0,
  size = 50,
): Promise<PageResponse<MessageResponse>> {
  const res = await api.get<{ ok: boolean; data: PageResponse<MessageResponse> }>(
    `/channels/${channelId}/messages`,
    { params: { page, size } },
  );
  const data = res.data.data;
  return {
    ...data,
    content: data.content.map((m) => ({ ...m, replyCount: m.replyCount ?? 0 })),
  };
}

export async function fetchThread(
  channelId: string,
  messageId: string,
): Promise<ThreadViewResponse> {
  const res = await api.get<{ ok: boolean; data: ThreadViewResponse }>(
    `/channels/${channelId}/messages/${messageId}/thread`,
  );
  return res.data.data;
}

export async function addReaction(
  channelId: string,
  messageId: string,
  emoji: string,
): Promise<MessageResponse> {
  const res = await api.post<{ ok: boolean; data: MessageResponse }>(
    `/channels/${channelId}/messages/${messageId}/reactions`,
    { emoji },
  );
  return res.data.data;
}

export async function removeReaction(
  channelId: string,
  messageId: string,
  emoji: string,
): Promise<MessageResponse> {
  const res = await api.delete<{ ok: boolean; data: MessageResponse }>(
    `/channels/${channelId}/messages/${messageId}/reactions/${encodeURIComponent(emoji)}`,
  );
  return res.data.data;
}
