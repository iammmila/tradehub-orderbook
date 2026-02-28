import React from 'react'
import './Orders.scss';
import { Helmet } from 'react-helmet';
import OrdersTable from "../../../components/Orders/OrdersTable/OrdersTable"
const Orders = () => {
  return (
    <div className='ordersPage'>
      <Helmet>
        <title>Orders | Trading</title>
        <meta name='description' content='It is Orders page of Trading Application' />
      </Helmet>
      <div className="ordersPage__header">
        <h1 className="ordersPage__header-title">Orders</h1>
      </div>
      <div className="ordersPage__table">
        < OrdersTable />
      </div>
    </div>
  )
}

export default Orders