import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShieldAlert, CheckCircle, XCircle, Ban, UserX, RefreshCw } from 'lucide-react';
import {
  fetchFlags, approveFlag, removeFlag,
  fetchBans, banUser, liftBan,
  type ModerationFlagResponse, type FlagStatus,
} from './moderationApi';
import { cn } from '@/lib/utils';

interface Props {
  workspaceId: string;
}

type Tab = 'flags' | 'bans';

export default function ModerationDashboard({ workspaceId }: Props) {
  const [tab,          setTab]          = useState<Tab>('flags');
  const [statusFilter, setStatusFilter] = useState<FlagStatus | 'ALL'>('PENDING');

  return (
    <div className="flex-1 flex flex-col bg-white overflow-hidden">
      {/* Header */}
      <div className="shrink-0 h-14 px-5 flex items-center gap-3 border-b border-gray-200">
        <ShieldAlert className="h-5 w-5 text-amber-500" />
        <p className="font-semibold text-gray-900">Moderation Dashboard</p>
      </div>

      {/* Tabs */}
      <div className="shrink-0 flex border-b border-gray-200 px-5">
        {(['flags', 'bans'] as Tab[]).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={cn(
              'px-4 py-2.5 text-sm font-medium capitalize border-b-2 -mb-px transition-colors',
              tab === t
                ? 'border-brand-600 text-brand-700'
                : 'border-transparent text-gray-500 hover:text-gray-700',
            )}
          >
            {t === 'flags' ? 'Flagged Messages' : 'Bans'}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto">
        {tab === 'flags' && (
          <FlagsPanel
            workspaceId={workspaceId}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
          />
        )}
        {tab === 'bans' && <BansPanel workspaceId={workspaceId} />}
      </div>
    </div>
  );
}

// ── Flagged Messages Panel ────────────────────────────────────────────────────

function FlagsPanel({
  workspaceId,
  statusFilter,
  onStatusFilterChange,
}: {
  workspaceId:          string;
  statusFilter:         FlagStatus | 'ALL';
  onStatusFilterChange: (s: FlagStatus | 'ALL') => void;
}) {
  const queryClient = useQueryClient();
  const { data: flags = [], isLoading, refetch } = useQuery({
    queryKey: ['moderation-flags', workspaceId, statusFilter],
    queryFn:  () => fetchFlags(workspaceId, statusFilter === 'ALL' ? undefined : statusFilter),
  });

  const approveMutation = useMutation({
    mutationFn: (flagId: string) => approveFlag(workspaceId, flagId),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['moderation-flags', workspaceId] }),
  });
  const removeMutation = useMutation({
    mutationFn: (flagId: string) => removeFlag(workspaceId, flagId),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['moderation-flags', workspaceId] }),
  });

  return (
    <div className="p-5 space-y-4">
      {/* Filter bar */}
      <div className="flex items-center gap-2 flex-wrap">
        {(['PENDING', 'APPROVED', 'REMOVED', 'ALL'] as const).map((s) => (
          <button
            key={s}
            onClick={() => onStatusFilterChange(s)}
            className={cn(
              'px-3 py-1 rounded-full text-xs font-medium border transition-colors',
              statusFilter === s
                ? 'bg-brand-600 text-white border-brand-600'
                : 'border-gray-300 text-gray-600 hover:border-gray-400',
            )}
          >
            {s}
          </button>
        ))}
        <button onClick={() => refetch()} className="ml-auto text-gray-400 hover:text-gray-600">
          <RefreshCw className="h-4 w-4" />
        </button>
      </div>

      {isLoading && <p className="text-sm text-gray-400">Loading…</p>}
      {!isLoading && flags.length === 0 && (
        <p className="text-sm text-gray-400 py-8 text-center">No flagged messages.</p>
      )}

      {flags.map((flag) => (
        <FlagCard
          key={flag.flagId}
          flag={flag}
          onApprove={() => approveMutation.mutate(flag.flagId)}
          onRemove={() =>  removeMutation.mutate(flag.flagId)}
          loading={approveMutation.isPending || removeMutation.isPending}
        />
      ))}
    </div>
  );
}

