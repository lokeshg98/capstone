import { Hash } from 'lucide-react';
import { type ChannelResponse } from '@/features/channels/channelApi';
import MessageList  from './MessageList';
import MessageInput from './MessageInput';
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
    addOptimisticMessage,
    commitMessage,
    removeOptimisticMessage,
    currentUser,
  } = useChannelMessages(channel.id);

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
      <MessageList messages={messages} isLoading={isLoading} />

      {/* Composer */}
      <MessageInput
        channelId={channel.id}
        channelName={channel.name}
        addOptimisticMessage={addOptimisticMessage}
        commitMessage={commitMessage}
        removeOptimisticMessage={removeOptimisticMessage}
        currentUser={currentUser}
      />
    </div>
  );
}
