import React, { useEffect, useState } from 'react'
import './OpenOrdersCard.scss'
import { fetchOrders } from "../../../api/orders";
import Cards from '../Cards/Cards';

function isOpenLike(order) {
    return order.status === "NEW" || order.status === "PARTIALLY_FILLED";
}
const OpenOrdersCard = () => {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [openTotal, setOpenTotal] = useState(0);
    const [openBuy, setOpenBuy] = useState(0);
    const [openSell, setOpenSell] = useState(0);

    useEffect(() => {
        let alive = true;

        (async () => {
            try {
                setLoading(true);
                const orders = await fetchOrders(0, 200);
                if (!alive) return;

                const open = (orders || []).filter(isOpenLike);
                const buy = open.filter((o) => o.side === "BUY").length;
                const sell = open.filter((o) => o.side === "SELL").length;

                setOpenTotal(open.length);
                setOpenBuy(buy);
                setOpenSell(sell);
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
            title="Open Orders"
            value={openTotal}
            subtitle={`BUY ${openBuy} / SELL ${openSell}`}
            rightTag="LIVE"
            loading={loading}
            error={error}
        />
    );
}

export default OpenOrdersCard