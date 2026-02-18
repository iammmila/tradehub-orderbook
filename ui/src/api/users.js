import { api } from "./axios";

export async function getMe() {
  const res = await api.get("/v1/users/me");
  return res.data;
}

export async function updateMe(payload) {
  // PUT for optional fields
  const res = await api.put("/v1/users/me", payload);
  return res.data; // can be { user, token } or just user (see below)
}

export async function changePassword(payload) {
  const res = await api.put("/v1/users/me/password", payload);
  return res.data;
}