import React from "react";
import "./Cards.scss";

const Cards = ({ title, value, subtitle, rightTag, loading, error }) => {
  return (
    <div className="dashboard-card">
      <div className="dashboard-card__header">
        <div className="dashboard-card__title">{title}</div>
        {rightTag && <div className="dashboard-card__tag">{rightTag}</div>}
      </div>

      <div className="dashboard-card__body">
        {loading ? (
          <div className="dashboard-card__skeleton dashboard-card__skeleton--value" />
        ) : error ? (
          <div className="dashboard-card__error">Failed to load</div>
        ) : (
          <div className="dashboard-card__value">{value}</div>
        )}

        <div className="dashboard-card__subtitle">
          {loading ? (
            <div className="dashboard-card__skeleton dashboard-card__skeleton--sub" />
          ) : (
            subtitle
          )}
        </div>
      </div>
    </div>
  );
};

export default Cards;