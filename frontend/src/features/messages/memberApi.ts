import { api } from '@/lib/api';

export interface ChannelMemberResponse {
  userId:      string;
  memberId:    string;
  displayName: string | null;
  avatarUrl:   string | null;
  online:      boolean;
  roles:       string[];
}

export interface MentionSuggestion {
  userId:      string;
  displayName: string | null;
  avatarUrl:   string | null;
  isBot:       boolean;
}

export async function fetchChannelMembers(
  wsId: string,
  channelId: string,
): Promise<ChannelMemberResponse[]> {
  const res = await api.get<{ ok: boolean; data: ChannelMemberResponse[] }>(
    `/workspaces/${wsId}/channels/${channelId}/members`,
  );
  return res.data.data;
}

export async function searchMentions(
  wsId: string,
  channelId: string,
  query: string,
): Promise<MentionSuggestion[]> {
  const res = await api.get<{ ok: boolean; data: MentionSuggestion[] }>(
    `/workspaces/${wsId}/channels/${channelId}/members/search`,
    { params: { q: query } },
  );
  return res.data.data;
}
