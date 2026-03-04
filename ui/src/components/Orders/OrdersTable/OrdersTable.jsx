import React, { useContext, useEffect, useMemo, useState } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import TableFilters from "../../Dashboard/Tables/TableFilters/TableFilters";
import "../../Dashboard/Tables/TableBase.scss";
import "./OrdersTable.scss";
import Select from "../../Dashboard/Tables/Select/Select";

import { fetchOrdersPage, cancelOrder } from "../../../api/orders";
import { formatDate, formatTime } from "../../../utils/formatter";

import CreateOrderModal from "../CreateOrderModal/CreateOrderModal";
import ReplaceOrderModal from "../ReplaceOrderModal/ReplaceOrderModal";
import ConfirmDialog from "../ConfirmDialog/ConfirmDialog";
import { useNavigate } from "react-router-dom";
import OrderRow from "./OrderRow/OrderRow"
import { MainContext } from "../../../context/ContextProvider";
const COLS_COUNT = 15;

const OrdersTable = () => {
  const navigate = useNavigate();
  const { user } = useContext(MainContext);
  const isUnverified = !!user && user.verified === false;
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
    setRoutingMode("");
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

      <div className="ordersTable__tableWrap">
        <table className="table ordersTable__table">
          <thead>
            <tr>
              <th onClick={() => toggleSort("createdAt")} className="thSortable colTime">
                Time {sortBy === "createdAt" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th onClick={() => toggleSort("instrument")} className="thSortable colInstrument">
                Instrument {sortBy === "instrument" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>
              <th className="colSide">Side</th>

              <th className="thSortable colPrice" onClick={() => toggleSort("price")}>
                Price {sortBy === "price" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>

              <th className="thSortable colQty" onClick={() => toggleSort("quantity")}>
                Quantity {sortBy === "quantity" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>

              <th className="colRemain">Remaining</th>
              <th className="colType">Type</th>
              <th className="colMinExec">MinExec</th>
              <th className="colExchange">Exchange</th>
              <th className="colStatus">Status</th>
              <th className="colRouting">Routing</th>
              <th className="colBy">By</th>
              <th className="colReason">Reason</th>
              <th className="thActions colActions">Actions</th>
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <tr>
                <td colSpan={COLS_COUNT} className="table-empty">
                  Loading orders...
                </td>
              </tr>
            ) : filteredRows.length === 0 ? (
              <tr>
                <td colSpan={COLS_COUNT} className="table-empty">
                  {isUnverified ? (
                    <div className="ordersEmptyState ordersEmptyState--warning">
                      <div className="ordersEmptyState__title">Verify your email to create orders</div>
                      <div className="ordersEmptyState__text">
                        Your account is not verified yet. Please verify your email, then login again.
                      </div>
                    </div>
                  ) : (
                    <div className="ordersEmptyState">
                      <div className="ordersEmptyState__title">No orders found</div>
                      <div className="ordersEmptyState__text">Create your first order to see it here.</div>
                    </div>
                  )}
                </td>
              </tr>
            ) : (
              filteredRows.map((r) => (
                <OrderRow
                  key={r.id}
                  r={r}
                  onOpenDetails={goDetails}
                  onReplace={setReplaceTarget}
                  onCancel={setCancelTarget}
                />)))}
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