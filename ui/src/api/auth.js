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

export async function forgotPassword(email) {
  const res = await api.post("/v1/auth/forgot-password", { email });
  return res.data;
}

export async function resetPassword(token, newPassword) {
  const res = await api.post("/v1/auth/reset-password", { token, newPassword });
  return res.data;
}

export async function resendVerifyEmail(email) {
  const res = await api.post("/v1/auth/verify-email/request", { email });
  return res.data;
}

export async function confirmVerifyEmail(token) {
  const res = await api.post("/v1/auth/verify-email/confirm", { token });
  return res.data;
}

export const isAuthenticated = () => {
  return !!localStorage.getItem("token");
};