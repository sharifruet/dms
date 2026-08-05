import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  AlertTitle,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';
import { BerBidder, ExtractedField, StageDetail } from '../../types/procurement';
import DocumentChecklist from './DocumentChecklist';
import FieldRow from './FieldRow';
import BidderTable from './BidderTable';
import StageRecords from './StageRecords';

interface Props {
  packageId: number;
  stageCode: number;
  onChanged: () => void;
}

/**
 * One stage of the workspace: its documents, its captured values, and the gate.
 *
 * The Complete button is disabled until the stage is genuinely ready, and the reasons
 * are listed rather than hidden behind a failed request.
 */
const StagePanel: React.FC<Props> = ({ packageId, stageCode, onChanged }) => {
  const [detail, setDetail] = useState<StageDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dialog, setDialog] = useState<null | 'override' | 'notApplicable' | 'rework'>(null);
  const [dialogReason, setDialogReason] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setDetail(await procurementService.getStage(packageId, stageCode));
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not load this stage');
    } finally {
      setLoading(false);
    }
  }, [packageId, stageCode]);

  useEffect(() => {
    load();
  }, [load]);

  const refresh = async () => {
    await load();
    onChanged();
  };

  const handleUpload = async (file: File, docRole: string) => {
    const result = await procurementService.uploadDocument(file, packageId, stageCode, docRole);
    await refresh();
    return result;
  };

  const handleVerify = async (field: ExtractedField) => {
    await procurementService.verifyField(field.id);
    await refresh();
  };

  const handleOverride = async (field: ExtractedField, value: string) => {
    await procurementService.overrideField(field.id, value);
    await refresh();
  };

  const handleBulkVerify = async () => {
    await procurementService.bulkVerify(packageId, stageCode);
    await refresh();
  };

  const handleSaveBidders = async (bidders: BerBidder[]) => {
    await procurementService.saveBidders(packageId, bidders);
    await refresh();
  };

  const handleComplete = async (overrideReason?: string) => {
    setError(null);
    try {
      await procurementService.completeStage(packageId, stageCode, overrideReason);
      setDialog(null);
      setDialogReason('');
      await refresh();
    } catch (e: any) {
      if (e?.response?.status === 409) {
        setError('This stage is not ready yet — see the outstanding items above.');
        await load();
      } else {
        setError(e?.response?.data?.error || 'Could not complete this stage');
      }
    }
  };

  const handleNotApplicable = async () => {
    await procurementService.markNotApplicable(packageId, stageCode, dialogReason);
    setDialog(null);
    setDialogReason('');
    await refresh();
  };

  const handleRework = async () => {
    await procurementService.reworkStage(packageId, stageCode, dialogReason);
    setDialog(null);
    setDialogReason('');
    await refresh();
  };

  if (loading && !detail) {
    return (
      <Box sx={{ p: 4, textAlign: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }
  if (!detail) {
    return <Alert severity="error" sx={{ m: 2 }}>{error || 'Stage not found'}</Alert>;
  }

  const { readiness } = detail;
  const outstanding = [
    ...readiness.blockers,
    ...readiness.missingDocuments.map((d) => `Missing document: ${d}`),
    ...readiness.unconfirmedFields.map((f) => `Unverified field: ${f}`),
    ...readiness.validationErrors,
  ];
  const completed = detail.stage.status === 'COMPLETED';
  const notApplicable = !detail.stage.isApplicable;
  const pendingFields = detail.fields.filter((f) => f.status === 'OCR_SUGGESTED');

  return (
    <Box sx={{ p: 3, flexGrow: 1, overflow: 'auto' }}>
      <Stack direction="row" alignItems="center" spacing={2} sx={{ mb: 2 }}>
        <Typography variant="h6">
          Stage {detail.stageCode} — {detail.stageName}
        </Typography>
        {completed && <Chip label="Completed" color="success" size="small" />}
        {notApplicable && <Chip label="Not applicable" size="small" />}
        {detail.stage.status === 'REWORK' && <Chip label="Rework" color="error" size="small" />}
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      {notApplicable && detail.stage.notApplicableReason && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Marked not applicable: {detail.stage.notApplicableReason}
        </Alert>
      )}

      {detail.warnings.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          <AlertTitle>Worth checking</AlertTitle>
          {detail.warnings.map((w, i) => (
            <div key={i}>{w}</div>
          ))}
        </Alert>
      )}

      {!completed && !notApplicable && outstanding.length > 0 && (
        <Alert severity="info" sx={{ mb: 2 }}>
          <AlertTitle>Outstanding before this stage can be completed</AlertTitle>
          {outstanding.map((item, i) => (
            <div key={i}>• {item}</div>
          ))}
        </Alert>
      )}

      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <DocumentChecklist
          requirements={detail.requiredDocuments}
          uploaded={detail.documents}
          onUpload={handleUpload}
        />
      </Paper>

      <Paper variant="outlined" sx={{ p: 2 }}>
        <Stack direction="row" alignItems="center" sx={{ mb: 1 }}>
          <Typography variant="subtitle2" sx={{ flexGrow: 1 }}>
            Captured values
          </Typography>
          {pendingFields.length > 0 && (
            <Button size="small" onClick={handleBulkVerify}>
              Verify all {pendingFields.length}
            </Button>
          )}
        </Stack>

        {detail.fields.length === 0 ? (
          <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
            Nothing captured yet. Upload a document above, or enter the values by hand once
            the document is filed.
          </Typography>
        ) : (
          detail.fields.map((field) => (
            <FieldRow
              key={field.id}
              field={field}
              onVerify={handleVerify}
              onOverride={handleOverride}
            />
          ))
        )}
      </Paper>

      {stageCode === 4 && (
        <BidderTable bidders={detail.bidders || []} onSave={handleSaveBidders} />
      )}

      {[11, 12, 13, 14].includes(stageCode) && (
        <StageRecords packageId={packageId} stageCode={stageCode} detail={detail} onChanged={refresh} />
      )}

      <Divider sx={{ my: 3 }} />

      <Stack direction="row" spacing={2}>
        <Button
          variant="contained"
          disabled={completed || notApplicable || !readiness.ready}
          onClick={() => handleComplete()}
        >
          Complete stage
        </Button>
        {!completed && !notApplicable && !readiness.ready && (
          <Button color="warning" onClick={() => setDialog('override')}>
            Complete with override
          </Button>
        )}
        {!completed && !notApplicable && (
          <Button onClick={() => setDialog('notApplicable')}>Mark not applicable</Button>
        )}
        {completed && (
          <Button color="error" onClick={() => setDialog('rework')}>
            Send back for rework
          </Button>
        )}
      </Stack>

      <Dialog open={dialog !== null} onClose={() => setDialog(null)} fullWidth maxWidth="sm">
        <DialogTitle>
          {dialog === 'override' && 'Complete this stage anyway'}
          {dialog === 'notApplicable' && 'Mark this stage not applicable'}
          {dialog === 'rework' && 'Send this stage back for rework'}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {dialog === 'override' &&
              'The outstanding items above will remain outstanding. Your reason is recorded against the stage.'}
            {dialog === 'notApplicable' &&
              'The stage will be skipped and the next stage opened. Your reason is recorded.'}
            {dialog === 'rework' &&
              'Later stages keep their data and links, but are marked as needing revalidation.'}
          </Typography>
          <TextField
            autoFocus
            fullWidth
            multiline
            minRows={2}
            label="Reason (required)"
            value={dialogReason}
            onChange={(e) => setDialogReason(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialog(null)}>Cancel</Button>
          <Button
            variant="contained"
            disabled={!dialogReason.trim()}
            onClick={() => {
              if (dialog === 'override') handleComplete(dialogReason);
              if (dialog === 'notApplicable') handleNotApplicable();
              if (dialog === 'rework') handleRework();
            }}
          >
            Confirm
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default StagePanel;
