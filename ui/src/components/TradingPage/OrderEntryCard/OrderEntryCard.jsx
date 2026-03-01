import React, { useEffect, useMemo, useState } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import "./OrderEntryCard.scss";
import { createOrder } from "../../../api/orders";
import { formatMoney } from "../../../utils/formatter";
import { fetchRoutingPlan } from "../../../api/routing";
import Select from "../../Dashboard/Tables/Select/Select";

const EXCHANGES = ["XLON", "XNAS", "XTKS"];

function toNum(x) {
  const n = Number(x);
  return Number.isFinite(n) ? n : null;
}

const OrderEntryCard = ({ instrument, prefill, onSubmitted }) => {
  const [side, setSide] = useState("BUY");
  const [type, setType] = useState("LIMIT"); // LIMIT | MARKET
  const [price, setPrice] = useState("");
  const [quantity, setQuantity] = useState("");
  const [routingMode, setRoutingMode] = useState("AUTO"); // AUTO | MANUAL
  const [exchangeCode, setExchangeCode] = useState("XLON");

  const [planOpen, setPlanOpen] = useState(false);
  const [planLoading, setPlanLoading] = useState(false);
  const [planErr, setPlanErr] = useState(null);
  const [plan, setPlan] = useState(null);

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState(null);

  useEffect(() => {
    if (!prefill) return;
    if (prefill.side) setSide(prefill.side);
    if (prefill.price != null) setPrice(String(prefill.price));
  }, [prefill]);

  const p = useMemo(() => toNum(price), [price]);
  const q = useMemo(() => toNum(quantity), [quantity]);

  const approxValue = useMemo(() => {
    if (type !== "LIMIT") return null;
    if (p == null || q == null) return null;
    return p * q;
  }, [p, q, type]);

  const validate = () => {
    if (!instrument || !instrument.trim()) return "Instrument is required";
    if (!q || q <= 0) return "Quantity must be > 0";
    if (type === "LIMIT" && (!p || p <= 0)) return "Price must be > 0 for LIMIT";
    if (routingMode === "MANUAL" && !exchangeCode) return "Exchange is required for MANUAL";
    return null;
  };

  const submit = async () => {
    setErr(null);
    const v = validate();
    if (v) return setErr(v);

    try {
      setBusy(true);

      const payload = {
        instrument: instrument.trim(),
        side,
        type,
        quantity: q,
        ...(type === "LIMIT" ? { price: p } : {}),
        ...(routingMode === "MANUAL" ? { exchangeCode } : {}),
        routingMode,
      };
      await createOrder(payload);

      setQuantity("");
      setPlanOpen(false);
      setPlan(null);

      onSubmitted?.();
    } catch (e) {
      setErr(e?.response?.data?.message || e?.message || "Failed to create order");
    } finally {
      setBusy(false);
    }
  };

  const previewPlan = async () => {
    setPlanErr(null);
    setPlan(null);

    const v = validate();
    if (v) {
      setPlanOpen(true);
      setPlanErr(v);
      return;
    }

    try {
      setPlanOpen(true);
      setPlanLoading(true);

      const params = {
        instrument: instrument.trim(),
        side,
        type,
        quantity: q,
        ...(type === "LIMIT" ? { price: p } : {}),
      };

      const data = await fetchRoutingPlan(params);
      setPlan(data);
    } catch (e) {
      setPlanErr(e?.response?.data?.message || e?.message || "Failed to load routing plan");
    } finally {
      setPlanLoading(false);
    }
  };

  const subtitleText =
    err
      ? err
      : approxValue != null
        ? `Approx value: ${formatMoney(approxValue)}`
        : "Tip: click a price level in the orderbook to prefill";

  const planRows = plan?.quotes || plan?.ranked || [];
  const chosen = plan?.chosenExchange || plan?.chosen;

  const chosenRow = useMemo(() => {
    if (!chosen) return null;
    return planRows.find((r) => (r.exchangeCode || r.exchange) === chosen) || null;
  }, [planRows, chosen]);

  const totalFill = useMemo(() => {
    return planRows.reduce((sum, r) => {
      const n = Number(r.estimatedFillQty ?? r.fillQuantity ?? 0);
      return sum + (Number.isFinite(n) ? n : 0);
    }, 0);
  }, [planRows]);

  const routeModeLabel = routingMode === "AUTO" ? "AUTO (SOR)" : "MANUAL";
  const orderSummary = `${side} • ${type}${type === "LIMIT" && p != null ? ` @ ${formatMoney(p)}` : ""} • Qty ${q ?? "-"}`;

  return (
    <TableCard title="Order Entry" subtitle={subtitleText}>
      <div className="oe">
        <div className="oeRow">
          <div className="oeLabel">Instrument</div>
          <div className="oeValue mono">{instrument || "-"}</div>
        </div>

        {/* Side */}
        <div className="oeTabs">
          <button className={`oeTab ${side === "BUY" ? "oeTab--active" : ""}`} onClick={() => setSide("BUY")} type="button">
            Buy
          </button>
          <button className={`oeTab ${side === "SELL" ? "oeTab--active" : ""}`} onClick={() => setSide("SELL")} type="button">
            Sell
          </button>
        </div>

        {/* Type */}
        <div className="oeTabs">
          <button className={`oeTab ${type === "LIMIT" ? "oeTab--active" : ""}`} onClick={() => setType("LIMIT")} type="button">
            Limit
          </button>
          <button className={`oeTab ${type === "MARKET" ? "oeTab--active" : ""}`} onClick={() => setType("MARKET")} type="button">
            Market
          </button>
        </div>

        {/* Routing mode */}
        <div className="oeTabs">
          <button
            className={`oeTab ${routingMode === "AUTO" ? "oeTab--active" : ""}`}
            onClick={() => setRoutingMode("AUTO")}
            type="button"
          >
            AUTO (SOR)
          </button>
          <button
            className={`oeTab ${routingMode === "MANUAL" ? "oeTab--active" : ""}`}
            onClick={() => setRoutingMode("MANUAL")}
            type="button"
          >
            MANUAL
          </button>
        </div>

        <div className="oeForm">
          {type === "LIMIT" && (
            <label className="oeField">
              <span>Limit Price</span>
              <input
                className="input"
                value={price}
                onChange={(e) => setPrice(e.target.value)}
                placeholder="e.g. 110"
                inputMode="decimal"
              />
            </label>
          )}

          <label className="oeField">
            <span>Quantity</span>
            <input
              className="input"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              placeholder="e.g. 10"
              inputMode="numeric"
            />
          </label>

          {routingMode === "MANUAL" ? (
            <div className="oeField">
              <span>Exchange</span>
              <Select
                label={null}
                width={"100%"}
                value={exchangeCode}
                onChange={(v) => setExchangeCode(v)}
                options={EXCHANGES.map((x) => ({ label: x, value: x }))}
              />
            </div>
          ) : (
            <div className="oeField">
              <span>Smart Order Routing</span>
              <button
                className="ordersBtn ordersBtn--secondary"
                type="button"
                onClick={previewPlan}
                disabled={planLoading || busy}
              >
                {planLoading ? "Calculating…" : "Preview Routing Plan"}
              </button>
            </div>
          )}

          <button
            className={`ordersBtn ${side === "BUY" ? "ordersBtn--primary" : "ordersBtn--danger"}`}
            disabled={busy}
            onClick={submit}
            type="button"
          >
            {busy ? "Submitting…" : side === "BUY" ? "Place Buy Order" : "Place Sell Order"}
          </button>
        </div>

        {/* Plan panel */}
        {routingMode === "AUTO" && planOpen && (
          <div className="routePlan">
            <div className="routePlan__head">
              <div className="routePlan__title">Routing Plan</div>

              <div className="routePlan__chips">
                <span className="rpChip rpChip--muted">{routeModeLabel}</span>
                <span className="rpChip">{instrument || "-"}</span>
                <span className={`rpChip ${side === "BUY" ? "rpChip--buy" : "rpChip--sell"}`}>
                  {side}
                </span>
              </div>

              <button
                className="ordersBtn ordersBtn--secondary ordersBtn--sm"
                type="button"
                onClick={() => setPlanOpen(false)}
              >
                Close
              </button>
            </div>

            <div className="routePlan__sub">
              <div className="routePlan__summary">{orderSummary}</div>
              <div className="routePlan__hint">
                The router compares venues using estimated fill and effective price (fees + slippage).
              </div>
            </div>

            {planErr ? (
              <div className="routePlan__state routePlan__state--error">
                <div className="routePlan__stateTitle">Can’t build routing plan</div>
                <div className="routePlan__stateMsg">{String(planErr)}</div>
              </div>
            ) : planLoading ? (
              <div className="routePlan__state routePlan__state--loading">
                <div className="routePlan__spinner" />
                <div>
                  <div className="routePlan__stateTitle">Calculating best venue…</div>
                  <div className="routePlan__stateMsg">Fetching quotes and scoring exchanges.</div>
                </div>
              </div>
            ) : planRows.length === 0 ? (
              <div className="routePlan__state routePlan__state--empty">
                <div className="routePlan__stateTitle">No quotes</div>
                <div className="routePlan__stateMsg">The router returned zero candidate venues for this request.</div>
              </div>
            ) : (
              <>
                <div className="routePlan__cards">
                  <div className="rpCard">
                    <div className="rpCard__k">Chosen Venue</div>
                    <div className="rpCard__v mono">{chosen || "-"}</div>
                    <div className="rpCard__s">Top score after fees and fill</div>
                  </div>

                  <div className="rpCard">
                    <div className="rpCard__k">Total Est. Fill</div>
                    <div className="rpCard__v mono">{totalFill || 0}</div>
                    <div className="rpCard__s">Sum of venue estimates</div>
                  </div>

                  <div className="rpCard">
                    <div className="rpCard__k">Chosen Effective</div>
                    <div className="rpCard__v mono">
                      {chosenRow?.effectivePrice != null || chosenRow?.effectiveVwap != null
                        ? formatMoney(chosenRow?.effectivePrice ?? chosenRow?.effectiveVwap)
                        : "-"}
                    </div>
                    <div className="rpCard__s">After fees & scoring</div>
                  </div>
                </div>

                <div className="routePlan__tableWrap">
                  <table className="rpTable">
                    <thead>
                      <tr>
                        <th>Venue</th>
                        <th className="taR">Est. Fill</th>
                        <th className="taR">VWAP</th>
                        <th className="taR">Effective</th>
                        <th>Why</th>
                      </tr>
                    </thead>
                    <tbody>
                      {planRows.map((r, idx) => {
                        const ex = r.exchangeCode || r.exchange || "-";
                        const isChosen = chosen && ex === chosen;

                        const fill = r.estimatedFillQty ?? r.fillQuantity ?? null;
                        const vwap = r.vwap != null ? formatMoney(r.vwap) : "-";
                        const eff =
                          r.effectivePrice != null || r.effectiveVwap != null
                            ? formatMoney(r.effectivePrice ?? r.effectiveVwap)
                            : "-";

                        return (
                          <tr key={`${ex}-${idx}`} className={isChosen ? "is-chosen" : ""}>
                            <td className="mono">
                              <div className="rpVenue">
                                <span className="rpVenue__code">{ex}</span>
                                {isChosen ? <span className="rpBadge">Chosen</span> : null}
                              </div>
                            </td>
                            <td className="mono taR">{fill ?? "-"}</td>
                            <td className="mono taR">{vwap}</td>
                            <td className="mono taR">{eff}</td>
                            <td title={r.reason || ""}>
                              <span className="ellipsis">{r.reason || "-"}</span>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                <div className="routePlan__footer">
                  Tip: if the chosen venue surprises you, check the “Why” column (fees, maker/taker, touch price, or fill limit).
                </div>
              </>
            )}
          </div>
        )}
      </div>
    </TableCard>
  );
};

export default OrderEntryCard;