import React, { useEffect, useState } from 'react'
import Cards from '../Cards/Cards';
import "./FillRateCard.scss";
import { fetchOrders } from '../../../api/orders';
import { isSameLocalDay, safeNumber } from "../../../utils/helpers";

const FillRateCard = () => {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [fillRate, setFillRate] = useState(0);
    const [partialCount, setPartialCount] = useState(0);

    useEffect(() => {
        let alive = true;

        (async () => {
            try {
                setLoading(true);
                const orders = await fetchOrders(0, 200);
                if (!alive) return;

                const today = new Date();
                const todays = (orders || []).filter((o) => isSameLocalDay(o.createdAt, today));

                const totalQty = todays.reduce((sum, o) => sum + safeNumber(o.quantity), 0);
                const filledQty = todays.reduce((sum, o) => {
                    const q = safeNumber(o.quantity);
                    const rem = safeNumber(o.remainingQuantity);
                    return sum + Math.max(0, q - rem);
                }, 0);

                const partial = todays.filter((o) => o.status === "PARTIALLY_FILLED").length;

                const pct = totalQty > 0 ? (filledQty / totalQty) * 100 : 0;

                setFillRate(pct);
                setPartialCount(partial);
                setError(null);
            } catch (e) {
                if (!alive) return;
                setError(e?.message || "error");
            } finally {
                if (alive) setLoading(false);
            }
        })();

        return () => { alive = false; };
    }, []);

    return (
        <Cards
            title="Fill Rate (Today)"
            value={`${fillRate.toFixed(1)}%`}
            subtitle={`${partialCount} partially filled`}
            loading={loading}
            error={error}
        />
    );
}

export default FillRateCard