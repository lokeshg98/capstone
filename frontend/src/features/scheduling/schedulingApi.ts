import { api } from '@/lib/api';

export type ScheduleType        = 'ONE_SHOT' | 'CRON';
export type ScheduledPostStatus = 'PENDING' | 'SENT' | 'CANCELLED' | 'ERROR';

export interface ScheduledPostResponse {
  id:             string;
  workspaceId:    string;
  channelId:      string;
  channelName:    string;
  body:           string;
  scheduleType:   ScheduleType;
  cronExpression: string | null;
  nextFireAt:     string;
  status:         ScheduledPostStatus;
  lastSentAt:     string | null;
  createdAt:      string;
}

export interface CreateScheduledPostRequest {
  channelId:      string;
  body:           string;
  scheduleType:   ScheduleType;
  fireAt?:        string;   // ISO-8601, required for ONE_SHOT
  cronExpression?: string;  // required for CRON
}

export async function fetchScheduledPosts(wsId: string): Promise<ScheduledPostResponse[]> {
  const res = await api.get<{ ok: boolean; data: ScheduledPostResponse[] }>(
    `/workspaces/${wsId}/scheduled-posts`,
  );
  return res.data.data;
}

export async function createScheduledPost(
  wsId: string,
  req: CreateScheduledPostRequest,
): Promise<ScheduledPostResponse> {
  const res = await api.post<{ ok: boolean; data: ScheduledPostResponse }>(
    `/workspaces/${wsId}/scheduled-posts`,
    req,
  );
  return res.data.data;
}

export async function cancelScheduledPost(wsId: string, postId: string): Promise<void> {
  await api.delete(`/workspaces/${wsId}/scheduled-posts/${postId}`);
}
