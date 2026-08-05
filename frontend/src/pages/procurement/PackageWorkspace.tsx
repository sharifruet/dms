import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Breadcrumbs,
  Chip,
  CircularProgress,
  Link,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';
import { ProcurementPackage, StageReadiness } from '../../types/procurement';
import StageRail from '../../components/procurement/StageRail';
import StagePanel from '../../components/procurement/StagePanel';
import BudgetPanel from '../../components/procurement/BudgetPanel';
import LinkageGraph from '../../components/procurement/LinkageGraph';

/**
 * The main screen: a package, its stage rail, and whichever stage is open.
 *
 * Everything a user needs to answer "where is this package and what is blocking it"
 * lives on one screen rather than being spread across document lists.
 */
const PackageWorkspace: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const packageId = Number(id);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [pkg, setPkg] = useState<ProcurementPackage | null>(null);
  const [progress, setProgress] = useState<StageReadiness[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState(0);

  const activeStage = Number(searchParams.get('stage')) || 0;

  const load = useCallback(async () => {
    try {
      const [pkgData, progressData] = await Promise.all([
        procurementService.getPackage(packageId),
        procurementService.getProgress(packageId),
      ]);
      setPkg(pkgData);
      setProgress(progressData);
      if (!searchParams.get('stage')) {
        setSearchParams({ stage: String(pkgData.currentStage || 1) }, { replace: true });
      }
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not load this package');
    } finally {
      setLoading(false);
    }
    // searchParams intentionally omitted: only the initial stage default depends on it
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [packageId]);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <Box sx={{ p: 6, textAlign: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }
  if (error || !pkg) {
    return <Alert severity="error" sx={{ m: 3 }}>{error || 'Package not found'}</Alert>;
  }

  const currentStage = activeStage || pkg.currentStage || 1;

  return (
    <Box sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Box sx={{ px: 3, pt: 2, pb: 1, borderBottom: 1, borderColor: 'divider' }}>
        <Breadcrumbs sx={{ mb: 1 }}>
          <Link
            component="button"
            underline="hover"
            color="inherit"
            onClick={() => navigate('/procurement/packages')}
          >
            Packages
          </Link>
          <Typography color="text.primary">{pkg.packageNumber}</Typography>
        </Breadcrumbs>

        <Stack direction="row" alignItems="center" spacing={2} flexWrap="wrap">
          <Typography variant="h5">{pkg.packageNumber}</Typography>
          {pkg.lotNumber && <Chip label={`Lot ${pkg.lotNumber}`} size="small" />}
          <Chip
            label={pkg.status}
            size="small"
            color={pkg.status === 'CLOSED' ? 'default' : 'primary'}
          />
          <Typography variant="body2" color="text.secondary">
            {pkg.packageDescription}
          </Typography>
        </Stack>

        <Tabs value={tab} onChange={(_, v) => setTab(v)} sx={{ mt: 1 }}>
          <Tab label="Stages" />
          <Tab label="Budget" />
          <Tab label="Linked documents" />
        </Tabs>
      </Box>

      {tab === 0 && (
        <Box sx={{ display: 'flex', flexGrow: 1, overflow: 'hidden' }}>
          <Box sx={{ overflow: 'auto' }}>
            <StageRail
              progress={progress}
              activeStage={currentStage}
              onSelect={(stage) => setSearchParams({ stage: String(stage) })}
            />
          </Box>
          <StagePanel packageId={packageId} stageCode={currentStage} onChanged={load} />
        </Box>
      )}

      {tab === 1 && <BudgetPanel packageId={packageId} />}
      {tab === 2 && <LinkageGraph packageId={packageId} />}
    </Box>
  );
};

export default PackageWorkspace;
