import React from 'react'
import './ProfileCard.scss'

const ProfileCard = ({ initials, uiUser }) => {

  return (
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
  )
}

export default ProfileCard