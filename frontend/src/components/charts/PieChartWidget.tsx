import React from 'react';
import { Box, Paper, Typography } from '@mui/material';
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';

interface DataPoint {
  name: string;
  value: number;
  [key: string]: string | number;
}

interface PieChartWidgetProps {
  title: string;
  data: DataPoint[];
  colors: string[];
  height?: number;
}

const PieChartWidget: React.FC<PieChartWidgetProps> = ({ 
  title, 
  data, 
  colors,
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
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            labelLine={false}
            label={({ name, percent }: any) => `${name}: ${(percent * 100).toFixed(0)}%`}
            outerRadius={80}
            fill="#8884d8"
            dataKey="value"
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={colors[index % colors.length]} />
            ))}
          </Pie>
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
        </PieChart>
      </ResponsiveContainer>
    </Paper>
  );
};

export default PieChartWidget;

