import React, { useEffect, useMemo, useState } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import TableFilters from "../../Dashboard/Tables/TableFilters/TableFilters";
import "../../Dashboard/Tables/TableBase.scss";
import "./OrdersTable.scss";
import Select from "../../Dashboard/Tables/Select/Select";

import { fetchOrdersPage, cancelOrder } from "../../../api/orders";
import { formatDate, formatMoney, formatNumber, formatTime } from "../../../utils/formatter";

import CreateOrderModal from "../CreateOrderModal/CreateOrderModal";
import ReplaceOrderModal from "../ReplaceOrderModal/ReplaceOrderModal";
import ConfirmDialog from "../ConfirmDialog/ConfirmDialog";

function statusBadgeClass(status) {
  const s = String(status || "").toUpperCase();
  if (s === "FILLED") return "badge badge--filled";
  if (s === "CANCELLED") return "badge badge--cancelled";
  if (s === "PARTIALLY_FILLED") return "badge badge--partial";
  return "badge badge--new";
}

function isCancellable(status) {
  const s = String(status || "").toUpperCase();
  return s === "NEW" || s === "PARTIALLY_FILLED";
}

function isReplaceable(status) {
  const s = String(status || "").toUpperCase();
  return s === "NEW" || s === "PARTIALLY_FILLED";
}

