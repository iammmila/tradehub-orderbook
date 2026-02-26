// src/ui/toast/ToastStack.jsx
import React from "react";
import "./ToastStack.scss";

export default function ToastStack({ toasts, onClose }) {
    return (
        <div className="toast-stack">
            {toasts.map((t) => (
                <div key={t.id} className={`toast ${t.type}`}>
                    <div className="toast-title">{t.title}</div>
                    {t.message && <div className="toast-message">{t.message}</div>}
                    <button className="toast-close" onClick={() => onClose(t.id)} aria-label="Close">
                        ×
                    </button>
                </div>
            ))}
        </div>
    );
}