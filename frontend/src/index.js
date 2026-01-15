import React from "react";
import ReactDOM from "react-dom/client";
import "./index.css";
import App from "./App";
import { BrowserRouter } from "react-router-dom";

// Root render bootstraps routing and the global app shell.
const root = ReactDOM.createRoot(document.getElementById("root"));
root.render(
  <React.StrictMode>
    {/* BrowserRouter enables client-side navigation for all routes. */}
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);
