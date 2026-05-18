import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Hash, Plus, ChevronLeft, MessageSquare, Bot, ShieldAlert, Settings } from 'lucide-react';
import { fetchChannels, createChannel, type ChannelResponse } from '@/features/channels/channelApi';
import ChatArea from '@/features/messages/ChatArea';
import AskBotPanel from '@/features/documents/AskBotPanel';
import ModerationDashboard from '@/features/moderation/ModerationDashboard';
import WorkspaceSettings from './WorkspaceSettings';
import { cn } from '@/lib/utils';
import { AiUsageSummary } from '@/features/dashboard/AiUsageSummary';

type ActiveView =
  | { kind: 'channel'; channelId: string }
  | { kind: 'ask-bot' }
  | { kind: 'moderation' }
  | { kind: 'settings' };

export default function WorkspacePage() {
  const { wsId, channelId } = useParams<{ wsId: string; channelId?: string }>();
  const navigate = useNavigate();
  const [activeView, setActiveView] = useState<ActiveView | null>(null);

  const { data: channels = [], refetch: refetchChannels } = useQuery({
    queryKey: ['channels', wsId],
    queryFn:  () => fetchChannels(wsId!),
    enabled:  !!wsId,
  });

  // Auto-navigate to the first channel when none is selected and no bot view is active
  if (!channelId && channels.length > 0 && (!activeView || activeView.kind === 'channel')) {
    navigate(`/workspaces/${wsId}/channels/${channels[0].id}`, { replace: true });
  }

  // Sync the active view with the URL channel param
  const resolvedView: ActiveView | null =
    activeView?.kind === 'ask-bot' || activeView?.kind === 'moderation' || activeView?.kind === 'settings'
      ? activeView
      : channelId
      ? { kind: 'channel', channelId }
      : null;

  const activeChannel = channels.find((c) => c.id === channelId) ?? null;

  const handleChannelClick = (id: string) => {
    setActiveView({ kind: 'channel', channelId: id });
    navigate(`/workspaces/${wsId}/channels/${id}`);
  };

  const handleAskBotClick = () => {
    setActiveView({ kind: 'ask-bot' });
  };

  const handleModerationClick = () => {
    setActiveView({ kind: 'moderation' });
  };

  const handleSettingsClick = () => {
    setActiveView({ kind: 'settings' });
  };

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      <WorkspaceSidebar
        wsId={wsId!}
        channels={channels}
        activeChannelId={resolvedView?.kind === 'channel' ? resolvedView.channelId : null}
        isBotActive={resolvedView?.kind === 'ask-bot'}
        isModerationActive={resolvedView?.kind === 'moderation'}
        isSettingsActive={resolvedView?.kind === 'settings'}
        onChannelClick={handleChannelClick}
        onAskBotClick={handleAskBotClick}
        onModerationClick={handleModerationClick}
        onSettingsClick={handleSettingsClick}
        onChannelCreated={refetchChannels}
      />

      <main className="flex-1 flex flex-col overflow-hidden">
        {resolvedView?.kind === 'settings' ? (
          <WorkspaceSettings workspaceId={wsId!} />
        ) : resolvedView?.kind === 'moderation' ? (
          <ModerationDashboard workspaceId={wsId!} />
        ) : resolvedView?.kind === 'ask-bot' ? (
          <AskBotPanel workspaceId={wsId!} />
        ) : activeChannel ? (
          <ChatArea channel={activeChannel} />
        ) : (
          <EmptyState />
        )}
      </main>
    </div>
  );
}

// ─── Sidebar ─────────────────────────────────────────────────────────────────

