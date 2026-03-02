import { useCallback, useEffect, useState } from "react";
import { fetchOrdersPage } from "../api/orders";

export function useRecentOrders() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [rows, setRows] = useState([]);

  const load = useCallback(async () => {
    let ok = true;
    try {
      setLoading(true);
      setError(null);

      // fetch more so "Today" filter is meaningful, then slice to 10 later
      const page = await fetchOrdersPage(0, 200, "createdAt,desc");
      if (!ok) return;

      setRows(page?.content || []);
    } catch (e) {
      if (!ok) return;
      const msg = e?.response
        ? `HTTP ${e.response.status} - ${JSON.stringify(e.response.data)}`
        : e?.message || "error";
      setError(msg);
    } finally {
      if (ok) setLoading(false);
    }
    return () => {
      ok = false;
    };
  }, []);

  useEffect(() => {
    load();
  }, [load]);
 
  return { loading, error, rows, orders: rows, reload: load };
}
