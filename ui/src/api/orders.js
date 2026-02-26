import { api } from "./axios";

export async function fetchOrders(page = 0, size = 200) {
  const res = await api.get("/v1/orders/my", { params: { page, size } });
  return res.data?.content || [];
}
