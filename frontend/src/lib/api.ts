import axios from 'axios';
import { useAuthStore } from '@/features/auth/useAuthStore';

/**
 * Pre-configured Axios instance.
 *
 * - Request interceptor:  injects the Bearer access token from the Zustand store.
 * - Response interceptor: on 401, attempts a silent token refresh via the
 *   HttpOnly refresh-token cookie, then retries the original request once.
 */
export const api = axios.create({
  baseURL: '/api',          // Vite proxies /api → localhost:8080
  withCredentials: true,    // required for the HttpOnly refresh-token cookie
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let isRefreshing = false;
let refreshQueue: Array<(token: string) => void> = [];

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;

    if (error.response?.status !== 401 || original._retry) {
      return Promise.reject(error);
    }

    if (isRefreshing) {
      // Queue the request until the in-flight refresh completes
      return new Promise((resolve) => {
        refreshQueue.push((token) => {
          original.headers.Authorization = `Bearer ${token}`;
          resolve(api(original));
        });
      });
    }

    original._retry = true;
    isRefreshing    = true;

    try {
      const { data } = await axios.post<{ ok: boolean; data: { accessToken: string } }>(
        '/api/auth/refresh',
        {},
        { withCredentials: true },
      );
      const newToken = data.data.accessToken;
      useAuthStore.getState().setAccessToken(newToken);
      refreshQueue.forEach((cb) => cb(newToken));
      refreshQueue = [];
      original.headers.Authorization = `Bearer ${newToken}`;
      return api(original);
    } catch {
      useAuthStore.getState().clear();
      window.location.href = '/login';
      return Promise.reject(error);
    } finally {
      isRefreshing = false;
    }
  },
);
