import React, { useState, useRef, useEffect, useMemo } from "react";
import "./NotificationContainer.scss";
import { FaRegBell, FaCheckDouble } from "react-icons/fa";
import NotificationModal from "./../NotificationModal/NotificationModal";
import {
    getNotifications,
    getUnreadCount,
    markNotificationRead,
    markAllNotificationsRead,
} from "../../api/notifications";

import { normalizeFromPage, normalizeDto } from "../../utils/notificationNormalize";
import toast from "react-hot-toast";
 const ICON_BY_TYPE = {
        ORDER_CREATED: "🟢",
        ORDER_CANCELLED: "🟠",
        ORDER_REPLACED: "🟣",
        ORDER_PARTIALLY_FILLED: "🟡",
        ORDER_FILLED: "✅",
        TRADE_EXECUTED: "💱",
    };
const NotificationContainer = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [selectedNotification, setSelectedNotification] = useState(null);
    const dropdownRef = useRef(null);

    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);

    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    const computedUnread = useMemo(
        () => notifications.filter((n) => !n.isRead).length,
        [notifications]
    );
    const badgeCount =
        typeof unreadCount === "number" && !Number.isNaN(unreadCount)
            ? unreadCount
            : computedUnread;

    async function loadNotificationsAndCount() {
        setLoading(true);
        setErrorMsg("");
        try {
            const pageDto = await getNotifications({ page: 0, size: 20 });
            const normalized = normalizeFromPage(pageDto);
            setNotifications(normalized);

            try {
                const c = await getUnreadCount();
                const count =
                    typeof c === "number" ? c : typeof c?.count === "number" ? c.count : null;

                if (count !== null) setUnreadCount(count);
                else setUnreadCount(normalized.filter((n) => !n.isRead).length);
            } catch {
                setUnreadCount(normalized.filter((n) => !n.isRead).length);
            }
        } catch (err) {
            setErrorMsg(err?.message || "Failed to load notifications.");
        } finally {
            setLoading(false);
        }
    }
    useEffect(() => {
        const handler = (e) => {
            const dto = e.detail;
            const n = normalizeDto(dto);

            setNotifications((prev) => {
                if (prev.some((x) => x.id === n.id)) return prev;
                return [n, ...prev].slice(0, 50);
            });

            setUnreadCount((c) => (typeof c === "number" ? c + (n.isRead ? 0 : 1) : c));
            toast(`${n.text}`, {
                icon: ICON_BY_TYPE[n.type] || "🔔",
            });
        };

        window.addEventListener("notif:ws", handler);
        return () => window.removeEventListener("notif:ws", handler);
    }, []);
    // initial REST load
    useEffect(() => {
        loadNotificationsAndCount();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // refresh when opening dropdown (optional)
    useEffect(() => {
        if (isOpen) loadNotificationsAndCount();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isOpen]);
   

    const markAllAsRead = async (e) => {
        e.stopPropagation();
        const prev = notifications;

        setNotifications((p) => p.map((n) => ({ ...n, isRead: true })));
        setUnreadCount(0);

        try {
            await markAllNotificationsRead();
        } catch (err) {
            setNotifications(prev);
            setUnreadCount(prev.filter((n) => !n.isRead).length);
            setErrorMsg(err?.message || "Failed to mark all as read.");
        }
    };

    const handleNotificationClick = async (notif) => {
        setSelectedNotification(notif);
        setIsOpen(false);
      
        if (notif.isRead) return;

        setNotifications((prev) =>
            prev.map((n) => (n.id === notif.id ? { ...n, isRead: true } : n))
        );
        setUnreadCount((c) => (typeof c === "number" ? Math.max(0, c - 1) : c));
        if (!notif?.id) {
            setErrorMsg("Notification id missing (cannot mark as read).");
            return;
        }
        try {
            await markNotificationRead(notif.id);
        } catch (err) {
            setNotifications((prev) =>
                prev.map((n) => (n.id === notif.id ? { ...n, isRead: false } : n))
            );
            setUnreadCount((c) => (typeof c === "number" ? c + 1 : c));
            setErrorMsg(err?.message || "Failed to mark as read.");
        }
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);
    return (
        <>
            <div className="notification-container" ref={dropdownRef}>
                <button className="bell-trigger" onClick={() => setIsOpen(!isOpen)}>
                    <FaRegBell className="icon" />
                    {badgeCount > 0 && <span className="badge">{badgeCount}</span>}
                </button>

                {isOpen && (
                    <div className="notification-dropdown">
                        <div className="dropdown-header">
                            <h3>Notifications</h3>
                            <button
                                className="read-all-btn"
                                onClick={markAllAsRead}
                                disabled={loading || notifications.length === 0}
                            >
                                <FaCheckDouble /> Read all
                            </button>
                        </div>

                        <div className="notification-list">
                            {loading && <div className="empty-state">Loading...</div>}

                            {!loading && errorMsg && <div className="empty-state">{errorMsg}</div>}

                            {!loading && !errorMsg && notifications.length > 0 ? (
                                notifications.map((n) => (
                                    <div
                                        key={n.id}
                                        className={`notification-item ${n.isRead ? "read" : "unread"}`}
                                        onClick={() => handleNotificationClick(n)}
                                    >
                                        <div className="item-content">
                                            <p>{n.text}</p>
                                            <span className="time">{n.time}</span>
                                        </div>
                                        {!n.isRead && <div className="unread-dot"></div>}
                                    </div>
                                ))
                            ) : (
                                !loading && !errorMsg && <div className="empty-state">No notifications</div>
                            )}
                        </div>
                    </div>
                )}
            </div>

            {selectedNotification && (
                <NotificationModal
                    notification={selectedNotification}
                    onClose={() => setSelectedNotification(null)}
                />
            )}
        </>
    );
};

export default NotificationContainer;