export function statusBadgeClass(status) {
  const s = String(status || "").toUpperCase();
  if (s === "FILLED") return "badge badge--filled";
  if (s === "CANCELLED") return "badge badge--cancelled";
  return "badge badge--new";
}

export function compare(a, b) {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
}

export function isSameLocalDay(dateLike, now = new Date()) {
  if (!dateLike) return false;
  const d = new Date(dateLike);
  return (
    d.getFullYear() === now.getFullYear() &&
    d.getMonth() === now.getMonth() &&
    d.getDate() === now.getDate()
  );
}
