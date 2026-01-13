import { Box, Button, Typography, useTheme } from "@mui/material";
import { tokens } from "../../theme";
import DownloadOutlinedIcon from "@mui/icons-material/DownloadOutlined";
import EmailIcon from "@mui/icons-material/Email";
import PointOfSaleIcon from "@mui/icons-material/PointOfSale";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import TrafficIcon from "@mui/icons-material/Traffic";
import SecurityIcon from "@mui/icons-material/Security";
import Header from "../../components/Header";
import BarChart from "../../components/BarChart";
import StatBox from "../../components/StatBox";
import ProgressCircle from "../../components/ProgressCircle";
import { useEffect, useState } from "react";
import { api } from "../../api";

const Dashboard = () => {
  const theme = useTheme();
  const colors = tokens(theme.palette.mode);

  const [metrics, setMetrics] = useState({
    consents: 0,
    presentations: 0,
    verifications: 0,
    agents: 0,
  });

  const [devices, setDevices] = useState([]);
  const [zkpStats, setZkpStats] = useState({
    total: 0,
    valid: 0,
  });

  useEffect(() => {
    const load = async () => {
      try {
        const [cons, presAgg, verifs, devs, oems] = await Promise.all([
          api.aggregator.listConsents().catch(() => []),
          api.aggregator.listPresentations().catch(() => []),
          api.tso.listVerifications().catch(() => []),
          api.agent.listDevices().catch(() => []),
          api.agent.listOems().catch(() => []),
        ]);

        const presentations = Array.isArray(presAgg) ? presAgg : [];
        const validZkp = presentations.filter((p) => !!p.proof).length;

        setMetrics({
          consents: cons.length,
          presentations: presentations.length,
          verifications: verifs.length,
          agents: oems.length,
        });

        setDevices(devs);
        setZkpStats({
          total: presentations.length,
          valid: validZkp,
        });
      } catch {
        // ignore
      }
    };
    load();
  }, []);

  const toProgress = (value, scale = 10) =>
    Math.min(1, scale > 0 ? value / scale : 0);

  const totalAvail = devices.reduce(
    (sum, d) => sum + (d.payload?.availableFlexKW || 0),
    0
  );

  const deviceBarData = devices.map((d) => ({
    device: d.payload.deviceId,
    flex: d.payload.availableFlexKW || 0,
  }));

  const zkpProgress =
    zkpStats.total > 0 ? zkpStats.valid / zkpStats.total : 0;

  return (
    <Box m="20px">
      {/* HEADER */}
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Header title="DASHBOARD" subtitle="Software Seminar 2025/26" />
        <Button
          sx={{
            backgroundColor: colors.blueAccent[700],
            color: colors.grey[100],
            fontWeight: "bold",
            padding: "10px 20px",
          }}
        >
          <DownloadOutlinedIcon sx={{ mr: "10px" }} />
          Download Reports
        </Button>
      </Box>

      {/* GRID */}
      <Box
        display="grid"
        gridTemplateColumns="repeat(12, 1fr)"
        gridAutoRows="140px"
        gap="20px"
      >
        {/* ROW 1 – StatBoxes weiter unten setzen */}
        <Box
          gridColumn="span 3"
          bgcolor={colors.primary[400]}
          display="flex"
          justifyContent="center"
          alignItems="flex-end"
          pb="20px"
        >
          <StatBox
            title={metrics.consents.toString()}
            subtitle="Consents Sent"
            progress={toProgress(metrics.consents, 10)}
            icon={<EmailIcon sx={{ color: colors.greenAccent[600], fontSize: 26 }} />}
          />
        </Box>

        <Box
          gridColumn="span 3"
          bgcolor={colors.primary[400]}
          display="flex"
          justifyContent="center"
          alignItems="flex-end"
          pb="20px"
        >
          <StatBox
            title={metrics.presentations.toString()}
            subtitle="Presentations"
            progress={toProgress(metrics.presentations, 10)}
            icon={<PointOfSaleIcon sx={{ color: colors.greenAccent[600], fontSize: 26 }} />}
          />
        </Box>

        <Box
          gridColumn="span 3"
          bgcolor={colors.primary[400]}
          display="flex"
          justifyContent="center"
          alignItems="flex-end"
          pb="20px"
        >
          <StatBox
            title={metrics.agents.toString()}
            subtitle="Active Agents"
            progress={toProgress(metrics.agents, 5)}
            icon={<PersonAddIcon sx={{ color: colors.greenAccent[600], fontSize: 26 }} />}
          />
        </Box>

        <Box
          gridColumn="span 3"
          bgcolor={colors.primary[400]}
          display="flex"
          justifyContent="center"
          alignItems="flex-end"
          pb="20px"
        >
          <StatBox
            title={`${metrics.consents + metrics.presentations}`}
            subtitle="System Traffic"
            progress={toProgress(metrics.consents + metrics.presentations, 20)}
            icon={<TrafficIcon sx={{ color: colors.greenAccent[600], fontSize: 26 }} />}
          />
        </Box>

        {/* ROW 2 – AVAILABLE FLEX (GRÜN UND NACH OBEN) */}
        <Box gridColumn="span 8" gridRow="span 2" bgcolor={colors.primary[400]}>
          <Box p="20px">
            <Typography variant="h5" fontWeight="600">
              Available Flex (kW)
            </Typography>
            <Typography variant="h3" color={colors.greenAccent[500]}>
              {totalAvail}
            </Typography>
          </Box>

          <Box height="240px" px="10px" pt="10px">
            <BarChart
              isDashboard
              data={deviceBarData}
              keys={["flex"]}
              indexBy="device"
              colors={[colors.greenAccent[500]]} // grüne Säulen
              enableLabel={false}
              tooltip={{
                format: (value) => `${value} kW`,
              }}
              axisBottom={{
                tickRotation: -30,
                tickSize: 0,
              }}
              axisLeft={{
                tickSize: 0,
              }}
              gridY
              rounded
            />
          </Box>
        </Box>

        {/* ROW 2 – ZKP CHECKER */}
        <Box
          gridColumn="span 4"
          gridRow="span 2"
          bgcolor={colors.primary[400]}
          p="30px"
        >
          <Typography variant="h5" fontWeight="600">
            Presentation Checker
          </Typography>

          <Box mt="25px" display="flex" flexDirection="column" alignItems="center">
            <ProgressCircle size={120} progress={zkpProgress} />
            <Typography mt="15px" color={colors.greenAccent[500]}>
              {Math.round(zkpProgress * 100)}% valid
            </Typography>
            <Typography variant="body2">
              {zkpStats.valid} / {zkpStats.total} presentations
            </Typography>
            <SecurityIcon sx={{ mt: "10px", color: colors.greenAccent[500] }} />
          </Box>
        </Box>
      </Box>
    </Box>
  );
};

export default Dashboard;
