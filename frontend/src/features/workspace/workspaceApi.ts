import { api } from '@/lib/api';

export interface WorkspaceResponse {
  id:          string;
  orgId:       string;
  name:        string;
  slug:        string;
  description: string | null;
  myRole:      'ADMIN' | 'MODERATOR' | 'MEMBER';
  createdAt:   string;
}

export async function fetchWorkspaces(orgId: string): Promise<WorkspaceResponse[]> {
  const res = await api.get<{ ok: boolean; data: WorkspaceResponse[] }>(
    `/organizations/${orgId}/workspaces`,
  );
  return res.data.data;
}

export async function createWorkspace(
  orgId: string,
  payload: { name: string; description?: string },
): Promise<WorkspaceResponse> {
  const res = await api.post<{ ok: boolean; data: WorkspaceResponse }>(
    `/organizations/${orgId}/workspaces`,
    payload,
  );
  return res.data.data;
}

export async function joinWorkspace(orgId: string, wsId: string): Promise<WorkspaceResponse> {
  const res = await api.post<{ ok: boolean; data: WorkspaceResponse }>(
    `/organizations/${orgId}/workspaces/${wsId}/join`,
  );
  return res.data.data;
}

export async function fetchWelcomeTemplate(wsId: string): Promise<string | null> {
  const res = await api.get<{ ok: boolean; data: string | null }>(`/workspaces/${wsId}/welcome-message`);
  return res.data.data;
}

export async function updateWelcomeTemplate(wsId: string, template: string): Promise<void> {
  await api.put(`/workspaces/${wsId}/welcome-message`, { template });
}
