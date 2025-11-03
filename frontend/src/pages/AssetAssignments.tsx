import React, { useEffect, useState } from 'react';
import { AssetAssignment, AssignmentAPI } from '../services/assetService';
import { Box, Typography, Table, TableHead, TableRow, TableCell, TableBody, Paper } from '@mui/material';

const AssetAssignments: React.FC = () => {
  const [assignments, setAssignments] = useState<AssetAssignment[]>([]);

  useEffect(() => {
    AssignmentAPI.listAssignments({ page: 0, size: 20 })
      .then((res) => setAssignments(res.data.content))
      .catch(() => setAssignments([]));
  }, []);

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" sx={{ mb: 2, fontWeight: 600 }}>Asset Assignments</Typography>
      <Paper>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Asset Tag</TableCell>
              <TableCell>Product</TableCell>
              <TableCell>User ID</TableCell>
              <TableCell>Start Date</TableCell>
              <TableCell>End Date</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {assignments.map((as) => (
              <TableRow key={as.id}>
                <TableCell>{as.asset?.assetTag}</TableCell>
                <TableCell>{as.asset?.product?.name}</TableCell>
                <TableCell>{as.user?.id}</TableCell>
                <TableCell>{as.startDate}</TableCell>
                <TableCell>{as.endDate}</TableCell>
                <TableCell>{as.status}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
};

export default AssetAssignments;
