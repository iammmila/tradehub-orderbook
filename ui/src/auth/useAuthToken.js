import { useEffect, useState } from "react";

export function useAuthToken() {
  const [token, setToken] = useState(() => localStorage.getItem("token"));

  useEffect(() => {
    const onStorage = () => setToken(localStorage.getItem("token"));
    window.addEventListener("storage", onStorage);

    // custom event (same tab)
    const onAuth = () => setToken(localStorage.getItem("token"));
    window.addEventListener("auth:token", onAuth);

    return () => {
      window.removeEventListener("storage", onStorage);
      window.removeEventListener("auth:token", onAuth);
    };
  }, []);

  return token;
}
