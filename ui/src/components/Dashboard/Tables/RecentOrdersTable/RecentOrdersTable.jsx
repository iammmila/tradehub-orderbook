import React, { useMemo, useState } from 'react'
import "../TableBase.scss"
import "./RecentOrdersTable.scss"
import TableCard from '../TableCard/TableCard';
import TableFilters from '../TableFilters/TableFilters';
import { useNavigate } from 'react-router-dom';
import RecentOrdersGrid from './RecentOrdersGrid';
import { useRecentOrders } from '../../../../hooks/useRecentOrders';
import { compare, isSameLocalDay } from "../../../../utils/recentOrdersUtils";

const RecentOrdersTable = () => {
  const navigate = useNavigate();
  const { loading, error, rows, reload } = useRecentOrders();

  // UI filters
  const [search, setSearch] = useState("");
  const [side, setSide] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [sort, setSort] = useState("NEWEST");

  // NEW: Range filter
  const [range, setRange] = useState("ALL"); // TODAY | ALL

  const filtered = useMemo(() => {
    const q = search.trim().toUpperCase();
    let out = [...(rows || [])];

    // Range
    if (range === "TODAY") {
      const now = new Date();
      out = out.filter((o) => isSameLocalDay(o.createdAt, now));
    }

    // Filters
    if (q) out = out.filter((o) => String(o.instrument || "").toUpperCase().includes(q));
    if (side !== "ALL") out = out.filter((o) => o.side === side);
    if (status !== "ALL") out = out.filter((o) => String(o.status || "").toUpperCase() === status);

    // Sorting
    out.sort((a, b) => {
      if (sort === "NEWEST") return compare(new Date(b.createdAt).getTime(), new Date(a.createdAt).getTime());
      if (sort === "OLDEST") return compare(new Date(a.createdAt).getTime(), new Date(b.createdAt).getTime());
      if (sort === "PRICE_DESC") return compare(Number(b.price), Number(a.price));
      if (sort === "PRICE_ASC") return compare(Number(a.price), Number(b.price));
      if (sort === "QTY_DESC") return compare(Number(b.quantity), Number(a.quantity));
      if (sort === "QTY_ASC") return compare(Number(a.quantity), Number(b.quantity));
      return 0;
    });

    // Dashboard: keep compact
    return out.slice(0, 10);
  }, [rows, search, side, status, sort, range]);

  const reset = () => {
    setSearch("");
    setSide("ALL");
    setStatus("ALL");
    setSort("NEWEST");
    setRange("ALL");
  };
  const rightSlot = (
    <div className="table-actions">
      <span className="table-pill">{loading ? "Loading..." : `${rows.length} rows`}</span>
      <button className="table-link" type="button" onClick={() => navigate("/app/orders")}>
        View all
      </button>
      <button className="table-link" type="button" onClick={reload}>
        Refresh
      </button>
    </div>
  );

  return (
    <TableCard title="Recent Orders" subtitle="Latest orders snapshot" rightSlot={rightSlot}>
      <TableFilters
        search={search}
        onSearch={setSearch}
        selects={[
          {
            label: "Time",
            value: range,
            onChange: setRange,
            options: [
              { value: "ALL", label: "All" },
              { value: "TODAY", label: "Today" },
            ],
          },
          {
            label: "Side",
            value: side,
            onChange: setSide,
            options: [
              { value: "ALL", label: "All" },
              { value: "BUY", label: "BUY" },
              { value: "SELL", label: "SELL" },
            ],
          },
          {
            label: "Status",
            value: status,
            onChange: setStatus,
            options: [
              { value: "ALL", label: "All" },
              { value: "NEW", label: "NEW" },
              { value: "FILLED", label: "FILLED" },
              { value: "CANCELLED", label: "CANCELLED" },
            ],
          },
          {
            label: "Sort",
            value: sort,
            onChange: setSort,
            options: [
              { value: "NEWEST", label: "Newest" },
              { value: "OLDEST", label: "Oldest" },
              { value: "PRICE_DESC", label: "Price ↓" },
              { value: "PRICE_ASC", label: "Price ↑" },
              { value: "QTY_DESC", label: "Quantity ↓" },
              { value: "QTY_ASC", label: "Quantity ↑" },
            ],
          },
        ]}
        right={
          <button className="table-reset" type="button" onClick={reset}>
            Reset
          </button>
        }
      />

      <div className="table-wrap">
        {error ? (
          <div className="table-error">{error}</div>
        ) : loading ? (
          <div className="table-empty">Loading...</div>
        ) : filtered.length === 0 ? (
          <div className="table-empty">No results. Try clearing filters.</div>
        ) : (
          <RecentOrdersGrid rows={filtered} />
        )}
      </div>
    </TableCard>
  );
};
export default RecentOrdersTable;