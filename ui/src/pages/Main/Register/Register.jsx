import React, { useContext} from 'react'
import './Register.scss'
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../../../api/auth';
import { VscEye, VscEyeClosed } from "react-icons/vsc";
import { MainContext } from '../../../context/ContextProvider';
function Register() {
  const {
    form, setForm,
    showPassword, setShowPassword,
    error, setError
  } = useContext(MainContext);

  const navigate = useNavigate();

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      await register(form);
      navigate("/login", { replace: true });
    } catch (err) {
      setError(err?.response?.data?.message || "Register failed");
    }
  };
  return (
    <div className="auth-container">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h2>Create Account</h2>
        <p>Join us today!</p>

        <div className="name-row">
          <input
            value={form.firstName}
            onChange={onChange}
            name="firstName"
            type="text"
            placeholder="First Name"
            required />
          <input
            value={form.lastName}
            onChange={onChange}
            name="lastName"
            type="text"
            placeholder="Last Name"
            required />
        </div>

        <div className="input-group">
          <input
            value={form.username}
            onChange={onChange}
            name="username"
            type="text"
            placeholder="Username"
            required />
        </div>

        <div className="input-group">
          <input
            value={form.email}
            onChange={onChange}
            name="email"
            type="email"
            placeholder="Email"
            required />
        </div>

        <div className="input-group password-group">
          <input
            value={form.password}
            onChange={onChange}
            name="password"
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

        <button type="submit" className="auth-btn">Register</button>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <div className="auth-footer">
          <span>Already have an account? <Link to="/login">Log in</Link></span>
        </div>
      </form>
    </div>
  )
}

export default Register