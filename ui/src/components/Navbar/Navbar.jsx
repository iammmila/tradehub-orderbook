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

  // 2. Define Public vs Private sections
  const sectionsPublic = useMemo(() => [
    { id: "home", label: "Home" },
    { id: "about", label: "About" },
    { id: "features", label: "Features" },
    { id: "contact", label: "Contact" },
  ], []);

  const sectionsPrivate = useMemo(() => [
    { id: "dashboard", label: "Dashboard" },
    { id: "orders", label: "Orders" },
    { id: "trades", label: "Trades" },
    // { id: "contact", label: "Contact" },
  ], []);

  // 3. Set Initial Active State based on Login
  const [activeSection, setActiveSection] = useState(isLoggedIn ? "dashboard" : "home");

  // 4. Update active tab if login status changes manually
  useEffect(() => {
    setActiveSection(isLoggedIn ? "dashboard" : "home");
  }, [isLoggedIn]);

  // 5. Scroll Spy Logic
  useEffect(() => {
    // Only run scroll-spy on the landing page (/)
    if (location.pathname !== "/") return;

    // Get the IDs for the current view
    const currentSections = isLoggedIn ? sectionsPrivate : sectionsPublic;

    const elements = currentSections
      .map((s) => document.getElementById(s.id))
      .filter(Boolean);

    if (elements.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries
          .filter((e) => e.isIntersecting)
          .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];

        if (visible?.target?.id) {
          setActiveSection(visible.target.id);
        }
      },
      {
        root: null,
        rootMargin: "-35% 0px -55% 0px", // Focus on the middle of the viewport
        threshold: [0, 0.1, 0.25, 0.5, 0.75],
      }
    );

    elements.forEach((el) => observer.observe(el));

    return () => observer.disconnect();
  }, [sectionsPublic, sectionsPrivate, location.pathname, isLoggedIn]);

  // 6. Navigation Handler
  const scrollTo = (id) => {
    if (location.pathname !== "/") {
      navigate("/", { replace: false });
      // Small timeout to allow the DOM to render after navigation
      setTimeout(() => {
        document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
      }, 100);
      return;
    }

    document.getElementById(id)?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <nav className={`navbar ${location.pathname === "/" ? "is-landing" : "is-app"}`}>
      <div className="nav-container">
        <div className="logo" onClick={() => navigate(isLoggedIn ? "/app" : "/")}>
          TRADE<span>HUB</span>
        </div>

        <ul className="nav-links">
          {/* Dynamically Map based on Login Status */}
          {(isLoggedIn ? sectionsPrivate : sectionsPublic).map((s) => (
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