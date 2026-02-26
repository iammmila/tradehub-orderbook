import React from 'react'
import "./Dashboard.scss";
import OpenOrdersCard from '../../../components/Dashboard/OpenOrdersCard/OpenOrdersCard';
import OrdersTodayCard from '../../../components/Dashboard/OrdersTodayCard/OrdersTodayCard';
import TradesTodayCard from '../../../components/Dashboard/TradesTodayCard/TradesTodayCard';
import FillRateCard from '../../../components/Dashboard/FillRateCard/FillRateCard';

const Dashboard = () => {
  return (
    <div className="dashWrap">
      <div className="dashGrid">
        <OpenOrdersCard />
        <OrdersTodayCard />
        <TradesTodayCard />
        <FillRateCard />
      </div>
    </div>
  )
}

export default Dashboard