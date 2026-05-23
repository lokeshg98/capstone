import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchThread,
  type MessageResponse,
  type PageResponse,
  type WsOutboundEvent,
} from '@/features/messages/messageApi';
import { useWebSocket } from '@/context/WebSocketContext';

export function useThreadMessages(channelId: string | null, threadRootId: string | null) {
  const qc = useQueryClient();
  const { connected, subscribe } = useWebSocket();

  const queryKey = ['thread', channelId, threadRootId];

  const query = useQuery({
    queryKey,
    queryFn: () => fetchThread(channelId!, threadRootId!),
    enabled: !!channelId && !!threadRootId,
  });

  useEffect(() => {
    if (!channelId || !threadRootId || !connected) return;

    const unsub = subscribe(`/topic/channels/${channelId}`, (frame) => {
      let event: WsOutboundEvent;
      try {
        let parsed: unknown = JSON.parse(frame.body);
        if (typeof parsed === 'string') parsed = JSON.parse(parsed);
        event = parsed as WsOutboundEvent;
      } catch {
        return;
      }

      if (event.eventType === 'MESSAGE_CREATED') {
        const newMsg = event.data as MessageResponse;
        if (newMsg.threadRootId !== threadRootId) return;

        qc.setQueryData(queryKey, (prev: Awaited<ReturnType<typeof fetchThread>> | undefined) => {
          if (!prev) return prev;
          if (prev.replies.some((m) => m.id === newMsg.id)) return prev;
          const next = { ...prev, replies: [...prev.replies, newMsg] };
          if (!prev.summary && next.replies.length >= 9) {
            setTimeout(() => qc.invalidateQueries({ queryKey }), 4000);
          }
          return next;
        });

        qc.setQueryData<PageResponse<MessageResponse>>(['messages', channelId], (prev) => {
          if (!prev) return prev;
          return {
            ...prev,
            content: prev.content.map((m) =>
              m.id === threadRootId ? { ...m, replyCount: m.replyCount + 1 } : m,
            ),
          };
        });
        return;
      }

      if (event.eventType === 'MESSAGE_UPDATED') {
        const updated = event.data as MessageResponse;
        if (updated.threadRootId !== threadRootId && updated.id !== threadRootId) return;

        if (updated.status === 'HIDDEN' || updated.status === 'DELETED' || updated.status === 'FLAGGED') {
          qc.setQueryData(queryKey, (prev: Awaited<ReturnType<typeof fetchThread>> | undefined) => {
            if (!prev) return prev;
            if (updated.id === threadRootId) return prev;
            return {
              ...prev,
              replies: prev.replies.filter((m) => m.id !== updated.id),
            };
          });
        }
      }
    });

    return unsub;
  }, [channelId, threadRootId, connected]); // eslint-disable-line react-hooks/exhaustive-deps

  return query;
}
