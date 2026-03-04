// src/pages/Auth/VerifyEmailSent/VerifyEmailSent.jsx
import React, { useContext, useState } from "react";
import "./VerifyEmailSent.scss";
import { useNavigate } from "react-router-dom";
import { Helmet } from "react-helmet";
import { FiArrowLeft } from "react-icons/fi";
import toast from "react-hot-toast";
import { resendVerifyEmail } from "../../../api/auth";
import { MainContext } from "../../../context/ContextProvider";

const VerifyEmailSent = () => {
    const navigate = useNavigate();
    const { user } = useContext(MainContext);

    const [email, setEmail] = useState(user?.email || "");
    const [loading, setLoading] = useState(false);
    const goBack = () => {
        if (window.history.length > 1) navigate(-1);
        else navigate("/app/dashboard", { replace: true }); // or "/login"
    };
    const onResend = async () => {
        const e = email.trim();
        if (!e) {
            toast.error("Email is required.");
            return;
        }
        setLoading(true);
        const minDelay = new Promise((resolve) => setTimeout(resolve, 2000));
        try {
            await Promise.all([
                resendVerifyEmail(e),
                minDelay
            ]);
            toast.success("If the email exists, we sent a verification link.");
        } catch (err) {
            await minDelay; 
            toast.success("If the email exists, we sent a verification link.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <Helmet>
                <title>Verify Email | Trading</title>
            </Helmet>

            <div className="auth-card">
                <button
                    type="button"
                    className="back-btn"
                    onClick={goBack}
                    aria-label="Back to login"
                >
                    <FiArrowLeft />
                </button>

                <h2>Verify your email</h2>

                <p className="auth-success">
                    We’ll send you a verification link. Please check your inbox and spam.
                </p>

                {!user?.email && (
                    <div className="field">
                        <label>Email</label>
                        <input
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="you@example.com"
                            type="email"
                            autoComplete="email"
                        />
                    </div>
                )}

                <div className="sent-actions">
                    <button className="auth-btn" onClick={onResend} disabled={loading}>
                        {loading ? "Sending..." : "Send verification email"}
                    </button>
                    <button className="auth-btn" onClick={()=>navigate("/app/dashboard")} disabled={loading}>
                        Dashboard
                    </button>
                </div>
            </div>
        </div>
    );
};

export default VerifyEmailSent;