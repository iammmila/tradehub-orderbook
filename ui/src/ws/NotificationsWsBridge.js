// NotificationsWsBridge.jsx
import { useEffect, useRef } from "react";
import { createNotificationsSocket } from "./notificationsSocket";
import { useWsStatus } from "../context/WsStatusContext";

export default function NotificationsWsBridge({ token, onNotification }) {
  const onNotificationRef = useRef(onNotification);
  const { setStatus } = useWsStatus();

  useEffect(() => {
    onNotificationRef.current = onNotification;
  }, [onNotification]);

  useEffect(() => {
    if (!token) return;

    const socket = createNotificationsSocket({
      getToken: () => token,
      onNotification: (dto) => onNotificationRef.current?.(dto), // stable ref
      onStatus: (s) => {
        setStatus(s);
      },
    });

    socket.connect();
    return () => socket.disconnect();
  }, [token, setStatus]);

  return null;
}
