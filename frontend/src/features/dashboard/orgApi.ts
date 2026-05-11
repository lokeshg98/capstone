import { api } from '@/lib/api';

export interface OrgResponse {
  id:        string;
  name:      string;
  slug:      string;
  ownerId:   string;
  myRole:    'OWNER' | 'ADMIN' | 'MEMBER';
  createdAt: string;
}

export interface CreateOrgRequest {
  name: string;
}

export async function fetchMyOrgs(): Promise<OrgResponse[]> {
  const res = await api.get<{ ok: boolean; data: OrgResponse[] }>('/organizations');
  return res.data.data;
}

export async function createOrg(data: CreateOrgRequest): Promise<OrgResponse> {
  const res = await api.post<{ ok: boolean; data: OrgResponse }>('/organizations', data);
  return res.data.data;
}
