import { api } from "./axios";

/**
 * GET /api/v1/orderbook?instrument=TST1
 * returns { instrument, bids:[], asks:[] }
 */
export async function fetchOrderBook(instrument) {
  const res = await api.get("/v1/orderbook", {
    params: { instrument },
  });
  return res.data;
}
