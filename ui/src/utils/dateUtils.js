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
export function hourToAmPmLabel(hourStr) {
  const h = Number(hourStr); // "00".."23"
  if (!Number.isFinite(h)) return hourStr;

  const suffix = h >= 12 ? "PM" : "AM";
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${hour12} ${suffix}`;
}
