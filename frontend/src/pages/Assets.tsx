import React, { useEffect, useState } from 'react';
import { Asset, AssetAPI } from '../services/assetService';
import { Box, Typography, Table, TableHead, TableRow, TableCell, TableBody, Paper } from '@mui/material';

const Assets: React.FC = () => {
  const [assets, setAssets] = useState<Asset[]>([]);

  useEffect(() => {
    AssetAPI.listAssets({ page: 0, size: 20 })
      .then((res) => setAssets(res.data.content))
      .catch(() => setAssets([]));
  }, []);

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" sx={{ mb: 2, fontWeight: 600 }}>Assets</Typography>
      <Paper>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Asset Tag</TableCell>
              <TableCell>Product</TableCell>
              <TableCell>Serial No</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Location</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {assets.map((a) => (
              <TableRow key={a.id}>
                <TableCell>{a.assetTag}</TableCell>
                <TableCell>{a.product?.name}</TableCell>
                <TableCell>{a.serialNo}</TableCell>
                <TableCell>{a.status}</TableCell>
                <TableCell>{a.location}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
};

export default Assets;
