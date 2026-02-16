import { api } from "./axios";

export async function login({ username, password }) {
  // adjust endpoint names to your backend
  const res = await api.post("/auth/login", { username, password });
  return res.data; // expecting { token: "..." } or just token
}

export async function register(payload) {
  const res = await api.post("/auth/register", payload);
  return res.data;
}

export const isAuthenticated = () => {
  return !!localStorage.getItem("token");
};