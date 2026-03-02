import React, { useMemo } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import "../../Dashboard/Tables/TableBase.scss";
import "./OrderBookPanel.scss";

import { formatMoney, formatNumber } from "../../../utils/formatter";
const rowKey = (r, idx) => {
  // prefer stable id if you trust it
  if (r?.id != null) return String(r.id);

  // fallback: deterministic key
  const p = r?.price ?? "na";
  const q = r?.quantity ?? "na";
  const rem = r?.remainingQuantity ?? "na";
  const ex = r?.exchangeCode ?? r?.exchange ?? "na";
  const t = r?.createdAt ?? "na";

  return `${ex}|${p}|${q}|${rem}|${t}|${idx}`;
};

const levelKey = (lvl, idx) => {
  const p = lvl?.price ?? "na";
  const tq = lvl?.totalQuantity ?? lvl?.quantity ?? "na";
  return `${p}|${tq}|${idx}`;
};

function safeNum(x) {
  const n = Number(x);
  return Number.isFinite(n) ? n : null;
}
const OrderBookPanel = ({
  instrument,
  book,
  loading,
  error,
  onPickPrice,
  showLevels,
  onToggleLevels,
  refreshing,
}) => {
  const isLevelsMode = !!showLevels;

  const bids = useMemo(() => {
    const arr = (book?.bids || []).slice();
    arr.sort((a, b) => (safeNum(b.price) ?? 0) - (safeNum(a.price) ?? 0));
    return arr;
  }, [book]);

  const asks = useMemo(() => {
    const arr = (book?.asks || []).slice();
    arr.sort((a, b) => (safeNum(a.price) ?? 0) - (safeNum(b.price) ?? 0));
    return arr;
  }, [book]);

  const bidLevels = useMemo(() => {
    const arr = (book?.bidLevels || book?.bids || []).slice();
    arr.sort((a, b) => (safeNum(b.price) ?? 0) - (safeNum(a.price) ?? 0));
    return arr;
  }, [book]);

  const askLevels = useMemo(() => {
    const arr = (book?.askLevels || book?.asks || []).slice();
    arr.sort((a, b) => (safeNum(a.price) ?? 0) - (safeNum(b.price) ?? 0));;
    return arr;
  }, [book]);

  const bestBid = (isLevelsMode ? bidLevels : bids)?.[0]?.price ?? null;
  const bestAsk = (isLevelsMode ? askLevels : asks)?.[0]?.price ?? null;
  const spread = bestBid != null && bestAsk != null ? safeNum(bestAsk) - safeNum(bestBid) : null;
  const subtitle =
    loading
      ? "Loading…"
      : error
        ? String(error)
        : refreshing
          ? "Updating…"
          : spread != null
            ? `Spread: ${formatMoney(spread)}`
            : "";

  return (
    <TableCard
      title={`Orderbook — ${instrument || "-"}`}
      subtitle={subtitle}
      rightSlot={
        <label className="toggle toggle--inline">
          <input
            type="checkbox"
            checked={!!showLevels}
            onChange={(e) => onToggleLevels?.(e.target.checked)}
          />
          <span className="toggle__track" />
          <span className="toggle__text">Grouped</span>
        </label>
      }
    >
      <div className="obGrid">
        {/* BIDS */}
        <div className="obSide">
          <div className="obSide__title">Buy offers</div>

          <div className="obTableWrap">
            <table className="obTable">
              <thead>
                {isLevelsMode ? (
                  <tr>
                    <th>Price</th>
                    <th>Total</th>
                  </tr>
                ) : (
                  <tr>
                    <th>Price</th>
                    <th>Qty</th>
                    <th>Left</th>
                  </tr>
                )}
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={isLevelsMode ? 2 : 3} className="obEmpty">
                      Loading…
                    </td>
                  </tr>
                ) : (isLevelsMode ? bidLevels : bids).length === 0 ? (
                  <tr>
                    <td colSpan={isLevelsMode ? 2 : 3} className="obEmpty">
                        No Buy offers
                    </td>
                  </tr>
                ) : isLevelsMode ? (
                  bidLevels.map((r, idx) => (
                    <tr key={levelKey(r, idx)} className="obRow obRow--bid">
                      <td>
                        <button
                          className="obPriceBtn"
                          type="button"
                          onClick={() => onPickPrice?.("BUY", r.price)}
                          title="Click to prefill Buy price"
                        >
                          {r.price != null ? formatMoney(r.price) : "-"}
                        </button>
                      </td>
                      <td className="mono">
                        {r.totalQuantity != null
                          ? formatNumber(r.totalQuantity)
                          : r.quantity != null
                            ? formatNumber(r.quantity)
                            : "-"}
                      </td>
                    </tr>
                  ))
                ) : (
                  bids.map((r, idx) => (
                    <tr key={rowKey(r, idx)} className="obRow obRow--bid">
                      <td>
                        <button
                          className="obPriceBtn"
                          type="button"
                          onClick={() => onPickPrice?.("BUY", r.price)}
                          title="Click to prefill Buy price"
                        >
                          {r.price != null ? formatMoney(r.price) : "-"}
                        </button>
                      </td>
                      <td className="mono">{r.quantity != null ? formatNumber(r.quantity) : "-"}</td>
                      <td className="mono">{r.remainingQuantity != null ? formatNumber(r.remainingQuantity) : "-"}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* ASKS */}
        <div className="obSide">
          <div className="obSide__title">Sell offers</div>

          <div className="obTableWrap">
            <table className="obTable">
              <thead>
                {isLevelsMode ? (
                  <tr>
                    <th>Price</th>
                    <th>Total</th>
                  </tr>
                ) : (
                  <tr>
                    <th>Price</th>
                    <th>Qty</th>
                    <th>Remain</th>
                  </tr>
                )}
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={isLevelsMode ? 2 : 3} className="obEmpty">
                      Loading…
                    </td>
                  </tr>
                ) : (isLevelsMode ? askLevels : asks).length === 0 ? (
                  <tr>
                    <td colSpan={isLevelsMode ? 2 : 3} className="obEmpty">
                        No Sell offers
                    </td>
                  </tr>
                ) : isLevelsMode ? (
                  askLevels.map((r, idx) => (
                    <tr key={levelKey(r, idx)} className="obRow obRow--ask">
                      <td>
                        <button
                          className="obPriceBtn"
                          type="button"
                          onClick={() => onPickPrice?.("SELL", r.price)}
                          title="Click to prefill Sell price"
                        >
                          {r.price != null ? formatMoney(r.price) : "-"}
                        </button>
                      </td>
                      <td className="mono">
                        {r.totalQuantity != null
                          ? formatNumber(r.totalQuantity)
                          : r.quantity != null
                            ? formatNumber(r.quantity)
                            : "-"}
                      </td>
                    </tr>
                  ))
                ) : (
                  asks.map((r, idx) => (
                    <tr key={rowKey(r, idx)} className="obRow obRow--ask">
                      <td>
                        <button
                          className="obPriceBtn"
                          type="button"
                          onClick={() => onPickPrice?.("SELL", r.price)}
                          title="Click to prefill Sell price"
                        >
                          {r.price != null ? formatMoney(r.price) : "-"}
                        </button>
                      </td>
                      <td className="mono">{r.quantity != null ? formatNumber(r.quantity) : "-"}</td>
                      <td className="mono">{r.remainingQuantity != null ? formatNumber(r.remainingQuantity) : "-"}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </TableCard>
  );
};

export default OrderBookPanel;