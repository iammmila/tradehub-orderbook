import axios from "axios";

export const api = axios.create({
  baseURL: "http://localhost:8080/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  const url = config.url || "";
  const isAuthRoute =
    url.includes("/v1/auth/login") || url.includes("/v1/auth/register");

  if (token && !isAuthRoute) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    delete config.headers.Authorization;
  }

  return config;
});