import { api } from '@/lib/api';

export interface UserProfileResponse {
  id:            string;
  email:         string;
  displayName:   string | null;
  avatarUrl:     string | null;
  statusMessage: string | null;
  aboutMe:       string | null;
  interests:     string | null;
  contactInfo:   string | null;
  organizations: OrgMembership[];
  workspaces:    WorkspaceMembership[];
}

export interface OrgMembership {
  orgId:   string;
  orgName: string;
  role:    string;
}

export interface WorkspaceMembership {
  workspaceId:   string;
  workspaceName: string;
  orgId:         string;
  roles:         string[];
}

export async function fetchMyProfile(): Promise<UserProfileResponse> {
  const res = await api.get<{ ok: boolean; data: UserProfileResponse }>('/auth/me');
  return res.data.data;
}

export async function fetchUserProfile(userId: string): Promise<UserProfileResponse> {
  const res = await api.get<{ ok: boolean; data: UserProfileResponse }>(`/auth/profile/${userId}`);
  return res.data.data;
}

export async function updateMyProfile(payload: {
  statusMessage?: string | null;
  aboutMe?: string | null;
  interests?: string | null;
  contactInfo?: string | null;
}): Promise<UserProfileResponse> {
  const res = await api.put<{ ok: boolean; data: UserProfileResponse }>('/auth/profile', payload);
  return res.data.data;
}
