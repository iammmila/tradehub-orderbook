import React, { useEffect, useMemo, useState, useCallback } from "react";
import TableCard from "../../Dashboard/Tables/TableCard/TableCard";
import TableFilters from "../../Dashboard/Tables/TableFilters/TableFilters";
import "../../Dashboard/Tables/TableBase.scss";
import "./TradesTable.scss";
import Select from "../../Dashboard/Tables/Select/Select";

import { fetchMyTradesPage } from "../../../api/trades";
import { fetchUserById } from "../../../api/users";

import { formatDate, formatMoney, formatNumber, formatTime } from "../../../utils/formatter";
import UserChip from "../UserChip/UserChip";

const TradesTable = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);

  // Filters
  const [instrument, setInstrument] = useState("");

  // Sorting (server-side like OrdersTable)
  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDir, setSortDir] = useState("desc");

  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);

  // userId -> username
  const [usernames, setUsernames] = useState({});

  const sortParam = useMemo(() => `${sortBy},${sortDir}`, [sortBy, sortDir]);

  const resolveUsernames = useCallback(
    async (trades) => {
      const ids = new Set();
      trades.forEach((t) => {
        if (t?.buyerUserId != null) ids.add(t.buyerUserId);
        if (t?.sellerUserId != null) ids.add(t.sellerUserId);
      });

      const missing = [...ids].filter((id) => usernames[id] == null);
      if (missing.length === 0) return;

      const updates = {};
      await Promise.all(
        missing.map(async (id) => {
          try {
            const u = await fetchUserById(id);
            updates[id] = u?.username || `User ${id}`;
          } catch {
            updates[id] = `User ${id}`;
          }
        })
      );

      setUsernames((prev) => ({ ...prev, ...updates }));
    },
    [usernames]
  );

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const data = await fetchMyTradesPage(page, size, sortParam, instrument || undefined);
      const content = data?.content || [];

      setRows(content);
      setTotalPages(data?.totalPages ?? 0);
      setLastUpdatedAt(new Date());

      await resolveUsernames(content);
    } catch (e) {
      setError(e?.response?.data?.message || e?.message || "Failed to load trades");
    } finally {
      setLoading(false);
    }
  }, [page, size, sortParam, instrument, resolveUsernames]);

  useEffect(() => {
    load();
  }, [load]);

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
    setSortBy("createdAt");
    setSortDir("desc");
    setPage(0);
  };

  const filteredRows = useMemo(() => {
    const q = instrument.trim().toUpperCase();
    let out = [...(rows || [])];

    if (q) {
      out = out.filter((t) => String(t.instrument || "").toUpperCase().includes(q));
    }
    return out;
  }, [rows, instrument]);

  return (
    <TableCard
      title="My Trades"
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
        onSearch={(v) => setInstrument(v)}
        placeholder="Search instrument"
        right={
          <div className="tradesTable__topActions">
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

              <th onClick={() => toggleSort("price")} className="thSortable">
                Price {sortBy === "price" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>

              <th onClick={() => toggleSort("quantity")} className="thSortable">
                Quantity {sortBy === "quantity" ? (sortDir === "asc" ? "↑" : "↓") : ""}
              </th>

              <th>Buyer</th>
              <th>Seller</th>
            </tr>
          </thead>

          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="table-empty">
                  Loading trades...
                </td>
              </tr>
            ) : filteredRows.length === 0 ? (
              <tr>
                <td colSpan={6} className="table-empty">
                  No trades found.
                </td>
              </tr>
            ) : (
              filteredRows.map((t) => {
                const timeStr = t.createdAt
                  ? `${formatDate(t.createdAt)} ${formatTime(t.createdAt)}`
                  : "-";
                return (
                  <tr key={t.id}>
                    <td>{timeStr}</td>
                    <td className="mono">{t.instrument || "-"}</td>
                    <td className="mono">{t.price != null ? formatMoney(t.price) : "-"}</td>
                    <td className="mono">{t.quantity != null ? formatNumber(t.quantity) : "-"}</td>
                    <td>
                      <UserChip
                        userId={t.buyerUserId}
                        username={usernames[t.buyerUserId]}
                      />
                    </td>
                    <td>
                      <UserChip
                        userId={t.sellerUserId}
                        username={usernames[t.sellerUserId]}
                      />
                    </td>
                  </tr>
                );
              })
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
    </TableCard>
  );
};

export default TradesTable;