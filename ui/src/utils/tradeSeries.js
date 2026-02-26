import { getHourLabel, isSameLocalDay, safeNumber } from "./dateUtils";

// returns [{ hour:"00", trades:0, volume:0 }, ...]
export function buildTradesTodayByHour(trades) {
  const today = new Date();

  // init 24 buckets
  const buckets = Array.from({ length: 24 }, (_, i) => ({
    hour: String(i).padStart(2, "0"),
    trades: 0,
    volume: 0,
  }));

  for (const t of trades || []) {
    if (!t?.createdAt) continue;
    if (!isSameLocalDay(t.createdAt, today)) continue;

    const hour = getHourLabel(t.createdAt);
    const idx = Number(hour);

    const qty = safeNumber(t.quantity);
    const price = safeNumber(t.price);

    buckets[idx].trades += 1;
    buckets[idx].volume += qty * price;
  }

  return buckets;
}

// returns [{ name:"BT", value: 12 }, ...] sorted desc
export function buildInstrumentDistributionToday(trades, topN = 6) {
  const today = new Date();
  const map = new Map();

  for (const t of trades || []) {
    if (!t?.createdAt) continue;
    if (!isSameLocalDay(t.createdAt, today)) continue;

    const key = t.instrument || "UNKNOWN";
    map.set(key, (map.get(key) || 0) + 1);
  }

  const arr = Array.from(map.entries())
    .map(([name, value]) => ({ name, value }))
    .sort((a, b) => b.value - a.value);

  // group others
  const head = arr.slice(0, topN);
  const tail = arr.slice(topN);

  const othersValue = tail.reduce((sum, x) => sum + x.value, 0);
  if (othersValue > 0) head.push({ name: "Others", value: othersValue });

  return head;
}
