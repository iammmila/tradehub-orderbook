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
import OrderRoot from "../pages/Main/OrderRoot/OrderRoot";
import OrderDetails from "../pages/Main/OrderDetails/OrderDetails";
import TradeDetails from "../pages/Main/TradeDetails/TradeDetails";
import TradeRoot from "../pages/Main/TradeRoot/TradeRoot";
import OAuth2Success from "../pages/Main/OAuth2Success/OAuth2Success"
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
            element: <OrderRoot />,
            children: [
              {
                path: "",
                element: <Orders />,
              },
              {
                path: ":id",
                element: <OrderDetails />,
              },
            ],
          },
          {
            path: "trades",
            element: <TradeRoot />,
            children: [
              {
                path: "",
                element: <Trades />,
              },
              {
                path: ":id",
                element: <TradeDetails />,
              },
            ],
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
  { path: "/oauth2/success", element:<OAuth2Success/> },
  // fallback
  { path: "*", element: <Navigate to="/" replace /> },
];
