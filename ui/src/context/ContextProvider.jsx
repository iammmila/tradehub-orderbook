import React, { useState, useRef, useEffect } from 'react'
import { createContext } from 'react'
import axios from "axios"
export const MainContext = createContext(null)

function ContextProvider({ children }) {
    //! LOGIN & REGISTER PAGES
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [form, setForm] = useState({
        username: "",
        password: "",
        email: "",
        firstName: "",
        lastName: ""
    });

    const values = {
        username, setUsername,
        password, setPassword,
        showPassword, setShowPassword,
        error, setError,
        form, setForm,

    }
    return (
        <MainContext.Provider value={values}>
            {children}
        </MainContext.Provider>
    )
}
export default ContextProvider