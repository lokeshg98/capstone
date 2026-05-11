import { api } from '@/lib/api';

export interface UserProfile {
  id:          string;
  email:       string;
  displayName: string | null;
  avatarUrl:   string | null;
}

export async function fetchMe(): Promise<UserProfile> {
  const res = await api.get<{ ok: boolean; data: UserProfile }>('/auth/me');
  return res.data.data;
}

export async function logout(): Promise<void> {
  await api.post('/auth/logout');
}
