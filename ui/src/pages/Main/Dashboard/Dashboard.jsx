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

const Dashboard = () => {

  return (
    <div className="dashWrap">
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

      <div className="dashGrid dashGrid--tables">
        <RecentOrdersTable />
        <RecentTradesTable />
      </div>
    </div>
  )
}

export default Dashboard