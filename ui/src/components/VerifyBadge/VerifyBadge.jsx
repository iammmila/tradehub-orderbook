import React from "react";
import "./VerifyBadge.scss";
import { FiInfo } from "react-icons/fi";
import { useNavigate } from "react-router-dom";

const VerifyBadge = () => {
    const navigate = useNavigate();

    return (
        <button
            type="button"
            className="verify-badge"
            onClick={() => navigate("/verify-email/sent")}
            aria-label="Verify account"
            title="Verify account"
        >
            <FiInfo className="verify-badge__icon" />
            <span className="verify-badge__text">Verify Account</span>
        </button>
    );
};

export default VerifyBadge;