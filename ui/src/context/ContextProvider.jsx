import React, { useState, useRef, useEffect } from 'react'
import { createContext } from 'react'
import axios from "axios"
export const MainContext = createContext(null)

function ContextProvider({ children }) {

    const values = {

    }
    return (
        <MainContext.Provider value={values}>
            {children}
        </MainContext.Provider>
    )
}
export default ContextProvider