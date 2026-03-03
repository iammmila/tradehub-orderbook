// NotificationsWsBridge.jsx
import { useEffect, useRef } from "react";
import { createNotificationsSocket } from "./notificationsSocket";
import { useWsStatus } from "../context/WsStatusContext";

export default function NotificationsWsBridge({ token, onNotification }) {
  const onNotificationRef = useRef(onNotification);
  const { setStatus } = useWsStatus();
  const socketRef = useRef(null);

  useEffect(() => {
    onNotificationRef.current = onNotification;
  }, [onNotification]);

  // create socket ONCE (do not recreate per token)
  useEffect(() => {
    socketRef.current = createNotificationsSocket({
      // always read fresh token when connecting
      getToken: () => localStorage.getItem("token") || "",
      onNotification: (dto) => onNotificationRef.current?.(dto),
      onStatus: (s) => setStatus(s),
    });

    // connect immediately if token already exists
    if (localStorage.getItem("token")) {
      socketRef.current.connect();
    }

    return () => {
      socketRef.current?.disconnect();
      socketRef.current = null;
    };
  }, [setStatus]);

  // reconnect when token prop changes (normal login/logout)
  useEffect(() => {
    if (!token) return;
    socketRef.current?.disconnect();
    socketRef.current?.connect();
  }, [token]);

  // IMPORTANT: reconnect after Google OAuth success (auth:token event)
  useEffect(() => {
    const onAuthToken = () => {
      socketRef.current?.disconnect();
      socketRef.current?.connect();
    };

    window.addEventListener("auth:token", onAuthToken);
    return () => window.removeEventListener("auth:token", onAuthToken);
  }, []);

  return null;
}
