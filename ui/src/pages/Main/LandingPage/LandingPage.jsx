import React from 'react'
import './LandingPage.scss'
import Landing from '../../../components/Landing/Landing/Landing'
import About from '../../../components/Landing/About/About'
import Features from '../../../components/Landing/Features/Features'
import { isAuthenticated } from '../../../api/auth';
import { Navigate } from 'react-router-dom';
import Contact from '../../../components/Landing/Contact/Contact';
import { Helmet } from 'react-helmet'
const LandingPage = () => {
  if (isAuthenticated()) return <Navigate to="/app" replace />;

  return (
    <>
      <Helmet>
        <title>Landing | Trading</title>
        <meta name='description' content='It is Landing page of Trading Application' />
      </Helmet>
      <Landing />
      <About />
      <Features />
      <Contact />
    </>
  )
}

export default LandingPage