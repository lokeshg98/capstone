import { api } from '@/lib/api';

export interface UserDigestPreferences {
  userId:               string;
  weeklyDigestEnabled:  boolean;
  updatedAt:            string;
}

export interface N8nIntegrationInfo {
  webhookUrl:       string;
  apiKeyHeader:     string;
  configured:       boolean;
  cronExample:      string;
  cronDescription:  string;
}

export async function fetchDigestPreferences(): Promise<UserDigestPreferences> {
  const res = await api.get<{ ok: boolean; data: UserDigestPreferences }>(
    '/me/digest-preferences',
  );
  return res.data.data;
}

export async function updateDigestPreferences(
  weeklyDigestEnabled: boolean,
): Promise<UserDigestPreferences> {
  const res = await api.put<{ ok: boolean; data: UserDigestPreferences }>(
    '/me/digest-preferences',
    { weeklyDigestEnabled },
  );
  return res.data.data;
}

export async function fetchN8nIntegrationInfo(): Promise<N8nIntegrationInfo> {
  const res = await api.get<{ ok: boolean; data: N8nIntegrationInfo }>(
    '/n8n/integration',
  );
  return res.data.data;
}
