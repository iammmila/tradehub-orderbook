import React from 'react';
import './NotificationModal.scss';
import { IoClose } from "react-icons/io5";

const NotificationModal = ({ notification, onClose }) => {
    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <button className="close-btn" onClick={onClose}>
                    <IoClose />
                </button>
                <div className="modal-header">
                    <span className="modal-tag">Notification</span>
                    <h2>{notification.text}</h2>
                    <span className="modal-time">{notification.time}</span>
                </div>
                <div className="modal-body">
                    <p>{notification.details}</p>
                </div>
                <div className="modal-footer">
                    <button className="btn-confirm" onClick={onClose}>Understood</button>
                </div>
            </div>
        </div>
    );
};

export default NotificationModal;