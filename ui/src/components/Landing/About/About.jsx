import React from 'react'
import './About.scss'

const About = () => {
  return (
    <section className="about-section" id="about">
      <div className="about-container">
        <div className="about-text">
          <h6>OUR MISSION</h6>
          <h2>Empowering the <span>Next Generation</span> of Traders.</h2>
          <p>TradeHub was built on the principle that professional-grade trading tools should be accessible to everyone. We combine speed, security, and simplicity.</p>
        </div>
        <div className="about-stats">
          <div className="stat-item"><h4>$2B+</h4><p>Trading Volume</p></div>
          <div className="stat-item"><h4>0.1ms</h4><p>Execution Speed</p></div>
        </div>
      </div>
    </section>
  )
}

export default About