import { api } from "./axios";

export async function fetchMyTrades(
  page = 0,
  size = 500,
  sort = "createdAt,desc",
  instrument,
) {
  const res = await api.get("/v1/trades/my", {
    params: { page, size, sort, instrument },
  });

  // Spring Page
  return res.data?.content || [];
}
