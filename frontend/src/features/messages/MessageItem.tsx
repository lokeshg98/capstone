import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { MessageSquare, SmilePlus } from 'lucide-react';
import { type MessageResponse, addReaction, removeReaction } from './messageApi';
import EmojiPicker from '@/features/emoji/EmojiPicker';
import { type EmojiSearchResult } from '@/features/emoji/emojiApi';
import AttachmentDisplay from './AttachmentDisplay';
import { cn } from '@/lib/utils';
import { Loader2 } from 'lucide-react';

interface Props {
  message:      MessageResponse;
  channelId:    string;
  isOwn:        boolean;
  inThread?:    boolean;
  onOpenThread?: (message: MessageResponse) => void;
}


export default function MessageItem({
  message,
  channelId,
  isOwn,
  inThread,
  onOpenThread,
}: Props) {
  const [showEmoji, setShowEmoji] = useState(false);
  const qc = useQueryClient();

  const reactionMut = useMutation({
    mutationFn: async (emoji: string) => {
      const existing = message.reactions.find((r) => r.emoji === emoji);
      if (existing?.reactedByMe) {
        return removeReaction(channelId, message.id, emoji);
      }
      return addReaction(channelId, message.id, emoji);
    },
    onSuccess: (updated) => {
      qc.setQueryData<MessageResponse[]>(['messages', channelId], (prev = []) =>
        prev.map((m) => (m.id === updated.id ? updated : m)),
      );
      if (message.threadRootId) {
        qc.setQueryData(['thread', channelId, message.threadRootId], (prev: unknown) => {
          if (!prev || typeof prev !== 'object' || !('replies' in prev)) return prev;
          const thread = prev as { root: MessageResponse; replies: MessageResponse[]; summary: unknown };
          return {
            ...thread,
            replies: thread.replies.map((m) => (m.id === updated.id ? updated : m)),
            root: thread.root.id === updated.id ? updated : thread.root,
          };
        });
      }
    },
  });

  const handleReaction = (emoji: EmojiSearchResult) => {
    reactionMut.mutate(emoji.unicode);
  };

  const initials = (message.author.displayName ?? message.author.id)
    .slice(0, 2)
    .toUpperCase();

  const time = new Date(message.createdAt).toLocaleTimeString([], {
    hour:   '2-digit',
    minute: '2-digit',
  });

  return (
    <div className={cn('flex items-start gap-3 px-4 py-1.5 group hover:bg-gray-50 relative', message.pending && 'opacity-70')}>
      <div className="h-8 w-8 rounded-full bg-brand-600 flex items-center justify-center shrink-0 mt-0.5">
        {message.author.avatarUrl ? (
          <img
            src={message.author.avatarUrl}
            alt={message.author.displayName ?? ''}
            className="h-8 w-8 rounded-full object-cover"
          />
        ) : (
          <span className="text-white text-xs font-semibold">{initials}</span>
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-baseline gap-2">
          <span className={cn('text-sm font-semibold', isOwn ? 'text-brand-600' : 'text-gray-900')}>
            {message.author.displayName ?? 'Unknown'}
          </span>
          <span className="text-xs text-gray-400">{time}</span>
          {message.pending && (
            <span className="inline-flex items-center gap-1 text-xs text-gray-400 italic">
              <Loader2 className="h-3 w-3 animate-spin" />
              Sending…
            </span>
          )}
          {message.edited && (
            <span className="text-xs text-gray-400 italic">(edited)</span>
          )}
        </div>

        {message.status === 'DELETED' ? (
          <p className="text-sm text-gray-400 italic">This message was deleted.</p>
        ) : (
          <>
            <p className={cn('text-sm whitespace-pre-wrap break-words', message.pending ? 'text-gray-500' : 'text-gray-800')}>
              {message.body}
            </p>
            {message.attachment && (
              <AttachmentDisplay attachment={message.attachment} />
            )}
          </>
        )}

        {message.reactions.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1">
            {message.reactions.map((r) => (
              <button
                key={r.emoji}
                type="button"
                onClick={() => reactionMut.mutate(r.emoji)}
                className={cn(
                  'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs transition-colors',
                  r.reactedByMe
                    ? 'border-brand-400 bg-brand-50 text-brand-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50',
                )}
              >
                {r.emoji} {r.count}
              </button>
            ))}
          </div>
        )}

        <div className="flex items-center gap-2 mt-0.5">
          {!inThread && onOpenThread && message.status !== 'DELETED' && (
            <button
              type="button"
              onClick={() => onOpenThread(message)}
              className={cn(
                'inline-flex items-center gap-1 text-xs text-gray-500 hover:text-brand-600',
                'opacity-0 group-hover:opacity-100 transition-opacity',
                (message.replyCount ?? 0) > 0 && 'opacity-100',
              )}
            >
              <MessageSquare className="h-3.5 w-3.5" />
              {(message.replyCount ?? 0) > 0
                ? `${message.replyCount} ${message.replyCount === 1 ? 'reply' : 'replies'}`
                : 'Reply in thread'}
            </button>
          )}

          {message.status !== 'DELETED' && (
            <div className="relative">
              <button
                type="button"
                onClick={() => setShowEmoji((v) => !v)}
                className={cn(
                  'inline-flex items-center gap-1 text-xs text-gray-500 hover:text-brand-600',
                  'opacity-0 group-hover:opacity-100 transition-opacity',
                )}
              >
                <SmilePlus className="h-3.5 w-3.5" />
                React
              </button>
              {showEmoji && (
                <EmojiPicker
                  className="left-0 bottom-auto top-full mt-1"
                  onSelect={handleReaction}
                  onClose={() => setShowEmoji(false)}
                />
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
