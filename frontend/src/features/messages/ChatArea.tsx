import { useMemo, useState } from 'react';
import { Hash, Users } from 'lucide-react';
import { type ChannelResponse } from '@/features/channels/channelApi';
import { type MessageResponse } from './messageApi';
import MessageList  from './MessageList';
import MessageInput from './MessageInput';
import ThreadPanel  from './ThreadPanel';
import { useChannelMessages } from '@/hooks/useChannelMessages';
import { cn } from '@/lib/utils';

interface Props {
  channel:         ChannelResponse;
  showMembers:     boolean;
  onToggleMembers: () => void;
}

export default function ChatArea({ channel, showMembers, onToggleMembers }: Props) {
  const {
    data: messages = [],
    isLoading,
    currentUser,
  } = useChannelMessages(channel.id);
  const [pendingMessages, setPendingMessages] = useState<MessageResponse[]>([]);
  const [openThread, setOpenThread] = useState<MessageResponse | null>(null);

  const displayMessages = useMemo(() => {
    const byId = new Map<string, MessageResponse>();
    [...messages, ...pendingMessages].forEach((msg) => byId.set(msg.id, msg));
    return Array.from(byId.values());
  }, [messages, pendingMessages]);

  return (
    <div className="flex h-full min-w-0">
      <div className="flex flex-col h-full flex-1 min-w-0">
        <header className="h-14 shrink-0 px-5 flex items-center gap-2 border-b border-gray-200 bg-white">
          <Hash className="h-4 w-4 text-gray-400" />
          <span className="font-semibold text-gray-900">{channel.name}</span>
          {channel.description && (
            <>
              <span className="text-gray-300 select-none">|</span>
              <span className="text-sm text-gray-500 truncate">{channel.description}</span>
            </>
          )}
          <div className="ml-auto flex items-center gap-2">
            <button
              onClick={onToggleMembers}
              className={cn(
                'h-8 w-8 rounded-lg flex items-center justify-center transition-colors',
                showMembers ? 'bg-gray-100 text-gray-600' : 'text-gray-400 hover:bg-gray-100',
              )}
              title={showMembers ? 'Hide member list' : 'Show member list'}
            >
              <Users className="h-4 w-4" />
            </button>
          </div>
        </header>

        <MessageList
          messages={displayMessages}
          channelId={channel.id}
          isLoading={isLoading}
          onOpenThread={setOpenThread}
        />

        {!openThread && (
          <MessageInput
            channelId={channel.id}
            channelName={channel.name}
            addOptimisticMessage={(msg) => setPendingMessages((prev) => [...prev, msg])}
            commitMessage={(tempId, msg) => {
              setPendingMessages((prev) => prev.map((item) => (item.id === tempId ? msg : item)));
            }}
            removeOptimisticMessage={(id) => setPendingMessages((prev) => prev.filter((item) => item.id !== id))}
            currentUser={currentUser}
          />
        )}
      </div>

      {openThread && (
        <ThreadPanel
          channelId={channel.id}
          channelName={channel.name}
          rootMessage={openThread}
          onClose={() => setOpenThread(null)}
        />
      )}
    </div>
  );
}
