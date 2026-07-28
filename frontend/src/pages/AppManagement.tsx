import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import {
  BusinessCenter as AppManagementIcon,
  Inventory2 as PackageIcon,
} from '@mui/icons-material';
import {
  procurementPackageService,
  ProcurementPackage,
} from '../services/procurementPackageService';

const AppManagement: React.FC = () => {
  const [packages, setPackages] = useState<ProcurementPackage[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadPackages = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await procurementPackageService.getPackages();
        setPackages(response.content || []);
      } catch (err: any) {
        setError(err.response?.data?.error || 'Failed to load APP management data');
      } finally {
        setLoading(false);
      }
    };

    loadPackages();
  }, []);

  return (
    <Box sx={{ p: { xs: 2, md: 4 } }}>
      <Box sx={{ mb: 4 }}>
        <Typography
          variant="h4"
          sx={{
            fontWeight: 700,
            fontSize: { xs: '1.5rem', md: '1.875rem' },
            color: '#111827',
            mb: 1,
            letterSpacing: '-0.02em',
          }}
        >
          APP Management
        </Typography>
        <Typography
          variant="body2"
          sx={{
            color: '#6b7280',
            fontSize: { xs: '0.875rem', md: '0.9375rem' },
          }}
        >
          Procurement package listing powered by the backend APP management API.
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card
        sx={{
          borderRadius: 3,
          boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.12)',
          border: '1px solid #f3f4f6',
          mb: 3,
        }}
      >
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Box
              sx={{
                width: 52,
                height: 52,
                borderRadius: 2,
                backgroundColor: '#eff6ff',
                color: '#2563eb',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                flexShrink: 0,
              }}
            >
              <AppManagementIcon />
            </Box>
            <Box>
              <Typography variant="h6" sx={{ fontWeight: 600, color: '#111827' }}>
                APP Packages
              </Typography>
              <Typography variant="body2" sx={{ color: '#6b7280' }}>
                Showing package number, material description, method/type, source of fund, and a
                temporary static status.
              </Typography>
            </Box>
          </Box>
        </CardContent>
      </Card>

      <Paper
        elevation={0}
        sx={{
          borderRadius: 3,
          border: '1px solid #e5e7eb',
          overflow: 'hidden',
        }}
      >
        {loading ? (
          <Box
            sx={{
              minHeight: 320,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <CircularProgress />
          </Box>
        ) : (
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow sx={{ backgroundColor: '#f9fafb' }}>
                  <TableCell sx={{ fontWeight: 700 }}>Package No</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Description of Materials</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Procurement Method and Types</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Source of fund</TableCell>
                  <TableCell sx={{ fontWeight: 700 }}>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {packages.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 8 }}>
                      <Box
                        sx={{
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'center',
                          gap: 1.5,
                          color: '#6b7280',
                        }}
                      >
                        <PackageIcon sx={{ fontSize: 36, color: '#9ca3af' }} />
                        <Typography variant="body1">No procurement packages found.</Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
                ) : (
                  packages.map((pkg) => (
                    <TableRow
                      key={pkg.id}
                      hover
                      sx={{
                        '&:last-child td, &:last-child th': { border: 0 },
                      }}
                    >
                      <TableCell sx={{ fontWeight: 600, color: '#1f2937' }}>
                        {pkg.packageNo || '-'}
                      </TableCell>
                      <TableCell sx={{ color: '#374151', maxWidth: 420 }}>
                        <Box
                          sx={{
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}
                        >
                          {pkg.description || '-'}
                        </Box>
                      </TableCell>
                      <TableCell sx={{ color: '#374151' }}>
                        {pkg.procurementMethod || '-'}
                      </TableCell>
                      <TableCell sx={{ color: '#374151' }}>
                        {pkg.sourceOfFund || '-'}
                      </TableCell>
                      <TableCell>
                        <Chip
                          label="Pending"
                          size="small"
                          sx={{
                            backgroundColor: '#fef3c7',
                            color: '#92400e',
                            fontWeight: 600,
                          }}
                        />
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </Box>
  );
};

export default AppManagement;
