import React, { useState, useRef, useEffect } from 'react'
import { createContext } from 'react'
import { isAuthenticated } from '../api/auth';
import { getMe } from '../api/users';
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
    const [fieldErrors, setFieldErrors] = useState({});

    //!USERMENU, SETTINGS
    const [user, setUser] = useState(null);
    const [loadingUser, setLoadingUser] = useState(false);
    const fetchMe = async () => {
        if (!isAuthenticated()) {
            setUser(null);
            return;
        }

        setLoadingUser(true);
        try {
            const me = await getMe();
            setUser(me);
        } catch (err) {
            // token invalid/expired -> logout
            localStorage.removeItem("token");
            setUser(null);
        } finally {
            setLoadingUser(false);
        }
    };

    // load user on first app load if token exists
    useEffect(() => {
        fetchMe();
    }, []);

    const logout = () => {
        localStorage.removeItem("token");
        setUser(null);
    };

    const values = {
        username, setUsername,
        password, setPassword,
        showPassword, setShowPassword,
        error, setError,
        form, setForm,
        fieldErrors, setFieldErrors,
        user, setUser,
        loadingUser, setLoadingUser,
        fetchMe,
        logout
    }
    return (
        <MainContext.Provider value={values}>
            {children}
        </MainContext.Provider>
    )
}
export default ContextProvider