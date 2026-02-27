import React, { useEffect, useMemo, useState } from 'react'
import "../TableBase.scss"
import "./RecentOrdersTable.scss"
import TableCard from '../TableCard/TableCard';
import TableFilters from '../TableFilters/TableFilters';
import { fetchOrdersPage } from '../../../../api/orders';
import { formatDate, formatMoney, formatNumber, formatTime } from '../../../../utils/formatter';
function statusBadgeClass(status) {
  const s = String(status || "").toUpperCase();
  if (s === "FILLED") return "badge badge--filled";
  if (s === "CANCELLED") return "badge badge--cancelled";
  return "badge badge--new"; // NEW default
}
function compare(a, b) {
  if (a < b) return -1;
  if (a > b) return 1;
  return 0;
}

const RecentOrdersTable = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [rows, setRows] = useState([]);

  // UI filters
  const [search, setSearch] = useState("");
  const [side, setSide] = useState("ALL");
  const [status, setStatus] = useState("ALL");
  const [sort, setSort] = useState("NEWEST");

  useEffect(() => {
    let alive = true;

    (async () => {
      try {
        setLoading(true);
        const page = await fetchOrdersPage(0, 10, "createdAt,desc");
        if (!alive) return;

        setRows(page?.content || []);
        setError(null);
      } catch (e) {
        if (!alive) return;
        const msg = e?.response
          ? `HTTP ${e.response.status} - ${JSON.stringify(e.response.data)}`
          : (e?.message || "error");
        setError(msg);
      } finally {
        if (alive) setLoading(false);
      }
    })();

    return () => { alive = false; };
  }, []);

  const filtered = useMemo(() => {
    const q = search.trim().toUpperCase();

    let out = [...(rows || [])];

    if (q) out = out.filter((o) => String(o.instrument || "").toUpperCase().includes(q));
    if (side !== "ALL") out = out.filter((o) => o.side === side);
    if (status !== "ALL") out = out.filter((o) => String(o.status || "").toUpperCase() === status);

    // sorting
    out.sort((a, b) => {
      if (sort === "NEWEST") return compare(new Date(b.createdAt).getTime(), new Date(a.createdAt).getTime());
      if (sort === "OLDEST") return compare(new Date(a.createdAt).getTime(), new Date(b.createdAt).getTime());
      if (sort === "PRICE_DESC") return compare(Number(b.price), Number(a.price));
      if (sort === "PRICE_ASC") return compare(Number(a.price), Number(b.price));
      if (sort === "QTY_DESC") return compare(Number(b.quantity), Number(a.quantity));
      if (sort === "QTY_ASC") return compare(Number(a.quantity), Number(b.quantity));
      return 0;
    });

    // keep dashboard table compact
    return out.slice(0, 10);
  }, [rows, search, side, status, sort]);

  const rightSlot = (
    <div className="table-actions">
      <span className="table-pill">{loading ? "Loading..." : `${rows.length} rows`}</span>
      <button className="table-link" type="button" onClick={() => window.location.assign("/app/orders")}>
        View all
      </button>
    </div>
  );

  return (
    <TableCard title="Recent Orders" subtitle="Latest 10 orders" rightSlot={rightSlot}>
      <TableFilters
        search={search}
        onSearch={setSearch}
        selects={[
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
          <button
            className="table-reset"
            type="button"
            onClick={() => {
              setSearch("");
              setSide("ALL");
              setStatus("ALL");
              setSort("NEWEST");
            }}
          >
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
          <table className="table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Instrument</th>
                <th>Side</th>
                <th>Price</th>
                <th>Quantity</th>
                <th>Remaining</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {filtered.map((o) => (
                <tr key={o.id}>
                  <td>
                    <div className="t-strong">{formatTime(o.createdAt)}</div>
                    <div className="t-muted">{formatDate(o.createdAt)}</div>
                  </td>
                  <td className="t-strong">{o.instrument}</td>
                  <td>
                    <span className={`badge ${o.side === "BUY" ? "badge--buy" : "badge--sell"}`}>{o.side}</span>
                  </td>
                  <td>{formatMoney(o.price)}</td>
                  <td>{formatNumber(o.quantity, 0)}</td>
                  <td>{formatNumber(o.remainingQuantity, 0)}</td>
                  <td>
                    <span className={statusBadgeClass(o.status)}>{o.status}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </TableCard>
  );
}
export default RecentOrdersTable