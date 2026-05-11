import { api } from '@/lib/api';

export interface ChannelResponse {
  id:          string;
  workspaceId: string;
  name:        string;
  slug:        string;
  type:        'PUBLIC' | 'PRIVATE' | 'DM' | 'GROUP_DM';
  description: string | null;
  isMember:    boolean;
  createdAt:   string;
}

export async function fetchChannels(wsId: string): Promise<ChannelResponse[]> {
  const res = await api.get<{ ok: boolean; data: ChannelResponse[] }>(
    `/workspaces/${wsId}/channels`,
  );
  return res.data.data;
}

export async function createChannel(
  wsId: string,
  data: { name: string; description?: string },
): Promise<ChannelResponse> {
  const res = await api.post<{ ok: boolean; data: ChannelResponse }>(
    `/workspaces/${wsId}/channels`,
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
