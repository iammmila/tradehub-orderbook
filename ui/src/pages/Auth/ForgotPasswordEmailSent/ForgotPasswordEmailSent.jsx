import React from "react";
import "./ForgotPasswordEmailSent.scss";
import { Link, useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet";
import { FiArrowLeft } from "react-icons/fi";
const ForgotPasswordEmailSent = () => {
    const navigate = useNavigate();

    return (
        <div className="auth-container">
            <Helmet>
                <title>Email Sent | Trading</title>
            </Helmet>

            <div className="auth-card">
                <button
                    type="button"
                    className="back-btn"
                    onClick={() => navigate("/login", { replace: true })}
                    aria-label="Back to login"
                >
                    <FiArrowLeft />
                </button>

                <h2>Check your email</h2>
                <p className="auth-success">
                    If the email exists, we sent a password reset link. Please check your inbox and spam.
                </p>

                <div className="sent-actions">
                    <Link className="auth-btn" to="/login">Back to login</Link>
                    <Link className="auth-btn secondary" to="/forgot-password">Send again</Link>
                </div>
            </div>
        </div>
    );
}

export default ForgotPasswordEmailSent