import React, { useRef, useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Tooltip,
  Typography,
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { DocumentLink, StageDocumentRequirement, UploadResult } from '../../types/procurement';

interface Props {
  requirements: StageDocumentRequirement[];
  uploaded: DocumentLink[];
  onUpload: (file: File, docRole: string) => Promise<UploadResult>;
}

/**
 * The document checklist for a stage. Uploads are optional: a file still feeds OCR,
 * but its absence does not block completion. Each row uploads its own document so
 * the user never has to work out which file satisfies which slot.
 */
const DocumentChecklist: React.FC<Props> = ({ requirements, uploaded, onUpload }) => {
  const [busyRole, setBusyRole] = useState<string | null>(null);
  const [message, setMessage] = useState<{ role: string; text: string; severity: 'success' | 'warning' } | null>(null);
  const inputRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const isUploaded = (docRole: string) => uploaded.some((d) => d.docRole === docRole);

  const handleFile = async (docRole: string, file?: File | null) => {
    if (!file) return;
    setBusyRole(docRole);
    setMessage(null);
    try {
      const result = await onUpload(file, docRole);
      if (result.ocrError) {
        // OCR failing is not an upload failure - the document is filed either way
        setMessage({ role: docRole, text: result.ocrError, severity: 'warning' });
      } else {
        const count = result.fields?.length ?? 0;
        setMessage({
          role: docRole,
          text: count > 0
            ? `${count} value${count === 1 ? '' : 's'} read from the document — please check them below`
            : 'Uploaded. No values were configured for this document type.',
          severity: 'success',
        });
      }
    } catch (e: any) {
      setMessage({
        role: docRole,
        text: e?.response?.data?.error || 'Upload failed',
        severity: 'warning',
      });
    } finally {
      setBusyRole(null);
    }
  };

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1 }}>
        Documents
        <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 1, fontWeight: 400 }}>
          optional — upload to extract fields
        </Typography>
      </Typography>
      <List dense disablePadding>
        {requirements.map((req) => {
          const done = isUploaded(req.docRole);
          return (
            <React.Fragment key={req.docRole}>
              <ListItem
                sx={{ px: 1, borderBottom: 1, borderColor: 'divider' }}
                secondaryAction={
                  <Tooltip title={done ? 'Upload another version' : 'Upload this document'}>
                    <span>
                      <IconButton
                        edge="end"
                        size="small"
                        disabled={busyRole === req.docRole}
                        onClick={() => inputRefs.current[req.docRole]?.click()}
                      >
                        {busyRole === req.docRole ? (
                          <CircularProgress size={18} />
                        ) : (
                          <UploadFileIcon fontSize="small" />
                        )}
                      </IconButton>
                    </span>
                  </Tooltip>
                }
              >
                {done ? (
                  <CheckCircleIcon fontSize="small" color="success" sx={{ mr: 1 }} />
                ) : (
                  <RadioButtonUncheckedIcon fontSize="small" sx={{ mr: 1, color: 'text.disabled' }} />
                )}
                <ListItemText
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <span>{req.docLabel}</span>
                      {!req.isMandatory && <Chip label="optional" size="small" sx={{ height: 18 }} />}
                      {req.isConditional && (
                        <Chip label="if applicable" size="small" sx={{ height: 18 }} />
                      )}
                    </Box>
                  }
                />
                <input
                  type="file"
                  hidden
                  ref={(el) => {
                    inputRefs.current[req.docRole] = el;
                  }}
                  onChange={(e) => {
                    handleFile(req.docRole, e.target.files?.[0]);
                    e.target.value = '';
                  }}
                />
              </ListItem>
              {message?.role === req.docRole && (
                <Alert severity={message.severity} sx={{ my: 1 }} onClose={() => setMessage(null)}>
                  {message.text}
                </Alert>
              )}
            </React.Fragment>
          );
        })}
      </List>
    </Box>
  );
};

export default DocumentChecklist;
