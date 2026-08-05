import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';
import { STAGE_NAMES } from '../../types/procurement';

interface Props {
  packageId: number;
}

/**
 * The linked graph for a package, rendered as the tree from the requirements: APP at
 * the root, everything else descending from it.
 *
 * This is the answer to "show me everything about this package in one place" - the
 * thing a flat document list could never do.
 */
const LinkageGraph: React.FC<Props> = ({ packageId }) => {
  const [graph, setGraph] = useState<Record<string, any> | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      setGraph(await procurementService.getGraph(packageId));
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not load the linked documents');
    }
  }, [packageId]);

  useEffect(() => {
    load();
  }, [load]);

  if (error) return <Alert severity="error" sx={{ m: 3 }}>{error}</Alert>;
  if (!graph) {
    return <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>;
  }

  const node = (label: string, detail?: string, depth = 0, chip?: string) => (
    <Box sx={{ pl: depth * 3, py: 0.5, display: 'flex', alignItems: 'center', gap: 1 }}>
      <Typography variant="body2" sx={{ fontFamily: 'monospace', color: 'text.secondary' }}>
        {depth > 0 ? '└─' : ''}
      </Typography>
      <Typography variant="body2" sx={{ fontWeight: depth === 0 ? 600 : 400 }}>
        {label}
      </Typography>
      {detail && (
        <Typography variant="caption" color="text.secondary">{detail}</Typography>
      )}
      {chip && <Chip label={chip} size="small" sx={{ height: 18 }} />}
    </Box>
  );

  const documents: any[] = graph.documents || [];
  const documentsByStage = documents.reduce((acc: Record<number, any[]>, d: any) => {
    (acc[d.stageCode] = acc[d.stageCode] || []).push(d);
    return acc;
  }, {});

  return (
    <Box sx={{ p: 3, overflow: 'auto' }}>
      <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>Linked records</Typography>

        {node(`Package ${graph.package?.packageNumber}`, graph.package?.packageDescription, 0)}
        {graph.tender && node(
          'Tender',
          `${graph.tender.procurementMethod || ''} closing ${graph.tender.closingDate || '—'}`,
          1,
        )}
        {graph.opening && node(
          'Bid opening',
          `${graph.opening.numberOfBidders ?? '—'} bidders`,
          2,
        )}
        {graph.evaluation && node(
          'Evaluation (BER)',
          graph.evaluation.oceValue ? `OCE ${graph.evaluation.oceValue}` : undefined,
          3,
        )}
        {(graph.bidders || []).map((b: any) => node(
          b.bidderName,
          `${b.biddingPrice ?? '—'} · deviation ${b.deviationPct ?? '—'}%`,
          4,
          b.isAwarded ? 'awarded' : undefined,
        ))}
        {graph.contract && node(
          `Contract ${graph.contract.contractNumber}`,
          `${graph.contract.contractValue ?? '—'} ${graph.contract.currency || ''}`,
          2,
        )}
        {(graph.letterOfCredits || []).map((lc: any) => node(
          `LC ${lc.lcNumber || '(draft)'}`,
          `expires ${lc.lcExpiryDate || '—'}`,
          3,
        ))}
        {(graph.deliveries || []).map((d: any) => node(
          `Delivery ${d.deliveryReferenceNumber || ''}`,
          `${d.deliveryDate || ''} · qty ${d.deliveredQuantity ?? '—'}`,
          3,
          d.isFinal ? 'final' : undefined,
        ))}
        {(graph.invoices || []).map((i: any) => node(
          `Invoice ${i.invoiceNumber}`,
          `${i.invoiceAmount ?? '—'} ${i.currency || ''}`,
          4,
        ))}
        {(graph.payments || []).map((p: any) => node(
          `Payment ${p.voucherNumber || ''}`,
          `${p.paymentAmount ?? '—'} on ${p.paymentDate || '—'}`,
          5,
        ))}
      </Paper>

      <Paper variant="outlined" sx={{ p: 2 }}>
        <Typography variant="subtitle2" sx={{ mb: 1 }}>
          Documents ({documents.length})
        </Typography>
        {documents.length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No documents uploaded for this package yet.
          </Typography>
        )}
        {Object.keys(documentsByStage)
          .map(Number)
          .sort((a, b) => a - b)
          .map((stage) => (
            <Box key={stage} sx={{ mb: 1.5 }}>
              <Typography variant="caption" color="text.secondary">
                Stage {stage} — {STAGE_NAMES[stage]}
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 0.5 }}>
                {documentsByStage[stage].map((d: any) => (
                  <Chip key={d.id} label={d.docRole} size="small" variant="outlined" />
                ))}
              </Stack>
            </Box>
          ))}
      </Paper>
    </Box>
  );
};

export default LinkageGraph;
