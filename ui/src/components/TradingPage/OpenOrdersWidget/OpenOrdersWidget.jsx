import React, { useEffect, useState, useCallback } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import "../../Dashboard/Tables/TableBase.scss";
import "./OpenOrdersWidget.scss";

import { fetchOrdersPage, cancelOrder } from "../../../api/orders";
import { formatDate, formatTime, formatMoney, formatNumber } from "../../../utils/formatter";

const OpenOrdersWidget = ({ instrument, refreshKey, onChanged }) => {
  const [loading, setLoading] = useState(true);
  const [rows, setRows] = useState([]);
  const [err, setErr] = useState(null);

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setErr(null);

      const data = await fetchOrdersPage(0, 10, "createdAt,desc");

      const all = data?.content || [];
      const open = all.filter((o) => {
        const st = String(o.status || "").toUpperCase();
        const okStatus = st === "NEW" || st === "PARTIALLY_FILLED";
        const okInstr = instrument
          ? String(o.instrument || "").toUpperCase() === String(instrument).toUpperCase()
          : true;
        return okStatus && okInstr;
      });

      setRows(open);
    } catch (e) {
      setErr(e?.message || "Failed to load open orders");
    } finally {
      setLoading(false);
    }
  }, [instrument]);

  useEffect(() => {
    load();
  }, [load, instrument, refreshKey]);

  const cancel = async (id) => {
    try {
      await cancelOrder(id);
      setRows((prev) => prev.filter((x) => x.id !== id));
      onChanged?.();
    } catch (e) {
      setErr(e?.message || "Cancel failed");
    }
  };

  return (
    <TableCard
      title="Open Orders"
      subtitle={err ? err : "Your active orders for selected instrument"}
      right={
        <button className="ordersBtn ordersBtn--secondary ordersBtn--sm" onClick={load} type="button">
          Refresh
        </button>
      }
    >
      <div className="ooWrap">
        <table className="table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Side</th>
              <th>Price</th>
              <th>Remain</th>
              <th>Instrument</th>
              <th></th>
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5} className="table-empty">Loading…</td>
              </tr>
            ) : rows.length === 0 ? (
              <tr>
                <td colSpan={5} className="table-empty">No open orders.</td>
              </tr>
            ) : (
              rows.map((o) => (
                <tr key={o.id}>
                  <td>{o.createdAt ? `${formatDate(o.createdAt)} ${formatTime(o.createdAt)}` : "-"}</td>
                  <td className="mono">{String(o.side || "-").toUpperCase()}</td>
                  <td className="mono">{o.price != null ? formatMoney(o.price) : "-"}</td>
                  <td className="mono">{o.remainingQuantity != null ? formatNumber(o.remainingQuantity) : "-"}</td>
                  <td className="mono">{o.instrument || "-"}</td>
                  <td className="ooActions">
                    <button
                      className="ordersBtn ordersBtn--danger ordersBtn--sm"
                      onClick={() => cancel(o.id)}
                      type="button"
                    >
                      Cancel
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </TableCard>
  );
};

export default OpenOrdersWidget;