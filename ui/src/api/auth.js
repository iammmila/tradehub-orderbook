import { api } from "./axios";

export async function login({ username, password }) {
  localStorage.removeItem("token"); // optional safety
  const res = await api.post("/v1/auth/login", { username, password });
  const token = res.data?.token;
  if (token) {
    localStorage.setItem("token", token);
    window.dispatchEvent(new Event("auth:token"));
  }

  return res.data;
}

export async function register(payload) {
  const res = await api.post("/v1/auth/register", payload);
  return res.data;
}

export const isAuthenticated = () => {
  return !!localStorage.getItem("token");
};