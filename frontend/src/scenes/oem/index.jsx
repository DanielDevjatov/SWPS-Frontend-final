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
} from "@mui/material";
import Header from "../../components/Header";
import { tokens } from "../../theme";
import { useEffect, useState } from "react";
import { api } from "../../api";

/* OEM view: read-only access to OEMs and device specs stored by Agent. */
const OEM = () => {
  const theme = useTheme();
  // Theme tokens keep the view aligned with the global palette.
  const colors = tokens(theme.palette.mode);

  /* Agent wallet data surfaced for OEM visibility. */
  const [oems, setOems] = useState([]);
  const [devices, setDevices] = useState([]);
  const [status, setStatus] = useState(null);

  /* Load OEMs and devices from the Agent service. */
  const loadData = async () => {
    try {
      const [os, ds] = await Promise.all([api.agent.listOems(), api.agent.listDevices()]);
      setOems(os);
      setDevices(ds);
    } catch (e) {
      setStatus({ type: "error", message: e.message });
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Demo seeding is an optional shortcut to populate sample data.
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

  return (
    <Box m="20px">
      <Header title="OEM" subtitle="Device Specifications (from Agent wallet)" />

      {status && (
        <Box mb="10px">
          <Typography color={status.type === "error" ? colors.redAccent[400] : colors.greenAccent[400]}>
            {status.message}
          </Typography>
        </Box>
      )}

      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          OEMs
        </Typography>
        <Button sx={{ mt: "10px", mr: "10px" }} variant="contained" onClick={seed}>
          Seed Demo
        </Button>
        <Button sx={{ mt: "10px" }} variant="outlined" onClick={loadData}>
          Refresh
        </Button>
        <Divider sx={{ my: "10px" }} />
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>OEM ID</TableCell>
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

      <Box p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Devices
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
    </Box>
  );
};

export default OEM;
