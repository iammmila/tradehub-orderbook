import React, { useEffect, useMemo, useState } from "react";
import "./CreateOrderModal.scss";
import { createOrder } from "../../../api/orders";
import ModalPortal from "../../common/ModalPortal";
import Select from "../../Dashboard/Tables/Select/Select";
const TYPE_LIMIT = "LIMIT";
const TYPE_MARKET = "MARKET";
const TYPE_HIDDEN_LIMIT = "HIDDEN_LIMIT";
const TYPE_MIN_EXECUTION_SIZE = "MIN_EXECUTION_SIZE";
const initialValues = {
  instrument: "",
  side: "BUY",
  type: TYPE_LIMIT,
  routingMode: "AUTO",
  exchangeCode: "",
  price: "",
  quantity: "",
  minExecSize: "",
};
function isMarket(type) {
  return String(type || "").toUpperCase() === TYPE_MARKET;
}

function needsPrice(type) {
  return !isMarket(type);
}

function needsMinExec(type) {
  return String(type || "").toUpperCase() === TYPE_MIN_EXECUTION_SIZE;
}

function toUpperTrim(s) {
  return String(s || "").trim().toUpperCase();
}

function numOrEmpty(v) {
  if (v === "" || v == null) return "";
  return String(v);
}

function validate(values) {
  const errs = {};

  const instrument = toUpperTrim(values.instrument);
  if (!instrument) errs.instrument = "Instrument is required.";

  const qty = Number(values.quantity);
  if (!Number.isFinite(qty) || qty <= 0) errs.quantity = "Quantity must be > 0.";

  const type = String(values.type || "").toUpperCase();

  if (needsPrice(type)) {
    const p = Number(values.price);
    if (!Number.isFinite(p) || p <= 0) errs.price = "Price must be > 0 for this order type.";
  }

  if (needsMinExec(type)) {
    const m = Number(values.minExecSize);
    if (!Number.isFinite(m) || m < 0) errs.minExecSize = "Min exec size must be ≥ 0.";
    if (Number.isFinite(m) && Number.isFinite(qty) && m > qty) {
      errs.minExecSize = "Min exec size cannot be greater than quantity.";
    }
  }

  if (String(values.routingMode).toUpperCase() === "MANUAL") {
    const ex = toUpperTrim(values.exchangeCode);
    if (!ex) errs.exchangeCode = "Exchange is required in MANUAL mode.";
  }

  return errs;
}

