import {
  Box,
  Typography,
  useTheme,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Divider,
  Chip,
  Stack,
} from "@mui/material";
import Header from "../../components/Header";
import { tokens } from "../../theme";
import { useEffect, useState } from "react";
import { api } from "../../api";

const Aggregator = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [consents, setConsents] = useState([]);
  const [presentations, setPresentations] = useState([]);
  const [status, setStatus] = useState(null);

  /* ===== AUTOMATION STATE ===== */
  const [automationActive, setAutomationActive] = useState(false);
  const [intervalMinutes, setIntervalMinutes] = useState(null);

  const loadData = async () => {
    try {
      const [c, p] = await Promise.all([
        api.aggregator.listConsents(),
        api.aggregator.listPresentations(),
      ]);
      setConsents(c);
      setPresentations(p);
    } catch (e) {
      setStatus({ type: "error", message: e.message });
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  /* ===== MANUAL AGGREGATION ===== */
  const aggregate = async () => {
    setStatus(null);
    try {
      const pres = await api.aggregator.aggregate();
      setStatus({
        type: "success",
        message: `Presentation created: ${pres.id}`,
      });
      await loadData();
    } catch (e) {
      setStatus({ type: "error", message: e.message });
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
    setStatus({
      type: "success",
      message: "Automation stopped",
    });
  };

  return (
    <Box m="20px">
      <Header
        title="Aggregator"
        subtitle="Aggregate Consents, Create ZKP & Deliver to TSO"
      />

      {/* ===== STATUS ===== */}
      {status && (
        <Box mb="10px">
          <Typography
            color={
              status.type === "error"
                ? colors.redAccent[400]
                : colors.greenAccent[400]
            }
          >
            {status.message}
          </Typography>
        </Box>
      )}

      {/* ===== AGGREGATION ===== */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Aggregation
        </Typography>

        <Typography mt="10px" variant="body2">
          Received Consents: <b>{consents.length}</b>
        </Typography>

        <Button
          sx={{ mt: "20px" }}
          variant="contained"
          color="success"
          onClick={aggregate}
        >
          Create Aggregated Presentation
        </Button>

        <Button
          sx={{ mt: "20px", ml: "10px" }}
          variant="outlined"
          onClick={loadData}
        >
          Refresh
        </Button>
      </Box>

      {/* ===== RECEIVED CONSENTS ===== */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Received Consents
        </Typography>

        <Divider sx={{ my: "10px" }} />

        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Device</TableCell>
              <TableCell>Time Window</TableCell>
              <TableCell>Flex kW</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {consents.map((c) => (
              <TableRow key={c.id}>
                <TableCell>{c.id}</TableCell>
                <TableCell>
                  {c.payload?.deviceId || c.payload?.deviceID}
                </TableCell>
                <TableCell>
                  {c.payload?.timeWindow?.start} –{" "}
                  {c.payload?.timeWindow?.end}
                </TableCell>
                <TableCell>{c.payload?.maxFlexKW}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>

      {/* ===== PRESENTATIONS ===== */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Presentations
        </Typography>

        <Divider sx={{ my: "10px" }} />

        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Timestamp</TableCell>
              <TableCell>ID</TableCell>
              <TableCell>Consents</TableCell>
              <TableCell>Total kW</TableCell>
              <TableCell>ZKP</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {presentations.map((row) => (
              <TableRow key={row.id}>
                <TableCell>
                  {row.createdAt
                    ? new Date(row.createdAt).toLocaleString()
                    : ""}
                </TableCell>
                <TableCell>{row.id}</TableCell>
                <TableCell>{row.consentCount}</TableCell>
                <TableCell>{row.totalFlexKW}</TableCell>
                <TableCell>
                  <Chip
                    label={row.proof ? "CREATED" : "MISSING"}
                    color={row.proof ? "success" : "default"}
                    size="small"
                  />
                </TableCell>
                <TableCell sx={{ color: colors.greenAccent[400] }}>
                  {row.status}
                </TableCell>
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
          Automatically create aggregated presentations at fixed intervals.
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

export default Aggregator;
