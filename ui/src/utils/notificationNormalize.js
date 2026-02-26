// src/utils/notificationNormalize.js
export function timeAgo(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  const diffMs = Date.now() - date.getTime();
  const sec = Math.floor(diffMs / 1000);
  if (sec < 60) return `${sec}s ago`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}m ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr}h ago`;
  const day = Math.floor(hr / 24);
  return `${day}d ago`;
}

export function normalizeFromPage(pageDto) {
  const items = pageDto?.content ?? [];
  return items.map(normalizeDto);
}

export function normalizeDto(n) {
  return {
    id: n.id,
    text: n.title,
    details: n.message,
    isRead: Boolean(n.read),
    time: timeAgo(n.createdAt),
    createdAt: n.createdAt,
    type: n.type,
    entityType: n.entityType,
    entityId: n.entityId,
  };
}
