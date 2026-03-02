export function statusBadgeClass(status) {
  const s = String(status || "").toUpperCase();
  if (s === "FILLED") return "badge badge--filled";
  if (s === "CANCELLED") return "badge badge--cancelled";
  if (s === "PARTIALLY_FILLED") return "badge badge--partial";
  return "badge badge--new";
}

export function sideClass(side) {
  const s = String(side || "").toUpperCase();
  if (s === "BUY") return "badge badge--buy";
  if (s === "SELL") return "badge badge--sell";
  return "badge";
}

export function typeBadgeClass(type) {
  const t = String(type || "").toUpperCase();
  if (t === "MARKET") return "badge badge--typeMarket";
  if (t === "LIMIT") return "badge badge--typeLimit";
  if (t === "HIDDEN_LIMIT") return "badge badge--typeHidden";
  if (t === "MIN_EXECUTION_SIZE") return "badge badge--typeMinExec";
  return "badge";
}

export function routingBadgeClass(mode) {
  const m = String(mode || "").toUpperCase();
  if (m === "AUTO") return "badge badge--auto";
  if (m === "MANUAL") return "badge badge--manual";
  return "badge";
}

export function exchangeBadgeClass(exchangeCode) {
  const s = String(exchangeCode || "").toUpperCase();
  // you can map these however you like; keeping your existing idea:
  if (s === "XLON") return "badge badge--partial";
  if (s === "XNAS") return "badge badge--cancelled";
  if (s === "XTKS") return "badge badge--filled";
  return "badge badge--new";
}

export function routedByLabel(value) {
  const v = String(value || "").toUpperCase();
  if (v === "SOR") return "SOR";
  if (v === "USER") return "USER";
  return v || "-";
}

export function shortText(s, max = 34) {
  const str = String(s || "");
  if (!str) return "-";
  if (str.length <= max) return str;
  return str.slice(0, max - 1) + "…";
}

export function isCancellable(status) {
  return String(status || "").toUpperCase() === "NEW";
}

export function isReplaceable(status) {
  return String(status || "").toUpperCase() === "NEW";
}

export function isMinExecType(type) {
  return String(type || "").toUpperCase() === "MIN_EXECUTION_SIZE";
}
