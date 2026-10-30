import { Client } from '@stomp/stompjs';
import { useEffect, useRef, useState } from 'react';
import { getTokens } from './api';

export type SeatDelta = { showId: string; seatIds: string[]; state: 'HELD' | 'BOOKED' | 'AVAILABLE'; at: string };

/** Live seat deltas for a show; onConnect fires on every reconnect so callers can resync. */
export function useSeatEvents(showId: string | undefined, onDelta: (d: SeatDelta) => void, onConnect: () => void) {
  const [live, setLive] = useState(false);
  const handlers = useRef({ onDelta, onConnect });
  handlers.current = { onDelta, onConnect };

  useEffect(() => {
    if (!showId) return;
    const url = () => {
      const proto = location.protocol === 'https:' ? 'wss' : 'ws';
      return `${proto}://${location.host}/ws?access_token=${encodeURIComponent(getTokens()?.accessToken ?? '')}`;
    };
    const client = new Client({ brokerURL: url(), reconnectDelay: 3000 });
    client.beforeConnect = () => {
      client.brokerURL = url(); // pick up a refreshed token on reconnect
    };
    client.onConnect = () => {
      setLive(true);
      client.subscribe(`/topic/shows/${showId}`, (m) => handlers.current.onDelta(JSON.parse(m.body)));
      handlers.current.onConnect();
    };
    client.onWebSocketClose = () => setLive(false);
    client.activate();
    return () => {
      void client.deactivate();
    };
  }, [showId]);

  return live;
}
