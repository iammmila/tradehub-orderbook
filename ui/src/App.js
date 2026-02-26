import { createBrowserRouter, RouterProvider } from "react-router-dom";
import { ROUTES } from "./routes/routes";
import { HelmetProvider } from "react-helmet-async";
import NotificationsWsBridge from "./ws/NotificationsWsBridge";
import { useAuthToken } from "./auth/useAuthToken";
import toast, { Toaster } from "react-hot-toast";
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

      <Toaster position="bottom-left" />

      {/* your routes / layout */}
      <HelmetProvider>
        <RouterProvider router={router} />
      </HelmetProvider>
    </>
  );
}

export default App;
