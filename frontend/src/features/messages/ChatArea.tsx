import { useMemo, useState } from 'react';
import { Hash } from 'lucide-react';
import { type ChannelResponse } from '@/features/channels/channelApi';
import MessageList  from './MessageList';
import MessageInput from './MessageInput';
import { type MessageResponse } from './messageApi';
import { useChannelMessages } from '@/hooks/useChannelMessages';

interface Props {
  channel: ChannelResponse;
}

/**
 * Full-height chat area: header + message list + input box.
 * Layout: flex column that fills the parent.
 */
export default function ChatArea({ channel }: Props) {
  const {
    data: messages = [],
    isLoading,
    currentUser,
  } = useChannelMessages(channel.id);
  const [pendingMessages, setPendingMessages] = useState<MessageResponse[]>([]);

  const displayMessages = useMemo(() => {
    const byId = new Map<string, MessageResponse>();
    [...messages, ...pendingMessages].forEach((msg) => byId.set(msg.id, msg));
    return Array.from(byId.values());
  }, [messages, pendingMessages]);

  return (
    <div className="flex flex-col h-full">
      {/* Channel header */}
      <header className="h-14 shrink-0 px-5 flex items-center gap-2 border-b border-gray-200 bg-white">
        <Hash className="h-4 w-4 text-gray-400" />
        <span className="font-semibold text-gray-900">{channel.name}</span>
        {channel.description && (
          <>
            <span className="text-gray-300 select-none">|</span>
            <span className="text-sm text-gray-500 truncate">{channel.description}</span>
          </>
        )}
      </header>

      {/* Messages */}
      <MessageList messages={displayMessages} isLoading={isLoading} />

      {/* Composer */}
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
    </div>
  );
}
