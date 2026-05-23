import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  fetchMessages,
  type PageResponse,
  type MessageResponse,
  type WsOutboundEvent,
} from '@/features/messages/messageApi';
import { useWebSocket } from '@/context/WebSocketContext';
import { useAuthStore } from '@/features/auth/useAuthStore';

/**
 * Combines HTTP-loaded message history with real-time WebSocket updates.
 */
export function useChannelMessages(channelId: string | null) {
  const qc               = useQueryClient();
  const { connected, subscribe } = useWebSocket();
  const currentUser = useAuthStore((s) => s.user);

  const queryKey = ['messages', channelId];

  const query = useQuery({
    queryKey,
    queryFn:  () => fetchMessages(channelId!),
    enabled:  !!channelId,
    select:   (data) => data.content,
  });

  useEffect(() => {
    if (!channelId || !connected) return;

    const unsub = subscribe(`/topic/channels/${channelId}`, (frame) => {
      let event: WsOutboundEvent;
      try {
        let parsed: unknown = JSON.parse(frame.body);
        if (typeof parsed === 'string') parsed = JSON.parse(parsed);
        event = parsed as WsOutboundEvent;
      } catch {
        return;
      }

      switch (event.eventType) {
        case 'MESSAGE_CREATED': {
          const newMsg = event.data as MessageResponse;
          if (newMsg.threadRootId != null) {
            qc.setQueryData<PageResponse<MessageResponse>>(queryKey, (prev) => {
              if (!prev) return prev;
              return {
                ...prev,
                content: prev.content.map((m) =>
                  m.id === newMsg.threadRootId
                    ? { ...m, replyCount: (m.replyCount ?? 0) + 1 }
                    : m,
                ),
              };
            });
            break;
          }
          qc.setQueryData<PageResponse<MessageResponse>>(queryKey, (prev) => {
            if (!prev) return prev;
            const withoutPending = prev.content.filter((m) => {
              if (!m.pending) return true;
              return !matchesPendingMessage(m, newMsg);
            });
            if (withoutPending.some((m) => m.id === newMsg.id)) {
              return { ...prev, content: withoutPending };
            }
            return {
              ...prev,
              content: [...withoutPending, { ...newMsg, replyCount: newMsg.replyCount ?? 0 }],
            };
          });
          break;
        }
        case 'MESSAGE_UPDATED':
        case 'REACTION_UPDATED': {
          const updated = event.data as MessageResponse;
          if (updated.threadRootId != null) {
            qc.setQueryData(['thread', channelId, updated.threadRootId], (prev: unknown) => {
              if (!prev || typeof prev !== 'object' || !('replies' in prev)) return prev;
              const thread = prev as { root: MessageResponse; replies: MessageResponse[]; summary: unknown };
              return {
                ...thread,
                replies: thread.replies.map((m) => (m.id === updated.id ? updated : m)),
                root: thread.root.id === updated.id ? updated : thread.root,
              };
            });
            break;
          }
          if (updated.status === 'HIDDEN' || updated.status === 'DELETED' || updated.status === 'FLAGGED') {
            qc.setQueryData<PageResponse<MessageResponse>>(queryKey, (prev) => {
              if (!prev) return prev;
              return {
                ...prev,
                content: prev.content.filter((m) => m.id !== updated.id),
              };
            });
            break;
          }
          qc.setQueryData<PageResponse<MessageResponse>>(queryKey, (prev) => {
            if (!prev) return prev;
            const exists = prev.content.some((m) => m.id === updated.id);
            if (exists) {
              return {
                ...prev,
                content: prev.content.map((m) => (m.id === updated.id ? updated : m)),
              };
            }
            return {
              ...prev,
              content: [...prev.content, { ...updated, replyCount: updated.replyCount ?? 0 }].sort(
                (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
              ),
            };
          });
          break;
        }
      }
    });

    return unsub;
  }, [channelId, connected]); // eslint-disable-line react-hooks/exhaustive-deps

  return {
    ...query,
    currentUser,
  };
}

function matchesPendingMessage(pending: MessageResponse, real: MessageResponse) {
  return pending.author.id === real.author.id
    && pending.body === real.body
    && pending.threadRootId === real.threadRootId
    && (pending.attachment?.id ?? null) === (real.attachment?.id ?? null);
}
