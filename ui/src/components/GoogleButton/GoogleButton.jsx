import React from "react";
import { GOOGLE_AUTH_URL } from "../../utils/oauth";
import "./GoogleButton.scss";
import { FcGoogle } from "react-icons/fc";

const GoogleButton = ({ text = "Continue with Google" }) => {
    const onClick = () => {
        // full-page redirect to gateway -> auth-service -> google
        window.location.assign(GOOGLE_AUTH_URL);
    };

    return (
        <button type="button" className="google-btn" onClick={onClick}>
            <span className="google-btn__icon" aria-hidden="true"><FcGoogle /></span>
            <span>{text}</span>
        </button>
    );
}
export default GoogleButton;