// NotificationsWsBridge.jsx
import { useEffect, useRef } from "react";
import { createNotificationsSocket } from "./notificationsSocket";

export default function NotificationsWsBridge({ token, onNotification }) {
  const onNotificationRef = useRef(onNotification);

  useEffect(() => {
    onNotificationRef.current = onNotification;
  }, [onNotification]);

  useEffect(() => {
    if (!token) return;

    const socket = createNotificationsSocket({
      getToken: () => token,
      onNotification: (dto) => onNotificationRef.current?.(dto), // stable ref
      onStatus: (s) => console.log("WS status:", s),
    });

    socket.connect();
    return () => socket.disconnect();
  }, [token]); 

  return null;
}
