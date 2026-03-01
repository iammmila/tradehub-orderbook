import { api } from "./axios";

export async function fetchRoutingPlan(params) {
  const res = await api.get("/v1/route/plan", { params });
  return res.data;
}
