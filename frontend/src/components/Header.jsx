import { Typography, Box, useTheme } from "@mui/material";
import { tokens } from "../theme";

/* Reusable page header with title and subtitle styling. */
const Header = ({ title, subtitle }) => {
  const theme = useTheme();
  // Tokenized colors keep headers consistent across pages.
  const colors = tokens(theme.palette.mode);
  return (
    <Box mb="30px">
      <Typography
        variant="h2"
        color={colors.grey[100]}
        fontWeight="bold"
        sx={{ m: "0 0 5px 0" }}
      >
        {title}
      </Typography>
      <Typography variant="h5" color={colors.greenAccent[400]}>
        {subtitle}
      </Typography>
    </Box>
  );
};

export default Header;
