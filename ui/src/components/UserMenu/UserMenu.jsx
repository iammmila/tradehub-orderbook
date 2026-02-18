import React, { useContext, useEffect, useRef, useState } from 'react'
import './UserMenu.scss'
import { FiChevronDown, FiHelpCircle, FiLogOut, FiSettings, FiUser } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { MainContext } from '../../context/ContextProvider';

const UserMenu = () => {
    const { user, logout } = useContext(MainContext);
    const [open, setOpen] = useState(false);
    const navigate = useNavigate();
    const wrapperRef = useRef(null);
    const cap = (s = "") => (s ? s.charAt(0).toUpperCase() + s.slice(1) : "");

    const name = user ? `${cap(user.firstName)} ${cap(user.lastName)}` : "User";
    const role = user?.role?.name || user?.role || "Trader";
    const initials = user ? `${user.firstName.slice(0, 1).toUpperCase()}${user.lastName.slice(0, 1).toUpperCase()}` : user.username;
    // close on outside click + ESC
    useEffect(() => {
        const onClickOutside = (e) => {
            if (!wrapperRef.current) return;
            if (!wrapperRef.current.contains(e.target)) setOpen(false);
        };

        const onEsc = (e) => {
            if (e.key === "Escape") setOpen(false);
        };

        document.addEventListener("mousedown", onClickOutside);
        document.addEventListener("keydown", onEsc);
        return () => {
            document.removeEventListener("mousedown", onClickOutside);
            document.removeEventListener("keydown", onEsc);
        };
    }, []);

    const handleLogout = () => {
        logout();
        setOpen(false);
        navigate("/", { replace: true });
    };
    return (
        <div className="user-menu" ref={wrapperRef}>
            <button
                type="button"
                className="user-menu__trigger"
                onClick={() => setOpen((p) => !p)}
                aria-haspopup="menu"
                aria-expanded={open}
            >
                <div className="user-menu__avatar">{initials}</div>
                <FiChevronDown className={`user-menu__chevron ${open ? "is-open" : ""}`} />
            </button>

            {open && (
                <div className="user-menu__dropdown" role="menu">
                    <div className="user-menu__header">
                        <div className="user-menu__name">{name}</div>
                        <div className="user-menu__role">{role}</div>
                    </div>

                    <div className="user-menu__divider" />

                    <button
                        type="button"
                        className="user-menu__item"
                        onClick={() => {
                            setOpen(false);
                            navigate("/app/profile");
                        }}
                        role="menuitem"
                    >
                        <FiUser />
                        <span>My Profile</span>
                    </button>

                    <button
                        type="button"
                        className="user-menu__item"
                        onClick={() => {
                            setOpen(false);
                            navigate("/app/settings");
                        }}
                        role="menuitem"
                    >
                        <FiSettings />
                        <span>Settings</span>
                    </button>

                    <button
                        type="button"
                        className="user-menu__item"
                        onClick={() => {
                            setOpen(false);
                            navigate("/app/support");
                        }}
                        role="menuitem"
                    >
                        <FiHelpCircle />
                        <span>Help &amp; Support</span>
                    </button>

                    <div className="user-menu__divider" />

                    <button type="button" className="user-menu__item danger" onClick={handleLogout} role="menuitem">
                        <FiLogOut />
                        <span>Logout</span>
                    </button>
                </div>
            )}
        </div>
    )
}

export default UserMenu