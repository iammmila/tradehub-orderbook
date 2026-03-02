import React from 'react'
import "./Dashboard.scss";
import OpenOrdersCard from '../../../components/Dashboard/OpenOrdersCard/OpenOrdersCard';
import OrdersTodayCard from '../../../components/Dashboard/OrdersTodayCard/OrdersTodayCard';
import TradesTodayCard from '../../../components/Dashboard/TradesTodayCard/TradesTodayCard';
import FillRateCard from '../../../components/Dashboard/FillRateCard/FillRateCard';
import TradesOverTimeChart from '../../../components/Dashboard/Charts/TradesOverTimeChart/TradesOverTimeChart';
import InstrumentDistributionDonut from '../../../components/Dashboard/Charts/InstrumentDistributionDonut/InstrumentDistributionDonut';
import RecentOrdersTable from '../../../components/Dashboard/Tables/RecentOrdersTable/RecentOrdersTable';
import RecentTradesTable from '../../../components/Dashboard/Tables/RecentTradesTable/RecentTradesTable';
import LiveOrdersBar from '../../../components/Dashboard/LiveOrdersBar/LiveOrdersBar';
import { Helmet } from 'react-helmet';
import RoutingSummaryCard from '../../../components/Dashboard/RoutingSummaryCard/RoutingSummaryCard';
import { useRecentOrders } from '../../../hooks/useRecentOrders';

const Dashboard = () => {
  const { loading, error, orders } = useRecentOrders();
  const lastUpdatedAt = React.useMemo(() => (loading ? null : new Date()), [loading]);
  return (
    <div className="dashWrap">
      <Helmet>
        <title>Dashboard | Trading</title>
        <meta name='description' content='It is Dashboard page of Trading Application' />
      </Helmet>
      {/* Row 1: KPI cards */}
      <div className="dashGrid dashGrid--kpi">
        <OpenOrdersCard />
        <OrdersTodayCard />
        <TradesTodayCard />
        <FillRateCard />
      </div>
      <div className="dashGrid">
        <LiveOrdersBar speed={60} />
      </div>

      {/* Row 2: Charts */}
      <div className="dashGrid dashGrid--charts">
        <div className="dashCol dashCol--big">
          <TradesOverTimeChart />
        </div>
        <div className="dashCol dashCol--small">
          <InstrumentDistributionDonut />
        </div>
      </div>

      {/* NEW: routing summary row (optional placement) */}
      <div className="dashGrid ">
        <RoutingSummaryCard
          orders={orders}
          loading={loading}
          error={error}
          lastUpdatedAt={lastUpdatedAt} />
      </div>
      <div className="dashGrid dashGrid--tables">
        <RecentOrdersTable />
        <RecentTradesTable />
      </div>
    </div>
  )
}

export default Dashboard