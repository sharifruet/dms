import React from 'react';
import { Box, Paper, Typography } from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

interface DataPoint {
  name: string;
  [key: string]: string | number;
}

interface BarChartWidgetProps {
  title: string;
  data: DataPoint[];
  bars: Array<{
    dataKey: string;
    color: string;
    name: string;
  }>;
  height?: number;
}

const BarChartWidget: React.FC<BarChartWidgetProps> = ({ 
  title, 
  data, 
  bars,
  height = 300 
}) => {
  return (
    <Paper
      elevation={0}
      sx={{
        p: 3,
        borderRadius: 3,
        border: '1px solid #e5e7eb',
        height: '100%',
      }}
    >
      <Typography
        variant="h6"
        sx={{
          fontWeight: 600,
          fontSize: '1.125rem',
          color: '#1f2937',
          mb: 3,
        }}
      >
        {title}
      </Typography>
      <ResponsiveContainer width="100%" height={height}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey="name" 
            stroke="#6b7280"
            style={{ fontSize: '0.875rem' }}
          />
          <YAxis 
            stroke="#6b7280"
            style={{ fontSize: '0.875rem' }}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: '#ffffff',
              border: '1px solid #e5e7eb',
              borderRadius: '8px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
            }}
          />
          <Legend 
            wrapperStyle={{
              fontSize: '0.875rem',
            }}
          />
          {bars.map((bar) => (
            <Bar
              key={bar.dataKey}
              dataKey={bar.dataKey}
              fill={bar.color}
              name={bar.name}
              radius={[8, 8, 0, 0]}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </Paper>
  );
};

export default BarChartWidget;

