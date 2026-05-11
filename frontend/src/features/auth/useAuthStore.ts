import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface UserProfile {
  id:          string;
  email:       string;
  displayName: string | null;
  avatarUrl:   string | null;
}

interface AuthState {
  accessToken: string | null;
  user:        UserProfile | null;
  setAccessToken: (token: string) => void;
  setUser:        (user: UserProfile) => void;
  clear:          () => void;
  isAuthenticated: () => boolean;
}

/**
 * Zustand auth store persisted to sessionStorage.
 *
 * We deliberately use sessionStorage (not localStorage) for the access token
 * so it's cleared when the tab closes.  The HttpOnly refresh cookie lets the
 * user stay logged in across sessions without us touching localStorage.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      user:        null,

      setAccessToken: (token) => set({ accessToken: token }),
      setUser:        (user)  => set({ user }),
      clear:          ()      => set({ accessToken: null, user: null }),
      isAuthenticated: ()     => get().accessToken !== null,
    }),
    {
      name:    'cb-auth',
      storage: {
        getItem:    (k) => JSON.parse(sessionStorage.getItem(k) ?? 'null'),
        setItem:    (k, v) => sessionStorage.setItem(k, JSON.stringify(v)),
        removeItem: (k) => sessionStorage.removeItem(k),
      },
    },
  ),
);
