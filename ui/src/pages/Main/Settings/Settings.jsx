import React, { useContext } from "react";
import "./Settings.scss";
import { MainContext } from "../../../context/ContextProvider";
import { useSettingsForm } from "../../../hooks/useSettingsForm";

import ProfileCard from "../../../components/Settings/ProfileCard/ProfileCard";
import AlertBox from "../../../components/Settings/AlertBox/AlertBox";
import FieldRow from "../../../components/Settings/FieldRow/FieldRow";
import PasswordEditor from "../../../components/Settings/PasswordEditor/PasswordEditor";

const fields = [
  { key: "firstName", label: "First Name", type: "text" },
  { key: "lastName", label: "Last Name", type: "text" },
  { key: "username", label: "Username", type: "text" },
  { key: "email", label: "Email", type: "email" },
  { key: "password", label: "Password", type: "password" },
];

export default function Settings() {
  const { user, setUser } = useContext(MainContext);

  const form = useSettingsForm(user, setUser);

  if (!form.uiUser) {
    return (
      <div className="settings-page">
        <div className="settings-container">
          <div className="settings-header">
            <div className="breadcrumb">TradeHub / <span>Settings</span></div>
            <h1>Account <em>Settings</em></h1>
            <p>Loading your profile…</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="settings-page">
      <div className="settings-container">
        <div className="settings-header">
          <div className="breadcrumb">TradeHub / <span>Settings</span></div>
          <h1>Account <em>Settings</em></h1>
          <p>View and manage your profile information</p>
        </div>

        <ProfileCard initials={form.initials} uiUser={form.uiUser} />

        <AlertBox error={form.error} fieldErrors={form.fieldErrors} />

        <div className="fields-section">
          {fields.map((field, i) => (
            <div key={field.key}>
              {field.key !== "password" ? (
                <FieldRow
                  field={field}
                  uiUser={form.uiUser}
                  editing={form.editing}
                  draft={form.draft}
                  setDraft={form.setDraft}
                  startEdit={form.startEdit}
                  cancelEdit={form.cancelEdit}
                  saveField={form.saveField}
                />
              ) : (
                <div className={`field-row ${form.editing.password ? "is-editing" : ""}`}>
                  <div className="field-top">
                    <div className="field-left">
                      <div className="field-label">Password</div>
                      <div className="field-value is-password">••••••••••</div>
                    </div>
                    {!form.editing.password && (
                      <button className="edit-btn" onClick={() => form.startEdit("password")}>
                        Edit
                      </button>
                    )}
                  </div>

                  <PasswordEditor
                    editing={form.editing}
                    pwDraft={form.pwDraft}
                    setPwDraft={form.setPwDraft}
                    showPw={form.showPw}
                    setShowPw={form.setShowPw}
                    saveField={form.saveField}
                    cancelEdit={form.cancelEdit}
                  />
                </div>
              )}

              {i < fields.length - 1 && <div className="divider" />}
            </div>
          ))}
        </div>

        <div className={`actions-footer ${form.isAnyEditing ? "visible" : ""}`}>
          <p>
            You have <span>unsaved changes</span>
          </p>
          <button className="btn-save-all" onClick={form.saveAll}>
            Save All Changes
          </button>
        </div>
      </div>

      {form.needRelogin && (
        <div className="settings-warning">
          <div className="toast-dot" />
          Username updated. Please logout and login again to refresh your session.
        </div>
      )}

      {form.saved && (
        <div className="toast">
          <div className="toast-dot" />
          Changes saved successfully
        </div>
      )}
    </div>
  );
}
