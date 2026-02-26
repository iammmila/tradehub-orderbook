import { useEffect, useState } from "react";
import { fetchMyTrades } from "../api/trades";

export function useDashboardTrades() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [trades, setTrades] = useState([]);

  useEffect(() => {
    let alive = true;

    (async () => {
      try {
        setLoading(true);
        // big size so charts are correct
        const data = await fetchMyTrades(0, 500, "createdAt,desc");
        if (!alive) return;

        setTrades(data || []);
        setError(null);
      } catch (e) {
        if (!alive) return;
        const msg = e?.response
          ? `HTTP ${e.response.status} - ${JSON.stringify(e.response.data)}`
          : e?.message || "error";
        setError(msg);
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, []);

  return { trades, loading, error };
}
