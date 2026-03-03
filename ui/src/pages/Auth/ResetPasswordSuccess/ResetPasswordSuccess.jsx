import React from "react";
import "./ResetPasswordSuccess.scss";
import { Link } from "react-router-dom";
import { Helmet } from "react-helmet";

const ResetPasswordSuccess = () => {
    return (
        <div className="auth-container">
            <Helmet>
                <title>Password Updated | Trading</title>
            </Helmet>

            <div className="auth-card">
                <h2>Password updated</h2>
                <p className="auth-success">Your password has been changed successfully.</p>

                <Link className="auth-btn" to="/login">
                    Go to login
                </Link>
            </div>
        </div>
    );
}

export default ResetPasswordSuccess