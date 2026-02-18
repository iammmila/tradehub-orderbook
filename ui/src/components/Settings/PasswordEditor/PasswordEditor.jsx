import React from "react";
import "./PasswordEditor.scss";

import { VscEye, VscEyeClosed } from "react-icons/vsc";

export default function PasswordEditor({
    editing,
    pwDraft,
    setPwDraft,
    showPw,
    setShowPw,
    saveField,
    cancelEdit,
}) {
    if (!editing.password) return null;

    return (
        <div className="edit-input-group password-grid">
            <div className="password-input">
                <input
                    className="edit-input"
                    type={showPw.current ? "text" : "password"}
                    placeholder="Current password"
                    value={pwDraft.currentPassword}
                    onChange={(e) => setPwDraft((p) => ({ ...p, currentPassword: e.target.value }))}
                    autoFocus
                />
                <button
                    type="button"
                    className="pw-toggle"
                    onClick={() => setShowPw((p) => ({ ...p, current: !p.current }))}
                >
                    {showPw.current ? <VscEyeClosed /> : <VscEye />}
                </button>
            </div>

            <div className="password-input">
                <input
                    className="edit-input"
                    type={showPw.next ? "text" : "password"}
                    placeholder="New password"
                    value={pwDraft.newPassword}
                    onChange={(e) => setPwDraft((p) => ({ ...p, newPassword: e.target.value }))}
                />
                <button
                    type="button"
                    className="pw-toggle"
                    onClick={() => setShowPw((p) => ({ ...p, next: !p.next }))}
                >
                    {showPw.next ? <VscEyeClosed /> : <VscEye />}
                </button>
            </div>

            <button className="btn-save" onClick={() => saveField("password")}>
                Save
            </button>
            <button className="btn-cancel" onClick={() => cancelEdit("password")}>
                ✕
            </button>
        </div>
    );
}
