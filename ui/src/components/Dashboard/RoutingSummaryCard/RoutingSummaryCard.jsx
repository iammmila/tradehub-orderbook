import React, { useMemo } from "react";
import TableCard from "../Tables/TableCard/TableCard";
import { formatDate, formatTime } from "../../../utils/formatter";
import "./RoutingSummaryCard.scss"

const isSameLocalDay = (dateLike, now = new Date()) => {
    if (!dateLike) return false;
    const d = new Date(dateLike);
    return (
        d.getFullYear() === now.getFullYear() &&
        d.getMonth() === now.getMonth() &&
        d.getDate() === now.getDate()
    );
};

const pickMostCommon = (items) => {
    const m = new Map();
    items.forEach((x) => {
        if (!x) return;
        m.set(x, (m.get(x) || 0) + 1);
    });
    let best = null;
    let bestCount = 0;
    for (const [k, v] of m.entries()) {
        if (v > bestCount) {
            best = k;
            bestCount = v;
        }
    }
    return { value: best, count: bestCount };
};

const RoutingSummaryCard = ({ orders = [], loading, error, lastUpdatedAt }) => {
    const stats = useMemo(() => {
        const now = new Date();
        const todays = (orders || []).filter((o) => isSameLocalDay(o.createdAt, now));
        const autos = todays.filter((o) => String(o.routingMode || "").toUpperCase() === "AUTO");

        const most = pickMostCommon(autos.map((o) => o.exchangeCode));
        const latestAuto = autos[0]; // createdAt desc from API

        return {
            todaysCount: todays.length,
            autoCount: autos.length,
            manualCount: Math.max(0, todays.length - autos.length),
            mostExchange: most.value,
            mostExchangeCount: most.count,
            latestReason: latestAuto?.routeReason || null,
        };
    }, [orders]);

    const subtitle = loading
        ? "Loading..."
        : error
            ? "Error"
            : lastUpdatedAt
                ? `Last updated: ${formatDate(lastUpdatedAt)} ${formatTime(lastUpdatedAt)}`
                : "Today";

    return (
        <TableCard title="Routing Summary" subtitle={subtitle}>
            <div className="routingSum__container">
                {error ? (
                    <div className="routingSum__state routingSum__state--error">
                        <div className="routingSum__stateTitle">Couldn’t load routing data</div>
                        <div className="routingSum__stateText">{String(error)}</div>
                    </div>
                ) : loading ? (
                    <div className="routingSum__skeleton">
                        <div className="routingSum__skRow">
                            <div className="routingSum__skBox" />
                            <div className="routingSum__skBox" />
                            <div className="routingSum__skBox" />
                        </div>
                        <div className="routingSum__skLine" />
                        <div className="routingSum__skLine routingSum__skLine--short" />
                    </div>
                ) : stats.todaysCount === 0 ? (
                    <div className="routingSum__state">
                        <div className="routingSum__stateTitle">No orders today</div>
                        <div className="routingSum__stateText">Create an order to see routing stats here.</div>
                    </div>
                ) : (
                    <div className="routingSum">
                        {/* KPI row */}
                        <div className="routingSum__kpis">
                            <div className="routingSum__kpi">
                                <div className="routingSum__kpiLabel">Orders today</div>
                                <div className="routingSum__kpiValue">{stats.todaysCount}</div>
                                <div className="routingSum__kpiHint">All routing modes</div>
                            </div>

                            <div className="routingSum__kpi routingSum__kpi--auto">
                                <div className="routingSum__kpiLabel">AUTO</div>
                                <div className="routingSum__kpiValue">{stats.autoCount}</div>
                                <div className="routingSum__pillRow">
                                    <span className="routingSum__pill routingSum__pill--auto">SOR</span>
                                    <span className="routingSum__pill routingSum__pill--muted">
                                        {stats.todaysCount > 0 ? Math.round((stats.autoCount / stats.todaysCount) * 100) : 0}%
                                    </span>
                                </div>
                            </div>

                            <div className="routingSum__kpi routingSum__kpi--manual">
                                <div className="routingSum__kpiLabel">MANUAL</div>
                                <div className="routingSum__kpiValue">{stats.manualCount}</div>
                                <div className="routingSum__kpiHint">User-selected venue</div>
                            </div>
                        </div>

                        {/* Divider */}
                        <div className="routingSum__divider" />

                        {/* Details */}
                        <div className="routingSum__details">
                            <div className="routingSum__detailRow">
                                <div className="routingSum__detailLabel">Top exchange (AUTO)</div>
                                <div className="routingSum__detailValue">
                                    {stats.mostExchange ? (
                                        <>
                                            <span className="routingSum__chip">{stats.mostExchange}</span>
                                            <span className="routingSum__muted">({stats.mostExchangeCount}x)</span>
                                        </>
                                    ) : (
                                        <span className="routingSum__muted">—</span>
                                    )}
                                </div>
                            </div>

                            <div className="routingSum__detailRow routingSum__detailRow--reason">
                                <div className="routingSum__detailLabel">Latest route reason</div>
                                <div className="routingSum__detailValue">
                                    {stats.latestReason ? (
                                        <span className="routingSum__reason" title={stats.latestReason}>
                                            {stats.latestReason}
                                        </span>
                                    ) : (
                                        <span className="routingSum__muted">—</span>
                                    )}
                                </div>
                            </div>

                            <div className="routingSum__note">
                                Router picks the best venue by fill + effective price (fees included).
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </TableCard>
    );
};

export default RoutingSummaryCard;