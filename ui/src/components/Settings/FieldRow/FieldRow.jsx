import React from 'react'
import './FieldRow.scss'
const MASKED = "••••••••••";
const FieldRow = ({
    field,
    uiUser,
    editing,
    draft,
    setDraft,
    startEdit,
    cancelEdit,
    saveField,
}) => {
    return (
        <div className={`field-row ${editing[field.key] ? "is-editing" : ""}`}>
            <div className="field-top">
                <div className="field-left">
                    <div className="field-label">{field.label}</div>
                    <div className={`field-value ${field.key === "password" ? "is-password" : ""}`}>
                        {field.key === "password" ? MASKED : uiUser[field.key]}
                    </div>
                </div>

                {!editing[field.key] && (
                    <button className="edit-btn" onClick={() => startEdit(field.key)}>
                        Edit
                    </button>
                )}
            </div>

            {editing[field.key] && field.key !== "password" && (
                <div className="edit-input-group">
                    <input
                        className="edit-input"
                        type={field.type}
                        placeholder={`New ${field.label.toLowerCase()}...`}
                        value={draft[field.key] ?? ""}
                        onChange={(e) => setDraft((p) => ({ ...p, [field.key]: e.target.value }))}
                        autoFocus
                    />
                    <button className="btn-save" onClick={() => saveField(field.key)}>
                        Save
                    </button>
                    <button className="btn-cancel" onClick={() => cancelEdit(field.key)}>
                        ✕
                    </button>
                </div>
            )}
        </div>
    );
}

export default FieldRow