import React, { useMemo } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import "../../Dashboard/Tables/TableBase.scss";
import "./OrderBookPanel.scss";

import { formatMoney, formatNumber } from "../../../utils/formatter";

const OrderBookPanel = ({ instrument, book, loading, error, onPickPrice }) => {
  const bids = useMemo(() => (book?.bids || []).slice().sort((a, b) => (b.price ?? 0) - (a.price ?? 0)), [book]);
  const asks = useMemo(() => (book?.asks || []).slice().sort((a, b) => (a.price ?? 0) - (b.price ?? 0)), [book]);

  const bestBid = bids?.[0]?.price ?? null;
  const bestAsk = asks?.[0]?.price ?? null;
  const spread = bestBid != null && bestAsk != null ? bestAsk - bestBid : null;

  return (
    <TableCard
      title={`Orderbook — ${instrument || "-"}`}
      subtitle={
        loading ? "Loading…" : error ? String(error) : spread != null ? `Spread: ${formatMoney(spread)}` : ""
      }
    >
      <div className="obGrid">
        <div className="obSide">
          <div className="obSide__title">Bids</div>

          <div className="obTableWrap">
            <table className="obTable">
              <thead>
                <tr>
                  <th>Price</th>
                  <th>Qty</th>
                  <th>Remain</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr><td colSpan={3} className="obEmpty">Loading…</td></tr>
                ) : bids.length === 0 ? (
                  <tr><td colSpan={3} className="obEmpty">No bids</td></tr>
                ) : (
                  bids.map((r) => (
                    <tr key={r.id} className="obRow obRow--bid">
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

        <div className="obSide">
          <div className="obSide__title">Asks</div>

          <div className="obTableWrap">
            <table className="obTable">
              <thead>
                <tr>
                  <th>Price</th>
                  <th>Qty</th>
                  <th>Remain</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr><td colSpan={3} className="obEmpty">Loading…</td></tr>
                ) : asks.length === 0 ? (
                  <tr><td colSpan={3} className="obEmpty">No asks</td></tr>
                ) : (
                  asks.map((r) => (
                    <tr key={r.id} className="obRow obRow--ask">
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