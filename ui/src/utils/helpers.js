export function isSameLocalDay(isoString, day) {
  const d = new Date(isoString);
  return (
    d.getFullYear() === day.getFullYear() &&
    d.getMonth() === day.getMonth() &&
    d.getDate() === day.getDate()
  );
}

export function safeNumber(n) {
  const x = Number(n);
  return Number.isFinite(x) ? x : 0;
}
export function formatMoney(n) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "0";
  return x.toLocaleString(undefined, { maximumFractionDigits: 2 });
}