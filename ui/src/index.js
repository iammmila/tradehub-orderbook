import React from "react";
import ReactDOM from "react-dom/client";
import "./index.scss";
import App from "./App";
import ContextProvider from "./context/ContextProvider";
import { WsStatusProvider } from "./context/WsStatusContext";

const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
  <ContextProvider>
    <WsStatusProvider>
      <App />
    </WsStatusProvider>
  </ContextProvider>,
);
