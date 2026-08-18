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
import {
  BerBidder,
  ExtractedField,
  MasterListValue,
  StageDetail,
} from '../../types/procurement';
import DocumentChecklist from './DocumentChecklist';
import FieldRow from './FieldRow';
import SourcePreview from './SourcePreview';
import FieldHistoryDialog from './FieldHistoryDialog';
import PriceScheduleTable from './PriceScheduleTable';
import BidderTable from './BidderTable';
import StageRecords from './StageRecords';
import TenderAttempts from './TenderAttempts';
import ManualFieldForm from './ManualFieldForm';
import useProcurementRole from '../../hooks/useProcurementRole';

/** Tender Advertisement — where a failed tender is re-tendered (Q-2). */
const STAGE_TENDER = 2;
/** Letter of Credit — applicability derived from Procurement Type (Q-5). */
const STAGE_LC = 9;
/**
 * Bill Submission and Payment. Their money ceilings are hard blocks with no override
 * (Q-12), so offering "complete anyway" on these stages would promise something the
 * server refuses.
 */
const MONEY_STAGES = [13, 14];

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
  // Where a value came from, and what it has been (REQ-X3, REQ-P5)
  const [sourceField, setSourceField] = useState<ExtractedField | null>(null);
  const [historyField, setHistoryField] = useState<ExtractedField | null>(null);
  // The permitted Type / Method / Nature values, so Stage 2 offers them rather than
  // leaving the user to guess the spelling (Q-8, REQ-2.4)
  const [masterLists, setMasterLists] = useState<Record<string, MasterListValue[]>>({});
  const { canApprove } = useProcurementRole();

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

  useEffect(() => {
    let cancelled = false;
    procurementService
      .getMasterLists()
      .then((lists) => {
        if (!cancelled) setMasterLists(lists || {});
      })
      .catch(() => {
        // Free text stays a legitimate fallback: the server flags an unmatched value as a
        // warning, so a missing list must not stop the field being filled in
      });
    return () => {
      cancelled = true;
    };
  }, []);

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

  const handleSaveManualFields = async (values: Record<string, string>) => {
    await procurementService.saveStageFields(packageId, stageCode, values);
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

  // Stage 9 opens with applicability derived from the tender's Procurement Type: ICT
  // suggests a Letter of Credit is needed (REQ-2.5). It is a suggestion the user may
  // overrule, so it is shown as guidance rather than enforced.
  const lcStage = stageCode === STAGE_LC;
  const lcSuggested = readiness.applicabilitySuggested !== false;
  const contradictsSuggestion = lcStage && !completed
    && (notApplicable ? lcSuggested : !lcSuggested);

  // Money ceilings are refused at the point of saving an invoice or payment, so a
  // stage-level override cannot rescue them (Q-12)
  const overrideAvailable = !MONEY_STAGES.includes(stageCode);

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

      {lcStage && !completed && (
        <Alert severity={contradictsSuggestion ? 'warning' : 'info'} sx={{ mb: 2 }}>
          <AlertTitle>
            {lcSuggested
              ? 'This looks like an international tender'
              : 'This does not look like an international tender'}
          </AlertTitle>
          {lcSuggested
            ? 'The Tender Notice records Procurement Type as ICT, so a Letter of Credit is normally required. '
            : 'The Tender Notice does not record Procurement Type as ICT, so this stage is normally not applicable. '}
          {contradictsSuggestion
            ? 'You have gone the other way, which is allowed — the choice is recorded against the stage.'
            : 'You can still decide otherwise; this is guidance, not a rule.'}
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
              onShowSource={setSourceField}
              onShowHistory={setHistoryField}
            />
          ))
        )}
      </Paper>

      <ManualFieldForm
        catalogue={detail.catalogue}
        fields={detail.fields}
        masterLists={masterLists}
        disabled={completed || notApplicable}
        onSave={handleSaveManualFields}
      />

      {stageCode === STAGE_TENDER && (
        <TenderAttempts packageId={packageId} onChanged={refresh} />
      )}

      {stageCode === 10 && (
        <PriceScheduleTable
          packageId={packageId}
          disabled={completed || notApplicable}
          onChanged={refresh}
        />
      )}

      {stageCode === 4 && (
        <BidderTable bidders={detail.bidders || []} onSave={handleSaveBidders} />
      )}

      {[11, 12, 13, 14].includes(stageCode) && (
        <StageRecords packageId={packageId} stageCode={stageCode} detail={detail} onChanged={refresh} />
      )}

      <Divider sx={{ my: 3 }} />

      {/*
        Approving is the Checker's job (Q-17). A Maker sees the stage and everything on it
        but not these buttons - offering an action the server will refuse with a 403 reads
        as a broken feature rather than a permission boundary.
      */}
      {canApprove ? (
        <Stack direction="row" spacing={2}>
          <Button
            variant="contained"
            disabled={completed || notApplicable || !readiness.ready}
            onClick={() => handleComplete()}
          >
            Complete stage
          </Button>
          {!completed && !notApplicable && !readiness.ready && overrideAvailable && (
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
      ) : (
        <Alert severity="info">
          Capture and correct values here as needed. Completing the stage is a Checker's
          action.
        </Alert>
      )}

      {canApprove && !completed && !notApplicable && !readiness.ready && !overrideAvailable && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
          Amounts on this stage cannot be overridden — an invoice or payment that would
          breach the contract value is refused when it is saved, not here.
        </Typography>
      )}

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

      <SourcePreview field={sourceField} onClose={() => setSourceField(null)} />
      <FieldHistoryDialog field={historyField} onClose={() => setHistoryField(null)} />
    </Box>
  );
};

export default StagePanel;
