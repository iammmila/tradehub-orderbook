import React from 'react'
import "./ChartCard.scss"
const ChartCard = ({ title, subtitle, rightTag, children }) => {
  return (
      <div className="chart-card">
          <div className="chart-card__head">
              <div className="chart-card__left">
                  <div className="chart-card__title">{title}</div>
                  {subtitle && <div className="chart-card__subtitle">{subtitle}</div>}
              </div>
              {rightTag && <div className="chart-card__tag">{rightTag}</div>}
          </div>

          <div className="chart-card__body">{children}</div>
      </div>
  )
}

export default ChartCard