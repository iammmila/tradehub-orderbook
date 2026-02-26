// src/ui/toast/useToast.js
import { useCallback, useState } from "react";

let idSeq = 1;

export function useToast() {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const show = useCallback(
    (toast) => {
      const id = idSeq++;
      const duration = toast.duration ?? 3500;

      setToasts((prev) => [
        ...prev,
        {
          id,
          title: toast.title ?? "Notification",
          message: toast.message ?? "",
          type: toast.type ?? "info",
        },
      ]);

      window.setTimeout(() => remove(id), duration);
      return id;
    },
    [remove],
  );

  return { toasts, show, remove };
}
