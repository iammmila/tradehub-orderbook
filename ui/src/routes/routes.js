import MainRoot from "../pages/Main/MainRoot/MainRoot";
import Home from "../pages/Main/Home/Home";
import Register from "../pages/Main/Register/Register";
import Login from "../pages/Main/Login/Login";
import ProtectedRoute from "./ProtectedRoute";
import { Navigate } from "react-router-dom";
import PublicRoot from "../pages/Main/PublicRoot/PublicRoot";
import LandingPage from "../pages/Main/LandingPage/LandingPage";

export const ROUTES = [
  // PUBLIC
  {
    path: "/",
    element: <PublicRoot />,
    children: [{ index: true, element: <LandingPage /> }],
  },

  // PROTECTED USER APP
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: "/app",
        element: <MainRoot />,
        children: [{ index: true, element: <Home /> }],
      },
    ],
  },
  { path: "/login", element: <Login /> },
  { path: "/register", element: <Register /> },
  // fallback
  { path: "*", element: <Navigate to="/" replace /> },
];
