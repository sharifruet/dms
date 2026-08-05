import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';
import { STAGE_COUNT, STAGE_NAMES } from '../../types/procurement';

/**
 * Where the estate stands: how many packages sit at each stage, what is expiring, and
 * what needs someone's attention.
 */
const ProcurementDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [data, setData] = useState<Record<string, any> | null>(null);
  const [expiries, setExpiries] = useState<any[]>([]);
  const [exceptions, setExceptions] = useState<Record<string, any> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const [dashboard, expiryRows, exceptionRows] = await Promise.all([
          procurementService.getDashboard(),
          procurementService.getExpiries(30),
          procurementService.getExceptions(),
        ]);
        setData(dashboard);
        setExpiries(expiryRows);
        setExceptions(exceptionRows);
      } catch (e: any) {
        setError(e?.response?.data?.error || 'Could not load the dashboard');
      }
    })();
  }, []);

  if (error) return <Alert severity="error" sx={{ m: 3 }}>{error}</Alert>;
  if (!data) return <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>;

  const byStage: any[] = data.byStage || [];
  const countFor = (stage: number) =>
    byStage.find((s) => Number(s.stageCode) === stage)?.count || 0;

  const exceptionCount =
    (exceptions?.orphanedDocuments?.length || 0) +
    (exceptions?.brokenLinks?.length || 0) +
    (exceptions?.failedOcrJobs?.length || 0);

  const tile = (label: string, value: React.ReactNode, onClick?: () => void, color?: string) => (
    <Grid item xs={6} md={3}>
      <Card
        variant="outlined"
        sx={{ cursor: onClick ? 'pointer' : 'default' }}
        onClick={onClick}
      >
        <CardContent>
          <Typography variant="caption" color="text.secondary">{label}</Typography>
          <Typography variant="h4" sx={{ color: color || 'text.primary' }}>{value}</Typography>
        </CardContent>
      </Card>
    </Grid>
  );

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" sx={{ mb: 3 }}>Procurement</Typography>

      <Grid container spacing={2} sx={{ mb: 4 }}>
        {tile('Active packages', data.totalActive ?? 0, () => navigate('/procurement/packages'))}
        {tile('Closed packages', data.totalClosed ?? 0,
          () => navigate('/procurement/packages?status=CLOSED'))}
        {tile('Expiring in 30 days', expiries.length,
          () => navigate('/procurement/expiries'),
          expiries.length > 0 ? '#ed6c02' : undefined)}
        {tile('Needs attention', exceptionCount,
          () => navigate('/procurement/exceptions'),
          exceptionCount > 0 ? '#d32f2f' : undefined)}
      </Grid>

      <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle2" sx={{ mb: 2 }}>Packages by stage</Typography>
        <Stack spacing={0.5}>
          {Array.from({ length: STAGE_COUNT }, (_, i) => i + 1).map((stage) => {
            const count = Number(countFor(stage));
            const max = Math.max(...Array.from({ length: STAGE_COUNT }, (_, i) => Number(countFor(i + 1))), 1);
            return (
              <Box
                key={stage}
                sx={{ display: 'flex', alignItems: 'center', gap: 2, cursor: 'pointer' }}
                onClick={() => navigate(`/procurement/packages?stage=${stage}`)}
              >
                <Typography variant="body2" sx={{ width: 220, flexShrink: 0 }}>
                  {stage}. {STAGE_NAMES[stage]}
                </Typography>
                <Box sx={{ flexGrow: 1, height: 18, backgroundColor: 'action.hover', borderRadius: 1 }}>
                  <Box
                    sx={{
                      width: `${(count / max) * 100}%`,
                      height: '100%',
                      backgroundColor: 'primary.main',
                      borderRadius: 1,
                      minWidth: count > 0 ? 4 : 0,
                    }}
                  />
                </Box>
                <Typography variant="body2" sx={{ width: 40, textAlign: 'right' }}>
                  {count}
                </Typography>
              </Box>
            );
          })}
        </Stack>
      </Paper>

      <Paper variant="outlined" sx={{ p: 2 }}>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>Expiring soonest</Typography>
        {expiries.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            Nothing expiring in the next 30 days.
          </Typography>
        ) : (
          <Stack spacing={1}>
            {expiries.slice(0, 8).map((e) => (
              <Stack key={e.id} direction="row" spacing={2} alignItems="center">
                <Chip size="small" label={e.expiryType} variant="outlined" />
                <Typography variant="body2" sx={{ flexGrow: 1 }}>
                  {e.entityType || 'Instrument'}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {e.expiryDate?.substring(0, 10)}
                </Typography>
              </Stack>
            ))}
          </Stack>
        )}
      </Paper>
    </Box>
  );
};

export default ProcurementDashboard;
