import React, { useContext, useEffect, useMemo, useState } from 'react';
import './Navbar.scss';
import { useLocation, useNavigate } from 'react-router-dom';
import UserMenu from '../UserMenu/UserMenu';
import { MainContext } from '../../context/ContextProvider';
import NotificationContainer from '../NotificationContainer/NotificationContainer';

function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useContext(MainContext);

  // 1. Determine login status
  const isLoggedIn = !!localStorage.getItem("token");
  const isLanding = location.pathname === "/";

  // 2. Define Public vs Private sections
  const sectionsPublic = useMemo(
    () => [
      { id: "home", label: "Home" },
      { id: "about", label: "About" },
      { id: "features", label: "Features" },
      { id: "contact", label: "Contact" },
    ],
    []
  );

  const sectionsPrivate = useMemo(
    () => [
      { path: "/app/dashboard", label: "Dashboard" },
      { path: "/app/trading", label: "Trading" },
      { path: "/app/orders", label: "Orders" },
      { path: "/app/trades", label: "Trades" },
    ],
    []
  );


  // 3. Set Initial Active State based on Login
  const [activeSection, setActiveSection] = useState("home");

  // 4. Update active tab if login status changes manually
  useEffect(() => {
    setActiveSection(isLoggedIn ? "dashboard" : "home");
  }, [isLoggedIn]);

  // 5. Scroll Spy Logic
  useEffect(() => {
    if (!isLanding) return;

    const elements = sectionsPublic
      .map((s) => document.getElementById(s.id))
      .filter(Boolean);

    if (elements.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

        if (visible?.target?.id) setActiveSection(visible.target.id);
      },
      { root: null, rootMargin: "-35% 0px -55% 0px", threshold: [0, 0.1, 0.25, 0.5, 0.75] }
    );

    elements.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [isLanding, sectionsPublic]);

  const handlePublicClick = (id) => {
    if (!isLanding) {
      navigate("/", { replace: false });
      setTimeout(() => {
        document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
      }, 100);
      return;
    }
    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
  };

  const handlePrivateClick = (path) => {
    navigate(path);
  };

  return (
    <nav className={`navbar ${location.pathname === "/" ? "is-landing" : "is-app"}`}>
      <div className="nav-container">
        <div className="logo" onClick={() => navigate(isLoggedIn ? "/app" : "/")}>
          TRADE<span>HUB</span>
        </div>

        <ul className="nav-links">
          {/* Dynamically Map based on Login Status */}
          {!isLoggedIn ? (
            // PUBLIC LINKS (scroll sections)
            sectionsPublic.map((s) => (
              <li key={s.id}>
                <button
                  type="button"
                  className={`nav-link ${activeSection === s.id ? "active" : ""}`}
                  onClick={() => handlePublicClick(s.id)}
                >
                  {s.label}
                </button>
              </li>
            ))
          ) : (
            // PRIVATE LINKS (routes)
            sectionsPrivate.map((s) => (
              <li key={s.path}>
                <button
                  type="button"
                  className={`nav-link ${location.pathname === s.path ? "active" : ""}`}
                  onClick={() => handlePrivateClick(s.path)}
                >
                  {s.label}
                </button>
              </li>
            ))
          )}
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
            user && (
              <>
                <NotificationContainer />
                <UserMenu />
              </>
            )
          )}
        </div>
      </div>
    </nav>
  );
}

export default Navbar;