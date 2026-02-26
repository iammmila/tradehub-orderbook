import { api } from "./axios";

export async function fetchTrades(page = 0, size = 200) {
  const res = await api.get("/v1/trades/my", { params: { page, size } });
  return res.data?.content || [];
}