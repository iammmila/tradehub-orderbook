import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { ROUTES } from "./routes/routes";
import { HelmetProvider } from "react-helmet-async";
import NotificationsWsBridge from "./ws/NotificationsWsBridge";
import { useAuthToken } from "./auth/useAuthToken";
import { Toaster } from "react-hot-toast";
import { useCallback } from "react";

const router = createBrowserRouter(ROUTES);

function App() {
  const token = useAuthToken();
  const handleNotification = useCallback((dto) => {
    window.dispatchEvent(new CustomEvent("notif:ws", { detail: dto }));
  }, []);
  return (
    <>
      <NotificationsWsBridge
        token={token}
        onNotification={handleNotification}
      />

      <Toaster
        position="bottom-left"
        toastOptions={{
          duration: 5000,
          style: {
            background: "#111827",
            color: "#fff",
            border: "1px solid rgba(255,255,255,0.12)",
            borderRadius: "14px",
            padding: "12px 14px",
            boxShadow: "0 12px 30px rgba(0,0,0,0.35)",
          },
        }}
      />
      <HelmetProvider>
        <RouterProvider router={router} />
      </HelmetProvider>
    </>
  );
}

export default App;
