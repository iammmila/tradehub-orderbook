import React, { useMemo } from 'react'
import "./TradesOverTimeChart.scss"
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import ChartCard from '../ChartCard/ChartCard';
import { useDashboardTrades } from '../../../../hooks/useDashboardTrades';
import { buildTradesTodayByHour } from '../../../../utils/tradeSeries';
const TradesOverTimeChart = () => {
  const { trades, loading, error } = useDashboardTrades();

  const data = useMemo(() => buildTradesTodayByHour(trades), [trades]);

  return (
    <ChartCard
      title="Trades Over Time (Today)"
      subtitle={error ? error : "Hourly trades count (00–23)"}
      rightTag={loading ? "Loading..." : "Today"}
    >
      <div className="trades-over-time">
        {error ? (
          <div className="chart-error">Cannot load trades</div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data} margin={{ top: 8, right: 10, left: -10, bottom: 0 }}>
              <CartesianGrid stroke="rgba(255,255,255,0.06)" />
              <XAxis dataKey="hour" stroke="rgba(255,255,255,0.55)" tickLine={false} axisLine={false} />
              <YAxis stroke="rgba(255,255,255,0.55)" tickLine={false} axisLine={false} allowDecimals={false} />
              <Tooltip
                contentStyle={{
                  background: "rgba(10, 14, 22, 0.95)",
                  border: "1px solid rgba(255,255,255,0.10)",
                  borderRadius: "12px",
                  color: "rgba(255,255,255,0.9)",
                }}
                labelStyle={{ color: "rgba(255,255,255,0.7)" }}
              />
              <Line
                type="monotone"
                dataKey="trades"
                stroke="rgba(99, 102, 241, 0.95)"
                strokeWidth={2.4}
                dot={false}
                activeDot={{ r: 4 }}
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </ChartCard>)
}

export default TradesOverTimeChart