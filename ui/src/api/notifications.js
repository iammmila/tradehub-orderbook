// src/api/notifications.js
import { api } from "./axios";

/**
 * Expected endpoints:
 * GET  /v1/notifications
 * GET  /v1/notifications/unread-count
 * POST  /v1/notifications/{id}/read
 * POST  /v1/notifications/read-all
 */

export async function getNotifications({ page = 0, size = 20 } = {}) {
  const res = await api.get("/v1/notifications", { params: { page, size } });
  return res.data; // <-- returns the whole page object with .content
}

export async function getUnreadCount() {
  const res = await api.get("/v1/notifications/unread-count");
  return res.data;
}

export async function markNotificationRead(id) {
  const res = await api.post(`/v1/notifications/${id}/read`);
  return res.data;
}

export async function markAllNotificationsRead() {
  const res = await api.post(`/v1/notifications/read-all`);
  return res.data;
}
