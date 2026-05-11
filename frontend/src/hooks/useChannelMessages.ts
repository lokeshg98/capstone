import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchMessages,
  type MessageResponse,
  type WsOutboundEvent,
} from '@/features/messages/messageApi';
import { useWebSocket } from '@/context/WebSocketContext';

/**
 * Combines HTTP-loaded message history with real-time WebSocket updates.
 *
 * Strategy:
 *  1. React Query fetches the initial page via HTTP.
 *  2. A STOMP subscription pushes new events into the same query cache entry,
 *     so the UI re-renders automatically without polling.
 *  3. Deduplication prevents a message from appearing twice if the HTTP response
 *     and the WS event arrive close together.
 */
export function useChannelMessages(channelId: string | null) {
  const qc               = useQueryClient();
  const { connected, subscribe } = useWebSocket();

  const queryKey = ['messages', channelId];

  const query = useQuery({
    queryKey,
    queryFn:  () => fetchMessages(channelId!),
    enabled:  !!channelId,
    select:   (data) => data.content,   // unwrap the Page<> wrapper
  });

  useEffect(() => {
    if (!channelId || !connected) return;

    const unsub = subscribe(`/topic/channels/${channelId}`, (frame) => {
      const event = JSON.parse(frame.body) as WsOutboundEvent;

      switch (event.eventType) {
        case 'MESSAGE_CREATED': {
          const newMsg = event.data as MessageResponse;
          qc.setQueryData<MessageResponse[]>(queryKey, (prev = []) => {
            if (prev.some((m) => m.id === newMsg.id)) return prev;  // deduplicate
            return [...prev, newMsg];
          });
          break;
        }
        case 'MESSAGE_UPDATED':
        case 'REACTION_UPDATED': {
          const updated = event.data as MessageResponse;
          qc.setQueryData<MessageResponse[]>(queryKey, (prev = []) =>
            prev.map((m) => (m.id === updated.id ? updated : m)),
          );
          break;
        }
        // TYPING events are handled separately by the TypingIndicator component
      }
    });

    return unsub;
  }, [channelId, connected]); // eslint-disable-line react-hooks/exhaustive-deps

  return query;
}
