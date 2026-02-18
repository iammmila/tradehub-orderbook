import { useMemo, useState } from "react";
import { changePassword, updateMe } from "../api/users";
import { parseApiError } from "../utils/apiError";

const MASKED = "••••••••••";
const cap = (s = "") => (s ? s.charAt(0).toUpperCase() + s.slice(1) : "");

export function useSettingsForm(user, setUser) {
  const [editing, setEditing] = useState({});
  const [draft, setDraft] = useState({});
  const [pwDraft, setPwDraft] = useState({
    currentPassword: "",
    newPassword: "",
  });
  const [showPw, setShowPw] = useState({ current: false, next: false });

  const [saved, setSaved] = useState(false);
  const [needRelogin, setNeedRelogin] = useState(false);

  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState([]);

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

  const initials = useMemo(() => {
    if (!uiUser) return "U";
    return (
      (uiUser.firstName?.[0] || "U") + (uiUser.lastName?.[0] || "")
    ).toUpperCase();
  }, [uiUser]);

  const clearErrors = () => {
    setError("");
    setFieldErrors([]);
  };

  const showSavedToast = () => {
    setSaved(true);
    setTimeout(() => setSaved(false), 2500);
  };

  const startEdit = (key) => {
    clearErrors();
    setEditing((p) => ({ ...p, [key]: true }));
    setShowPw({ current: false, next: false });

    if (key === "password") {
      setPwDraft({ currentPassword: "", newPassword: "" });
      return;
    }
    setDraft((p) => ({ ...p, [key]: uiUser?.[key] ?? "" }));
  };

  const cancelEdit = (key) => {
    clearErrors();
    setEditing((p) => ({ ...p, [key]: false }));

    if (key === "password") {
      setPwDraft({ currentPassword: "", newPassword: "" });
      setShowPw({ current: false, next: false });
      return;
    }

    setDraft((p) => {
      const n = { ...p };
      delete n[key];
      return n;
    });
  };

  const saveField = async (key) => {
    clearErrors();
    try {
      if (key === "password") {
        await changePassword({
          currentPassword: pwDraft.currentPassword,
          newPassword: pwDraft.newPassword,
        });

        setEditing((p) => ({ ...p, password: false }));
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

      setEditing((p) => ({ ...p, [key]: false }));
      setDraft((p) => {
        const n = { ...p };
        delete n[key];
        return n;
      });

      showSavedToast();
    } catch (err) {
      const parsed = parseApiError(err, "Update failed");
      setError(parsed.message);
      setFieldErrors(parsed.fieldErrors);
    }
  };

  const saveAll = async () => {
    clearErrors();

    const payload = {};
    Object.keys(draft).forEach((k) => {
      const value = (draft[k] ?? "").trim();
      if (value) payload[k] = value;
    });

    const shouldSavePassword = !!editing.password;
    const hasPasswordValues =
      pwDraft.currentPassword.trim().length > 0 ||
      pwDraft.newPassword.trim().length > 0;

    try {
      // password first
      if (shouldSavePassword && hasPasswordValues) {
        await changePassword({
          currentPassword: pwDraft.currentPassword,
          newPassword: pwDraft.newPassword,
        });

        setEditing((p) => ({ ...p, password: false }));
        setPwDraft({ currentPassword: "", newPassword: "" });
        setShowPw({ current: false, next: false });
      }

      // then profile
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
      const parsed = parseApiError(err, "Save all failed");
      setError(parsed.message);
      setFieldErrors(parsed.fieldErrors);
    }
  };

  const isAnyEditing = Object.values(editing).some(Boolean);

  return {
    uiUser,
    initials,

    editing,
    draft,
    setDraft,

    pwDraft,
    setPwDraft,
    showPw,
    setShowPw,

    startEdit,
    cancelEdit,
    saveField,
    saveAll,

    saved,
    needRelogin,

    error,
    fieldErrors,
    clearErrors,

    isAnyEditing,
  };
}
