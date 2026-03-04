// src/pages/Auth/VerifyEmailConfirm/VerifyEmailConfirm.jsx
import React, { useContext, useEffect, useRef, useState } from "react";
import "./VerifyEmailConfirm.scss";
import { Helmet } from "react-helmet";
import { Link, useSearchParams } from "react-router-dom";
import toast from "react-hot-toast";
import { confirmVerifyEmail } from "../../../api/auth";
import { MainContext } from "../../../context/ContextProvider";

const VerifyEmailConfirm = () => {
    const [params] = useSearchParams();
    const token = params.get("token") || "";

    const { refreshUser } = useContext(MainContext);

    const [status, setStatus] = useState("loading"); // loading | success | error
    const ran = useRef(false);

    useEffect(() => {
        if (ran.current) return;
        ran.current = true;

        const run = async () => {
            if (!token) {
                setStatus("error");
                return;
            }

            try {
                await confirmVerifyEmail(token);
                await refreshUser?.(); // updates user.verified => true
                setStatus("success");
                toast.success("Email verified.");
            } catch (e) {
                setStatus("error");
                toast.error("Verification failed or link expired.");
            }
        };

        run();
    }, [token, refreshUser]);

    return (
        <div className="auth-container">
            <Helmet>
                <title>Verify Email | Trading</title>
            </Helmet>

            <div className="auth-card">
                {status === "loading" && (
                    <>
                        <h2>Verifying...</h2>
                        <p className="auth-success">Please wait a moment.</p>
                    </>
                )}

                {status === "success" && (
                    <>
                        <h2>Email verified ✅</h2>
                        <p className="auth-success">Your account is verified. You can continue.</p>

                        <div className="sent-actions">
                            <Link className="auth-btn" to="/login">
                                Login
                            </Link>
                        </div>
                    </>
                )}

                {status === "error" && (
                    <>
                        <h2>Verification failed</h2>
                        <p className="auth-success">
                            The link may be invalid or expired. Please request a new one.
                        </p>

                        <div className="sent-actions">
                            <Link className="auth-btn" to="/verify-email/sent">
                                Send again
                            </Link>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
};

export default VerifyEmailConfirm;