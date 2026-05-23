import { useState } from 'react';
import { ChevronRight, ChevronDown, Wrench, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import type { AgentCitationPayload, ProposalPayload } from './agentApi';
import { cn } from '@/lib/utils';

export interface ToolCallEntry {
  id:     string;
  name:   string;
  args:   string;
  result: string | undefined;
}

interface TimelineProps {
  thinking:          string | null;
  toolCalls:         ToolCallEntry[];
  citations:         AgentCitationPayload[];
  proposedAction:    ProposalPayload | null;
  proposalStatus:    'pending' | 'confirmed' | 'declined' | null;
  proposalBusy:      boolean;
  onConfirmAction: () => void;
  onDeclineAction: () => void;
}

export function AgentTimeline({
  thinking,
  toolCalls,
  citations,
  proposedAction,
  proposalStatus,
  proposalBusy,
  onConfirmAction,
  onDeclineAction,
}: TimelineProps) {
  const showThinking = thinking && toolCalls.length === 0;
  const hasExtras
    = !!thinking
    || toolCalls.length > 0
    || citations.length > 0
    || !!proposedAction;

  if (!hasExtras) return null;

  return (
    <div className="mt-3 space-y-2 border-t border-gray-200/80 pt-3">
      {showThinking && (
        <p className="text-xs text-gray-500 flex items-center gap-1.5">
          <Loader2 className="h-3 w-3 animate-spin shrink-0" />
          {thinking}
        </p>
      )}

      {toolCalls.length > 0 && (
        <ul className="space-y-1">
          {toolCalls.map((t) => (
            <ToolCallRow key={t.id} entry={t} />
          ))}
        </ul>
      )}

      {citations.length > 0 && (
        <CitationsPanel citations={citations} />
      )}

      {proposedAction && (
        <ConfirmActionCard
          proposal={proposedAction}
          status={proposalStatus}
          busy={proposalBusy}
          onConfirm={onConfirmAction}
          onDecline={onDeclineAction}
        />
      )}
    </div>
  );
}

function ToolCallRow({ entry }: { entry: ToolCallEntry }) {
  const [open, setOpen] = useState(false);
  const hasDetail = Boolean(entry.args || entry.result);

  return (
    <li className="rounded-lg border border-gray-200 bg-white/80 text-xs overflow-hidden">
      <button
        type="button"
        onClick={() => hasDetail && setOpen(!open)}
        className={cn(
          'w-full flex items-center gap-2 px-2 py-1.5 text-left transition-colors',
          hasDetail ? 'hover:bg-gray-50 cursor-pointer' : 'cursor-default',
        )}
      >
        {hasDetail
          ? (open ? <ChevronDown className="h-3.5 w-3.5 text-gray-400 shrink-0" /> : <ChevronRight className="h-3.5 w-3.5 text-gray-400 shrink-0" />)
          : <span className="w-3.5 shrink-0" />}
        <Wrench className="h-3.5 w-3.5 text-brand-500 shrink-0" />
        <span className="font-medium text-gray-800 truncate">{entry.name}</span>
        {!entry.result && (
          <Loader2 className="h-3 w-3 text-gray-400 animate-spin ml-auto shrink-0" />
        )}
      </button>
      {open && hasDetail && (
        <div className="px-2 pb-2 pt-0 space-y-2 border-t border-gray-100 bg-gray-50/60">
          {entry.args && (
            <div>
              <p className="text-[10px] uppercase tracking-wide text-gray-400 mb-0.5">Input</p>
              <pre className="text-[11px] text-gray-700 whitespace-pre-wrap break-words max-h-32 overflow-y-auto">
                {entry.args}
              </pre>
            </div>
          )}
          {entry.result && (
            <div>
              <p className="text-[10px] uppercase tracking-wide text-gray-400 mb-0.5">Result</p>
              <pre className="text-[11px] text-gray-700 whitespace-pre-wrap break-words max-h-40 overflow-y-auto">
                {entry.result}
              </pre>
            </div>
          )}
        </div>
      )}
    </li>
  );
}

function CitationsPanel({ citations }: { citations: AgentCitationPayload[] }) {
  return (
    <div className="rounded-lg border border-gray-200 bg-gray-50/50 p-2">
      <p className="text-[10px] font-semibold uppercase tracking-wide text-gray-500 mb-2">
        Sources ({citations.length})
      </p>
      <ul className="space-y-2 max-h-48 overflow-y-auto">
        {citations.map((c, i) => (
          <li key={`${c.documentTitle}-${c.chunkIndex}-${i}`} className="text-xs border-l-2 border-brand-400 pl-2">
            <p className="font-medium text-gray-800 truncate">{c.documentTitle}</p>
            <p className="text-gray-600 mt-0.5 line-clamp-4 whitespace-pre-wrap">{c.chunkText}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}

function ConfirmActionCard({
  proposal,
  status,
  busy,
  onConfirm,
  onDecline,
}: {
  proposal:   ProposalPayload;
  status:     'pending' | 'confirmed' | 'declined' | null;
  busy:       boolean;
  onConfirm:  () => void;
  onDecline:  () => void;
}) {
  const s = status ?? 'pending';
  return (
    <div className="rounded-lg border border-amber-200 bg-amber-50/80 p-3 text-xs">
      <p className="font-semibold text-amber-900">Scheduled post proposal</p>
      <p className="text-amber-800/90 mt-1 whitespace-pre-wrap">{proposal.summary}</p>
      <p className="text-[10px] text-amber-700/70 mt-1">Type: {proposal.actionType}</p>

      {s === 'pending' && (
        <div className="flex gap-2 mt-3">
          <button
            type="button"
            disabled={busy}
            onClick={onConfirm}
            className={cn(
              'flex-1 py-1.5 rounded-md text-sm font-medium transition-colors',
              busy ? 'bg-gray-200 text-gray-400' : 'bg-brand-600 text-white hover:bg-brand-700',
            )}
          >
            Confirm
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={onDecline}
            className={cn(
              'flex-1 py-1.5 rounded-md text-sm font-medium border transition-colors',
              busy ? 'border-gray-200 text-gray-400' : 'border-gray-300 text-gray-700 hover:bg-gray-100',
            )}
          >
            Decline
          </button>
        </div>
      )}

      {s === 'confirmed' && (
        <p className="mt-2 flex items-center gap-1 text-green-700 font-medium">
          <CheckCircle2 className="h-4 w-4" /> Scheduled post created.
        </p>
      )}
      {s === 'declined' && (
        <p className="mt-2 flex items-center gap-1 text-gray-600 font-medium">
          <XCircle className="h-4 w-4" /> Proposal declined.
        </p>
      )}
    </div>
  );
}
