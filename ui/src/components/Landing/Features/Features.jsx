import React from 'react'
import './Features.scss'

const Features = () => {
    const features = [
        { title: "Real-time Data", desc: "Institutional-grade low-latency data feeds for instant decision making." },
        { title: "Advanced Charts", desc: "Professional technical analysis tools with over 100+ indicators." },
        { title: "Secure Wallet", desc: "Multi-sig cold storage and end-to-end encryption for your assets." }
    ];
    return (
        <section className="features-section" id="features">
            <div className="container">
                <h2>Built for <span>Performance.</span></h2>
                <div className="features-grid">
                    {features.map((f, i) => (
                        <div key={i} className="feature-card">
                            <h3>{f.title}</h3>
                            <p>{f.desc}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    )
}

export default Features