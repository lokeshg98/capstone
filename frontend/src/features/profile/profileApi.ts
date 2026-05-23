import { api } from '@/lib/api';

export type NotificationMode = 'ALL' | 'MENTIONS' | 'FOLLOWED_THREADS';

export interface UserProfileDetail {
  id:                 string;
  email:              string;
  displayName:        string | null;
  avatarUrl:          string | null;
  statusMessage:      string | null;
  aboutMe:            string | null;
  phone:              string | null;
  showEmail:          boolean;
  showPhone:          boolean;
  interests:          string[];
  notificationMode:   NotificationMode;
  profileUpdatedAt:   string | null;
}

export interface UpdateUserProfileRequest {
  statusMessage?:     string;
  aboutMe?:           string;
  phone?:             string;
  showEmail?:         boolean;
  showPhone?:         boolean;
  interests?:         string[];
  notificationMode?:  NotificationMode;
}

export async function fetchMyProfile(): Promise<UserProfileDetail> {
  const res = await api.get<{ ok: boolean; data: UserProfileDetail }>('/me/profile');
  return res.data.data;
}

export async function updateMyProfile(req: UpdateUserProfileRequest): Promise<UserProfileDetail> {
  const res = await api.put<{ ok: boolean; data: UserProfileDetail }>('/me/profile', req);
  return res.data.data;
}
