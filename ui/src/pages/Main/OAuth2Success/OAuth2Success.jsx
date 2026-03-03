import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { OAUTH_SUCCESS_PATH } from "../../../utils/oauth";

function parseHashToken() {
    // URL looks like: /oauth2/success#token=XXXX
    const hash = window.location.hash || "";
    const params = new URLSearchParams(hash.startsWith("#") ? hash.slice(1) : hash);
    return params.get("token");
}

const OAuth2Success = () => {
    const navigate = useNavigate();

    useEffect(() => {
        const token = parseHashToken();

        if (!token) {
            // no token => go login
            navigate("/login", { replace: true });
            return;
        }

        localStorage.setItem("token", token);
        window.dispatchEvent(new Event("auth:token"));

        // clean URL (remove token from address bar)
        window.history.replaceState({}, document.title, OAUTH_SUCCESS_PATH);

        setTimeout(() => navigate("/app"), 2000);
    }, [navigate]);

    return (
        <div className="auth-container">
            <div className="auth-card">
                <h2>Signing you in…</h2>
                <p>Please wait.</p>
            </div>
        </div>
    );
}
export default OAuth2Success;