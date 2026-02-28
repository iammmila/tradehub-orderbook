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
  const params = { page, size, sort };
  if (instrument) params.instrument = instrument;
  if (side) params.side = side;
  if (status) params.status = status;

  const res = await api.get("/v1/orders/my", { params });
  return res.data; // Page<OrderResponse>
}

export async function createOrder(payload) {
  const res = await api.post("/v1/orders", payload);
  return res.data;
}

export async function cancelOrder(orderId) {
  const res = await api.delete(`/v1/orders/${orderId}`);
  return res.data;
}

export async function replaceOrder(orderId, payload) {
  const res = await api.patch(`/v1/orders/${orderId}`, payload);
  return res.data;
}