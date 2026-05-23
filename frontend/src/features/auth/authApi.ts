import { api } from '@/lib/api';

export interface UserProfile {
  id:          string;
  email:       string;
  displayName: string | null;
  avatarUrl:   string | null;
}

export interface LocalLoginRequest {
  email:    string;
  password: string;
}

export interface LocalRegisterRequest extends LocalLoginRequest {
  displayName?: string;
}

export async function fetchMe(): Promise<UserProfile> {
  const res = await api.get<{ ok: boolean; data: UserProfile }>('/auth/me');
  return res.data.data;
}

export async function logout(): Promise<void> {
  await api.post('/auth/logout');
}

export async function loginLocal(data: LocalLoginRequest): Promise<string> {
  const res = await api.post<{ ok: boolean; data: { accessToken: string } }>('/auth/login', data);
  return res.data.data.accessToken;
}

export async function registerLocal(data: LocalRegisterRequest): Promise<string> {
  const res = await api.post<{ ok: boolean; data: { accessToken: string } }>('/auth/register', data);
  return res.data.data.accessToken;
}
