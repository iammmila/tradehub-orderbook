import { api } from "./axios";

export async function fetchOrderBook({
  instrument,
  aggregated = true,
  levels = false,
  signal,
}) {
  const inst = String(instrument || "").trim();
  if (!inst)
    return { bids: [], asks: [], aggregated, levels, instrument: null };

  const res = await api.get("/v1/orderbook", {
    params: {
      instrument: inst,
      aggregated,
      ...(levels ? { levels: true } : {}),
    },
    signal,
  });

  return res.data;
}
