import React, { useState } from 'react';
import { Box, Tabs, Tab } from '@mui/material';
import ExecutiveDashboard from '../components/dashboard/ExecutiveDashboard';
import OperationsDashboard from '../components/dashboard/OperationsDashboard';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel({ children, value, index }: TabPanelProps) {
  if (value !== index) return null;
  return <Box role="tabpanel">{children}</Box>;
}

const Dashboard: React.FC = () => {
  const [tab, setTab] = useState(0);

  return (
    <Box sx={{ minHeight: '100vh' }}>
      <Box
        sx={{
          px: { xs: 2, md: 4 },
          pt: { xs: 2, md: 3 },
          pb: 0,
          backgroundColor: tab === 0 ? '#f4f6f9' : '#ffffff',
          borderBottom: tab === 0 ? 'none' : '1px solid #eef2f7',
        }}
      >
        <Tabs
          value={tab}
          onChange={(_, v) => setTab(v)}
          sx={{
            minHeight: 44,
            '& .MuiTab-root': {
              textTransform: 'none',
              fontWeight: 600,
              fontSize: '0.9rem',
              minHeight: 44,
            },
            '& .Mui-selected': { color: '#0e7cc4' },
            '& .MuiTabs-indicator': { backgroundColor: '#0e7cc4' },
          }}
        >
          <Tab label="Executive Dashboard" />
          <Tab label="Operations Dashboard" />
        </Tabs>
      </Box>

      <TabPanel value={tab} index={0}>
        <ExecutiveDashboard />
      </TabPanel>
      <TabPanel value={tab} index={1}>
        <OperationsDashboard />
      </TabPanel>
    </Box>
  );
};

export default Dashboard;
