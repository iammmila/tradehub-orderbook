import React, { useState } from 'react'
import './Login.scss'
import { useNavigate } from 'react-router-dom';
import { login } from '../../../api/auth';
function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const data = await login({ username, password });

      const token = data.token ?? data; // supports both response styles
      localStorage.setItem("token", token);

      navigate("/", { replace: true });
    } catch (err) {
      setError(err?.response?.data?.message || "Login failed");
    }
  };

  return (
    <div className="auth-container">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h2>Welcome Back</h2>
        <p>Please enter your details</p>

        <div className="input-group">
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            type="text"
            placeholder="Username" required />
        </div>

        <div className="input-group">
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            placeholder="Password" required />
        </div>

        <button type="submit" className="auth-btn">Sign In</button>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <div className="auth-footer">
          <span>Don't have an account? <a href="/register">Sign up</a></span>
        </div>
      </form>
    </div>
  )
}

export default Login