import React, { useEffect, useMemo, useState } from "react";
import "./LiveOrdersBar.scss";
import OrderItem from "./OrderItem/OrderItem";
import { useWsStatus } from "../../../context/WsStatusContext";
import { fetchOrdersPage } from "../../../api/orders";

const ALLOWED = new Set(["NEW", "PARTIALLY_FILLED"]);
const LiveOrdersBar = ({ speed = 26, pauseOnHover = true, className = "" }) => {
    const { status } = useWsStatus();
    const connected = status === "connected";
    const [orders, setOrders] = useState([]);

    // fetch recent orders (real data) and keep only NEW + PARTIALLY_FILLED
    useEffect(() => {
        let alive = true;

        (async () => {
            try {
                const page = await fetchOrdersPage(0, 50, "createdAt,desc");
                if (!alive) return;

                const filtered = (page?.content || []).filter((o) =>
                    ALLOWED.has(String(o.status || "").toUpperCase())
                );

                setOrders(filtered.slice(0, 20));
            } catch {
                if (alive) setOrders([]);
            }
        })();

        return () => {
            alive = false;
        };
    }, []);

    const loopItems = useMemo(() => {
        const base = orders || [];
        return base.length ? [...base, ...base] : [];
    }, [orders]);

    return (
        <div className={`livebar ${className}`}>
            <div className="livebar__left">
                <span className={`livebar__dot ${connected ? "is-live" : "is-off"}`} />
                <div className="livebar__label">
                    <div className="livebar__title">LIVE ORDERS</div>
                    <div className="livebar__subtitle">{connected ? "Connected" : "Disconnected"}</div>
                </div>
            </div>

            <div
                className={`livebar__marquee ${pauseOnHover ? "pause-on-hover" : ""}`}
                style={{ ["--speed"]: `${speed}s` }}
            >
                {!orders.length ? (
                    <div className="livebar__empty">No NEW / PARTIAL orders</div>
                ) : (
                    <div className="livebar__track">
                        {loopItems.map((o, i) => (
                            <OrderItem key={`${o.id}-${i}`} o={o} />
                        ))}
                    </div>
                )}
            </div>
        </div>
  )
}

export default LiveOrdersBar