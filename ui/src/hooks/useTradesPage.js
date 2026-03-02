import { useCallback, useEffect, useMemo, useState } from "react";
import { fetchMyTradesPage } from "../api/trades";

export function useTradesPage({ instrumentApi = "" }) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);

  const [pageInfo, setPageInfo] = useState({ number: 0, totalElements: 0 });
  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDir, setSortDir] = useState("desc");

  const sortParam = useMemo(() => `${sortBy},${sortDir}`, [sortBy, sortDir]);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await fetchMyTradesPage(
        page,
        size,
        sortParam,
        instrumentApi || undefined,
      );
      const content = data?.content || [];
      const p = data?.page || {};

      setRows(content);
      setPageInfo({
        number: p?.number ?? page,
        totalElements: p?.totalElements ?? 0,
      });
      setLastUpdatedAt(new Date());
    } catch (e) {
      setError(
        e?.response?.data?.message || e?.message || "Failed to load trades",
      );
    } finally {
      setLoading(false);
    }
  }, [page, size, sortParam, instrumentApi]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleSort = useCallback((field) => {
    setPage(0);
    setSortBy((prev) => {
      if (prev !== field) {
        setSortDir("desc");
        return field;
      }
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
      return prev;
    });
  }, []);

  const resetSort = useCallback(() => {
    setSortBy("createdAt");
    setSortDir("desc");
    setPage(0);
  }, []);

  return {
    loading,
    error,
    rows,
    page,
    size,
    pageInfo,
    lastUpdatedAt,
    sortBy,
    sortDir,
    toggleSort,
    setPage,
    setSize,
    resetSort,
    reload: load,
  };
}
