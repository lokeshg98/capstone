import { useEffect, useState, useCallback } from 'react';
import { useWebSocket } from '@/context/WebSocketContext';

interface PresencePayload {
  userId: string;
  online: boolean;
}

export function usePresence(workspaceId: string | null | undefined) {
  const { connected, publish, subscribe } = useWebSocket();
  const [onlineUsers, setOnlineUsers] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!workspaceId || !connected) return;

    publish(`/app/presence/${workspaceId}/join`, {});

    const unsub = subscribe(
      `/topic/workspaces/${workspaceId}/presence`,
      (frame) => {
        const event = JSON.parse(frame.body);
        if (event.eventType !== 'PRESENCE_UPDATED') return;
        const payload = event.data as PresencePayload;
        setOnlineUsers((prev) => {
          const next = new Set(prev);
          if (payload.online) next.add(payload.userId);
          else next.delete(payload.userId);
          return next;
        });
      },
    );

    return () => {
      publish(`/app/presence/${workspaceId}/leave`, {});
      unsub();
      setOnlineUsers(new Set());
    };
  }, [workspaceId, connected]); // eslint-disable-line react-hooks/exhaustive-deps

  const isOnline = useCallback(
    (userId: string) => onlineUsers.has(userId),
    [onlineUsers],
  );

  return { isOnline };
}
