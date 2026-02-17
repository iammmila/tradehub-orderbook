import React from 'react'
import './MainRoot.scss'
import { Outlet } from 'react-router-dom'
import Navbar from '../../../components/Navbar/Navbar'
import Footer from '../../../components/Footer/Footer'

function MainRoot() {
  return (
    <div className='app-shell'>
      <Navbar />
      <main className='main-content'>
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

export default MainRoot