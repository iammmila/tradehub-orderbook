export function formatTime(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${hh}:${mm}`;
}

export function formatDate(iso) {
  if (!iso) return "-";
  const d = new Date(iso);
  const dd = String(d.getDate()).padStart(2, "0");
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const yy = d.getFullYear();
  return `${dd}.${mm}.${yy}`;
}

export function formatNumber(n, maxFrac = 2) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "-";
  return x.toLocaleString(undefined, { maximumFractionDigits: maxFrac });
}

export function formatMoney(n) {
  const x = Number(n);
  if (!Number.isFinite(x)) return "-";
  return x.toLocaleString(undefined, { maximumFractionDigits: 2 });
}
