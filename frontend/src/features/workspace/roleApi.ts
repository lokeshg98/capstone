import { api } from '@/lib/api';

export interface WorkspaceRoleResponse {
  id:        string;
  name:      string;
  isSystem:  boolean;
  createdAt: string;
}

export interface MemberRolesResponse {
  memberId:     string;
  userId:       string;
  userDisplayName: string;
  userEmail:    string;
  roles:        string[];
}

export async function fetchRoles(wsId: string): Promise<WorkspaceRoleResponse[]> {
  const res = await api.get<{ ok: boolean; data: WorkspaceRoleResponse[] }>(
    `/workspaces/${wsId}/roles`,
  );
  return res.data.data;
}

export async function createRole(wsId: string, name: string): Promise<WorkspaceRoleResponse> {
  const res = await api.post<{ ok: boolean; data: WorkspaceRoleResponse }>(
    `/workspaces/${wsId}/roles`,
    { name },
  );
  return res.data.data;
}

export async function deleteRole(wsId: string, roleId: string): Promise<void> {
  await api.delete(`/workspaces/${wsId}/roles/${roleId}`);
}

export async function fetchMembersWithRoles(wsId: string): Promise<MemberRolesResponse[]> {
  const res = await api.get<{ ok: boolean; data: MemberRolesResponse[] }>(
    `/workspaces/${wsId}/members/roles`,
  );
  return res.data.data;
}

export async function assignRoleToMember(
  wsId: string,
  memberId: string,
  roleId: string,
): Promise<void> {
  await api.post(`/workspaces/${wsId}/members/${memberId}/roles/${roleId}`);
}

export async function removeRoleFromMember(
  wsId: string,
  memberId: string,
  roleId: string,
): Promise<void> {
  await api.delete(`/workspaces/${wsId}/members/${memberId}/roles/${roleId}`);
}
