import React from 'react'
import './TableCard.scss'

const TableCard = ({ title, subtitle, rightSlot, children }) => {
  return (
    <div className="table-card">
      <div className="table-card__head">
        <div className="table-card__left">
          <div className="table-card__title">{title}</div>
          {subtitle && <div className="table-card__subtitle">{subtitle}</div>}
        </div>
        {rightSlot && <div className="table-card__right">{rightSlot}</div>}
      </div>

      <div className="table-card__body">{children}</div>
    </div>
  )
}

export default TableCard