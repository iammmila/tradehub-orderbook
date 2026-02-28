import React from 'react'
import './Trades.scss';
import { Helmet } from 'react-helmet';
import TradesTable from '../../../components/Trades/TradesTable/TradesTable';

const Trades = () => {
  return (
    <div className='tradesPage'>
      <Helmet>
        <title>Trades | Trading</title>
        <meta name='description' content='It is Trades page of Trading Application' />
      </Helmet>
      <div className="tradesPage__header">
        <h1 className="tradesPage__header-title">Trades</h1>
      </div>
      <div className="tradesPage__table">
        < TradesTable />
      </div>
    </div>
  )
}

export default Trades