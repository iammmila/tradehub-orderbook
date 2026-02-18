import React from 'react'
import './AlertBox.scss'

const AlertBox = ({ error, fieldErrors }) => {
    if (!error && (!fieldErrors || fieldErrors.length === 0)) return null;

    return (
        <div className="settings-error">
            {error && <div className="settings-error__title">{error}</div>}
            {fieldErrors?.length > 0 && (
                <ul className="settings-error__list">
                    {fieldErrors.map((fe, idx) => (
                        <li key={idx}>
                            <span className="fe-field">{fe.field}</span>: {fe.message}
                        </li>
                    ))}
                </ul>
            )}
        </div>)
}

export default AlertBox