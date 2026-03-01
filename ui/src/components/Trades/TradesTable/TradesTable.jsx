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

  const [pageInfo, setPageInfo] = useState({
    number: 0,
    totalElements: 0,
  });

  const [search, setSearch] = useState("");         
  const [instrumentApi, setInstrumentApi] = useState("");

  const [sortBy, setSortBy] = useState("createdAt");
  const [sortDir, setSortDir] = useState("desc");

  const [lastUpdatedAt, setLastUpdatedAt] = useState(null);
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

      const data = await fetchMyTradesPage(page, size, sortParam, instrumentApi || undefined);

      const content = data?.content || [];
      const p = data?.page || {};

      setRows(content);
      setPageInfo({
        number: p?.number ?? page,
        totalElements: p?.totalElements ?? 0,
      });

      setLastUpdatedAt(new Date());
      await resolveUsernames(content);
    } catch (e) {
      setError(e?.response?.data?.message || e?.message || "Failed to load trades");
    } finally {
      setLoading(false);
    }
  }, [page, size, sortParam, instrumentApi, resolveUsernames]);

  useEffect(() => {
    load();
  }, [load]);

  const toggleSort = (field) => {
    setPage(0);
    setSortBy((prev) => {
      if (prev !== field) {
        setSortDir("desc");
        return field;
      }
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
      return prev;
    });
  };

  const resetFilters = () => {
    setSearch("");
    setInstrumentApi("");
    setSortBy("createdAt");
    setSortDir("desc");
    setPage(0);
  };

  const filteredRows = useMemo(() => {
    const q = search.trim().toUpperCase();
    let out = [...(rows || [])];

    if (q) {
      out = out.filter((t) =>
        String(t.instrument || "").toUpperCase().includes(q)
      );
    }

    const dir = sortDir === "asc" ? 1 : -1;

    out.sort((a, b) => {
      const av = a?.[sortBy];
      const bv = b?.[sortBy];

      if (av == null && bv == null) return 0;
      if (av == null) return 1;
      if (bv == null) return -1;

      if (sortBy === "createdAt") {
        const ad = new Date(av).getTime();
        const bd = new Date(bv).getTime();
        return (ad - bd) * dir;
      }

      if (typeof av === "number" && typeof bv === "number") {
        return (av - bv) * dir;
      }

      const as = String(av).toUpperCase();
      const bs = String(bv).toUpperCase();
      if (as < bs) return -1 * dir;
      if (as > bs) return 1 * dir;
      return 0;
    });

    return out;
  }, [rows, search, sortBy, sortDir]);

  const totalElements = pageInfo.totalElements ?? 0;
  const lastPageIndex = totalElements > 0 ? Math.max(0, Math.ceil(totalElements / size) - 1) : 0;
  const isFirstPage = page <= 0;
  const isLastPage = totalElements > 0 ? page >= lastPageIndex : (rows?.length ?? 0) < size;

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
        search={search}
        onSearch={(v) => setSearch(v)}
        placeholder="Search instrument (client-side)…"
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
                      <UserChip userId={t.buyerUserId} username={usernames[t.buyerUserId]} />
                    </td>
                    <td>
                      <UserChip userId={t.sellerUserId} username={usernames[t.sellerUserId]} />
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
            disabled={loading || isFirstPage}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            Prev
          </button>

          <div className="ordersTable__pageInfo">
            Page <b>{page + 1}</b> / <b>{Math.max(lastPageIndex + 1, 1)}</b>
          </div>

          <button
            className="ordersBtn ordersBtn--secondary"
            disabled={loading || isLastPage}
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