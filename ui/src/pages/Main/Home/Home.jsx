import React from 'react'
import './Home.scss'
import { Helmet } from 'react-helmet-async'
function Home() {
  return (
    <section className="hero-section">
      <Helmet>
        <title>Home | Trading</title>
        <meta name='description' content='It is Home page of Trading Application' />
      </Helmet>

      <div className="hero-content">
        <h1>Trade the Future <br /> <span>with Precision.</span></h1>
        <p>Real-time analytics, lightning-fast execution, and institutional-grade security for the modern trader.</p>
        {/* <div className="hero-btns">
          <button className="get-started">Open Free Account</button>
        </div> */}
      </div>
    </section>
  )
}

export default Home