import React from 'react'
import './Navbar.scss'
import { useNavigate } from 'react-router-dom';

function Navbar() {
  const navigate = useNavigate();
  
  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login", { replace: true });
  };

  return (
    <div>
      <button className='btn' onClick={handleLogout}>logout</button>
      Navbar</div>
  )
}

export default Navbar