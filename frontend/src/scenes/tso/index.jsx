import {
  Box,
  Typography,
  useTheme,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Divider,
  Button,
} from "@mui/material";
import Header from "../../components/Header";
import { tokens } from "../../theme";
import { useEffect, useState, useMemo } from "react";
import { api } from "../../api";

/* ===== RECHARTS ===== */
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";

const TSO = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [presentations, setPresentations] = useState([]);
  const [verifications, setVerifications] = useState([]);
  const [aggPresentations, setAggPresentations] = useState([]);
  const [status, setStatus] = useState(null);

  const loadData = async () => {
    try {
      const [tp, tv, ap] = await Promise.all([
        api.tso.listPresentations(),
        api.tso.listVerifications(),
        api.aggregator.listPresentations(),
      ]);
      // Präsentationsnummer hinzufügen
      const tpNumbered = tp.map((p, idx) => ({ ...p, presentationNumber: idx + 1 }));
      setPresentations(tpNumbered);
      setVerifications(tv);
      setAggPresentations(ap);
    } catch (e) {
      setStatus({ type: "error", message: e.message });
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const verifyLatest = async () => {
    setStatus(null);
    if (!aggPresentations.length) {
      setStatus({ type: "error", message: "No aggregator presentations available" });
      return;
    }
    const latest = aggPresentations[aggPresentations.length - 1];
    try {
      const result = await api.tso.verifyPresentation(latest);
      setStatus({ type: "success", message: `Verification: ${result.status}` });
      await loadData();
    } catch (e) {
      setStatus({ type: "error", message: e.message });
    }
  };

  /* ===== CHART DATA ===== */

  // Last 7 Days
  const lastWeekKwh = useMemo(() => {
    const days = [...Array(7)].map((_, i) => {
      const d = new Date();
      d.setDate(d.getDate() - i);
      d.setHours(0, 0, 0, 0);
      return d;
    }).reverse();

    return days.map((dayDate) => {
      const dayStart = new Date(dayDate);
      const dayEnd = new Date(dayDate);
      dayEnd.setHours(23, 59, 59, 999);

      const sum = presentations.reduce((acc, p) => {
        if (!p.createdAt) return acc;

        const created =
          p.createdAt instanceof Date
            ? p.createdAt
            : new Date(p.createdAt);

        if (created >= dayStart && created <= dayEnd) {
          return acc + (p.totalFlexKW || 0);
        }
        return acc;
      }, 0);

      return {
        day: dayDate.toLocaleDateString("en-US", { weekday: "short" }),
        kwh: sum,
      };
    });
  }, [presentations]);

  // Today Distribution: kWh pro Stunde (0-24) für heute
  const todayDistribution = useMemo(() => {
    const todayStart = new Date();
    todayStart.setHours(0, 0, 0, 0);
    const todayEnd = new Date();
    todayEnd.setHours(23, 59, 59, 999);

    const data = [];
    presentations
      .filter(p => {
        if (!p.createdAt) return false;
        const created = p.createdAt instanceof Date ? p.createdAt : new Date(p.createdAt);
        return created >= todayStart && created <= todayEnd;
      })
      .forEach(p => {
        // kWh pro Stunde gleichmäßig verteilen
        const hours = 24;
        const kwhPerHour = (p.totalFlexKW || 0) / hours;
        for (let h = 0; h < hours; h++) {
          data.push({
            hour: `${h}:00-${h + 1}:00`,
            kwh: kwhPerHour,
            presentationNumber: p.presentationNumber,
          });
        }
      });
    return data;
  }, [presentations]);

  // Tomorrow Forecast: noch verfügbare kWh aus Aggregator-Präsentationen
  const tomorrowKwh = useMemo(() => {
    const todayStart = new Date();
    todayStart.setHours(0, 0, 0, 0);
    const todayEnd = new Date();
    todayEnd.setHours(23, 59, 59, 999);

    return aggPresentations.map((ap, idx) => {
      const totalKW = ap.totalFlexKW || 0;

      const usedToday = presentations
        .filter(p => {
          if (!p.createdAt) return false;
          const created = p.createdAt instanceof Date ? p.createdAt : new Date(p.createdAt);
          return created >= todayStart && created <= todayEnd;
        })
        .reduce((acc, p) => acc + (p.totalFlexKW || 0), 0);

      return {
        hour: idx + 1, // Präsentationsnummer für X-Achse
        kwh: Math.max(totalKW - usedToday, 0),
      };
    });
  }, [aggPresentations, presentations]);

  return (
    <Box m="20px">
      <Header title="TSO" subtitle="Verification & System Log" />

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

      {/* VERIFY AGGREGATOR PRESENTATION */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Verify latest Aggregator Presentation
        </Typography>
        <Button sx={{ mt: "10px", mr: "10px" }} variant="contained" onClick={verifyLatest}>
          Verify
        </Button>
        <Button sx={{ mt: "10px" }} variant="outlined" onClick={loadData}>
          Refresh
        </Button>
      </Box>

      {/* TSO PRESENTATIONS */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          TSO Presentations
        </Typography>
        <Divider sx={{ my: "10px" }} />
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Nr.</TableCell>
              <TableCell>ID</TableCell>
              <TableCell>timeWindow</TableCell>
              <TableCell>Total kW</TableCell>
              <TableCell>Consents</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {presentations.map((p) => (
              <TableRow key={p.id}>
                <TableCell>{p.presentationNumber}</TableCell>
                <TableCell>{p.id}</TableCell>
                <TableCell>
                  {p.timeWindow?.start} - {p.timeWindow?.end}
                </TableCell>
                <TableCell>{p.totalFlexKW}</TableCell>
                <TableCell>{p.consentCount}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>

      {/* VERIFICATIONS */}
      <Box mb="20px" p="20px" borderRadius="8px" bgcolor={colors.primary[400]}>
        <Typography variant="h5" color={colors.greenAccent[500]}>
          Verifications
        </Typography>
        <Divider sx={{ my: "10px" }} />
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Presentation Nr.</TableCell>
              <TableCell>Verification ID</TableCell>
              <TableCell>Presentation ID</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Reason</TableCell>
              <TableCell>Checked At</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {verifications.map((v) => {
              const pres = presentations.find(p => p.id === v.presentationId);
              return (
                <TableRow key={v.verificationId || v.id}>
                  <TableCell>{pres?.presentationNumber || "-"}</TableCell>
                  <TableCell>{v.verificationId || v.id}</TableCell>
                  <TableCell>{v.presentationId || v.id}</TableCell>
                  <TableCell
                    sx={{
                      color:
                        v.status === "valid" || v.status === "verified"
                          ? colors.greenAccent[400]
                          : colors.redAccent[400],
                    }}
                  >
                    {v.status}
                  </TableCell>
                  <TableCell>{v.reason || ""}</TableCell>
                  <TableCell>
                    {v.checkedAt ? new Date(v.checkedAt).toLocaleString() : ""}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </Box>

      {/* ENERGY OVERVIEW CHARTS */}
      <Box display="grid" gridTemplateColumns="repeat(12, 1fr)" gap="20px">
        {/* Last Week */}
        <Box p="20px" borderRadius="8px" bgcolor={colors.primary[400]} gridColumn="span 12">
          <Typography variant="h5" color={colors.greenAccent[500]} mb="10px">
            kWh – Last 7 Days
          </Typography>
          <Box height="250px">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={lastWeekKwh}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="kwh" fill={colors.greenAccent[400]} />
              </BarChart>
            </ResponsiveContainer>
          </Box>
        </Box>

        {/* Tomorrow */}
        <Box p="20px" borderRadius="8px" bgcolor={colors.primary[400]} gridColumn="span 6">
          <Typography variant="h5" color={colors.greenAccent[500]} mb="10px">
            kWh – Tomorrow Forecast
          </Typography>
          <Box height="250px">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={tomorrowKwh}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="hour" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="kwh" fill={colors.greenAccent[600]} />
              </BarChart>
            </ResponsiveContainer>
          </Box>
        </Box>

        {/* Today Distribution */}
        <Box p="20px" borderRadius="8px" bgcolor={colors.primary[400]} gridColumn="span 6">
          <Typography variant="h5" color={colors.greenAccent[500]} mb="10px">
            kWh – Today Distribution (per hour)
          </Typography>
          <Box height="250px">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={todayDistribution}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="hour" />
                <YAxis />
                <Tooltip
                  formatter={(value, name, props) => [`${value.toFixed(2)} kWh`, `Presentation ${props.payload.presentationNumber}`]}
                />
                <Bar dataKey="kwh" fill={colors.greenAccent[400]} />
              </BarChart>
            </ResponsiveContainer>
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

export default TSO;
