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
    <nav className="navbar">
      <div className="nav-container">
        <div className="logo">TRADE<span>HUB</span></div>
        <ul className="nav-links">
          <li><a href="#home">Home</a></li>
          <li><a href="#about">About</a></li>
          <li><a href="#features">Features</a></li>
          <li><a href="#contact">Contact</a></li>
        </ul>
        <div className="nav-actions">
          <button className="btn-secondary" onClick={() => navigate("/login")}>Login</button>
          <button className="btn-primary" onClick={() => navigate("/register")}>Register</button>
        </div>
      </div>
    </nav>
  )
}

export default Navbar