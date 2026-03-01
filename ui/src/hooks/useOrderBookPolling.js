import { useCallback, useEffect, useRef, useState } from "react";
import { fetchOrderBook } from "../api/orderbook";

export default function useOrderBookPolling({
  instrument,
  showLevels,
  pollMs = 2000,
  enabled = true,
} = {}) {
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const firstLoadedRef = useRef(false);
  const timerRef = useRef(null);
  const abortRef = useRef(null);

  const lastHashRef = useRef("");

  const stopTimer = useCallback(() => {
    if (timerRef.current) clearInterval(timerRef.current);
    timerRef.current = null;
  }, []);

  const abortInFlight = useCallback(() => {
    if (abortRef.current) abortRef.current.abort();
    abortRef.current = null;
  }, []);

  const runFetch = useCallback(
    async ({ forceSpinner = false } = {}) => {
      const inst = String(instrument || "").trim();
      if (!inst) {
        stopTimer();
        abortInFlight();
        setError(null);
        setLoading(false);
        setRefreshing(false);
        firstLoadedRef.current = false;
        lastHashRef.current = "";
        setBook(null);
        return;
      }

      abortInFlight();
      const ac = new AbortController();
      abortRef.current = ac;

      const isFirst = !firstLoadedRef.current;

      setError(null);
      if (isFirst || forceSpinner) setLoading(true);
      else setRefreshing(true);

      try {
        const data = await fetchOrderBook({
          instrument: inst,
          aggregated: true,
          levels: !!showLevels,
          signal: ac.signal,
        });

        if (ac.signal.aborted) return;

        const nextHash = JSON.stringify({
          levels: !!showLevels,
          bids: data?.bids ?? [],
          asks: data?.asks ?? [],
          bidLevels: data?.bidLevels ?? [],
          askLevels: data?.askLevels ?? [],
        });

        if (nextHash !== lastHashRef.current) {
          lastHashRef.current = nextHash;
          setBook(data);
        }

        firstLoadedRef.current = true;
      } catch (e) {
        if (e?.name === "AbortError" || e?.code === "ERR_CANCELED") return;
        setError(
          e?.response?.data?.message ||
            e?.message ||
            "Failed to load orderbook",
        );
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [instrument, showLevels, abortInFlight, stopTimer],
  );

  const refreshNow = useCallback(
    () => runFetch({ forceSpinner: false }),
    [runFetch],
  );

  useEffect(() => {
    runFetch({ forceSpinner: true });

    stopTimer();
    if (enabled) {
      timerRef.current = setInterval(
        () => runFetch({ forceSpinner: false }),
        pollMs,
      );
    }

    return () => {
      stopTimer();
      abortInFlight();
    };
  }, [runFetch, enabled, pollMs, stopTimer, abortInFlight]);

  return { book, loading, refreshing, error, refreshNow };
}
