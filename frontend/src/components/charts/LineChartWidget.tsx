import React from 'react';
import { Box, Paper, Typography, useTheme } from '@mui/material';
import {
  LineChart,
  Line,
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

interface LineChartWidgetProps {
  title: string;
  data: DataPoint[];
  lines: Array<{
    dataKey: string;
    color: string;
    name: string;
  }>;
  height?: number;
}

const LineChartWidget: React.FC<LineChartWidgetProps> = ({ 
  title, 
  data, 
  lines,
  height = 300 
}) => {
  const theme = useTheme();

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
        <LineChart data={data}>
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
          {lines.map((line) => (
            <Line
              key={line.dataKey}
              type="monotone"
              dataKey={line.dataKey}
              stroke={line.color}
              strokeWidth={2}
              name={line.name}
              dot={{ fill: line.color, r: 4 }}
              activeDot={{ r: 6 }}
            />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </Paper>
  );
};

export default LineChartWidget;

