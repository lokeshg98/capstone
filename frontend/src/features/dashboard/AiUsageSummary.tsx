import { useQuery } from '@tanstack/react-query';
import { Coins, Cpu } from 'lucide-react';
import { fetchMyLlmUsage } from './usageApi';
import { cn } from '@/lib/utils';

function usd(n: number) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(Number.isFinite(n) ? n : 0);
}

function tok(n: number) {
  return new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 }).format(Number.isFinite(n) ? n : 0);
}

/**
 * Shows this user's OpenAI token totals and estimated cost, plus deployment-wide ("project") totals.
 */
export function AiUsageSummary({ variant = 'card' }: { variant?: 'card' | 'sidebar' }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['llm-usage'],
    queryFn:  fetchMyLlmUsage,
    staleTime: 30_000,
  });

  if (isLoading) {
    return variant === 'sidebar'
      ? <SidebarSkeleton />
      : <CardSkeleton />;
  }

  if (isError || !data) {
    return variant === 'sidebar'
      ? null
      : (
        <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
          AI usage stats are unavailable.
        </div>
        );
  }

  const userTotalTok = data.userInputTokens + data.userOutputTokens;
  const projTotalTok = data.projectInputTokens + data.projectOutputTokens;

  if (variant === 'sidebar') {
    return (
      <div className="px-3 py-2.5 border-b border-gray-700 text-[11px] text-gray-400 leading-snug space-y-1.5">
        <div className="flex items-center gap-1.5 text-gray-300 font-medium">
          <Cpu className="h-3.5 w-3.5 shrink-0" />
          <span>AI usage</span>
        </div>
        <div>
          <span className="text-gray-500">You: </span>
          <span className="text-gray-200">{tok(userTotalTok)} tok</span>
          <span className="text-gray-500"> · </span>
          <span className="text-emerald-400/90">{usd(data.userTotalCostUsd)}</span>
        </div>
        <div>
          <span className="text-gray-500">All users: </span>
          <span className="text-gray-200">{tok(projTotalTok)} tok</span>
          <span className="text-gray-500"> · </span>
          <span className="text-emerald-400/90">{usd(data.projectTotalCostUsd)}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="h-10 w-10 rounded-lg bg-violet-100 flex items-center justify-center shrink-0">
          <Coins className="h-5 w-5 text-violet-600" />
        </div>
        <div className="flex-1 min-w-0">
          <h2 className="text-sm font-semibold text-gray-900">AI usage &amp; cost</h2>
          <p className="text-xs text-gray-500 mt-0.5">
            Estimated from OpenAI token counts (chat, embeddings, moderation classifier). Rates are configurable in the backend.
          </p>
          <dl className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
            <div className="rounded-lg bg-gray-50 px-3 py-2.5 border border-gray-100">
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wide">Your totals</dt>
              <dd className="mt-1 text-gray-900">
                <span className="font-semibold">{tok(data.userInputTokens)}</span>
                <span className="text-gray-400"> in</span>
                {' + '}
                <span className="font-semibold">{tok(data.userOutputTokens)}</span>
                <span className="text-gray-400"> out</span>
                <span className="block mt-1 text-violet-700 font-semibold">{usd(data.userTotalCostUsd)}</span>
              </dd>
            </div>
            <div className="rounded-lg bg-gray-50 px-3 py-2.5 border border-gray-100">
              <dt className="text-xs font-medium text-gray-500 uppercase tracking-wide">Project (all users)</dt>
              <dd className="mt-1 text-gray-900">
                <span className="font-semibold">{tok(data.projectInputTokens)}</span>
                <span className="text-gray-400"> in</span>
                {' + '}
                <span className="font-semibold">{tok(data.projectOutputTokens)}</span>
                <span className="text-gray-400"> out</span>
                <span className="block mt-1 text-violet-700 font-semibold">{usd(data.projectTotalCostUsd)}</span>
              </dd>
            </div>
          </dl>
        </div>
      </div>
    </div>
  );
}

function CardSkeleton() {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 animate-pulse">
      <div className="h-4 w-48 bg-gray-200 rounded" />
      <div className="mt-3 h-3 w-full max-w-md bg-gray-100 rounded" />
      <div className="mt-4 grid grid-cols-2 gap-4">
        <div className="h-20 bg-gray-100 rounded-lg" />
        <div className="h-20 bg-gray-100 rounded-lg" />
      </div>
    </div>
  );
}

function SidebarSkeleton() {
  return <div className={cn('px-3 py-2.5 border-b border-gray-700 h-16 animate-pulse bg-gray-800/50')} />;
}
