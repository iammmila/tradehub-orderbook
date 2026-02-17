import React from 'react'
import './Landing.scss'
import { Helmet } from 'react-helmet'
import { isAuthenticated } from '../../../api/auth';
import { Navigate } from 'react-router-dom';

function Landing() {
  if (isAuthenticated()) return <Navigate to="/app" replace />;
  return (
    <section className="hero-section">
      <Helmet>
        <title>Landing | Trading</title>
        <meta name='description' content='It is Landing page of Trading Application' />
      </Helmet>

      <div className="hero-content">
        <h1>Trade the Future <br /> <span>with Precision.</span></h1>
        <p>Real-time analytics, lightning-fast execution, and institutional-grade security for the modern trader.</p>
        <div className="hero-btns">
          <button className="get-started">Open Free Account</button>
        </div>
      </div>
    </section>
  )
}

export default Landing