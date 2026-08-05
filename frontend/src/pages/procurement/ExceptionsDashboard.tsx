import React, { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  CircularProgress,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import procurementService from '../../services/procurementService';

/**
 * Work that fell through the cracks.
 *
 * Documents nobody linked, graph edges that no longer resolve, and OCR runs that
 * failed. These are listed rather than swallowed, because each one is a job for a
 * person, not a bug to hide.
 */
const ExceptionsDashboard: React.FC = () => {
  const [data, setData] = useState<Record<string, any> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        setData(await procurementService.getExceptions());
      } catch (e: any) {
        setError(e?.response?.data?.error || 'Could not load exceptions');
      }
    })();
  }, []);

  if (error) return <Alert severity="error" sx={{ m: 3 }}>{error}</Alert>;
  if (!data) return <Box sx={{ p: 6, textAlign: 'center' }}><CircularProgress /></Box>;

  const orphans: any[] = data.orphanedDocuments || [];
  const broken: any[] = data.brokenLinks || [];
  const failedOcr: any[] = data.failedOcrJobs || [];

  const section = (
    title: string,
    description: string,
    rows: any[],
    columns: string[],
    render: (row: any) => React.ReactNode,
  ) => (
    <Paper variant="outlined" sx={{ p: 2, mb: 3 }}>
      <Typography variant="subtitle2">{title} ({rows.length})</Typography>
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 1 }}>
        {description}
      </Typography>
      {rows.length === 0 ? (
        <Typography variant="body2" color="success.main">Nothing outstanding.</Typography>
      ) : (
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((c) => <TableCell key={c}>{c}</TableCell>)}
            </TableRow>
          </TableHead>
          <TableBody>{rows.map(render)}</TableBody>
        </Table>
      )}
    </Paper>
  );

  return (
    <Box sx={{ p: 3 }}>
      <Typography variant="h5" sx={{ mb: 3 }}>Needs attention</Typography>

      {section(
        'Unlinked documents',
        'Uploaded but never attached to a package, so they cannot be found from the lifecycle.',
        orphans,
        ['File', 'Type', 'Uploaded'],
        (row) => (
          <TableRow key={row.id}>
            <TableCell>{row.originalName || row.fileName}</TableCell>
            <TableCell>{row.documentType}</TableCell>
            <TableCell>{row.createdAt?.substring(0, 10)}</TableCell>
          </TableRow>
        ),
      )}

      {section(
        'Broken links',
        'A link whose document or package no longer resolves.',
        broken,
        ['Document', 'Entity', 'Stage', 'Role'],
        (row) => (
          <TableRow key={row.id}>
            <TableCell>#{row.documentId}</TableCell>
            <TableCell>{row.entityType}</TableCell>
            <TableCell>{row.stageCode}</TableCell>
            <TableCell>{row.docRole}</TableCell>
          </TableRow>
        ),
      )}

      {section(
        'Failed OCR runs',
        'The document is filed, but no text could be read from it — the values need entering by hand.',
        failedOcr,
        ['Document', 'Attempt', 'Error', 'When'],
        (row) => (
          <TableRow key={row.id}>
            <TableCell>#{row.documentId}</TableCell>
            <TableCell>{row.attemptNo}</TableCell>
            <TableCell sx={{ maxWidth: 400 }}>
              <Typography variant="body2" noWrap>{row.errorMessage}</Typography>
            </TableCell>
            <TableCell>{row.finishedAt?.substring(0, 10)}</TableCell>
          </TableRow>
        ),
      )}
    </Box>
  );
};

export default ExceptionsDashboard;
