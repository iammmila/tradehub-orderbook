import React, { useEffect, useState,  useContext, useMemo } from 'react'
import './RecentTradesTable.scss'
import TableCard from '../TableCard/TableCard';
import TableFilters from '../TableFilters/TableFilters';
import { fetchMyTradesPage } from '../../../../api/trades';
import { formatDate, formatMoney, formatNumber, formatTime } from '../../../../utils/formatter';
import "../TableBase.scss"
import { MainContext } from "../../../../context/ContextProvider";
function compare(a, b) {
    if (a < b) return -1;
    if (a > b) return 1;
    return 0;
}
const RecentTradesTable = () => {
    const { user } = useContext(MainContext);
    const myUserId = user?.id; 
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [rows, setRows] = useState([]);

    const [search, setSearch] = useState("");
    const [role, setRole] = useState("ANY"); // ANY | BUYER | SELLER
    const [sort, setSort] = useState("NEWEST");
    useEffect(() => {
        let alive = true;

        (async () => {
            try {
                setLoading(true);

                // fetch more so dashboard filters feel useful
                const page = await fetchMyTradesPage(0, 50, "createdAt,desc");
                if (!alive) return;

                setRows(page?.content || []);
                setError(null);
            } catch (e) {
                if (!alive) return;

                const msg = e?.response
                    ? `HTTP ${e.response.status} - ${JSON.stringify(e.response.data)}`
                    : e?.message || "error";

                setError(msg);
            } finally {
                if (alive) setLoading(false);
            }
        })();

        return () => {
            alive = false;
        };
    }, []);

    const filtered = useMemo(() => {
        const q = search.trim().toUpperCase();

        let out = [...(rows || [])];

        if (q) {
            out = out.filter((t) => String(t.instrument || "").toUpperCase().includes(q));
        }

        // Role filter (only if we know current user id)
        if (myUserId && role !== "ANY") {
            if (role === "BUYER") out = out.filter((t) => Number(t.buyerUserId) === Number(myUserId));
            if (role === "SELLER") out = out.filter((t) => Number(t.sellerUserId) === Number(myUserId));
        }

        // Sort
        out.sort((a, b) => {
            const totalA = Number(a.price) * Number(a.quantity);
            const totalB = Number(b.price) * Number(b.quantity);

            if (sort === "NEWEST") return compare(new Date(b.createdAt).getTime(), new Date(a.createdAt).getTime());
            if (sort === "OLDEST") return compare(new Date(a.createdAt).getTime(), new Date(b.createdAt).getTime());
            if (sort === "PRICE_DESC") return compare(Number(b.price), Number(a.price));
            if (sort === "PRICE_ASC") return compare(Number(a.price), Number(b.price));
            if (sort === "QTY_DESC") return compare(Number(b.quantity), Number(a.quantity));
            if (sort === "QTY_ASC") return compare(Number(a.quantity), Number(b.quantity));
            if (sort === "TOTAL_DESC") return compare(totalB, totalA);
            if (sort === "TOTAL_ASC") return compare(totalA, totalB);

            return 0;
        });

        // Keep dashboard compact
        return out.slice(0, 10);
    }, [rows, search, role, sort, myUserId]);

    const rightSlot = (
        <div className="table-actions">
            <span className="table-pill">{loading ? "Loading..." : `${filtered.length} shown`}</span>
            <button className="table-link" type="button" onClick={() => window.location.assign("/app/trades")}>
                View all
            </button>
        </div>
    );

    return (
        <TableCard title="Recent Trades" subtitle="Quick filters + newest first" rightSlot={rightSlot}>
            <TableFilters
                search={search}
                onSearch={setSearch}
                selects={[
                    {
                        label: "Role",
                        value: role,
                        onChange: setRole,
                        width: 150,
                        options: [
                            { value: "ANY", label: "Any" },
                            { value: "BUYER", label: "Buyer" },
                            { value: "SELLER", label: "Seller" },
                        ],
                    },
                    {
                        label: "Sort",
                        value: sort,
                        onChange: setSort,
                        width: 170,
                        options: [
                            { value: "NEWEST", label: "Newest" },
                            { value: "OLDEST", label: "Oldest" },
                            { value: "PRICE_DESC", label: "Price ↓" },
                            { value: "PRICE_ASC", label: "Price ↑" },
                            { value: "QTY_DESC", label: "Quantity ↓" },
                            { value: "QTY_ASC", label: "Quantity ↑" },
                            { value: "TOTAL_DESC", label: "Total ↓" },
                            { value: "TOTAL_ASC", label: "Total ↑" },
                        ],
                    },
                ]}
                right={
                    <button
                        className="table-reset"
                        type="button"
                        onClick={() => {
                            setSearch("");
                            setRole("ANY");
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
                                <th>Price</th>
                                <th>Quantity</th>
                                <th>Total</th>
                                <th>Orders</th>
                            </tr>
                        </thead>

                        <tbody>
                            {filtered.map((t) => {
                                const total = Number(t.price) * Number(t.quantity);

                                return (
                                    <tr key={t.id}>
                                        <td>
                                            <div className="t-strong">{formatTime(t.createdAt)}</div>
                                            <div className="t-muted">{formatDate(t.createdAt)}</div>
                                        </td>
                                        <td className="t-strong">{t.instrument}</td>
                                        <td>{formatMoney(t.price)}</td>
                                        <td>{formatNumber(t.quantity, 0)}</td>
                                        <td className="t-strong">{formatMoney(total)}</td>
                                        <td className="t-muted">
                                            {t.buyOrderId} / {t.sellOrderId}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                )}
            </div>
        </TableCard>
    );
}
export default RecentTradesTable