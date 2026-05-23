import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Building2, Plus, LogOut, ChevronRight, Trash2, User } from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/useAuthStore';
import { logout } from '@/features/auth/authApi';
import { fetchMyOrgs, createOrg, joinOrgBySlug, deleteOrg, type OrgResponse } from './orgApi';
import { fetchWorkspaces, createWorkspace } from '@/features/workspace/workspaceApi';
import { AiUsageSummary } from './AiUsageSummary';
import { cn } from '@/lib/utils';

export default function DashboardPage() {
  const user        = useAuthStore((s) => s.user);
  const clearAuth   = useAuthStore((s) => s.clear);
  const navigate    = useNavigate();
  const qc          = useQueryClient();

  const { data: orgs = [], isLoading } = useQuery({
    queryKey: ['orgs'],
    queryFn:  fetchMyOrgs,
  });

  const { mutate: doLogout, isPending: loggingOut } = useMutation({
    mutationFn: logout,
    onSettled:  () => { clearAuth(); navigate('/', { replace: true }); },
  });

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top bar */}
      <header className="sticky top-0 z-10 bg-white border-b border-gray-200">
        <div className="mx-auto max-w-5xl px-6 h-14 flex items-center justify-between">
          <span className="font-semibold text-gray-900">Community Bot</span>
          <div className="flex items-center gap-4">
            {user && (
              <Link
                to="/profile"
                className="flex items-center gap-1.5 text-sm text-gray-600 hover:text-brand-600 transition-colors"
              >
                <User className="h-4 w-4" />
                Profile
              </Link>
            )}
            {user && (
              <span className="text-sm text-gray-600 hidden sm:inline">
                {user.displayName ?? user.email}
              </span>
            )}
            <button
              onClick={() => doLogout()}
              disabled={loggingOut}
              className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-900 transition-colors"
            >
              <LogOut className="h-4 w-4" />
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-10 space-y-8">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">Your Organisations</h1>
          <CreateOrgButton onCreated={() => qc.invalidateQueries({ queryKey: ['orgs'] })} />
        </div>

        <AiUsageSummary />

        <JoinOrgPanel onJoined={() => qc.invalidateQueries({ queryKey: ['orgs'] })} />

        {isLoading ? (
          <OrgListSkeleton />
        ) : orgs.length === 0 ? (
          <EmptyState onCreated={() => qc.invalidateQueries({ queryKey: ['orgs'] })} />
        ) : (
          <OrgList
            orgs={orgs}
            currentUserId={user?.id ?? null}
            onChanged={() => qc.invalidateQueries({ queryKey: ['orgs'] })}
          />
        )}
      </main>
    </div>
  );
}

// ─── sub-components ───────────────────────────────────────────────────────────

