// src/ws/notificationsSocket.js
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

export function createNotificationsSocket({
  getToken,
  onNotification,
  onStatus,
}) {
  let client = null;

  function connect() {
    const token = getToken?.();
    if (!token) {
      onStatus?.("no-token");
      return;
    }
    const WS_URL = `http://localhost:8080/ws?token=${encodeURIComponent("Bearer " + token)}`;

    client = new Client({
      webSocketFactory: () => new SockJS(WS_URL),
      connectHeaders: {},
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        onStatus?.("connected");

        client.subscribe("/user/queue/notifications", (frame) => {
          try {
            const dto = JSON.parse(frame.body);
            onNotification?.(dto);
          } catch (e) {
            console.error("WS notification parse error:", e);
          }
        });
      },
      onStompError: (frame) => {
        console.error("STOMP error", frame.headers?.message, frame.body);
        onStatus?.("stomp-error");
      },
      onWebSocketClose: () => onStatus?.("closed"),
      onDisconnect: () => onStatus?.("disconnected"),
    });

    client.activate();
  }

  function disconnect() {
    try {
      client?.deactivate();
    } finally {
      client = null;
      onStatus?.("disconnected");
    }
  }

  return { connect, disconnect };
}
