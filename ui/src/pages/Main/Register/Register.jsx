import React, { useContext } from 'react'
import './Register.scss'
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../../../api/auth';
import { VscEye, VscEyeClosed } from "react-icons/vsc";
import { MainContext } from '../../../context/ContextProvider';
import { registerSchema } from "../../../schema/registerSchema";
import { Helmet } from 'react-helmet';
import { FiArrowLeft } from 'react-icons/fi';
import GoogleButton from '../../../components/GoogleButton/GoogleButton';

function Register() {
  const {
    form, setForm,
    showPassword, setShowPassword,
    fieldErrors, setFieldErrors
  } = useContext(MainContext);
  const navigate = useNavigate();
  const [error, setError] = React.useState("");
  const validateOneField = async (name, value) => {
    try {
      await registerSchema.validateAt(name, { ...form, [name]: value });
      setFieldErrors((prev) => ({ ...prev, [name]: "" }));
    } catch (e) {
      setFieldErrors((prev) => ({ ...prev, [name]: e.message }));
    }
  };

  const onChange = async (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));

    validateOneField(name, value); //validate while typing
  };

  const onBlur = async (e) => {
    const { name, value } = e.target;
    validateOneField(name, value);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    // validate whole form before sending
    try {
      await registerSchema.validate(form, { abortEarly: false });
      setFieldErrors({});
    } catch (e) {
      const mapped = {};
      e.inner?.forEach((err) => {
        if (err.path) mapped[err.path] = err.message;
      });
      setFieldErrors(mapped);
      return;
    }

    try {
      await register(form);
      navigate("/login", { replace: true });
      setForm({ username: "", password: "", email: "", firstName: "", lastName: "" });
      setFieldErrors({});
    } catch (err) {
      const data = err?.response?.data;

      // backend @Valid errors (fieldErrors array)
      if (data?.fieldErrors?.length) {
        const mapped = {};
        data.fieldErrors.forEach((fe) => {
          mapped[fe.field] = fe.message;
        });
        setFieldErrors(mapped);
        return;
      }

      // backend custom errors
      if (data?.code === "USER_EMAIL_ALREADY_EXISTS") {
        setFieldErrors((prev) => ({ ...prev, email: data.message }));
        return;
      }
      if (data?.code === "USER_USERNAME_ALREADY_EXISTS") {
        setFieldErrors((prev) => ({ ...prev, username: data.message }));
        return;
      }

      setError(data?.message || "Register failed");
    }
  };
  return (
    <div className="auth-container">
      <Helmet>
        <title>Register | Trading</title>
        <meta name='description' content='It is Register page of Trading Application' />
      </Helmet>
      <form className="auth-card" onSubmit={handleSubmit}>
        <button
          type="button"
          className="back-btn"
          onClick={() => window.history.length > 1 ? navigate(-1) : navigate("/")}
          aria-label="Back to landing"
        >
          <FiArrowLeft />
        </button>
        <h2>Create Account</h2>
        <p>Join us today!</p>
       
        <GoogleButton text="Continue with Google" />
        
        <div className="auth-divider">
          <span>or</span>
        </div>

        <div className="name-row">
          <input
            value={form.firstName}
            onChange={onChange}
            onBlur={onBlur}
            name="firstName"
            type="text"
            placeholder="First Name"
            required />
          <input
            value={form.lastName}
            onChange={onChange}
            onBlur={onBlur}
            name="lastName"
            type="text"
            placeholder="Last Name"
            required />
        </div>
        {fieldErrors.firstName && <small style={{ color: "red" }} className="error">{fieldErrors.firstName}</small>}
        {fieldErrors.lastName && <small style={{ color: "red" }} className="error">{fieldErrors.lastName}</small>}

        <div className="input-group">
          <input
            value={form.username}
            onChange={onChange}
            onBlur={onBlur}
            name="username"
            type="text"
            placeholder="Username"
            required />
          {fieldErrors.username && <small style={{ color: "red" }} className="error">{fieldErrors.username}</small>}
        </div>

        <div className="input-group">
          <input
            value={form.email}
            onChange={onChange}
            onBlur={onBlur}
            name="email"
            type="email"
            placeholder="Email"
            required />
          {fieldErrors.email && <small style={{ color: "red" }} className="error">{fieldErrors.email}</small>}
        </div>

        <div className="input-group password-group">
          <input
            value={form.password}
            onChange={onChange}
            onBlur={onBlur}
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
        {fieldErrors.password && <small style={{ color: "red" }} className="error">{fieldErrors.password}</small>}
        <button
          type="submit"
          className="auth-btn"
          disabled={Object.values(fieldErrors).some((v) => v)}
        >Register</button>
        {error && <p style={{ color: "red" }}>{error}</p>}
        <div className="auth-footer">
          <span>Already have an account? <Link to="/login">Log in</Link></span>
        </div>
      </form>
    </div>
  )
}

export default Register