const OrdersTable = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);

  // Filters (client-side like RecentOrdersTable)
  const [instrument, setInstrument] = useState("");
  const [side, setSide] = useState("");     // "" means All
  const [status, setStatus] = useState(""); // "" means All

  // Sorting (server-side kept as you had)
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDir, setSortDir] = useState("desc");

  // Modals
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [replaceTarget, setReplaceTarget] = useState(null);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [actionBusy, setActionBusy] = useState(false);

  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  const sortParam = useMemo(() => `${sortBy},${sortDir}`, [sortBy, sortDir]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);

      // ✅ IMPORTANT: instrument is NOT sent to API anymore
      // we filter instrument on client-side like RecentOrdersTable
      const data = await fetchOrdersPage(
        page,
        size,
        sortParam,
        undefined,
        side || undefined,
        status || undefined
      );

      setRows(data?.content || []);
      setTotalPages(data?.totalPages ?? 0);
      setLastUpdatedAt(new Date());
    } catch (e) {
      setError(e?.message || "Failed to load orders");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, side, status, sortParam]);

  // ✅ Client-side instrument filter same as RecentOrdersTable
  const filteredRows = useMemo(() => {
    const q = instrument.trim().toUpperCase();
    let out = [...(rows || [])];

    if (q) {
      out = out.filter((o) =>
        String(o.instrument || "").toUpperCase().includes(q)
      );
    }

    // side/status already filtered server-side, but keeping safe:
    if (side) out = out.filter((o) => String(o.side || "").toUpperCase() === String(side).toUpperCase());
    if (status) out = out.filter((o) => String(o.status || "").toUpperCase() === String(status).toUpperCase());

    return out;
  }, [rows, instrument, side, status]);

  const toggleSort = (field) => {
    if (sortBy !== field) {
      setSortBy(field);
      setSortDir("asc");
      setPage(0);
      return;
    }
    setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    setPage(0);
  };

  const resetFilters = () => {
    setInstrument("");
    setSide("");
    setStatus("");
    setSortBy("createdAt");
    setSortDir("desc");
    setPage(0);
  };

  const onCancelConfirm = async () => {
    if (!cancelTarget) return;
    try {
      setActionBusy(true);
      await cancelOrder(cancelTarget.id);

      setRows((prev) =>
        prev.map((r) =>
          r.id === cancelTarget.id ? { ...r, status: "CANCELLED" } : r
        )
      );

      setCancelTarget(null);
    } catch (e) {
      setError(e?.message || "Cancel failed");
    } finally {
      setActionBusy(false);
    }
  };

  return (
    <TableCard
      title="My Orders"
      subtitle={
        loading
          ? "Loading..."
          : error
            ? "Error"
            : lastUpdatedAt
              ? `Last updated: ${formatDate(lastUpdatedAt)} ${formatTime(lastUpdatedAt)}`
              : ""
      }
    >
      <TableFilters
        search={instrument}
        onSearch={(v) => {
          setInstrument(v);
          // ✅ do NOT reset page here (client-side filter)
          // if you want: setPage(0); (optional)
        }}
        selects={[
          {
            label: "Side",
            value: side,
            onChange: (v) => {
              setPage(0);
              setSide(v);
            },
            options: [
              { label: "All", value: "" },
              { label: "BUY", value: "BUY" },
              { label: "SELL", value: "SELL" },
            ],
            width: 140,
          },
          {
            label: "Status",
            value: status,
            onChange: (v) => {
              setPage(0);
              setStatus(v);
            },
            options: [
              { label: "All", value: "" },
              { label: "New", value: "NEW" },
              { label: "Partially filled", value: "PARTIALLY_FILLED" },
              { label: "Filled", value: "FILLED" },
              { label: "Cancelled", value: "CANCELLED" },
            ],
            width: 180,
          },
        ]}
        right={
          <div className="ordersTable__topActions">
            <button className="ordersBtn ordersBtn--primary" onClick={() => setIsCreateOpen(true)}>
              Create
            </button>
            <button className="ordersBtn ordersBtn--secondary" onClick={resetFilters}>
              Reset
            </button>
            <button className="ordersBtn ordersBtn--secondary" onClick={load}>
              Refresh
            </button>
          </div>
        }
      />

      {error && (
        <div className="table-error">
          <div>{error}</div>
          <button className="ordersBtn ordersBtn--secondary" onClick={load}>
            Retry
          </button>
        </div>
      )}

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th onClick={() => toggleSort("createdAt")} className="thSortable">
                Time {sortBy === "createdAt" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th onClick={() => toggleSort("instrument")} className="thSortable">
                Instrument {sortBy === "instrument" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th>Side</th>
              <th onClick={() => toggleSort("price")} className="thSortable">
                Price {sortBy === "price" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th onClick={() => toggleSort("quantity")} className="thSortable">
                Quantity {sortBy === "quantity" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th>Remaining</th>
              <th>Status</th>
              <th className="thActions">Actions</th>
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} className="table-empty">
                  Loading orders...
                </td>
              </tr>
            ) : filteredRows.length === 0 ? (
              <tr>
                <td colSpan={8} className="table-empty">
                  No orders found.
                </td>
              </tr>
            ) : (
              filteredRows.map((r) => (
                <tr key={r.id}>
                  <td>
                    {r.createdAt ? `${formatDate(r.createdAt)} ${formatTime(r.createdAt)}` : "-"}
                  </td>
                  <td className="mono">{r.instrument || "-"}</td>
                  <td className={String(r.side).toUpperCase() === "BUY" ? "sideBuy" : "sideSell"}>
                    {String(r.side || "-").toUpperCase()}
                  </td>
                  <td className="mono">{r.price != null ? formatMoney(r.price) : "-"}</td>
                  <td className="mono">{r.quantity != null ? formatNumber(r.quantity) : "-"}</td>
                  <td className="mono">{r.remainingQuantity != null ? formatNumber(r.remainingQuantity) : "-"}</td>
                  <td>
                    <span className={statusBadgeClass(r.status)}>{r.status}</span>
                  </td>
                  <td className="actionsCell">
                    <button
                      className="ordersBtn ordersBtn--secondary ordersBtn--sm"
                      disabled={!isReplaceable(r.status)}
                      onClick={() => setReplaceTarget(r)}
                    >
                      Replace
                    </button>

                    <button
                      className="ordersBtn ordersBtn--danger ordersBtn--sm"
                      disabled={!isCancellable(r.status)}
                      onClick={() => setCancelTarget(r)}
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

      <div className="ordersTable__footer">
        <div className="ordersTable__pager">
          <button
            className="ordersBtn ordersBtn--secondary"
            disabled={page <= 0}
            onClick={() => setPage((p) => p - 1)}
          >
            Prev
          </button>

          <div className="ordersTable__pageInfo">
            Page <b>{page + 1}</b> / <b>{Math.max(totalPages, 1)}</b>
          </div>

          <button
            className="ordersBtn ordersBtn--secondary"
            disabled={totalPages === 0 || page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </button>
        </div>

        <div className="ordersTable__pageSize">
          <Select
            label="Rows"
            value={String(size)}
            onChange={(v) => {
              setPage(0);
              setSize(Number(v));
            }}
            width={110}
            options={[
              { label: "10", value: "10" },
              { label: "20", value: "20" },
              { label: "50", value: "50" },
            ]}
          />
        </div>
      </div>

      <CreateOrderModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onCreated={() => {
          setIsCreateOpen(false);
          setPage(0);
          load();
        }}
      />

      <ReplaceOrderModal
        order={replaceTarget}
        isOpen={!!replaceTarget}
        onClose={() => setReplaceTarget(null)}
        onReplaced={() => {
          setReplaceTarget(null);
          load();
        }}
      />

      <ConfirmDialog
        isOpen={!!cancelTarget}
        title="Cancel order?"
        description={
          cancelTarget
            ? `Do you wanna Cancel this order  --- ${cancelTarget.instrument} ---${cancelTarget.side}?`
            : ""
        }
        confirmText={actionBusy ? "Cancelling..." : "Cancel order"}
        cancelText="Back"
        confirmVariant="danger"
        disabled={actionBusy}
        onConfirm={onCancelConfirm}
        onClose={() => {
          if (!actionBusy) setCancelTarget(null);
        }}
      />
    </TableCard>
  );
};

export default OrdersTable;