function WorkspaceSidebar({
  wsId,
  channels,
  activeChannelId,
  isBotActive,
  isModerationActive,
  isSettingsActive,
  onChannelClick,
  onAskBotClick,
  onModerationClick,
  onSettingsClick,
  onChannelCreated,
}: {
  wsId:               string;
  channels:           ChannelResponse[];
  activeChannelId:    string | null;
  isBotActive:        boolean;
  isModerationActive: boolean;
  isSettingsActive:   boolean;
  onChannelClick:     (id: string) => void;
  onAskBotClick:      () => void;
  onModerationClick:  () => void;
  onSettingsClick:    () => void;
  onChannelCreated:   () => void;
}) {
  const [creating, setCreating] = useState(false);
  const [newName,  setNewName]  = useState('');

  const handleCreate = async () => {
    if (!newName.trim()) return;
    await createChannel(wsId, { name: newName.trim() });
    setCreating(false);
    setNewName('');
    onChannelCreated();
  };

  return (
    <aside className="w-56 shrink-0 bg-gray-900 text-gray-200 flex flex-col">
      {/* Top bar */}
      <div className="h-14 px-4 flex items-center gap-2 border-b border-gray-700">
        <Link to="/dashboard" className="text-gray-400 hover:text-white transition-colors">
          <ChevronLeft className="h-4 w-4" />
        </Link>
        <span className="font-semibold text-white truncate">Workspace</span>
      </div>

      {/* Channel list */}
      <div className="flex-1 overflow-y-auto py-2">
        <div className="px-3 mb-1">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold uppercase tracking-wider text-gray-400">
              Channels
            </span>
            <button
              onClick={() => setCreating(true)}
              className="text-gray-400 hover:text-white transition-colors rounded p-0.5"
              title="New channel"
            >
              <Plus className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        {channels.map((ch) => (
          <button
            key={ch.id}
            onClick={() => onChannelClick(ch.id)}
            className={cn(
              'w-full flex items-center gap-2 px-3 py-1.5 rounded mx-1 text-sm transition-colors text-left',
              ch.id === activeChannelId
                ? 'bg-brand-600 text-white'
                : 'text-gray-300 hover:bg-gray-800',
            )}
          >
            <Hash className="h-3.5 w-3.5 shrink-0 opacity-70" />
            <span className="truncate">{ch.name}</span>
          </button>
        ))}

        {creating && (
          <div className="px-3 py-1.5 mx-1">
            <input
              autoFocus
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter')  handleCreate();
                if (e.key === 'Escape') { setCreating(false); setNewName(''); }
              }}
              placeholder="channel-name"
              className="w-full rounded bg-gray-700 text-white text-sm px-2 py-1 outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
        )}
      </div>

      {/* Utility entries pinned to the bottom of the sidebar */}
      <div className="shrink-0 border-t border-gray-700 flex flex-col">
        <AiUsageSummary variant="sidebar" />
        <div className="p-2 space-y-1">
        <button
          onClick={onAskBotClick}
          className={cn(
            'w-full flex items-center gap-2 px-3 py-2 rounded text-sm transition-colors',
            isBotActive ? 'bg-brand-600 text-white' : 'text-gray-300 hover:bg-gray-800',
          )}
        >
          <Bot className="h-4 w-4 shrink-0" />
          <span className="font-medium">Ask Bot</span>
        </button>
        <button
          onClick={onModerationClick}
          className={cn(
            'w-full flex items-center gap-2 px-3 py-2 rounded text-sm transition-colors',
            isModerationActive ? 'bg-brand-600 text-white' : 'text-gray-300 hover:bg-gray-800',
          )}
        >
          <ShieldAlert className="h-4 w-4 shrink-0" />
          <span className="font-medium">Moderation</span>
        </button>
        <button
          onClick={onSettingsClick}
          className={cn(
            'w-full flex items-center gap-2 px-3 py-2 rounded text-sm transition-colors',
            isSettingsActive ? 'bg-brand-600 text-white' : 'text-gray-300 hover:bg-gray-800',
          )}
        >
          <Settings className="h-4 w-4 shrink-0" />
          <span className="font-medium">Settings</span>
        </button>
        </div>
      </div>
    </aside>
  );
}

function EmptyState() {
  return (
    <div className="flex-1 flex items-center justify-center">
      <div className="text-center text-gray-400">
        <MessageSquare className="mx-auto h-10 w-10 mb-3 opacity-30" />
        <p className="text-sm">Select a channel to start chatting</p>
      </div>
    </div>
  );
}
