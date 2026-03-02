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

export function stripIds(input) {
  if (input == null) return "";
  let s = String(input);

  // Common patterns: "id=123", "orderId: 123", "tradeId 123", "order id 123"
  s = s.replace(
    /\b(?:orderId|tradeId|notificationId|id)\s*(?:[:=]|\s)\s*\d+\b/gi,
    "",
  );

  // Patterns like "#123" (often used as id references)
  s = s.replace(/#\s*\d+\b/g, "");

  // Parenthesized numeric ids: "(123)" or "( 123 )"
  s = s.replace(/\(\s*\d+\s*\)/g, "");

  // If backend inserts "Order 123" / "Trade 123" style:
  s = s.replace(/\b(?:Order|Trade|Notification)\s+\d+\b/gi, (m) => {
    // Keep the word, drop the number
    return m.replace(/\d+/g, "").trim();
  });

  // Clean separators left behind: " -  - " or "::"
  s = s.replace(/\s*[-–—:|]\s*(?=[-–—:|])/g, " ");
  s = s.replace(/\s{2,}/g, " ").trim();

  return s;
}

export function normalizeDto(n) {
  return {
    // Clean title + message so UI never shows internal ids
    text: stripIds(n?.title),
    details: stripIds(n?.message),

    isRead: Boolean(n?.read),
    time: timeAgo(n?.createdAt),
    createdAt: n?.createdAt,
    type: n?.type,
    entityType: n?.entityType,

    // If you later need navigation, keep raw ids here (NOT displayed)
    entityId: n?.entityId ?? n?.orderId ?? n?.tradeId ?? null,
  };
}