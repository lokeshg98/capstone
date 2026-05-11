import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import { useAuthStore } from '@/features/auth/useAuthStore';

const WS_URL = (import.meta.env.VITE_BACKEND_URL ?? 'http://localhost:8080').replace(/^http/, 'ws') + '/ws';

interface WebSocketContextValue {
  connected: boolean;
  /** Subscribe to a STOMP destination. Returns an unsubscribe function. */
  subscribe: (destination: string, cb: (msg: IMessage) => void) => () => void;
  /** Publish a JSON payload to a STOMP destination. */
  publish: (destination: string, body: object) => void;
}

const WebSocketContext = createContext<WebSocketContextValue>({
  connected:  false,
  subscribe:  () => () => {},
  publish:    () => {},
});

export function WebSocketProvider({ children }: { children: ReactNode }) {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const token     = useAuthStore((s) => s.accessToken);

  useEffect(() => {
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new WebSocket(WS_URL),
      connectHeaders:   { Authorization: `Bearer ${token}` },
      onConnect:        () => setConnected(true),
      onDisconnect:     () => setConnected(false),
      onStompError:     (frame) => console.error('STOMP error', frame),
      reconnectDelay:   5_000,
    });

    clientRef.current = client;
    client.activate();

    return () => {
      client.deactivate();
      setConnected(false);
    };
  }, [token]);

  /**
   * Subscribe once connected — if not yet connected the subscription is
   * queued by @stomp/stompjs and replayed on the next connect/reconnect.
   */
  const subscribe = useCallback(
    (destination: string, cb: (msg: IMessage) => void): (() => void) => {
      const client = clientRef.current;
      if (!client) return () => {};
      const sub = client.subscribe(destination, cb);
      return () => sub.unsubscribe();
    },
    [connected], // refresh stable reference when connection state changes
  );

  const publish = useCallback((destination: string, body: object) => {
    clientRef.current?.publish({ destination, body: JSON.stringify(body) });
  }, []);

  return (
    <WebSocketContext.Provider value={{ connected, subscribe, publish }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export const useWebSocket = () => useContext(WebSocketContext);
