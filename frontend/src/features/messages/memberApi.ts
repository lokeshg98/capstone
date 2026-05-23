import { api } from '@/lib/api';

export interface ChannelMemberResponse {
  userId:      string;
  memberId:    string;
  displayName: string | null;
  avatarUrl:   string | null;
  online:      boolean;
  roles:       string[];
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
