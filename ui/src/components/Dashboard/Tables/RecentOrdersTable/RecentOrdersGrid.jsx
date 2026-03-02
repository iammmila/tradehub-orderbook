import React from "react";
import { formatDate, formatMoney, formatNumber, formatTime } from "../../../../utils/formatter";
import { statusBadgeClass } from "../../../../utils/recentOrdersUtils";

const RecentOrdersGrid = ({ rows }) => {
    return (
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
                {rows.map((o) => (
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
    );
};

export default RecentOrdersGrid;