function FlagCard({
  flag, onApprove, onRemove, loading,
}: {
  flag:      ModerationFlagResponse;
  onApprove: () => void;
  onRemove:  () => void;
  loading:   boolean;
}) {
  const confidencePct = Math.round(flag.llmConfidence * 100);
  const reasonColor: Record<string, string> = {
    TOXIC:       'bg-red-100 text-red-700',
    HATE_SPEECH: 'bg-red-100 text-red-700',
    HARASSMENT:  'bg-orange-100 text-orange-700',
    SPAM:        'bg-yellow-100 text-yellow-700',
    THREAT:      'bg-red-100 text-red-700',
  };
  const badgeClass = reasonColor[flag.llmReason]
    ?? (flag.llmReason.startsWith('OPENAI_') ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-600');

  return (
    <div className="rounded-xl border border-gray-200 p-4 space-y-3">
      {/* Author + meta */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="h-7 w-7 rounded-full bg-brand-600 flex items-center justify-center text-white text-xs font-bold shrink-0">
            {(flag.messageAuthor.displayName ?? '?').slice(0, 2).toUpperCase()}
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-800">
              {flag.messageAuthor.displayName ?? 'Unknown'}
            </p>
            <p className="text-xs text-gray-400">
              {new Date(flag.flaggedAt).toLocaleString()}
            </p>
          </div>
        </div>
        <span className={cn(
          'shrink-0 text-xs px-2 py-0.5 rounded-full font-medium',
          badgeClass,
        )}>
          {flag.llmReason} {confidencePct}%
        </span>
      </div>

      {/* Message body */}
      <p className="text-sm text-gray-800 bg-gray-50 rounded-lg px-3 py-2 italic">
        "{flag.messageBody}"
      </p>

      {/* LLM explanation */}
      {flag.llmExplanation && (
        <p className="text-xs text-gray-500">
          <span className="font-medium">AI note:</span> {flag.llmExplanation}
        </p>
      )}

      {/* Actions — only shown for PENDING flags */}
      {flag.status === 'PENDING' && (
        <div className="flex items-center gap-2 pt-1">
          <button
            onClick={onApprove}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg bg-green-50 text-green-700 border border-green-200 hover:bg-green-100 disabled:opacity-50 transition-colors"
          >
            <CheckCircle className="h-3.5 w-3.5" />
            Approve (false positive)
          </button>
          <button
            onClick={onRemove}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 disabled:opacity-50 transition-colors"
          >
            <XCircle className="h-3.5 w-3.5" />
            Remove message
          </button>
        </div>
      )}

      {flag.status !== 'PENDING' && (
        <p className={cn(
          'text-xs font-medium',
          flag.status === 'APPROVED' ? 'text-green-600' : 'text-red-600',
        )}>
          {flag.status === 'APPROVED' ? '✓ Approved — message restored' : '✕ Removed — message hidden'}
        </p>
      )}
    </div>
  );
}

// ── Bans Panel ────────────────────────────────────────────────────────────────

function BansPanel({ workspaceId }: { workspaceId: string }) {
  const queryClient = useQueryClient();
  const { data: bans = [], isLoading } = useQuery({
    queryKey: ['moderation-bans', workspaceId],
    queryFn:  () => fetchBans(workspaceId),
  });

  const [showForm,   setShowForm]   = useState(false);
  const [targetId,   setTargetId]   = useState('');
  const [reason,     setReason]     = useState('');
  const [expiresAt,  setExpiresAt]  = useState('');

  const banMutation = useMutation({
    mutationFn: () => banUser(workspaceId, targetId.trim(), reason.trim(), expiresAt || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['moderation-bans', workspaceId] });
      setShowForm(false); setTargetId(''); setReason(''); setExpiresAt('');
    },
  });

  const liftMutation = useMutation({
    mutationFn: (userId: string) => liftBan(workspaceId, userId),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['moderation-bans', workspaceId] }),
  });

  return (
    <div className="p-5 space-y-4">
      <button
        onClick={() => setShowForm((s) => !s)}
        className="flex items-center gap-2 px-4 py-2 rounded-lg border border-amber-300 text-amber-700 text-sm font-medium hover:bg-amber-50 transition-colors"
      >
        <Ban className="h-4 w-4" />
        Ban a user
      </button>

      {showForm && (
        <div className="rounded-xl border border-gray-200 p-4 space-y-3">
          <input
            value={targetId}
            onChange={(e) => setTargetId(e.target.value)}
            placeholder="User ID (UUID)"
            className="w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-brand-500"
          />
          <input
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Reason for ban"
            className="w-full rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-brand-500"
          />
          <div>
            <label className="block text-xs text-gray-500 mb-1">Expires (leave blank for permanent)</label>
            <input
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:ring-2 focus:ring-brand-500"
            />
          </div>
          <button
            onClick={() => banMutation.mutate()}
            disabled={!targetId.trim() || !reason.trim() || banMutation.isPending}
            className="px-4 py-1.5 rounded-lg bg-amber-500 text-white text-sm font-medium hover:bg-amber-600 disabled:opacity-50 transition-colors"
          >
            {banMutation.isPending ? 'Banning…' : 'Confirm ban'}
          </button>
        </div>
      )}

      {isLoading && <p className="text-sm text-gray-400">Loading…</p>}
      {!isLoading && bans.length === 0 && (
        <p className="text-sm text-gray-400 py-8 text-center">No active bans.</p>
      )}

      {bans.map((ban) => (
        <div key={ban.id} className="flex items-center gap-3 rounded-xl border border-gray-200 px-4 py-3">
          <UserX className="h-5 w-5 text-red-400 shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-gray-800 truncate">
              {ban.userDisplayName ?? ban.userId}
            </p>
            <p className="text-xs text-gray-500 truncate">{ban.reason}</p>
            <p className="text-xs text-gray-400">
              {ban.expiresAt
                ? `Expires ${new Date(ban.expiresAt).toLocaleDateString()}`
                : 'Permanent'}
            </p>
          </div>
          <button
            onClick={() => liftMutation.mutate(ban.userId)}
            disabled={liftMutation.isPending}
            className="shrink-0 text-xs px-3 py-1 rounded-lg border border-gray-300 text-gray-600 hover:border-red-300 hover:text-red-600 transition-colors"
          >
            Lift ban
          </button>
        </div>
      ))}
    </div>
  );
}
