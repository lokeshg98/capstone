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
  kind:      'PDF' | 'DOCX';
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
  return res.data.data;
}

export async function addReaction(messageId: string, emoji: string): Promise<MessageResponse> {
  const res = await api.post<{ ok: boolean; data: MessageResponse }>(
    `/channels/x/messages/${messageId}/reactions`,   // channelId is in path but unused server-side
    { emoji },
  );
  return res.data.data;
}
