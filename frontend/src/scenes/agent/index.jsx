import {
  Box,
  Typography,
  useTheme,
  Select,
  MenuItem,
  TextField,
  Button,
  Divider,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Alert,
  Stack,
  Chip,
} from "@mui/material";
import Header from "../../components/Header";
import { tokens } from "../../theme";
import { useEffect, useState } from "react";
import { api } from "../../api";

const Agent = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [devices, setDevices] = useState([]);
  const [oems, setOems] = useState([]);
  const [whitelist, setWhitelist] = useState([]);
  const [status, setStatus] = useState(null);

  /* ===== AUTOMATION STATE ===== */
  const [automationActive, setAutomationActive] = useState(false);
  const [intervalMinutes, setIntervalMinutes] = useState(null);

  const [form, setForm] = useState({
    deviceId: "",
    maxFlexKW: "",
    timeWindowStart: 100,
    timeWindowEnd: 200,
  });

  const timeWindow = {
    start: Number(form.timeWindowStart),
    end: Number(form.timeWindowEnd),
  };

  const loadData = async () => {
    try {
      const [oemData, deviceData, wl] = await Promise.all([
        api.agent.listOems(),
        api.agent.listDevices(),
        api.agent.listWhitelist(),
      ]);
      setOems(oemData);
      setDevices(deviceData);
      setWhitelist(wl);
    } catch (e) {
      setStatus({ type: "error", message: e.message });
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  /* ===== SEED ===== */
  const seed = async () => {
    setStatus(null);
    try {
      await api.agent.seedDemo();
      setStatus({ type: "success", message: "Seeded OEM + devices" });
      await loadData();
    } catch (e) {
      setStatus({ type: "error", message: e.message });
    }
  };


  /* ===== CONSENT ===== */
  const pushConsent = async () => {
    setStatus(null);
    try {
      const payload = {
        deviceId: form.deviceId,
        maxFlexKW: Number(form.maxFlexKW),
        timeWindow,
      };
      const result = await api.agent.pushConsent(payload);
      setStatus({
        type: "success",
        message: `Consent sent: ${result.aggregatorResponse?.id || ""}`,
      });
      await loadData();
    } catch (e) {
      setStatus({ type: "error", message: e.body?.error || e.message });
    }
  };

  /* ===== AUTOMATION HANDLERS ===== */
  const startAutomation = (minutes) => {
    setIntervalMinutes(minutes);
    setAutomationActive(true);
    setStatus({
      type: "success",
      message: `Automation started (${minutes} min interval)`,
    });
  };

  const stopAutomation = () => {
    setAutomationActive(false);
    setIntervalMinutes(null);
    setStatus({ type: "success", message: "Automation stopped" });
  };

  return (
    <Box m="20px">
      <Header
        title="Agent"
        subtitle="Consent Credential Creation & Dispatch"
      />

      {/* ===== STATUS ===== */}
      <Stack spacing={2} mb="20px">
        {status && <Alert severity={status.type}>{status.message}</Alert>}
        <Box display="flex" gap="10px">
          <Button variant="contained" onClick={seed}>
            Seed OEM & Devices
          </Button>
          <Button variant="outlined" onClick={loadData}>
            Refresh Data
          </Button>
        </Box>
      </Stack>

      {/* ===== FORM ===== */}
      <Box p="20px" mb="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Consent Push
        </Typography>

        <Box
          mt="15px"
          display="grid"
          gridTemplateColumns="repeat(2, 1fr)"
          gap="20px"
        >
          <Select
            displayEmpty
            value={form.deviceId}
            onChange={(e) =>
              setForm({ ...form, deviceId: e.target.value })
            }
          >
            <MenuItem value="">
              <em>Select device</em>
            </MenuItem>
            {devices.map((d) => (
              <MenuItem
                key={d.payload.deviceId}
                value={d.payload.deviceId}
              >
                {d.payload.deviceName} ({d.payload.deviceId})
              </MenuItem>
            ))}
          </Select>

          <TextField
            label="Max Flex (kW)"
            type="number"
            value={form.maxFlexKW}
            onChange={(e) =>
              setForm({ ...form, maxFlexKW: e.target.value })
            }
          />

          <TextField
            label="TimeWindow Start"
            type="number"
            value={form.timeWindowStart}
            onChange={(e) =>
              setForm({ ...form, timeWindowStart: e.target.value })
            }
          />

          <TextField
            label="TimeWindow End"
            type="number"
            value={form.timeWindowEnd}
            onChange={(e) =>
              setForm({ ...form, timeWindowEnd: e.target.value })
            }
          />
        </Box>

        <Button
          sx={{ mt: "20px" }}
          variant="contained"
          color="success"
          onClick={pushConsent}
          disabled={!form.deviceId}
        >
          Create & Push Consent
        </Button>
      </Box>

      {/* ===== DEVICES ===== */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Devices (Agent Wallet)
        </Typography>
        <Divider sx={{ my: "10px" }} />
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Device</TableCell>
              <TableCell>Type</TableCell>
              <TableCell>OEM</TableCell>
              <TableCell>Area</TableCell>
              <TableCell>Rated kW</TableCell>
              <TableCell>Avail Flex kW</TableCell>
              <TableCell>Cap kW</TableCell>
              <TableCell>Location</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {devices.map((d) => (
              <TableRow key={d.payload.deviceId}>
                <TableCell>{d.payload.deviceName}</TableCell>
                <TableCell>{d.payload.deviceType}</TableCell>
                <TableCell>{d.payload.oemId}</TableCell>
                <TableCell>{d.payload.gridConnectionArea}</TableCell>
                <TableCell>{d.payload.ratedPowerKW}</TableCell>
                <TableCell>{d.payload.availableFlexKW}</TableCell>
                <TableCell>{d.payload.maxFlexCapKW}</TableCell>
                <TableCell>{d.payload.location}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>

      {/* ===== OEMS / WHITELIST ===== */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          OEMs & Whitelist
        </Typography>
        <Divider sx={{ my: "10px" }} />
        <Typography variant="body2" mb="10px">
          Whitelisted deviceIds: {whitelist.join(", ") || "none"}
        </Typography>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>OEM</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {oems.map((o) => (
              <TableRow key={o.payload.oemId}>
                <TableCell>{o.payload.oemId}</TableCell>
                <TableCell>{o.payload.name}</TableCell>
                <TableCell>{o.payload.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>

      {/* ===== AUTOMATION (BOTTOM) ===== */}
      <Box p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Automation
        </Typography>

        <Typography variant="body2" mt="10px" mb="10px">
          Automatically push consents at fixed intervals.
        </Typography>

        <Stack direction="row" spacing={2} mb="10px">
          {[5, 10, 15].map((min) => (
            <Button
              key={min}
              variant={intervalMinutes === min ? "contained" : "outlined"}
              color="success"
              onClick={() => startAutomation(min)}
              disabled={automationActive && intervalMinutes === min}
            >
              {min} min
            </Button>
          ))}

          <Button
            variant="outlined"
            color="error"
            onClick={stopAutomation}
            disabled={!automationActive}
          >
            Stop
          </Button>
        </Stack>

        <Chip
          label={
            automationActive
              ? `ACTIVE – every ${intervalMinutes} min`
              : "INACTIVE"
          }
          color={automationActive ? "success" : "default"}
        />
      </Box>
    </Box>
  );
};

export default Agent;
