import React, { useEffect, useMemo, useState } from "react";
import "./CreateOrderModal.scss";
import { createOrder } from "../../../api/orders";
import ModalPortal from "../../common/ModalPortal"; // adjust path
import Select from "../../Dashboard/Tables/Select/Select";
import { schema } from "../../../schema/createOrderSchema";
const initialValues = {
  instrument: "",
  side: "BUY",
  price: "",
  quantity: "",
};
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

  const validateField = async (name, nextValues) => {
    try {
      await schema.validateAt(name, nextValues);
      setErrors((prev) => ({ ...prev, [name]: undefined }));
      return true;
    } catch (err) {
      setErrors((prev) => ({ ...prev, [name]: err.message }));
      return false;
    }
  };

  const handleChange = async (name, rawValue) => {
    const nextValues = { ...values, [name]: rawValue };
    setValues(nextValues);

    // real-time validation only after user touched that field
    if (touched[name]) {
      await validateField(name, nextValues);
    }
  };

  const handleBlur = async (name) => {
    setTouched((prev) => ({ ...prev, [name]: true }));
    await validateField(name, values);
  };

  const canSubmit = useMemo(() => {
    // no submit while busy
    if (busy) return false;

    // quick check: all fields filled
    if (!values.instrument.trim()) return false;
    if (!values.side) return false;
    if (values.price === "" || values.quantity === "") return false;

    // must have no current errors for touched fields
    // (we’ll fully validate on submit anyway)
    const hasAnyError = Object.values(errors).some(Boolean);
    return !hasAnyError;
  }, [values, errors, busy]);

  const submit = async (e) => {
    e.preventDefault();
    if (busy) return;

    try {
      setBusy(true);
      setApiError(null);

      // Mark all as touched so errors show
      setTouched({
        instrument: true,
        side: true,
        price: true,
        quantity: true,
      });

      // Full validation
      const validated = await schema.validate(values, { abortEarly: false });

      await createOrder({
        instrument: validated.instrument.trim(),
        side: validated.side,
        price: Number(validated.price),
        quantity: Number(validated.quantity),
      });

      onCreated?.();
      onClose?.();
    } catch (err) {
      // Yup errors
      if (err?.name === "ValidationError") {
        const next = {};
        (err.inner || []).forEach((e2) => {
          if (e2.path) next[e2.path] = e2.message;
        });
        setErrors(next);
      } else {
        // API errors
        setApiError(err?.message || "Create order failed");
      }
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
                onChange={(e) => handleChange("instrument", e.target.value)}
                onBlur={() => handleBlur("instrument")}
                placeholder="e.g. BTC-USD"
              />
              {showError("instrument") && <div className="field__error">{errors.instrument}</div>}
            </label>

            <div className="field">
              <span>Side</span>
              <Select
                label=""
                value={values.side}
                onChange={(v) => {
                  setTouched((p) => ({ ...p, side: true }));
                  handleChange("side", String(v));
                }}
                width="100%"
                options={[
                  { label: "BUY", value: "BUY" },
                  { label: "SELL", value: "SELL" },
                ]}
              />
              {showError("side") && <div className="field__error">{errors.side}</div>}
            </div>

            <label className="field">
              <span>Price</span>
              <input
                value={values.price}
                onChange={(e) => handleChange("price", e.target.value)}
                onBlur={() => handleBlur("price")}
                type="number"
                step="0.01"
              />
              {showError("price") && <div className="field__error">{errors.price}</div>}
            </label>

            <label className="field">
              <span>Quantity</span>
              <input
                value={values.quantity}
                onChange={(e) => handleChange("quantity", e.target.value)}
                onBlur={() => handleBlur("quantity")}
                type="number"
                step="1"
              />
              {showError("quantity") && <div className="field__error">{errors.quantity}</div>}
            </label>

            <div className="modal__actions">
              <button type="button" className="ordersBtn ordersBtn--secondary" onClick={onClose} disabled={busy}>
                Cancel
              </button>
              <button type="submit" className="ordersBtn ordersBtn--primary" disabled={!canSubmit}>
                {busy ? "Creating..." : "Create"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </ModalPortal>
  );
};

export default CreateOrderModal;
//   const [instrument, setInstrument] = useState("");
//   const [side, setSide] = useState("BUY");
//   const [price, setPrice] = useState("");
//   const [quantity, setQuantity] = useState("");

//   const [busy, setBusy] = useState(false);
//   const [error, setError] = useState(null);

//   const canSubmit = useMemo(() => {
//     const p = Number(price);
//     const q = Number(quantity);
//     return instrument.trim() && (side === "BUY" || side === "SELL") && p > 0 && q > 0;
//   }, [instrument, side, price, quantity]);

//   const submit = async (e) => {
//     e.preventDefault();
//     if (!canSubmit) return;

//     try {
//       setBusy(true);
//       setError(null);

//       await createOrder({
//         instrument: instrument.trim(),
//         side,
//         price: Number(price),
//         quantity: Number(quantity),
//       });

//       setInstrument("");
//       setSide("BUY");
//       setPrice("");
//       setQuantity("");

//       onCreated?.();
//       onClose?.(); // best UX: close after success
//     } catch (e2) {
//       setError(e2?.message || "Create order failed");
//     } finally {
//       setBusy(false);
//     }
//   };

//   if (!isOpen) return null;

//   return (
//     <ModalPortal>
//       <div className="modal__backdrop" onMouseDown={onClose} role="dialog" aria-modal="true">
//         <div className="modal__card" onMouseDown={(e) => e.stopPropagation()}>
//           <div className="modal__header">
//             <h3>Create Order</h3>
//             <button
//               type="button"
//               className="ordersBtn ordersBtn--secondary ordersBtn--sm"
//               onClick={onClose}
//               disabled={busy}
//               aria-label="Close"
//             >
//               ✕
//             </button>
//           </div>

//           <form onSubmit={submit} className="modal__form">
//             {error && <div className="modal__error">{error}</div>}

//             <label className="field">
//               <span>Instrument</span>
//               <input
//                 value={instrument}
//                 onChange={(e) => setInstrument(e.target.value)}
//                 placeholder="e.g. BTC-USD"
//               />
//             </label>

//             <div className="field">
//               <span>Side</span>
//               <Select
//                 label=""              // keep empty (we already render span)
//                 value={side}
//                 onChange={(v) => setSide(String(v))}
//                 width="100%"
//                 options={[
//                   { label: "BUY", value: "BUY" },
//                   { label: "SELL", value: "SELL" },
//                 ]}
//               />
//             </div>

//             <label className="field">
//               <span>Price</span>
//               <input
//                 value={price}
//                 onChange={(e) => setPrice(e.target.value)}
//                 type="number"
//                 step="0.01"
//                 min="0"
//               />
//             </label>

//             <label className="field">
//               <span>Quantity</span>
//               <input
//                 value={quantity}
//                 onChange={(e) => setQuantity(e.target.value)}
//                 type="number"
//                 step="1"
//                 min="0"
//               />
//             </label>

//             <div className="modal__actions">
//               <button
//                 type="button"
//                 className="ordersBtn ordersBtn--secondary"
//                 onClick={onClose}
//                 disabled={busy}
//               >
//                 Cancel
//               </button>
//               <button
//                 type="submit"
//                 className="ordersBtn ordersBtn--primary"
//                 disabled={!canSubmit || busy}
//               >
//                 {busy ? "Creating..." : "Create"}
//               </button>
//             </div>
//           </form>
//         </div>
//       </div>
//     </ModalPortal>
//   );
// };

// export default CreateOrderModal;