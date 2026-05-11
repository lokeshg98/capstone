import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from './useAuthStore';
import { fetchMe } from './authApi';

/**
 * Landing page for the OAuth redirect: /auth/callback?token=<accessToken>
 *
 * 1. Reads the access token from the URL query param.
 * 2. Stores it in the Zustand store.
 * 3. Fetches the user profile (/api/auth/me).
 * 4. Redirects to /dashboard.
 *
 * If anything fails, redirects back to /login with an error flag.
 */
export default function AuthCallback() {
  const [params]  = useSearchParams();
  const navigate  = useNavigate();
  const setToken  = useAuthStore((s) => s.setAccessToken);
  const setUser   = useAuthStore((s) => s.setUser);
  const handled   = useRef(false);   // guard against StrictMode double-invocation

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const token = params.get('token');
    if (!token) {
      navigate('/login?error=no_token', { replace: true });
      return;
    }

    setToken(token);

    fetchMe()
      .then((profile) => {
        setUser(profile);
        navigate('/dashboard', { replace: true });
      })
      .catch(() => {
        navigate('/login?error=profile_fetch_failed', { replace: true });
      });
  }, []);   // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center space-y-3">
        <div className="h-8 w-8 mx-auto animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
        <p className="text-sm text-gray-500">Signing you in…</p>
      </div>
    </div>
  );
}
