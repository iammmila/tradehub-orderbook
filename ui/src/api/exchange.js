import { api } from "./axios";

export async function fetchExchanges() {
  const { data } = await api.get("/v1/exchanges");
  if (Array.isArray(data)) return data; 
  if (Array.isArray(data?.exchanges)) return data.exchanges;
  return [];
}
