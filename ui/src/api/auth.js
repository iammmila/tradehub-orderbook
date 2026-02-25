import { api } from "./axios";

export async function login({ username, password }) {
  localStorage.removeItem("token"); // optional safety
  const res = await api.post("/v1/auth/login", { username, password });
  return res.data;
}

export async function register(payload) {
  const res = await api.post("/v1/auth/register", payload);
  return res.data;
}

export const isAuthenticated = () => {
  return !!localStorage.getItem("token");
};