import { api } from '@/lib/api';

export interface UserProfileResponse {
  id:               string;
  email:            string;
  displayName:      string | null;
  avatarUrl:        string | null;
  statusMessage:    string | null;
  aboutMe:          string | null;
  phone:            string | null;
  showEmail:        boolean;
  showPhone:        boolean;
  interests:        string[];
  notificationMode: string;
  profileUpdatedAt: string | null;
}

export async function fetchMyProfile(): Promise<UserProfileResponse> {
  const res = await api.get<{ ok: boolean; data: UserProfileResponse }>('/auth/me');
  return res.data.data;
}

export async function fetchUserProfile(userId: string): Promise<UserProfileResponse> {
  const res = await api.get<{ ok: boolean; data: UserProfileResponse }>(`/auth/profile/${userId}`);
  return res.data.data;
}
