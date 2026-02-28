import MainRoot from "../pages/Main/MainRoot/MainRoot";
import Register from "../pages/Main/Register/Register";
import Login from "../pages/Main/Login/Login";
import ProtectedRoute from "./ProtectedRoute";
import { Navigate } from "react-router-dom";
import PublicRoot from "../pages/Main/PublicRoot/PublicRoot";
import LandingPage from "../pages/Main/LandingPage/LandingPage";
import Settings from "../pages/Main/Settings/Settings";
import Dashboard from "../pages/Main/Dashboard/Dashboard";
import Orders from "../pages/Main/Orders/Orders";
import Trades from "../pages/Main/Trades/Trades";
import TradingPage from "../pages/Main/TradingPage/TradingPage";

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
        children: [
          { index: true, element: <Navigate to="dashboard" replace /> },
          {
            path: "dashboard",
            element: <Dashboard />,
          },
          {
            path: "orders",
            element: <Orders />,
          },
          {
            path: "trades",
            element: <Trades />,
          },
          {
            path: "trading",
            element: <TradingPage />,
          },
          { path: "settings", element: <Settings /> },
        ],
      },
    ],
  },
  { path: "/login", element: <Login /> },
  { path: "/register", element: <Register /> },
  // fallback
  { path: "*", element: <Navigate to="/" replace /> },
];