const CreateOrderModal = ({ isOpen, onClose, onCreated }) => {
  const [values, setValues] = useState(initialValues);
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});

  const [busy, setBusy] = useState(false);
  const [apiError, setApiError] = useState(null);
  useEffect(() => {
    if (isOpen) {
      setValues(initialValues);
      setErrors({});
      setTouched({});
      setBusy(false);
      setApiError(null);
    }
  }, [isOpen]);

  const type = useMemo(() => String(values.type || "").toUpperCase(), [values.type]);
  const market = useMemo(() => isMarket(type), [type]);
  const showMinExec = useMemo(() => needsMinExec(type), [type]);
  const requirePrice = useMemo(() => needsPrice(type), [type]);
  useEffect(() => {
    if (!isOpen) return;
    const mode = String(values.routingMode || "").toUpperCase();
    if (mode === "MANUAL") {
      // Default exchange to XLON if not set
      if (!values.exchangeCode) {
        setValues((p) => ({ ...p, exchangeCode: "XLON" }));
        setErrors((p) => ({ ...p, exchangeCode: undefined }));
      }
    } else {
      // AUTO: clear manual exchange
      if (values.exchangeCode) {
        setValues((p) => ({ ...p, exchangeCode: "" }));
        setErrors((p) => ({ ...p, exchangeCode: undefined }));
      }
    }

    if (market && values.price !== "") {
      setValues((p) => ({ ...p, price: "" }));
      setErrors((p) => ({ ...p, price: undefined }));
    }

    if (!showMinExec && values.minExecSize !== "") {
      setValues((p) => ({ ...p, minExecSize: "" }));
      setErrors((p) => ({ ...p, minExecSize: undefined }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [market, showMinExec, values.routingMode, isOpen]);

  const setField = (name, raw) => {
    setValues((prev) => ({ ...prev, [name]: raw }));
    if (touched[name]) {
      const next = { ...values, [name]: raw };
      setErrors((prevErr) => ({ ...prevErr, ...validate(next) }));
    }
  };

  const blurField = (name) => {
    setTouched((p) => ({ ...p, [name]: true }));
    setErrors(validate(values));
  };

  const canSubmit = useMemo(() => {
    if (busy) return false;
    const errs = validate(values);
    return Object.values(errs).every((x) => !x);
  }, [values, busy]);

  const submit = async (e) => {
    e.preventDefault();
    if (busy) return;

    const nextTouched = {
      instrument: true,
      side: true,
      type: true,
      routingMode: true,
      quantity: true,
    };
    if (requirePrice) nextTouched.price = true;
    if (showMinExec) nextTouched.minExecSize = true;
    if (String(values.routingMode).toUpperCase() === "MANUAL") nextTouched.exchangeCode = true;

    setTouched(nextTouched);

    const errs = validate(values);
    setErrors(errs);
    if (Object.values(errs).some(Boolean)) return;

    try {
      setBusy(true);
      setApiError(null);

      const payload = {
        instrument: toUpperTrim(values.instrument),
        side: String(values.side).toUpperCase(),
        type: type,
        quantity: Number(values.quantity),
      };

      if (requirePrice) payload.price = Number(values.price);
      if (showMinExec && values.minExecSize !== "") payload.minExecSize = Number(values.minExecSize);
      if (String(values.routingMode).toUpperCase() === "MANUAL") {
        payload.exchangeCode = toUpperTrim(values.exchangeCode);
      }

      await createOrder(payload);

      onCreated?.();
      onClose?.();
    } catch (err) {
      setApiError(err?.message || "Create order failed");
    } finally {
      setBusy(false);
    }
  };

  if (!isOpen) return null;

  const showError = (name) => touched[name] && errors[name];

  return (
    <ModalPortal>
      <div className="modal__backdrop" onMouseDown={onClose} role="dialog" aria-modal="true">
        <div className="modal__card" onMouseDown={(e) => e.stopPropagation()}>
          <div className="modal__header">
            <h3>Create Order</h3>
            <button
              type="button"
              className="ordersBtn ordersBtn--secondary ordersBtn--sm"
              onClick={onClose}
              disabled={busy}
              aria-label="Close"
            >
              ✕
            </button>
          </div>

          <form onSubmit={submit} className="modal__form">
            {apiError && <div className="modal__error">{apiError}</div>}

            <label className="field">
              <span>Instrument</span>
              <input
                value={values.instrument}
                onChange={(e) => setField("instrument", e.target.value)}
                onBlur={() => blurField("instrument")}
                placeholder="e.g. BTC-USD"
                autoFocus
              />
              {showError("instrument") && <div className="field__error">{errors.instrument}</div>}
            </label>

            {/* Side + Type */}
            <div className="fieldRow">
              <div className="field">
                <span>Side</span>
                <Select
                  label=""
                  value={values.side}
                  onChange={(v) => {
                    setTouched((p) => ({ ...p, side: true }));
                    setField("side", String(v));
                  }}
                  width="100%"
                  options={[
                    { label: "BUY", value: "BUY" },
                    { label: "SELL", value: "SELL" },
                  ]}
                />
              </div>

              <div className="field">
                <span>Type</span>
                <Select
                  label=""
                  value={values.type}
                  onChange={(v) => {
                    setTouched((p) => ({ ...p, type: true }));
                    setField("type", String(v));
                  }}
                  width="100%"
                  options={[
                    { label: "LIMIT", value: TYPE_LIMIT },
                    { label: "MARKET", value: TYPE_MARKET },
                    { label: "HIDDEN_LIMIT", value: TYPE_HIDDEN_LIMIT },
                    { label: "MIN_EXECUTION_SIZE", value: TYPE_MIN_EXECUTION_SIZE },
                  ]}
                />
              </div>
              <div className="typeHint">
                {type === "LIMIT" && "LIMIT: executes at your price or better."}
                {type === "MARKET" && "MARKET: executes immediately; price is ignored."}
                {type === "HIDDEN_LIMIT" && "HIDDEN_LIMIT: not displayed in orderbook."}
                {type === "MIN_EXECUTION_SIZE" && "MIN_EXECUTION_SIZE: fills only if size ≥ minExecSize."}
              </div>
            </div>

            {/* Routing Mode + Exchange (conditional) */}
            <div className="fieldRow">
              <div className="field">
                <span>Routing</span>
                <div className="routingSwitch" role="radiogroup" aria-label="Routing mode">
                  <button
                    type="button"
                    className={`routingSwitch__btn ${values.routingMode === "AUTO" ? "isActive" : ""}`}
                    onClick={() => setField("routingMode", "AUTO")}
                    disabled={busy}
                  >
                    AUTO
                  </button>
                  <button
                    type="button"
                    className={`routingSwitch__btn ${values.routingMode === "MANUAL" ? "isActive" : ""}`}
                    onClick={() => setField("routingMode", "MANUAL")}
                    disabled={busy}
                  >
                    MANUAL
                  </button>
                </div>
              </div>

              {String(values.routingMode).toUpperCase() === "MANUAL" && (
                <div className="field">
                  <span>Exchange</span>
                  <Select
                    label=""
                    value={values.exchangeCode}
                    onChange={(v) => {
                      setTouched((p) => ({ ...p, exchangeCode: true }));
                      setField("exchangeCode", String(v));
                    }}
                    width="100%"
                    options={[
                      { label: "XLON", value: "XLON" },
                      { label: "XNAS", value: "XNAS" },
                      { label: "XTKS", value: "XTKS" },
                    ]}
                  />
                  {showError("exchangeCode") && <div className="field__error">{errors.exchangeCode}</div>}
                </div>
              )}
            </div>

            {/* Price + Quantity */}
            <div className="fieldRow">
              <label className="field">
                <span>
                  Price {market ? <span className="hint">(ignored for MARKET)</span> : null}
                </span>
                <input
                  value={numOrEmpty(values.price)}
                  onChange={(e) => setField("price", e.target.value)}
                  onBlur={() => blurField("price")}
                  type="number"
                  step="0.01"
                  inputMode="decimal"
                  disabled={market}
                  placeholder={market ? "—" : "e.g. 110.00"}
                />
                {showError("price") && <div className="field__error">{errors.price}</div>}
              </label>

              <label className="field">
                <span>Quantity</span>
                <input
                  value={numOrEmpty(values.quantity)}
                  onChange={(e) => setField("quantity", e.target.value)}
                  onBlur={() => blurField("quantity")}
                  type="number"
                  step="1"
                  inputMode="numeric"
                  min="1"
                />
                {showError("quantity") && <div className="field__error">{errors.quantity}</div>}
              </label>
            </div>

            {/* Min Exec Size (conditional) */}
            {showMinExec && (
              <label className="field">
                <span>Min Exec Size</span>
                <input
                  value={numOrEmpty(values.minExecSize)}
                  onChange={(e) => setField("minExecSize", e.target.value)}
                  onBlur={() => blurField("minExecSize")}
                  type="number"
                  step="1"
                  inputMode="numeric"
                  min="0"
                  placeholder="e.g. 10"
                />
                {showError("minExecSize") && <div className="field__error">{errors.minExecSize}</div>}
              </label>
            )}

            {/* Footer */}
            <div className="modal__actions">
              <button type="button" className="ordersBtn ordersBtn--secondary" onClick={onClose} disabled={busy}>
                Cancel
              </button>
              <button type="submit" className="ordersBtn ordersBtn--primary" disabled={!canSubmit}>
                {busy ? "Creating..." : "Create"}
              </button>
            </div>
            <div className="modal__note">
              {String(values.routingMode).toUpperCase() === "AUTO"
                ? "AUTO: router selects exchange automatically."
                : "MANUAL: your selected exchange will be used."}
            </div>
          </form>
        </div>
      </div>
    </ModalPortal>
  );
};

export default CreateOrderModal;