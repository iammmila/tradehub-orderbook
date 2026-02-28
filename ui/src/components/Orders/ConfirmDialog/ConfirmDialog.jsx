import React from "react";
import "./ConfirmDialog.scss";
import ModalPortal from "../../common/ModalPortal"; // adjust path

const ConfirmDialog = ({
    isOpen,
    title,
    description,
    confirmText = "Confirm",
    cancelText = "Cancel",
    confirmVariant = "primary", 
    disabled = false,
    onConfirm,
    onClose,
}) => {
    if (!isOpen) return null;

    const confirmClass =
        confirmVariant === "danger"
            ? "ordersBtn ordersBtn--danger"
            : "ordersBtn ordersBtn--primary";

    return (
        <ModalPortal>
            <div className="confirmDialog__backdrop" onMouseDown={onClose} role="dialog" aria-modal="true">
                <div className="confirmDialog__card" onMouseDown={(e) => e.stopPropagation()}>
                    <h3 className="confirmDialog__title">{title}</h3>
                    {description && <p className="confirmDialog__desc">{description}</p>}

                    <div className="confirmDialog__actions">
                        <button
                            type="button"
                            className="ordersBtn ordersBtn--secondary"
                            disabled={disabled}
                            onClick={onClose}
                        >
                            {cancelText}
                        </button>

                        <button
                            type="button"
                            className={confirmClass}
                            disabled={disabled}
                            onClick={onConfirm}
                        >
                            {confirmText}
                        </button>
                    </div>
                </div>
            </div>
        </ModalPortal>
    );
};

export default ConfirmDialog;