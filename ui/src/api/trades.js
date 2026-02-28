import { api } from "./axios";

export async function fetchMyTrades(
  page = 0,
  size = 10,
  sort = "createdAt,desc",
  instrument,
) {
  const res = await api.get("/v1/trades/my", {
    params: { page, size, sort, instrument },
  });
  return res.data?.content || [];
}
export async function fetchMyTradesPage({
  page = 0,
  size = 10,
  sort = "createdAt,desc",
  instrument = "",
}) {
  const params = { page, size, sort };
  if (instrument && instrument.trim()) params.instrument = instrument.trim();

  const res = await api.get("/v1/trades/my", { params });
  return res.data;
}
