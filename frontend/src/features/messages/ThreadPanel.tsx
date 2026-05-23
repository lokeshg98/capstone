import { X, Sparkles } from 'lucide-react';
import { useAuthStore } from '@/features/auth/useAuthStore';
import { useThreadMessages } from '@/hooks/useThreadMessages';
import MessageItem from './MessageItem';
import MessageInput from './MessageInput';
import { type MessageResponse } from './messageApi';

interface Props {
  workspaceId: string;
  channelId:   string;
  channelName: string;
  rootMessage: MessageResponse;
  onClose:     () => void;
}

export default function ThreadPanel({ workspaceId, channelId, channelName, rootMessage, onClose }: Props) {
  const myId   = useAuthStore((s) => s.user?.id);
  const myName = useAuthStore((s) => s.user?.displayName);
  const { data, isLoading } = useThreadMessages(channelId, rootMessage.id);

  const replies = data?.replies ?? [];
  const summary = data?.summary;

  const isMentioned = (msg: MessageResponse) =>
    !msg.author.id || msg.author.id !== myId
      ? myName != null && new RegExp('@' + escapeRegExp(myName), 'i').test(msg.body)
      : false;

  return (
    <aside className="w-96 shrink-0 border-l border-gray-200 bg-white flex flex-col h-full">
      <header className="h-14 shrink-0 px-4 flex items-center justify-between border-b border-gray-200">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-gray-900 truncate">Thread</p>
          <p className="text-xs text-gray-500 truncate">#{channelName}</p>
        </div>
        <button
          type="button"
          onClick={onClose}
          className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100"
          title="Close thread"
        >
          <X className="h-4 w-4" />
        </button>
      </header>

      {summary && (
        <div className="shrink-0 mx-3 mt-3 rounded-lg border border-violet-200 bg-violet-50 px-3 py-2.5">
          <div className="flex items-center gap-1.5 text-violet-800 text-xs font-semibold mb-1.5">
            <Sparkles className="h-3.5 w-3.5" />
            Otter-style digest · {summary.messageCount} messages
          </div>
          <p className="text-xs text-violet-900 whitespace-pre-wrap leading-relaxed max-h-40 overflow-y-auto">
            {summary.summaryBody}
          </p>
        </div>
      )}

      <div className="flex-1 overflow-y-auto py-2">
        <MessageItem
          message={data?.root ?? rootMessage}
          channelId={channelId}
          isOwn={rootMessage.author.id === myId}
          isMentioned={isMentioned(data?.root ?? rootMessage)}
          inThread
        />
        {isLoading && (
          <p className="px-4 py-2 text-xs text-gray-400">Loading replies…</p>
        )}
        {replies.map((msg) => (
          <MessageItem
            key={msg.id}
            message={msg}
            channelId={channelId}
            isOwn={msg.author.id === myId}
            isMentioned={isMentioned(msg)}
            inThread
          />
        ))}
      </div>

      <MessageInput
        workspaceId={workspaceId}
        channelId={channelId}
        channelName={channelName}
        threadRootId={rootMessage.id}
      />
    </aside>
  );
}

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
