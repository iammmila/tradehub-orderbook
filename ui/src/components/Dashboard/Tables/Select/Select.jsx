import React, { useEffect, useMemo, useRef, useState } from "react";
import "./Select.scss"

const Select = ({ options, value, onChange, label, width = 140 }) => {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);

  const current = useMemo(
    () => options.find((o) => o.value === value) || options[0],
    [options, value]
  );

  useEffect(() => {
    const onDoc = (e) => {
      if (!wrapRef.current) return;
      if (!wrapRef.current.contains(e.target)) setOpen(false);
    };
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);

  const handleKeyDown = (e) => {
    if (e.key === "Escape") setOpen(false);
    if (e.key === "Enter" || e.key === " ") setOpen((v) => !v);
  };

  return (
    <div className="dashboardSelect" ref={wrapRef} style={{ width }}>
      {label && <div className="dashboardSelect__label">{label}</div>}

      <button
        type="button"
        className={`dashboardSelect__btn ${open ? "is-open" : ""}`}
        onClick={() => setOpen((v) => !v)}
        onKeyDown={handleKeyDown}
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <span className="dashboardSelect__value">{current?.label}</span>
        <span className="dashboardSelect__chev" />
      </button>

      {open && (
        <div className="dashboardSelect__menu" role="listbox">
          {options.map((opt) => (
            <button
              key={opt.value}
              type="button"
              className={`dashboardSelect__opt ${opt.value === value ? "is-active" : ""}`}
              onClick={() => {
                onChange(opt.value);
                setOpen(false);
              }}
              role="option"
              aria-selected={opt.value === value}
            >
              <span className="dashboardSelect__optLabel">{opt.label}</span>
              {opt.value === value && <span className="dashboardSelect__check">✓</span>}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

export default Select