function OrgList({
  orgs,
  currentUserId,
  onChanged,
}: {
  orgs: OrgResponse[];
  currentUserId: string | null;
  onChanged: () => void;
}) {
  const navigate = useNavigate();
  const [deleteTarget, setDeleteTarget] = useState<OrgResponse | null>(null);

  const openOrg = async (org: OrgResponse) => {
    try {
      let workspaces = await fetchWorkspaces(org.id);
      if (workspaces.length === 0) {
        const ws = await createWorkspace(org.id, { name: 'General' });
        workspaces = [ws];
      }
      navigate(`/workspaces/${workspaces[0].id}`);
    } catch (err) {
      console.error('Failed to open organisation:', err);
    }
  };

  return (
    <>
      <ul className="divide-y divide-gray-200 rounded-xl border border-gray-200 bg-white overflow-hidden">
        {orgs.map((org) => {
          const isOwner = currentUserId != null && org.ownerId === currentUserId;
          return (
            <li key={org.id} className="flex items-stretch">
              <button
                type="button"
                onClick={() => void openOrg(org)}
                className="flex-1 flex items-center gap-4 px-5 py-4 text-left hover:bg-gray-50 transition-colors min-w-0"
              >
                <div className="h-9 w-9 rounded-lg bg-brand-100 flex items-center justify-center shrink-0">
                  <Building2 className="h-5 w-5 text-brand-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900 truncate">{org.name}</p>
                  <p className="text-sm text-gray-400">
                    slug: <code className="text-brand-600">{org.slug}</code> · {org.myRole.toLowerCase()}
                  </p>
                </div>
                <ChevronRight className="h-4 w-4 text-gray-400 shrink-0" />
              </button>
              {isOwner && (
                <button
                  type="button"
                  title="Delete organisation"
                  onClick={() => setDeleteTarget(org)}
                  className="px-4 border-l border-gray-100 text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors shrink-0"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              )}
            </li>
          );
        })}
      </ul>

      {deleteTarget && (
        <DeleteOrgDialog
          org={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onDeleted={() => { setDeleteTarget(null); onChanged(); }}
        />
      )}
    </>
  );
}

function DeleteOrgDialog({
  org,
  onClose,
  onDeleted,
}: {
  org: OrgResponse;
  onClose: () => void;
  onDeleted: () => void;
}) {
  const [step, setStep] = useState<'warn' | 'confirm'>('warn');
  const [typedSlug, setTypedSlug] = useState('');
  const [error, setError] = useState('');

  const { mutate, isPending } = useMutation({
    mutationFn: () => deleteOrg(org.id, typedSlug.trim()),
    onSuccess:  () => onDeleted(),
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
        ?? 'Could not delete organisation. Try again.';
      setError(msg);
    },
  });

  const slugMatches = typedSlug.trim().toLowerCase() === org.slug.toLowerCase();

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40" role="dialog" aria-modal="true">
      <div className="w-full max-w-md rounded-xl bg-white shadow-xl border border-gray-200 p-6">
        {step === 'warn' ? (
          <>
            <h2 className="text-lg font-semibold text-gray-900">Delete organisation?</h2>
            <p className="mt-3 text-sm text-gray-600 leading-relaxed">
              You are about to permanently delete <strong>{org.name}</strong>.
              This cannot be undone.
            </p>
            <p className="mt-3 text-sm text-red-700 bg-red-50 border border-red-100 rounded-lg px-3 py-2 leading-relaxed">
              All workspaces in this organisation will be removed, including every{' '}
              <strong>channel</strong> and <strong>message</strong>, uploaded FAQ documents,
              moderation history, and scheduled posts.
            </p>
            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={onClose}
                className="rounded-lg px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => { setStep('confirm'); setError(''); }}
                className="rounded-lg px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700"
              >
                Continue
              </button>
            </div>
          </>
        ) : (
          <>
            <h2 className="text-lg font-semibold text-gray-900">Confirm deletion</h2>
            <p className="mt-2 text-sm text-gray-600">
              Type <code className="text-red-700 bg-red-50 px-1 rounded">{org.slug}</code> to confirm.
              All channels and messages will be deleted.
            </p>
            <input
              autoFocus
              value={typedSlug}
              onChange={(e) => { setTypedSlug(e.target.value); setError(''); }}
              placeholder={org.slug}
              className="mt-4 w-full rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-red-500"
            />
            {error && <p className="mt-2 text-xs text-red-500">{error}</p>}
            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => { setStep('warn'); setTypedSlug(''); setError(''); }}
                className="rounded-lg px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100"
              >
                Back
              </button>
              <button
                type="button"
                disabled={!slugMatches || isPending}
                onClick={() => mutate()}
                className="rounded-lg px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 disabled:opacity-50"
              >
                {isPending ? 'Deleting…' : 'Delete organisation'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function JoinOrgPanel({ onJoined }: { onJoined: () => void }) {
  const [slug, setSlug]   = useState('');
  const [error, setError] = useState('');
  const [ok, setOk]       = useState('');

  const { mutate, isPending } = useMutation({
    mutationFn: joinOrgBySlug,
    onSuccess: (org) => {
      setOk(`Joined "${org.name}". Open it from the list below.`);
      setSlug('');
      setError('');
      onJoined();
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error
        ?? 'Could not join organisation. Check the slug and try again.';
      setError(msg);
      setOk('');
    },
  });

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <p className="text-sm font-medium text-gray-900">Join an organisation</p>
      <p className="text-xs text-gray-500 mt-0.5">
        Ask the owner for their org slug (shown on their dashboard), then join here.
      </p>
      <div className="mt-3 flex flex-wrap items-center gap-2">
        <input
          value={slug}
          onChange={(e) => { setSlug(e.target.value); setError(''); setOk(''); }}
          onKeyDown={(e) => { if (e.key === 'Enter' && slug.trim()) mutate(slug.trim()); }}
          placeholder="org-slug"
          className="rounded-lg border border-gray-300 px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-brand-500"
        />
        <button
          type="button"
          disabled={!slug.trim() || isPending}
          onClick={() => mutate(slug.trim())}
          className="rounded-lg bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-800 disabled:opacity-50"
        >
          {isPending ? 'Joining…' : 'Join'}
        </button>
      </div>
      {error && <p className="mt-2 text-xs text-red-500">{error}</p>}
      {ok && <p className="mt-2 text-xs text-green-600">{ok}</p>}
    </div>
  );
}

function CreateOrgButton({ onCreated }: { onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [error, setError] = useState('');

  const { mutate, isPending } = useMutation({
    mutationFn: createOrg,
    onSuccess:  () => { setOpen(false); setName(''); onCreated(); },
    onError:    () => setError('Failed to create organisation. Try again.'),
  });

  const submit = () => {
    if (name.trim().length < 2) { setError('Name must be at least 2 characters'); return; }
    setError('');
    mutate({ name: name.trim() });
  };

  if (!open) {
    return (
      <button
        onClick={() => setOpen(true)}
        className="flex items-center gap-2 rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 transition-colors"
      >
        <Plus className="h-4 w-4" />
        New Organisation
      </button>
    );
  }

  return (
    <div className="flex items-center gap-2">
      <input
        autoFocus
        value={name}
        onChange={(e) => setName(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Enter') submit(); if (e.key === 'Escape') setOpen(false); }}
        placeholder="Organisation name"
        className={cn(
          'rounded-lg border px-3 py-2 text-sm outline-none',
          'focus:ring-2 focus:ring-brand-500 focus:border-transparent',
          error ? 'border-red-400' : 'border-gray-300',
        )}
      />
      <button
        onClick={submit}
        disabled={isPending}
        className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-medium text-white hover:bg-brand-700 disabled:opacity-60 transition-colors"
      >
        {isPending ? 'Creating…' : 'Create'}
      </button>
      <button onClick={() => setOpen(false)} className="text-sm text-gray-500 hover:text-gray-700">
        Cancel
      </button>
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}

function EmptyState({ onCreated }: { onCreated: () => void }) {
  return (
    <div className="rounded-xl border-2 border-dashed border-gray-200 p-12 text-center">
      <Building2 className="mx-auto h-10 w-10 text-gray-300" />
      <h3 className="mt-4 text-base font-semibold text-gray-900">No organisations yet</h3>
      <p className="mt-1 text-sm text-gray-500">Create one to get started.</p>
      <CreateOrgButton onCreated={onCreated} />
    </div>
  );
}

function OrgListSkeleton() {
  return (
    <ul className="divide-y divide-gray-200 rounded-xl border border-gray-200 bg-white overflow-hidden animate-pulse">
      {[1, 2, 3].map((i) => (
        <li key={i} className="flex items-center gap-4 px-5 py-4">
          <div className="h-9 w-9 rounded-lg bg-gray-200" />
          <div className="flex-1 space-y-1">
            <div className="h-4 w-40 rounded bg-gray-200" />
            <div className="h-3 w-24 rounded bg-gray-100" />
          </div>
        </li>
      ))}
    </ul>
  );
}
