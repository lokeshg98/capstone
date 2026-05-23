import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchMe, loginLocal, registerLocal } from './authApi';
import { useAuthStore } from './useAuthStore';
import { cn } from '@/lib/utils';

const BACKEND = import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080';

type Mode = 'login' | 'register';

const DEMO_ACCOUNTS = [
  { email: 'admin@communitybot.local', password: 'Admin123!', label: 'Local Admin', role: 'Admin' },
  { email: 'mod@communitybot.local', password: 'Mod123!', label: 'Local Moderator', role: 'Moderator' },
  { email: 'user@communitybot.local', password: 'User123!', label: 'Local User', role: 'Member' },
  { email: 'user2@communitybot.local', password: 'User123!', label: 'Demo User Two', role: 'Member' },
  { email: 'user3@communitybot.local', password: 'User123!', label: 'Demo User Three', role: 'Member' },
] as const;

export default function LoginPage() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated());
  const setAccessToken   = useAuthStore((s) => s.setAccessToken);
  const setUser          = useAuthStore((s) => s.setUser);
  const navigate         = useNavigate();

  const [mode, setMode] = useState<Mode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isAuthenticated) navigate('/dashboard', { replace: true });
  }, [isAuthenticated, navigate]);

  const handleLocalAuth = async (nextMode: Mode) => {
    if (!email.trim() || !password.trim()) {
      setError('Email and password are required.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const accessToken = nextMode === 'login'
        ? await loginLocal({ email, password })
        : await registerLocal({ email, password, displayName: displayName || undefined });

      setAccessToken(accessToken);
      const me = await fetchMe();
      setUser(me);
      navigate('/dashboard', { replace: true });
    } catch (err: unknown) {
      const response = (err as {
        response?: {
          data?: {
            error?: string;
            details?: Record<string, string> | string;
          };
        };
      })?.response?.data;

      const validation = response?.details && typeof response.details === 'object'
        ? Object.values(response.details).join(' ')
        : null;

      setError(response?.error ?? validation ?? 'Unable to sign in right now.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md space-y-6 px-6 py-10">
        <p className="text-center">
          <a href="/" className="text-sm text-brand-600 hover:underline">← Back to home</a>
        </p>
        <div className="text-center">
          <div className="mx-auto h-12 w-12 rounded-xl bg-brand-600 flex items-center justify-center">
            <span className="text-white text-xl font-bold">CB</span>
          </div>
          <h1 className="mt-6 text-3xl font-bold tracking-tight text-gray-900">Community Bot</h1>
          <p className="mt-2 text-sm text-gray-500">Sign in with email or with Google/GitHub</p>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm space-y-4">
          <div className="flex rounded-lg bg-gray-100 p-1 text-sm font-medium">
            <button
              onClick={() => setMode('login')}
              className={cn(
                'flex-1 rounded-md px-3 py-2 transition-colors',
                mode === 'login' ? 'bg-white text-gray-900 shadow' : 'text-gray-500',
              )}
            >
              Sign in
            </button>
            <button
              onClick={() => setMode('register')}
              className={cn(
                'flex-1 rounded-md px-3 py-2 transition-colors',
                mode === 'register' ? 'bg-white text-gray-900 shadow' : 'text-gray-500',
              )}
            >
              Create account
            </button>
          </div>

          <div className="space-y-3">
            <label className="block text-sm font-medium text-gray-700">
              Email
              <input
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                type="email"
                autoComplete="email"
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500"
                placeholder="you@example.com"
              />
            </label>

            {mode === 'register' && (
              <label className="block text-sm font-medium text-gray-700">
                Display name
                <input
                  value={displayName}
                  onChange={(e) => setDisplayName(e.target.value)}
                  type="text"
                  autoComplete="nickname"
                  className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500"
                  placeholder="Your name"
                />
              </label>
            )}

            <label className="block text-sm font-medium text-gray-700">
              Password
              <input
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                type="password"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500"
                placeholder="••••••••"
              />
            </label>

            {error && <p className="text-sm text-red-600">{error}</p>}

            <button
              onClick={() => handleLocalAuth(mode)}
              disabled={loading}
              className="w-full rounded-lg bg-brand-600 px-4 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-700 disabled:opacity-60"
            >
              {loading ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
            </button>
          </div>

          <div className="border-t border-gray-200 pt-4 space-y-3">
            <OAuthButton
              href={`${BACKEND}/oauth2/authorization/google`}
              label="Continue with Google"
              icon={<GoogleIcon />}
            />
            <OAuthButton
              href={`${BACKEND}/oauth2/authorization/github`}
              label="Continue with GitHub"
              icon={<GitHubIcon />}
              className="bg-gray-900 text-white hover:bg-gray-800 border-gray-900"
            />
          </div>

          {import.meta.env.DEV && (
            <div className="rounded-lg bg-gray-50 p-3 text-xs text-gray-600 space-y-2">
              <p className="font-semibold text-gray-700">Demo local accounts</p>
              <ul className="space-y-2">
                {DEMO_ACCOUNTS.map((account) => (
                  <li key={account.email} className="rounded-md border border-gray-200 bg-white px-2.5 py-2">
                    <p className="font-medium text-gray-800">{account.label}</p>
                    <p className="mt-0.5 font-mono text-[11px] text-gray-600">{account.email}</p>
                    <p className="font-mono text-[11px] text-gray-600">Password: {account.password}</p>
                    <p className="text-[10px] text-gray-400 mt-0.5">{account.role}</p>
                    <button
                      type="button"
                      onClick={() => {
                        setEmail(account.email);
                        setPassword(account.password);
                        setError('');
                        setMode('login');
                      }}
                      className="mt-1.5 text-[11px] font-medium text-brand-600 hover:text-brand-700 hover:underline"
                    >
                      Use this account
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>

        <p className="text-center text-xs text-gray-400">By signing in you agree to the community guidelines.</p>
      </div>
    </div>
  );
}

function OAuthButton({
  href,
  label,
  icon,
  className,
}: {
  href: string;
  label: string;
  icon: React.ReactNode;
  className?: string;
}) {
  return (
    <a
      href={href}
      className={cn(
        'flex w-full items-center justify-center gap-3 rounded-lg border px-4 py-3 text-sm font-medium transition-colors',
        'bg-white text-gray-700 border-gray-300 hover:bg-gray-50',
        className,
      )}
    >
      {icon}
      {label}
    </a>
  );
}

function GoogleIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
      <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
    </svg>
  );
}

function GitHubIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5 fill-current" aria-hidden="true">
      <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
    </svg>
  );
}
