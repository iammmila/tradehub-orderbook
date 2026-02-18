import React from 'react'
import './Landing.scss'
import { useNavigate } from 'react-router-dom';

function Landing() {
  const navigate = useNavigate();
  return (
    <section className="hero-section" id="home">
      <div className="hero-content">
        <h1>Trade the Future <br /> <span>with Precision.</span></h1>
        <p>Real-time analytics, lightning-fast execution, and institutional-grade security for the modern trader.</p>
        <div className="hero-btns">
          <button className="get-started" onClick={() => navigate("/login")}>Open Free Account</button>
        </div>
      </div>
    </section>
  )
}

export default Landing