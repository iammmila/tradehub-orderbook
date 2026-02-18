import React, { useContext, useMemo, useState } from 'react'
import './Settings.scss'
import { MainContext } from '../../../context/ContextProvider'
import { changePassword, updateMe } from '../../../api/users';
import { VscEye, VscEyeClosed } from "react-icons/vsc";

const MASKED = "••••••••••";

const Settings = () => {
  const { user, setUser } = useContext(MainContext);

  const [editing, setEditing] = useState({});
  const [draft, setDraft] = useState({});
  const [saved, setSaved] = useState(false);

  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState([]);
  const [needRelogin, setNeedRelogin] = useState(false);

  const [showPw, setShowPw] = useState({
    current: false,
    next: false,
  });
  const [pwDraft, setPwDraft] = useState({
    currentPassword: "",
    newPassword: ""
  });

  const cap = (s = "") => (s ? s.charAt(0).toUpperCase() + s.slice(1) : "");

  const applyApiError = (err, fallback = "Update failed") => {
    const data = err?.response?.data;

    // Reset old errors
    setFieldErrors([]);

    // If backend sent fieldErrors, show them
    if (Array.isArray(data?.fieldErrors) && data.fieldErrors.length > 0) {
      setFieldErrors(data.fieldErrors);
      // show a general message too (optional)
      setError(data?.message || "Validation failed");
      return;
    }

    // Otherwise show message
    setError(data?.message || fallback);
  };
  const uiUser = useMemo(() => {
    if (!user) return null;
    return {
      firstName: cap(user.firstName ?? ""),
      lastName: cap(user.lastName ?? ""),
      username: user.username ?? "",
      email: user.email ?? "",
      password: MASKED,
    };
  }, [user]);

  if (!uiUser) {
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

  const fields = [
    { key: "firstName", label: "First Name", type: "text" },
    { key: "lastName", label: "Last Name", type: "text" },
    { key: "username", label: "Username", type: "text" },
    { key: "email", label: "Email", type: "email" },
    { key: "password", label: "Password", type: "password" },
  ];
  const clearErrors = () => {
    setError("");
    setFieldErrors([]);
  };

  const startEdit = (key) => {
    clearErrors();

    setEditing((prev) => ({ ...prev, [key]: true }));
    setShowPw({ current: false, next: false });

    if (key === "password") {
      setPwDraft({ currentPassword: "", newPassword: "" });
      return;
    }

    setDraft((prev) => ({ ...prev, [key]: uiUser[key] }));
  };

  const cancelEdit = (key) => {
    clearErrors();
    setEditing((prev) => ({ ...prev, [key]: false }));

    if (key === "password") {
      setPwDraft({ currentPassword: "", newPassword: "" });
      setShowPw({ current: false, next: false });
      return;
    }

    setDraft((prev) => {
      const n = { ...prev };
      delete n[key];
      return n;
    });
  };

  const showSavedToast = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 5000);
  };

  const saveField = async (key) => {
    clearErrors();

    try {
      if (key === "password") {
        await changePassword({
          currentPassword: pwDraft.currentPassword,
          newPassword: pwDraft.newPassword,
        });

        setEditing((prev) => ({ ...prev, password: false }));
        setPwDraft({ currentPassword: "", newPassword: "" });
        setShowPw({ current: false, next: false });
        showSavedToast();
        return;
      }

      const value = (draft[key] ?? "").trim();
      if (!value) return;

      const payload = { [key]: value };

      const result = await updateMe(payload);

      const updatedUser = result?.user ?? result;
      const newToken = result?.token;
      if (key === "username" && !newToken) setNeedRelogin(true);
      if (newToken) localStorage.setItem("token", newToken);

      setUser(updatedUser);

      setEditing((prev) => ({ ...prev, [key]: false }));
      setDraft((prev) => {
        const n = { ...prev };
        delete n[key];
        return n;
      });

      showSavedToast();
    } catch (err) {
      applyApiError(err, "Update failed");
    }
  };

  const isAnyEditing = Object.values(editing).some(Boolean);

  const saveAll = async () => {
    clearErrors();

    // Build PATCH payload from draft
    const payload = {};
    Object.keys(draft).forEach((k) => {
      const value = (draft[k] ?? "").trim();
      if (value) payload[k] = value;
    });

    const shouldSavePassword = !!editing.password; // user opened password edit
    const hasPasswordValues =
      pwDraft.currentPassword.trim().length > 0 || pwDraft.newPassword.trim().length > 0;

    try {
      // 1) If password section is being edited and user typed something -> validate/save password first
      if (shouldSavePassword && hasPasswordValues) {
        await changePassword({
          currentPassword: pwDraft.currentPassword,
          newPassword: pwDraft.newPassword,
        });

        // close password edit only if success
        setEditing((prev) => ({ ...prev, password: false }));
        setPwDraft({ currentPassword: "", newPassword: "" });
        setShowPw({ current: false, next: false });
      }

      // 2) Then update profile fields
      if (Object.keys(payload).length) {
        const result = await updateMe(payload);
        const updatedUser = result?.user ?? result;
        const newToken = result?.token;

        if (payload.username && !newToken) setNeedRelogin(true);
        if (newToken) localStorage.setItem("token", newToken);

        setUser(updatedUser);
      }

      setEditing({});
      setDraft({});
      showSavedToast();
    } catch (err) {
      applyApiError(err, "Save all failed");
    }
  };

  const initials =
    (uiUser.firstName?.[0] || "U") +
    (uiUser.lastName?.[0] || "");
  return (
    <div className="settings-page">
      <div className="settings-container">
        <div className="settings-header">
          <div className="breadcrumb">
            TradeHub / <span>Settings</span>
          </div>
          <h1>
            Account <em>Settings</em>
          </h1>
          <p>View and manage your profile information</p>
        </div>

        <div className="avatar-section">
          <div className="avatar-circle">{initials}</div>
          <div className="avatar-info">
            <div className="name">
              {uiUser.firstName} {uiUser.lastName}
            </div>
            <div className="role">Trader</div>
            <div className="joined">@{uiUser.username}</div>
          </div>
        </div>


        {(error || fieldErrors.length > 0) && (
          <div className="settings-error">
            {error && <div className="settings-error__title">{error}</div>}
            {fieldErrors.length > 0 && (
              <ul className="settings-error__list">
                {fieldErrors.map((fe, idx) => (
                  <li key={idx}>
                    <span className="fe-field">{fe.field}</span>: {fe.message}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="fields-section">
          {fields.map((field, i) => (
            <div key={field.key}>
              <div className={`field-row ${editing[field.key] ? "is-editing" : ""}`}>
                <div className="field-top">
                  <div className="field-left">
                    <div className="field-label">{field.label}</div>

                    <div className={`field-value ${field.key === "password" ? "is-password" : ""}`}>
                      {field.key === "password" ? MASKED : uiUser[field.key]}
                    </div>
                  </div>

                  {!editing[field.key] && (
                    <button
                      className="edit-btn"
                      onClick={() => startEdit(field.key)}
                      disabled={field.readOnly}
                      title={field.readOnly ? "This field can't be edited" : "Edit"}
                    >
                      Edit
                    </button>
                  )}
                </div>

                {/* EDIT UI */}
                {editing[field.key] && field.key !== "password" && (
                  <div className="edit-input-group">
                    <input
                      className="edit-input"
                      type={field.type}
                      placeholder={`New ${field.label.toLowerCase()}...`}
                      value={draft[field.key] ?? ""}
                      onChange={(e) =>
                        setDraft((prev) => ({ ...prev, [field.key]: e.target.value }))
                      }
                      autoFocus
                      disabled={field.readOnly}
                    />
                    <button className="btn-save" onClick={() => saveField(field.key)}>
                      Save
                    </button>
                    <button className="btn-cancel" onClick={() => cancelEdit(field.key)}>
                      ✕
                    </button>
                  </div>
                )}

                {/* PASSWORD EDIT UI */}
                {editing.password && field.key === "password" && (
                  <div className="edit-input-group password-grid">
                    {/* Current password */}
                    <div className="password-input">
                      <input
                        className="edit-input"
                        type={showPw.current ? "text" : "password"}
                        placeholder="Current password"
                        value={pwDraft.currentPassword}
                        onChange={(e) =>
                          setPwDraft((p) => ({ ...p, currentPassword: e.target.value }))
                        }
                        autoFocus
                      />
                      <button
                        type="button"
                        className="pw-toggle"
                        onClick={() => setShowPw((p) => ({ ...p, current: !p.current }))}
                        aria-label={showPw.current ? "Hide current password" : "Show current password"}
                      >
                        {showPw.current ? <VscEyeClosed /> : <VscEye />}
                      </button>
                    </div>

                    {/* New password */}
                    <div className="password-input">
                      <input
                        className="edit-input"
                        type={showPw.next ? "text" : "password"}
                        placeholder="New password"
                        value={pwDraft.newPassword}
                        onChange={(e) =>
                          setPwDraft((p) => ({ ...p, newPassword: e.target.value }))
                        }
                      />
                      <button
                        type="button"
                        className="pw-toggle"
                        onClick={() => setShowPw((p) => ({ ...p, next: !p.next }))}
                        aria-label={showPw.next ? "Hide new password" : "Show new password"}
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
                )}
              </div>

              {i < fields.length - 1 && <div className="divider" />}
            </div>
          ))}
        </div>
        <div className={`actions-footer ${isAnyEditing ? "visible" : ""}`}>
          <p>
            You have <span>unsaved changes</span>
          </p>
          <button className="btn-save-all" onClick={saveAll}>
            Save All Changes
          </button>
        </div>
      </div>
      {needRelogin && (
        <div className="settings-warning">
          <div className="toast-dot" />
          Username updated. Please logout and login again to refresh your session.
        </div>
      )}
      {saved && (
        <div className="toast">
          <div className="toast-dot" />
          Changes saved successfully
        </div>
      )}
    </div>
  )
}

export default Settings