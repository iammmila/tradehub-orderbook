import axios from "axios";

export const api = axios.create({
  baseURL: "http://localhost:8080/api", // change to your backend base
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");

  const url = config.url || "";
  const isAuthRoute =
    url.includes("/auth/login") || url.includes("/auth/register");

  if (token && !isAuthRoute) {
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    delete config.headers.Authorization;
  }

  return config;
});