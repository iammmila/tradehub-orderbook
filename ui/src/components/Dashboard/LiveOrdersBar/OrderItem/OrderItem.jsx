import React from 'react'
const ALLOWED = new Set(["NEW", "PARTIALLY_FILLED"]);
function formatMoney(value) {
    const n = Number(value);
    if (!Number.isFinite(n)) return "—";
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    }).format(n);
}

function formatNumber(n, maxFrac = 2) {
    const x = Number(n);
    if (!Number.isFinite(x)) return "—";
    return x.toLocaleString(undefined, { maximumFractionDigits: maxFrac });
}
const OrderItem = ({ o }) => {
    const side = String(o.side || "").toUpperCase(); 
    const status = String(o.status || "").toUpperCase(); 
    const isBuy = side === "BUY";

    return (
        <div className="orderChip">
            <span className={`orderChip__sideDot ${isBuy ? "is-buy" : "is-sell"}`} />

            <span className="orderChip__sym">{o.instrument || "—"}</span>

            <span className="orderChip__price">{formatMoney(o.price)}</span>

            <span className="orderChip__qty">
                <span className="orderChip__qtyLabel">Quantity</span>
                {formatNumber(o.remainingQuantity ?? o.quantity, 0)}
            </span>

            <span className={`orderChip__status ${status === "NEW" ? "is-new" : "is-partial"}`}>
                {status === "PARTIALLY_FILLED" ? "PARTIAL" : "NEW"}
            </span>
        </div>
    )
}

export default OrderItem