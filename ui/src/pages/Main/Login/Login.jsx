import React, { useContext } from 'react'
import './Login.scss'
import { Link, useNavigate } from 'react-router-dom';
import { login } from '../../../api/auth';
import { VscEye, VscEyeClosed } from "react-icons/vsc";
import { MainContext } from '../../../context/ContextProvider';
import { Helmet } from 'react-helmet';
import { FiArrowLeft } from "react-icons/fi";

function Login() {
  const { username, setUsername,
    password, setPassword,
    showPassword, setShowPassword,
    error, setError, fetchMe
  } = useContext(MainContext);

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const data = await login({ username, password });

      const token = data.token ?? data;
      localStorage.setItem("token", token);
      await fetchMe();
      navigate("/app", { replace: true });
      setUsername("");
      setPassword("");
    } catch (err) {
      setError(err?.response?.data?.message || "Login failed");
    }
  };

  return (
    <div className="auth-container">
      <Helmet>
        <title>Login | Trading</title>
        <meta name='description' content='It is Login page of Trading Application' />
      </Helmet>
     
      <form className="auth-card" onSubmit={handleSubmit}>
        <button
          type="button"
          className="back-btn"
          onClick={() => navigate("/", { replace: true })}
          aria-label="Back to landing"
        >
          <FiArrowLeft />
        </button>
        <h2>Welcome Back</h2>
        <p>Please enter your details</p>

        <div className="input-group">
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            type="text"
            placeholder="Username" required />
        </div>

        <div className="input-group password-group">
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type={showPassword ? "text" : "password"}
            placeholder="Password"
            required
          />

          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowPassword(prev => !prev)}
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? <VscEyeClosed /> : <VscEye />}
          </button>
        </div>

        <button type="submit" className="auth-btn">Sign In</button>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <div className="auth-footer">
          <span>Don't have an account? <Link to="/register">Sign up</Link></span>
        </div>
      </form>
    </div>
  )
}

export default Login