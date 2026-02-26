export function isSameLocalDay(isoString, day) {
  const d = new Date(isoString);
  return (
    d.getFullYear() === day.getFullYear() &&
    d.getMonth() === day.getMonth() &&
    d.getDate() === day.getDate()
  );
}

export function getHourLabel(isoString) {
  const d = new Date(isoString);
  const h = d.getHours();
  return String(h).padStart(2, "0"); // "00".."23"
}

export function safeNumber(n) {
  const x = Number(n);
  return Number.isFinite(x) ? x : 0;
}
