import { useState } from "react";
import { Routes, Route } from "react-router-dom";
import Topbar from "./scenes/global/Topbar";
import Sidebar from "./scenes/global/Sidebar";
import Dashboard from "./scenes/dashboard";
import Form from "./scenes/oem";
import OEM from "./scenes/oem";
import Agent from "./scenes/agent";
import Aggregator from "./scenes/aggregator";
import TSO from "./scenes/tso";
import { CssBaseline, ThemeProvider } from "@mui/material";
import { ColorModeContext, useMode } from "./theme";

function App() {
  /* Global theme and color-mode state for the entire app shell. */
  const [theme, colorMode] = useMode();
  const [isSidebar, setIsSidebar] = useState(true);

  return (
    <ColorModeContext.Provider value={colorMode}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {/* App shell wraps routing with shared navigation. */}
        <div className="app">
          <Sidebar isSidebar={isSidebar} />
          <main className="content">
            <Topbar setIsSidebar={setIsSidebar} />
            {/* Route mapping for the dashboard and feature views. */}
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/form" element={<Form />} />
              <Route path="/oem" element={<OEM/>} />
              <Route path="/agent" element={<Agent/>} />
              <Route path="/aggregator" element={<Aggregator/>} />
              <Route path="/tso" element={<TSO/>} />
            </Routes>
          </main>
        </div>
      </ThemeProvider>
    </ColorModeContext.Provider>
  );
}

export default App;
