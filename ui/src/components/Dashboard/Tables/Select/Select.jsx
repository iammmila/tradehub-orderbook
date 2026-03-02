import React, { useEffect, useMemo, useRef, useState } from "react";
import ReactDOM from "react-dom";
import "./Select.scss";

const Select = ({ options = [], value, onChange, label, width = 140 }) => {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef(null);
  const menuRef = useRef(null);
  const [pos, setPos] = useState(null);

  const current = useMemo(() => {
    const v = value == null ? null : String(value);
    return options.find((o) => String(o.value) === v) || null;
  }, [options, value]);

  const close = () => setOpen(false);

  useEffect(() => {
    const onDocPointerDown = (e) => {
      const wrap = wrapRef.current;
      const menu = menuRef.current;

      const inWrap = wrap?.contains(e.target);
      const inMenu = menu?.contains(e.target);

      if (!inWrap && !inMenu) close();
    };

    document.addEventListener("pointerdown", onDocPointerDown);
    return () => document.removeEventListener("pointerdown", onDocPointerDown);
  }, []);

  useEffect(() => {
    if (!open) return;

    const updatePos = () => {
      const el = wrapRef.current;
      if (!el) return;
      const r = el.getBoundingClientRect();
      setPos({
        left: r.left,
        top: r.bottom + 8,
        width: r.width,
      });
    };

    updatePos();
    window.addEventListener("resize", updatePos);
    window.addEventListener("scroll", updatePos, true);

    return () => {
      window.removeEventListener("resize", updatePos);
      window.removeEventListener("scroll", updatePos, true);
    };
  }, [open]);

  const handleKeyDown = (e) => {
    if (e.key === "Escape") close();
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      setOpen((v) => !v);
    }
  };

  const displayLabel = useMemo(() => {
    if (current?.label != null) return String(current.label);
    if (value != null && (typeof value === "string" || typeof value === "number")) return String(value);
    if (options?.[0]?.label != null) return String(options[0].label);
    return "";
  }, [current, value, options]);

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
        <span className="dashboardSelect__value">{displayLabel}</span>
        <span className="dashboardSelect__chev" />
      </button>

      {open &&
        pos &&
        ReactDOM.createPortal(
          <div
            ref={menuRef}
            className="dashboardSelect__menu dashboardSelect__menu--portal"
            role="listbox"
            style={{
              position: "fixed",
              left: pos.left,
              top: pos.top,
              width: pos.width,
              zIndex: 2147483647,
            }}
          >
            {options.map((opt) => {
              const active = String(opt.value) === String(value);
              return (
                <button
                  key={String(opt.value)}
                  type="button"
                  className={`dashboardSelect__opt ${active ? "is-active" : ""}`}
                  onClick={() => {
                    onChange?.(opt.value);
                    close();
                  }}
                  role="option"
                  aria-selected={active}
                >
                  <span className="dashboardSelect__optLabel">{opt.label}</span>
                  {active && <span className="dashboardSelect__check">✓</span>}
                </button>
              );
            })}
          </div>,
          document.body
        )}
    </div>
  );
};

export default Select;