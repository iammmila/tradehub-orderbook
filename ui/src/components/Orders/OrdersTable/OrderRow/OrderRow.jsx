import React from "react";
import { formatDate, formatMoney, formatNumber, formatTime } from "../../../../utils/formatter";
import {
    exchangeBadgeClass,
    isCancellable,
    isReplaceable,
    isMinExecType,
    routedByLabel,
    routingBadgeClass,
    shortText,
    sideClass,
    statusBadgeClass,
    typeBadgeClass,
} from "../../../../utils/orderBadges";

const OrderRow = ({ r, onOpenDetails, onReplace, onCancel }) => {
    const type = String(r.type || "-").toUpperCase();
    const rawType = String(r.type || "").toUpperCase();
    const typeLabel = rawType === "MIN_EXECUTION_SIZE"
        ? "Min. Exec. Size"
        : rawType || "-";
    return (
        <tr
            key={r.id}
            className="clickRow"
            role="button"
            tabIndex={0}
            onClick={() => onOpenDetails(r.id)}
            onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") onOpenDetails(r.id);
            }}
            title="Open order details"
        >
            <td className="colTime">
                {r.createdAt ? `${formatDate(r.createdAt)} ${formatTime(r.createdAt)}` : "-"}
            </td>

            <td className="colInstrument mono">{r.instrument || "-"}</td>

            <td className="colSide">
                <span className={sideClass(r.side)}>{String(r.side || "-").toUpperCase()}</span>
            </td>

            <td className=" colPrice  mono">{r.price != null ? formatMoney(r.price) : "-"}</td>

            <td className="colQty  mono">{r.quantity != null ? formatNumber(r.quantity) : "-"}</td>

            <td className=" colRemain  mono">{r.remainingQuantity != null ? formatNumber(r.remainingQuantity) : "-"}</td>

            {/* NEW: Type */}
            <td className="colType">
                <span className={typeBadgeClass(type)}>{typeLabel}</span>
            </td>

            {/* NEW: MinExec (only for MIN_EXECUTION_SIZE) */}
            <td className="colMinExec  mono">
                {isMinExecType(type) ? (r.minExecSize != null ? formatNumber(r.minExecSize) : "-") : "—"}
            </td>

            <td>
                <span className={`colExchange ${exchangeBadgeClass(r.exchangeCode)}`}>
                    {r.exchangeCode != null ? r.exchangeCode : "-"}
                </span>
            </td>

            <td>
                <span className={`colStatus ${statusBadgeClass(r.status)}`}>{String(r.status || "-").toUpperCase()}</span>
            </td>

            <td>
                <span className={`colRouting ${routingBadgeClass(r.routingMode)}`}>
                    {String(r.routingMode || "-").toUpperCase()}
                </span>
            </td>

            <td className="colBy  muted">{routedByLabel(r.routedBy)}</td>

            <td className="colReason reasonCell" title={r.routeReason || ""}>
                {shortText(r.routeReason, 42)}
            </td>

            <td className="colActions  actionsCell">
                <button
                    className="ordersBtn ordersBtn--secondary ordersBtn--sm"
                    disabled={!isReplaceable(r.status)}
                    onClick={(e) => {
                        e.stopPropagation();
                        onReplace(r);
                    }}
                >
                    Replace
                </button>

                <button
                    className="ordersBtn ordersBtn--danger ordersBtn--sm"
                    disabled={!isCancellable(r.status)}
                    onClick={(e) => {
                        e.stopPropagation();
                        onCancel(r);
                    }}
                >
                    Cancel
                </button>
            </td>
        </tr>
    );
};

export default OrderRow;