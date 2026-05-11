import { type MessageResponse } from './messageApi';
import AttachmentDisplay from './AttachmentDisplay';
import { cn } from '@/lib/utils';

interface Props {
  message:     MessageResponse;
  isOwn:       boolean;
}


export default function MessageItem({ message, isOwn }: Props) {
  const initials = (message.author.displayName ?? message.author.id)
    .slice(0, 2)
    .toUpperCase();

  const time = new Date(message.createdAt).toLocaleTimeString([], {
    hour:   '2-digit',
    minute: '2-digit',
  });

  return (
    <div className={cn('flex items-start gap-3 px-4 py-1.5 group hover:bg-gray-50')}>
      {/* Avatar */}
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

      {/* Body */}
      <div className="flex-1 min-w-0">
        <div className="flex items-baseline gap-2">
          <span className={cn('text-sm font-semibold', isOwn ? 'text-brand-600' : 'text-gray-900')}>
            {message.author.displayName ?? 'Unknown'}
          </span>
          <span className="text-xs text-gray-400">{time}</span>
          {message.edited && (
            <span className="text-xs text-gray-400 italic">(edited)</span>
          )}
        </div>

        {message.status === 'DELETED' ? (
          <p className="text-sm text-gray-400 italic">This message was deleted.</p>
        ) : (
          <>
            <p className="text-sm text-gray-800 whitespace-pre-wrap break-words">{message.body}</p>
            {message.attachment && (
              <AttachmentDisplay attachment={message.attachment} />
            )}
          </>
        )}

        {/* Reactions */}
        {message.reactions.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-1">
            {message.reactions.map((r) => (
              <span
                key={r.emoji}
                className={cn(
                  'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs',
                  r.reactedByMe
                    ? 'border-brand-400 bg-brand-50 text-brand-700'
                    : 'border-gray-200 bg-white text-gray-600',
                )}
              >
                {r.emoji} {r.count}
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
