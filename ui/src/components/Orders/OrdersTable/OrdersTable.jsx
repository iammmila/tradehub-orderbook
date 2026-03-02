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
import { useNavigate } from "react-router-dom";

function statusBadgeClass(status) {
  const s = String(status || "").toUpperCase();
  if (s === "FILLED") return "badge badge--filled";
  if (s === "CANCELLED") return "badge badge--cancelled";
  if (s === "PARTIALLY_FILLED") return "badge badge--partial";
  return "badge badge--new";
}
function exchangeBadgeClass(exchangeCode) {
  const s = String(exchangeCode || "").toUpperCase();
  if (s === "XLON") return "badge badge--partial";
  if (s === "XNAS") return "badge badge--cancelled";
  if (s === "XTKS") return "badge badge--filled";
  return "badge badge--new";
}
function routingBadgeClass(mode) {
  const m = String(mode || "").toUpperCase();
  if (m === "AUTO") return "badge badge--auto";
  if (m === "MANUAL") return "badge badge--manual";
  return "badge";
}

function routedByLabel(value) {
  const v = String(value || "").toUpperCase();
  if (v === "SOR") return "SOR";
  if (v === "USER") return "USER";
  return v || "-";
}

function shortText(s, max = 34) {
  const str = String(s || "");
  if (!str) return "-";
  if (str.length <= max) return str;
  return str.slice(0, max - 1) + "…";
}
function isCancellable(status) {
  const s = String(status || "").toUpperCase();
  return s === "NEW";
}

function isReplaceable(status) {
  const s = String(status || "").toUpperCase();
  return s === "NEW";
}

const OrdersTable = () => {
  const navigate = useNavigate();
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
  const [routingMode, setRoutingMode] = useState("");

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
  const goDetails = (id) => navigate(`/app/orders/${id}`);
  const load = async () => {
    try {
      setLoading(true);
      setError(null);

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

  const filteredRows = useMemo(() => {
    const q = instrument.trim().toUpperCase();
    let out = [...(rows || [])];

    if (q) {
      out = out.filter((o) =>
        String(o.instrument || "").toUpperCase().includes(q)
      );
    }

    if (side) out = out.filter((o) => String(o.side || "").toUpperCase() === String(side).toUpperCase());
    if (status) out = out.filter((o) => String(o.status || "").toUpperCase() === String(status).toUpperCase());
    if (routingMode) {
      out = out.filter((o) =>
        String(o.routingMode || "").toUpperCase() === String(routingMode).toUpperCase()
      );
    }
    return out;
  }, [rows, instrument, side, status, routingMode]);

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
            label: "Routing",
            value: routingMode,
            onChange: (v) => {
              setPage(0);
              setRoutingMode(v);
            },
            options: [
              { label: "All", value: "" },
              { label: "AUTO", value: "AUTO" },
              { label: "MANUAL", value: "MANUAL" },
            ],
            width: 160,
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
      {instrument.trim() && (
        <div className="ordersTable__note">
          Filtering instrument applies to the current page only.
        </div>
      )}
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
              <th>Exchange</th>
              <th>Status</th>
              <th>Routing</th>
              <th>By</th>
              <th>Reason</th>
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
                <tr
                  key={r.id}
                  className="clickRow"
                  role="button"
                  tabIndex={0}
                  onClick={() => goDetails(r.id)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") goDetails(r.id);
                  }}
                  title="Open order details"
                >
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
                    <span className={exchangeBadgeClass(r.exchangeCode)}>
                      {r.exchangeCode != null ? r.exchangeCode : "-"}
                    </span>
                  </td>
                  <td>
                    <span className={statusBadgeClass(r.status)}>{r.status}</span>
                  </td>
                  <td>
                    <span className={routingBadgeClass(r.routingMode)}>
                      {String(r.routingMode || "-").toUpperCase()}
                    </span>
                  </td>

                  <td className="muted">
                    {routedByLabel(r.routedBy)}
                  </td>

                  <td className="reasonCell" title={r.routeReason || ""}>
                    {shortText(r.routeReason, 42)}
                  </td>
                  <td className="actionsCell">
                    <button
                      className="ordersBtn ordersBtn--secondary ordersBtn--sm"
                      disabled={!isReplaceable(r.status)}
                      onClick={(e) => {
                        e.stopPropagation();
                        setReplaceTarget(r);
                      }}
                    >
                      Replace
                    </button>

                    <button
                      className="ordersBtn ordersBtn--danger ordersBtn--sm"
                      disabled={!isCancellable(r.status)}
                      onClick={(e) => {
                        e.stopPropagation();
                        setCancelTarget(r);
                      }}
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