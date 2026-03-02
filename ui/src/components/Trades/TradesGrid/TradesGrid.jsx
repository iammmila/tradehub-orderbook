import React from "react";
import { formatDate, formatMoney, formatNumber, formatTime } from "../../../utils/formatter";
import UserChip from "../UserChip/UserChip";

const sortArrow = (sortBy, sortDir, field) =>
    sortBy === field ? (sortDir === "asc" ? "↑" : "↓") : "";

const TradesGrid = ({ loading, rows, sortBy, sortDir, onSort, usernames }) => {
    const colCount = 7;

    return (
        <div className="table-wrap">
            <table className="table">
                <thead>
                    <tr>
                        <th onClick={() => onSort("createdAt")} className="thSortable">
                            Time {sortArrow(sortBy, sortDir, "createdAt")}
                        </th>
                        <th onClick={() => onSort("instrument")} className="thSortable">
                            Instrument {sortArrow(sortBy, sortDir, "instrument")}
                        </th>
                        <th onClick={() => onSort("price")} className="thSortable">
                            Price {sortArrow(sortBy, sortDir, "price")}
                        </th>
                        <th onClick={() => onSort("quantity")} className="thSortable">
                            Quantity {sortArrow(sortBy, sortDir, "quantity")}
                        </th>

                        {/* Make sortable only if backend supports it */}
                        <th>Exchange</th>

                        <th>Buyer</th>
                        <th>Seller</th>
                    </tr>
                </thead>

                <tbody>
                    {loading ? (
                        <tr>
                            <td colSpan={colCount} className="table-empty">Loading trades...</td>
                        </tr>
                    ) : (rows?.length ?? 0) === 0 ? (
                        <tr>
                            <td colSpan={colCount} className="table-empty">No trades found.</td>
                        </tr>
                    ) : (
                        rows.map((t) => {
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
                                        <span className="badge badge--venue">{t.exchangeCode || "-"}</span>
                                    </td>
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
    );
};

export default TradesGrid;