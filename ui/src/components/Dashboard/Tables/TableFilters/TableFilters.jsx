import React from 'react'
import "./TableFilters.scss"
import Select from "../Select/Select";

const TableFilters = ({
  search,
  onSearch,
  selects = [],
  right = null,
}) => {
  return (
    <div className="tbl-filters">
      <div className="tbl-filters__left">
        <div className="tbl-filters__search">
          <input
            value={search}
            onChange={(e) => onSearch(e.target.value)}
            placeholder="Search instrument…"
            aria-label="Search instrument"
          />
        </div>

        {selects.map((s) => (
          <Select
            key={s.label}
            label={s.label}
            value={s.value}
            options={s.options}
            onChange={s.onChange}
            width={s.width || 140}
          />
        ))}
      </div>

      <div className="tbl-filters__right">{right}</div>
    </div>
  )
}

export default TableFilters