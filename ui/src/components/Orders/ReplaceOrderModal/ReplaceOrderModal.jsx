import React, { useMemo, useState, useEffect } from "react";
import "./ReplaceOrderModal.scss";
import { replaceOrder } from "../../../api/orders";
import ModalPortal from "../../common/ModalPortal";   

const ReplaceOrderModal = ({ isOpen, onClose, onReplaced, order }) => {
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isOpen) {
      setPrice("");
      setQuantity("");
      setError(null);
      setBusy(false);
    }
  }, [isOpen, order?.id]);

  const canSubmit = useMemo(() => {
    const p = Number(price);
    const q = Number(quantity);
    const hasPrice = price !== "" && p > 0;
    const hasQty = quantity !== "" && q > 0;
    return !!order && (hasPrice || hasQty);
  }, [order, price, quantity]);

  const submit = async (e) => {
    e.preventDefault();
    if (!canSubmit || !order) return;

    try {
      setBusy(true);
      setError(null);

      const payload = {};
      if (price !== "") payload.price = Number(price);
      if (quantity !== "") payload.quantity = Number(quantity);

      await replaceOrder(order.id, payload);

      onReplaced?.();
      onClose?.(); // close on success
    } catch (e2) {
      setError(e2?.message || "Replace failed");
    } finally {
      setBusy(false);
    }
  };

  if (!isOpen) return null;

  return (
    <ModalPortal>
      <div className="modal__backdrop" onMouseDown={onClose} role="dialog" aria-modal="true">
        <div className="modal__card" onMouseDown={(e) => e.stopPropagation()}>
          <div className="modal__header">
            <h3>Replace Order</h3>
            <button
              type="button"
              className="ordersBtn ordersBtn--secondary ordersBtn--sm"
              onClick={onClose}
              disabled={busy}
              aria-label="Close"
            >
              ✕
            </button>
          </div>

          <div className="replace__meta">
            <div><b>Instrument: </b> {order?.instrument}</div>
            <div><b>Side: </b> {order?.side}</div>
          </div>

          <form onSubmit={submit} className="modal__form">
            {error && <div className="modal__error">{error}</div>}

            <label className="field">
              <span>New Price (optional)</span>
              <input
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                type="number"
                step="0.01"
                min="0"
              />
            </label>

            <label className="field">
              <span>New Quantity (optional)</span>
              <input
                value={quantity}
                onChange={(e) => setQuantity(e.target.value)}
                type="number"
                step="1"
                min="0"
              />
            </label>

            <div className="modal__actions">
              <button
                type="button"
                className="ordersBtn ordersBtn--secondary"
                onClick={onClose}
                disabled={busy}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="ordersBtn ordersBtn--primary"
                disabled={!canSubmit || busy}
              >
                {busy ? "Replacing..." : "Replace"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </ModalPortal>
  );
};

export default ReplaceOrderModal;