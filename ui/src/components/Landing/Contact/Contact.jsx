import React from 'react'
import './Contact.scss'

const Contact = () => {
  return (
    <section className="contact-section" id="contact">
      <div className="contact-box">
        <h2>Get in <span>Touch.</span></h2>
        <form onSubmit={(e) => e.preventDefault()}>
          <input type="email" placeholder="Enter your email" required />
          <textarea placeholder="How can we help?" rows="4"></textarea>
          <button type="submit" className="submit-btn">Send Message</button>
        </form>
      </div>
    </section>
  )
}

export default Contact