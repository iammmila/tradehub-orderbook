import React from 'react'
import "./UserChip.scss"
import toast from "react-hot-toast";

const UserChip = ({ userId, username }) => {
    const label = username || (userId != null ? `User ${userId}` : "-");
    const initial = String(label || "?").trim().charAt(0).toUpperCase() || "?";

    const copyId = async (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (userId == null) return;
        try {
            await navigator.clipboard.writeText(String(username));
            toast.success("Copied ✨");
        } catch {
            toast.error("Copy failed");

        }
    };

    if (userId == null) return <span>-</span>;

    return (
        <div className="userChip" title={`User ID: ${userId}`}>
            <span className="userChip__avatar">{initial}</span>

            <span className="userChip__name">{label}</span>

            <button className="userChip__copy" onClick={copyId} type="button">
                Copy
            </button>
        </div>
    );
};

export default UserChip