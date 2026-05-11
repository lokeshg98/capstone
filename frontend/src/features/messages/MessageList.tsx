import { useEffect, useRef } from 'react';
import { type MessageResponse } from './messageApi';
import MessageItem from './MessageItem';
import { useAuthStore } from '@/features/auth/useAuthStore';

interface Props {
  messages:  MessageResponse[];
  isLoading: boolean;
}

export default function MessageList({ messages, isLoading }: Props) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const myId      = useAuthStore((s) => s.user?.id);

  // Scroll to bottom whenever new messages arrive
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length]);

  if (isLoading) return <LoadingSkeleton />;

  if (messages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
        No messages yet. Say hello!
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto py-2">
      {messages.map((msg) => (
        <MessageItem key={msg.id} message={msg} isOwn={msg.author.id === myId} />
      ))}
      <div ref={bottomRef} />
    </div>
  );
}

function LoadingSkeleton() {
  return (
    <div className="flex-1 overflow-y-auto py-4 space-y-4 px-4 animate-pulse">
      {[1, 2, 3, 4, 5].map((i) => (
        <div key={i} className="flex items-start gap-3">
          <div className="h-8 w-8 rounded-full bg-gray-200 shrink-0" />
          <div className="flex-1 space-y-1.5">
            <div className="h-3 w-28 rounded bg-gray-200" />
            <div className="h-4 rounded bg-gray-100" style={{ width: `${40 + (i * 13) % 40}%` }} />
          </div>
        </div>
      ))}
    </div>
  );
}
