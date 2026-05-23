import { api } from '@/lib/api';

export interface ChannelResponse {
  id:              string;
  workspaceId:     string;
  name:            string;
  slug:            string;
  type:            'PUBLIC' | 'PRIVATE' | 'DM' | 'GROUP_DM';
  description:     string | null;
  isMember:        boolean;
  roleRestricted:  boolean;
  accessibleRoles: string[];
  createdAt:       string;
}

export async function fetchChannels(wsId: string): Promise<ChannelResponse[]> {
  const res = await api.get<{ ok: boolean; data: ChannelResponse[] }>(
    `/workspaces/${wsId}/channels`,
  );
  return res.data.data;
}

export async function createChannel(
  wsId: string,
  data: { name: string; description?: string; roleRestricted?: boolean; accessibleRoles?: string[] },
): Promise<ChannelResponse> {
  const res = await api.post<{ ok: boolean; data: ChannelResponse }>(
    `/workspaces/${wsId}/channels`,
    data,
  );
  return res.data.data;
}

export async function updateChannelRestrictions(
  wsId: string,
  channelId: string,
  data: { roleRestricted: boolean; accessibleRoles: string[] },
): Promise<ChannelResponse> {
  const res = await api.put<{ ok: boolean; data: ChannelResponse }>(
    `/workspaces/${wsId}/channels/${channelId}/restrictions`,
    data,
  );
  return res.data.data;
}

export async function joinChannel(wsId: string, channelId: string): Promise<ChannelResponse> {
  const res = await api.post<{ ok: boolean; data: ChannelResponse }>(
    `/workspaces/${wsId}/channels/${channelId}/join`,
  );
  return res.data.data;
}

export interface ChannelMemberResponse {
  id:          string;
  userId:      string;
  displayName: string | null;
  email:       string;
  avatarUrl:   string | null;
  joinedAt:    string;
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
