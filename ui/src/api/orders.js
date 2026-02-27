import { api } from "./axios";

export async function fetchOrders(
  page = 0,
  size = 10,
  sort = "createdAt,desc",
  instrument,
  side,
  status,
) {
  const res = await api.get("/v1/orders/my", {
    params: { page, size, sort, instrument, side, status },
  });
  return res.data?.content || [];
}

export async function fetchOrdersPage(
  page = 0,
  size = 10,
  sort = "createdAt,desc",
  instrument,
  side,
  status,
) {
  const res = await api.get("/v1/orders/my", {
    params: { page, size, sort, instrument, side, status },
  });
  return res.data;
}
