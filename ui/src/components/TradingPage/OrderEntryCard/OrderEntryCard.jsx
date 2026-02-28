import React, { useEffect, useMemo, useState } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import "./OrderEntryCard.scss";
import { createOrder } from "../../../api/orders";
import { formatMoney } from "../../../utils/formatter";

const OrderEntryCard = ({ instrument, prefill, onSubmitted }) => {
  const [side, setSide] = useState("BUY");
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("");

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  useEffect(() => {
    if (!prefill) return;
    if (prefill.side) setSide(prefill.side);
    if (prefill.price != null) setPrice(String(prefill.price));
  }, [prefill]);

  const approxValue = useMemo(() => {
    const p = Number(price);
    const q = Number(quantity);
    if (!Number.isFinite(p) || !Number.isFinite(q)) return null;
    return p * q;
  }, [price, quantity]);

  const submit = async () => {
    setErr(null);

    const p = Number(price);
    const q = Number(quantity);
    if (!instrument || !instrument.trim()) return setErr("Instrument is required");
    if (!Number.isFinite(p) || p <= 0) return setErr("Price must be > 0");
    if (!Number.isFinite(q) || q <= 0) return setErr("Quantity must be > 0");

    try {
      setBusy(true);
      await createOrder({
        instrument: instrument.trim(),
        side,
        price: p,
        quantity: q,
      });
      setQuantity("");
      onSubmitted?.();
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || "Failed to create order");
    } finally {
      setBusy(false);
    }
  };

  return (
    <TableCard
      title="Order Entry"
      subtitle={err ? err : approxValue != null ? `Approx value: ${formatMoney(approxValue)}` : "Click a price in orderbook to prefill"}
    >
      <div className="oe">
        <div className="oeRow">
          <div className="oeLabel">Instrument</div>
          <div className="oeValue mono">{instrument || "-"}</div>
        </div>

        <div className="oeTabs">
          <button
            className={`oeTab ${side === "BUY" ? "oeTab--active" : ""}`}
            onClick={() => setSide("BUY")}
            type="button"
          >
            Buy
          </button>
          <button
            className={`oeTab ${side === "SELL" ? "oeTab--active" : ""}`}
            onClick={() => setSide("SELL")}
            type="button"
          >
            Sell
          </button>
        </div>

        <div className="oeForm">
          <label className="oeField">
            <span>Price</span>
            <input className="input" value={price} onChange={(e) => setPrice(e.target.value)} placeholder="e.g. 110" />
          </label>

          <label className="oeField">
            <span>Quantity</span>
            <input className="input" value={quantity} onChange={(e) => setQuantity(e.target.value)} placeholder="e.g. 10" />
          </label>

          <button
            className={`ordersBtn ${side === "BUY" ? "ordersBtn--primary" : "ordersBtn--danger"}`}
            disabled={busy}
            onClick={submit}
            type="button"
          >
            {busy ? "Submitting…" : side === "BUY" ? "Place Buy" : "Place Sell"}
          </button>
        </div>
      </div>
    </TableCard>
  );
};

export default OrderEntryCard;