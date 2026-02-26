import React, { useMemo } from "react";
import ChartCard from "../ChartCard/ChartCard";
import "./InstrumentDistributionDonut.scss";

import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } from "recharts";

import { useDashboardTrades } from "../../../../hooks/useDashboardTrades";
import { buildInstrumentDistributionToday } from "../../../../utils/tradeSeries";

const COLORS = [
    "rgba(99, 102, 241, 0.95)",
    "rgba(168, 85, 247, 0.92)",
    "rgba(34, 211, 238, 0.92)",
    "rgba(16, 185, 129, 0.90)",
    "rgba(245, 158, 11, 0.90)",
    "rgba(244, 63, 94, 0.90)",
    "rgba(148, 163, 184, 0.85)",
];

const InstrumentDistributionDonut = () => {
    const { trades, loading, error } = useDashboardTrades();

    const data = useMemo(() => buildInstrumentDistributionToday(trades, 5), [trades]);
    const total = useMemo(() => data.reduce((s, x) => s + x.value, 0), [data]);

    return (
        <ChartCard
            title="Instrument Distribution (Today)"
            subtitle={error ? error : "Trades count by instrument"}
            rightTag={loading ? "Loading..." : `${total} trades`}
        >
            <div className="instrument-donut">
                {error ? (
                    <div className="chart-error">Cannot load trades</div>
                ) : total === 0 ? (
                    <div className="chart-empty">No trades today</div>
                ) : (
                    <>
                        <div className="instrument-donut__chart">
                            <ResponsiveContainer width="100%" height="100%">
                                <PieChart>
                                    <Tooltip
                                        contentStyle={{
                                            background: "rgba(10, 14, 22, 0.95)",
                                            border: "1px solid rgba(255,255,255,0.10)",
                                            borderRadius: "12px",
                                            color: "rgba(255,255,255,0.9)",
                                        }}
                                    />
                                    <Pie
                                        data={data}
                                        dataKey="value"
                                        nameKey="name"
                                        innerRadius="62%"
                                        outerRadius="88%"
                                        paddingAngle={2}
                                        stroke="rgba(255,255,255,0.06)"
                                    >
                                        {data.map((_, idx) => (
                                            <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                                        ))}
                                    </Pie>
                                </PieChart>
                            </ResponsiveContainer>

                            <div className="instrument-donut__center">
                                <div className="instrument-donut__centerValue">{total}</div>
                                <div className="instrument-donut__centerLabel">trades</div>
                            </div>
                        </div>

                        <div className="instrument-donut__legend">
                            {data.map((x, idx) => (
                                <div key={x.name} className="legend-item">
                                    <span className="legend-dot" style={{ background: COLORS[idx % COLORS.length] }} />
                                    <span className="legend-name">{x.name}</span>
                                    <span className="legend-value">{x.value}</span>
                                </div>
                            ))}
                        </div>
                    </>
                )}
            </div>
        </ChartCard>
    );
}
export default InstrumentDistributionDonut;