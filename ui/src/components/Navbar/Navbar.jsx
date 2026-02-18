import React, { useContext, useEffect, useMemo, useState } from 'react'
import './Navbar.scss'
import { useLocation, useNavigate } from 'react-router-dom';
import UserMenu from '../UserMenu/UserMenu';
import { MainContext } from '../../context/ContextProvider';

function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useContext(MainContext);
  const sections = useMemo(
    () => [
      { id: "home", label: "Home" },
      { id: "about", label: "About" },
      { id: "features", label: "Features" },
      { id: "contact", label: "Contact" },
    ],
    []
  );
  const [activeSection, setActiveSection] = useState("home");

  useEffect(() => {
    if (location.pathname !== "/") return;

    const elements = sections
      .map((s) => document.getElementById(s.id))
      .filter(Boolean);

    if (!elements.length) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

        if (visible?.target?.id) setActiveSection(visible.target.id);
      },
      {
        root: null,
        rootMargin: "-35% 0px -55% 0px",
        threshold: [0, 0.1, 0.25, 0.5, 0.75],
      }
    );

    elements.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [sections, location.pathname]);
  
  const scrollTo = (id) => {
    if (location.pathname !== "/") {
      navigate("/", { replace: false });
      setTimeout(() => {
        document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
      }, 50);
      return;
    }

    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
  };

  const isLoggedIn = !!localStorage.getItem("token");

  return (
    <nav className={`navbar ${location.pathname === "/" ? "is-landing" : "is-app"}`}>
      <div className="nav-container">
        <div className="logo" onClick={() => navigate(isLoggedIn ? "/app" : "/")}>
          TRADE<span>HUB</span>
        </div>

        <ul className="nav-links">
          {sections.map((s) => (
            <li key={s.id}>
              <button
                type="button"
                className={`nav-link ${activeSection === s.id ? "active" : ""}`}
                onClick={() => scrollTo(s.id)}
              >
                {s.label}
              </button>
            </li>
          ))}
        </ul>

        <div className="nav-actions">
          {!isLoggedIn ? (
            <>
              <button className="btn-secondary" onClick={() => navigate("/login")}>
                Login
              </button>
              <button className="btn-primary" onClick={() => navigate("/register")}>
                Register
              </button>
            </>
          ) : (
            user ? <UserMenu /> : null
          )}
        </div>
      </div>
    </nav>
  )
}

export default Navbar