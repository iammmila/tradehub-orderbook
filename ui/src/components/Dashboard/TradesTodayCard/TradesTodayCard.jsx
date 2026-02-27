import React, { useEffect, useState } from 'react'
import Cards from '../Cards/Cards';
import { fetchMyTrades } from '../../../api/trades';
import "./TradesTodayCard.scss";
import { isSameLocalDay, safeNumber, formatMoney } from "../../../utils/helpers";

const TradesTodayCard = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tradesToday, setTradesToday] = useState(0);
  const [volumeToday, setVolumeToday] = useState(0);

  useEffect(() => {
    let alive = true;

    (async () => {
      try {
        setLoading(true);
        const trades = await fetchMyTrades(0, 200);

        if (!alive) return;

        const today = new Date();
        const todays = (trades || []).filter((t) => isSameLocalDay(t.createdAt, today));

        const volume = todays.reduce((sum, t) => {
          const qty = safeNumber(t.quantity);
          const price = safeNumber(t.price);
          return sum + qty * price;
        }, 0);

        setTradesToday(todays.length);
        setVolumeToday(volume);
        setError(null);
      } catch (e) {
        if (!alive) return;

        const msg =
          e?.response
            ? `HTTP ${e.response.status} - ${JSON.stringify(e.response.data)}`
            : (e?.message || "error");

        console.error("TradesTodayCard error:", e);
        setError(msg);
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => {
      alive = false;
    };
  }, []);

  return (
    <Cards
      title="Trades Today"
      value={tradesToday}
      subtitle={`Volume ${formatMoney(volumeToday)}`}
      loading={loading}
      error={error}
    />
  );
}

export default TradesTodayCard