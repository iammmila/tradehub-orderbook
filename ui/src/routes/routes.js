import MainRoot from "../pages/Main/MainRoot";
import Home from "../pages/Main/Home/Home";
import Register from "../pages/Main/Register/Register";
import Login from "../pages/Main/Login/Login";
import ProtectedRoute from "./ProtectedRoute";

export const ROUTES = [
  {
    element: <ProtectedRoute />,
    children: [
      {
        path: "/",
        element: <MainRoot />,
        children: [
          { path: "", element: <Home /> },
          // add other pages here that must be protected
        ],
      },
    ],
  },
  { path: "/login", element: <Login /> },
  { path: "/register", element: <Register /> },
];
