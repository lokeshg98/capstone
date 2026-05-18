import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Building2, Plus, LogOut, ChevronRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/useAuthStore';
import { logout } from '@/features/auth/authApi';
import { fetchMyOrgs, createOrg, type OrgResponse } from './orgApi';
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
    onSettled:  () => { clearAuth(); navigate('/login', { replace: true }); },
  });

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top bar */}
      <header className="sticky top-0 z-10 bg-white border-b border-gray-200">
        <div className="mx-auto max-w-5xl px-6 h-14 flex items-center justify-between">
          <span className="font-semibold text-gray-900">Community Bot</span>
          <div className="flex items-center gap-4">
            {user && (
              <span className="text-sm text-gray-600">
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

        {isLoading ? (
          <OrgListSkeleton />
        ) : orgs.length === 0 ? (
          <EmptyState onCreated={() => qc.invalidateQueries({ queryKey: ['orgs'] })} />
        ) : (
          <OrgList orgs={orgs} />
        )}
      </main>
    </div>
  );
}

// ─── sub-components ───────────────────────────────────────────────────────────

function OrgList({ orgs }: { orgs: OrgResponse[] }) {
  const navigate = useNavigate();

  const openOrg = async (org: OrgResponse) => {
    try {
      let workspaces = await fetchWorkspaces(org.id);
      if (workspaces.length === 0) {
        const ws = await createWorkspace(org.id, { name: 'General' });
        workspaces = [ws];
      }
      navigate(`/workspaces/${workspaces[0].id}`);
    } catch {
      // silently ignore — backend may be down
    }
  };

  return (
    <ul className="divide-y divide-gray-200 rounded-xl border border-gray-200 bg-white overflow-hidden">
      {orgs.map((org) => (
        <li key={org.id}>
          <button
            onClick={() => openOrg(org)}
            className="w-full flex items-center gap-4 px-5 py-4 text-left hover:bg-gray-50 transition-colors"
          >
            <div className="h-9 w-9 rounded-lg bg-brand-100 flex items-center justify-center shrink-0">
              <Building2 className="h-5 w-5 text-brand-600" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="font-medium text-gray-900 truncate">{org.name}</p>
              <p className="text-sm text-gray-400">/{org.slug} · {org.myRole.toLowerCase()}</p>
            </div>
            <ChevronRight className="h-4 w-4 text-gray-400 shrink-0" />
          </button>
        </li>
      ))}
    </ul>
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
