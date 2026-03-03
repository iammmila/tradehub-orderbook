import React, { useState } from "react";
import "./ForgotPassword.scss";
import { Link, useNavigate } from "react-router-dom";
import { forgotPassword } from "../../../api/auth";
import { Helmet } from "react-helmet";
import { FiArrowLeft } from "react-icons/fi";

const ForgotPassword = () => {
    const [email, setEmail] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [done, setDone] = useState(false);
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");
        setSubmitting(true);

        try {
            await forgotPassword(email.trim());
            setDone(true);
        } catch (err) {
            setDone(true);
        } finally {
            setSubmitting(false);
            navigate("/forgot-password/sent", { replace: true });
        }
    };

    return (
        <div className="auth-container">
            <Helmet>
                <title>Forgot Password | Trading</title>
            </Helmet>

            <form className="auth-card" onSubmit={handleSubmit}>
                <button
                    type="button"
                    className="back-btn"
                    onClick={() => navigate("/login", { replace: true })}
                    aria-label="Back to login"
                >
                    <FiArrowLeft />
                </button>

                <h2>Forgot password</h2>

                {!done ? (
                    <>
                        <p>Enter your email and we’ll send a reset link.</p>

                        <div className="input-group">
                            <input
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                type="email"
                                placeholder="Email"
                                required
                            />
                        </div>

                        <button type="submit" className="auth-btn" disabled={submitting}>
                            {submitting ? "Sending..." : "Send reset link"}
                        </button>

                        {error && <p className="auth-error">{error}</p>}

                        <div className="auth-footer">
                            <span>
                                Remembered? <Link to="/login">Back to login</Link>
                            </span>
                        </div>
                    </>
                ) : (
                    <>
                        <p className="auth-success">
                            If the email exists, we sent a reset link. Please check your inbox (and spam).
                        </p>

                        <button
                            type="button"
                            className="auth-btn"
                            onClick={() => navigate("/login", { replace: true })}
                        >
                            Back to login
                        </button>

                        <button
                            type="button"
                            className="auth-btn secondary"
                            onClick={() => setDone(false)}
                        >
                            Try another email
                        </button>
                    </>
                )}
            </form>
        </div>
    );
}

export default ForgotPassword