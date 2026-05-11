import { api } from '@/lib/api';

// ── Moderation flags ──────────────────────────────────────────────────────────

export type FlagStatus = 'PENDING' | 'APPROVED' | 'REMOVED';

export interface ModerationFlagResponse {
  flagId:         string;
  messageId:      string;
  messageBody:    string;
  messageAuthor:  { id: string; displayName: string | null; avatarUrl: string | null };
  llmReason:      string;
  llmExplanation: string;
  llmConfidence:  number;
  status:         FlagStatus;
  flaggedAt:      string;
  reviewedBy:     string | null;
  reviewedAt:     string | null;
}

export async function fetchFlags(
  workspaceId: string,
  status?: FlagStatus,
): Promise<ModerationFlagResponse[]> {
  const res = await api.get<{ ok: boolean; data: ModerationFlagResponse[] }>(
    `/workspaces/${workspaceId}/moderation/flags`,
    { params: status ? { status } : {} },
  );
  return res.data.data;
}

export async function approveFlag(
  workspaceId: string,
  flagId: string,
): Promise<ModerationFlagResponse> {
  const res = await api.post<{ ok: boolean; data: ModerationFlagResponse }>(
    `/workspaces/${workspaceId}/moderation/flags/${flagId}/approve`,
  );
  return res.data.data;
}

export async function removeFlag(
  workspaceId: string,
  flagId: string,
): Promise<ModerationFlagResponse> {
  const res = await api.post<{ ok: boolean; data: ModerationFlagResponse }>(
    `/workspaces/${workspaceId}/moderation/flags/${flagId}/remove`,
  );
  return res.data.data;
}

// ── Bans ──────────────────────────────────────────────────────────────────────

export interface BanResponse {
  id:              string;
  workspaceId:     string;
  userId:          string;
  userDisplayName: string | null;
  reason:          string;
  expiresAt:       string | null;
  bannedAt:        string;
}

export async function fetchBans(workspaceId: string): Promise<BanResponse[]> {
  const res = await api.get<{ ok: boolean; data: BanResponse[] }>(
    `/workspaces/${workspaceId}/moderation/bans`,
  );
  return res.data.data;
}

export async function banUser(
  workspaceId: string,
  userId: string,
  reason: string,
  expiresAt?: string,
): Promise<BanResponse> {
  const res = await api.post<{ ok: boolean; data: BanResponse }>(
    `/workspaces/${workspaceId}/moderation/bans`,
    { userId, reason, expiresAt: expiresAt ?? null },
  );
  return res.data.data;
}

export async function liftBan(workspaceId: string, targetUserId: string): Promise<void> {
  await api.delete(`/workspaces/${workspaceId}/moderation/bans/${targetUserId}`);
}
