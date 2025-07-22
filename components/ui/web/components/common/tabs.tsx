import { FC } from 'react';
import { styled } from '@mui/material/styles';
import { Box, capitalize, Tab, Tabs } from '@mui/material';

interface StyledTabProps {
  label: string;
}
interface Props {
  tabs: string[];
  value: number;
  setValue: (value: number) => void;
}

const AntTabs = styled(Tabs)`
  borderBottom: '1px solid #e8e8e8',
  '& .MuiTabs-indicator': { backgroundColor: theme.palette.secondary.main }
`;

const AntTab = styled((props: StyledTabProps) => <Tab disableRipple {...props} />)(
  ({ theme }) => ({
    textTransform: 'none',
    minWidth: 0,
    [theme.breakpoints.up('sm')]: { minWidth: 0 },
    fontWeight: theme.typography.fontWeightRegular,
    marginRight: theme.spacing(1),
    color: 'rgba(0, 0, 0, 0.85)',
    fontFamily: [
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
      '"Apple Color Emoji"',
      '"Segoe UI Emoji"',
      '"Segoe UI Symbol"'
    ].join(','),
    '&:hover': {
      color: theme.palette.secondary.main,
      opacity: 1
    },
    '&.Mui-selected': {
      color: theme.palette.secondary.main,
      fontWeight: theme.typography.fontWeightMedium
    },
    '&.Mui-focusVisible': { backgroundColor: '#d1eaff' }
  })
);

export const CustomizedTabs: FC<Props> = ({ tabs, value, setValue }) => {

  const handleChange = (event: React.SyntheticEvent, newValue: number) => {
    setValue(newValue);
  };

  return (
    <Box sx={{ width: '100%', bgcolor: '#fff' }}>
      <AntTabs value={value} onChange={handleChange} aria-label="tabs">
        {tabs.map(t =>
          <AntTab key={t} label={capitalize(t)} />
        )}
      </AntTabs>
    </Box>
  );
};