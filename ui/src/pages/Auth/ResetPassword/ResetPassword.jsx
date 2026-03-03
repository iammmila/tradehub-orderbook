import React, { useMemo, useState } from "react";
import "./ResetPassword.scss";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { resetPassword } from "../../../api/auth";
import { Helmet } from "react-helmet";
import { FiArrowLeft } from "react-icons/fi";
import { VscEye, VscEyeClosed } from "react-icons/vsc";

const ResetPassword = () => {
    const [params] = useSearchParams();
    const token = useMemo(() => params.get("token") || "", [params]);

    const [newPassword, setNewPassword] = useState("");
    const [confirm, setConfirm] = useState("");
    const [show, setShow] = useState(false);

    const [submitting, setSubmitting] = useState(false);
    const [done, setDone] = useState(false);
    const [error, setError] = useState("");

    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError("");

        if (!token) {
            setError("Invalid reset link. Please request a new one.");
            return;
        }
        if (newPassword.length < 8) {
            setError("Password must be at least 8 characters.");
            return;
        }
        if (newPassword !== confirm) {
            setError("Passwords do not match.");
            return;
        }

        setSubmitting(true);
        try {
            await resetPassword(token, newPassword);
            navigate("/reset-password/success", { replace: true });
        } catch (err) {
            setError(err?.response?.data?.message || "Invalid or expired token.");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="auth-container">
            <Helmet>
                <title>Reset Password | Trading</title>
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

                <h2>Reset password</h2>

                {!done ? (
                    <>
                        <p>Choose a new password.</p>

                        <div className="input-group password-group">
                            <input
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                type={show ? "text" : "password"}
                                placeholder="New password"
                                required
                            />
                            <button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShow((p) => !p)}
                                aria-label={show ? "Hide password" : "Show password"}
                            >
                                {show ? <VscEyeClosed /> : <VscEye />}
                            </button>
                        </div>

                        <div className="input-group">
                            <input
                                value={confirm}
                                onChange={(e) => setConfirm(e.target.value)}
                                type={show ? "text" : "password"}
                                placeholder="Confirm new password"
                                required
                            />
                        </div>

                        <button type="submit" className="auth-btn" disabled={submitting}>
                            {submitting ? "Saving..." : "Update password"}
                        </button>

                        {error && <p className="auth-error">{error}</p>}

                        <div className="auth-footer">
                            <span>
                                Need a new link? <Link to="/forgot-password">Request again</Link>
                            </span>
                        </div>
                    </>
                ) : (
                    <>
                        <p className="auth-success">Password updated successfully.</p>
                        <button
                            type="button"
                            className="auth-btn"
                            onClick={() => navigate("/login", { replace: true })}
                        >
                            Go to login
                        </button>
                    </>
                )}
            </form>
        </div>
    );
}
export default ResetPassword