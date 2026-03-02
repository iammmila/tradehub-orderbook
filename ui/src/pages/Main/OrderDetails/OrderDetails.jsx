// pages/Orders/OrderDetails.jsx (adjust path)
import React, { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import TableCard from "../../../components/Dashboard/Tables/TableCard/TableCard"; // adjust import
import { fetchOrderById } from "../../../api/orders"; // adjust import
import { formatMoney, formatNumber } from "../../../utils/formatter"; // adjust import
import { formatDate, formatTime } from "../../../utils/formatter"; // adjust import (or your own)
import "./OrderDetails.scss";
import { Helmet } from "react-helmet";

const pretty = (v) => (v === null || v === undefined || v === "" ? "-" : String(v));

const fmtDateTime = (iso) => {
    if (!iso) return "-";
    return `${formatDate(iso)} ${formatTime(iso)}`;
};

const OrderDetails = () => {
    const { id } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [order, setOrder] = useState(null);

    useEffect(() => {
        let alive = true;

        (async () => {
            try {
                setLoading(true);
                setError(null);

                const data = await fetchOrderById(id);
                if (!alive) return;

                setOrder(data);
            } catch (e) {
                if (!alive) return;
                setError(e?.message || "Failed to load order details.");
            } finally {
                if (alive) setLoading(false);
            }
        })();

        return () => {
            alive = false;
        };
    }, [id]);
    const rows = useMemo(() => {
        const o = order || {};

        return [
            // { k: "Order ID", v: pretty(o.id) },
            { k: "Instrument", v: pretty(o.instrument) },
            { k: "Side", v: pretty(o.side) },
            { k: "Type", v: pretty(o.type) },

            { k: "Price", v: o.price != null ? formatMoney(o.price) : "-" },
            { k: "Quantity", v: o.quantity != null ? formatNumber(o.quantity) : "-" },
            { k: "Remaining Quantity", v: o.remainingQuantity != null ? formatNumber(o.remainingQuantity) : "-" },

            { k: "Status", v: pretty(o.status) },
            { k: "Exchange", v: pretty(o.exchangeCode) },

            { k: "Routing Mode", v: pretty(o.routingMode) },
            { k: "Routed By", v: pretty(o.routedBy) },
            { k: "Route Reason", v: pretty(o.routeReason) },

            { k: "Min Execution Size", v: o.minExecSize != null ? formatNumber(o.minExecSize) : "-" },
            { k: "Visible", v: o.visible === true ? "Yes" : o.visible === false ? "No" : "-" },

            // { k: "User ID", v: pretty(o.userId) },
            { k: "Created At", v: fmtDateTime(o.createdAt) },
        ];
    }, [order]);

    return (
        <div className="orderDetails">
            <Helmet>
                <title>Order Details | Trading</title>
                <meta name='description' content="It is Order's Details page of Trading Application" />
            </Helmet>
            <div className="orderDetails-container">
                <button className="ordersBtn--secondary" onClick={() => navigate(-1)}>
                    &larr; BACK TO TERMINAL
                </button>

                <TableCard
                    title="ORDER MANIFEST"
                    rightSlot={
                        order?.status && (
                            <div className="pill--status">
                                {order.status.toUpperCase()}
                            </div>
                        )
                    }
                >
                    {loading ? (
                        <div className="detailsState">DECRYPTING DATA...</div>
                    ) : error ? (
                        <div className="detailsState detailsState--error">
                            <h3>SYSTEM FAILURE</h3>
                            <p>{error}</p>
                        </div>
                    ) : (
                        <div className="kvGrid">
                            {rows.map((r) => (
                                <div key={r.k} className="kvRow" data-key={r.k}>
                                    <span className="kvKey">{r.k}</span>
                                    <span className={`kvVal ${r.k.toLowerCase().includes('id') ||
                                        r.k.toLowerCase().includes('at') ? 'mono' : ''
                                        } ${r.v.toString().toLowerCase() === 'sell' ? 'val-sell' : ''
                                        }`}>
                                        {r.v}
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}
                </TableCard>
            </div>
        </div>
    );
}

export default OrderDetails;