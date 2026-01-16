import { useTheme } from "@mui/material";
import { ResponsiveBar } from "@nivo/bar";
import { tokens } from "../theme";

/* Generic bar chart with optional dashboard-friendly defaults. */
const BarChart = ({ isDashboard = false, data: customData, keys, indexBy, yLabel, yTickValues, maxValue }) => {
  const theme = useTheme();
  // Tokens keep chart colors consistent with the active theme.
  const colors = tokens(theme.palette.mode);
  // Fall back to mock data when no dataset is supplied.
  const barData = customData ?? [];
  const barKeys = keys || ["hot dog", "burger", "sandwich", "kebab", "fries", "donut"];
  const barIndex = indexBy || "country";

  return (
    <ResponsiveBar
      data={barData}
      theme={{
        // added
        axis: {
          domain: {
            line: {
              stroke: colors.grey[100],
            },
          },
          legend: {
            text: {
              fill: colors.grey[100],
            },
          },
          ticks: {
            line: {
              stroke: colors.grey[100],
              strokeWidth: 1,
            },
            text: {
              fill: colors.grey[100],
            },
          },
        },
        legends: {
          text: {
            fill: colors.grey[100],
          },
        },
      }}
      keys={barKeys}
      indexBy={barIndex}
      margin={{ top: 50, right: 130, bottom: 50, left: 60 }}
      padding={0.3}
      valueScale={{ type: "linear" }}
      indexScale={{ type: "band", round: true }}
      colors={["#3DA085"]}
      maxValue={maxValue || "auto"}
      defs={[
        {
          id: "dots",
          type: "patternDots",
          background: "inherit",
          color: "#38bcb2",
          size: 4,
          padding: 1,
          stagger: true,
        },
        {
          id: "lines",
          type: "patternLines",
          background: "inherit",
          color: "#eed312",
          rotation: -45,
          lineWidth: 6,
          spacing: 10,
        },
      ]}
      borderColor={{
        from: "color",
        modifiers: [["darker", "1.6"]],
      }}
      axisTop={null}
      axisRight={null}
      axisBottom={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
        legend: isDashboard ? undefined : barIndex, // changed
        legendPosition: "middle",
        legendOffset: 32,
      }}
      axisLeft={{
        tickSize: 5,
        tickPadding: 5,
        tickRotation: 0,
        legend: isDashboard ? undefined : yLabel || "value", // changed
        legendPosition: "middle",
        legendOffset: -40,
        tickValues: yTickValues || undefined,
      }}
      tooltip={(datum) => (
        <div
          style={{
            padding: "6px 9px",
            background: "#111827",
            color: "#ffffff",
            border: "1px solid #374151",
            borderRadius: "6px",
            fontSize: "12px",
          }}
        >
          <strong>{datum.indexValue}</strong>: {datum.value}
        </div>
      )}
      enableLabel={false}
      labelSkipWidth={12}
      labelSkipHeight={12}
      labelTextColor={{
        from: "color",
        modifiers: [["darker", 1.6]],
      }}
      legends={[
        {
          dataFrom: "keys",
          anchor: "bottom-right",
          direction: "column",
          justify: false,
          translateX: 120,
          translateY: 0,
          itemsSpacing: 2,
          itemWidth: 100,
          itemHeight: 20,
          itemDirection: "left-to-right",
          itemOpacity: 0.85,
          symbolSize: 20,
          effects: [
            {
              on: "hover",
              style: {
                itemOpacity: 1,
              },
            },
          ],
        },
      ]}
      role="application"
      barAriaLabel={function (e) {
        return e.id + ": " + e.formattedValue + " in country: " + e.indexValue;
      }}
    />
  );
};

export default BarChart;
