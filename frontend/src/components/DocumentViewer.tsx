import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  IconButton,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  Divider,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Grid,
} from '@mui/material';
import {
  Close as CloseIcon,
  Download as DownloadIcon,
  ZoomIn as ZoomInIcon,
  ZoomOut as ZoomOutIcon,
  ChevronLeft as PrevIcon,
  ChevronRight as NextIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import { Document, documentService } from '../services/documentService';
import { DocumentTypeField } from '../types/document';
import { folderService, Folder } from '../services/folderService';
import DocumentRelationships from './DocumentRelationships';

interface DocumentViewerProps {
  open: boolean;
  onClose: () => void;
  document: Document | null;
  onDownload?: () => void;
}

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`document-tabpanel-${index}`}
      aria-labelledby={`document-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const DocumentViewer: React.FC<DocumentViewerProps> = ({
  open,
  onClose,
  document: doc,
  onDownload,
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tabValue, setTabValue] = useState(0);
  const [zoom, setZoom] = useState(100);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [ocrText, setOcrText] = useState<string | null>(null);
  const [ocrProcessing, setOcrProcessing] = useState<boolean>(false);
  const [ocrConfidence, setOcrConfidence] = useState<number | null>(null);
  const [ocrError, setOcrError] = useState<string | null>(null);
  const [typeFields, setTypeFields] = useState<DocumentTypeField[]>([]);
  const [editingMetadata, setEditingMetadata] = useState(false);
  const [metadataValues, setMetadataValues] = useState<Record<string, string>>({});
  const [savingMetadata, setSavingMetadata] = useState(false);
  const [allFolders, setAllFolders] = useState<Folder[]>([]);
  const [documentFolderId, setDocumentFolderId] = useState<number | null>(null);
  const [loadingFolders, setLoadingFolders] = useState(false);
  const [savingFolder, setSavingFolder] = useState(false);

  useEffect(() => {
    if (open && doc) {
      loadDocumentPreview();
      loadFolders();
    }
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [open, doc]);

  // Load folders for dropdown
  const loadFolders = async () => {
    setLoadingFolders(true);
    try {
      const folders = await folderService.getFolderTree();
      // Flatten the folder tree to get all folders
      const flattenFolders = (folders: Folder[]): Folder[] => {
        const result: Folder[] = [];
        folders.forEach(folder => {
          result.push(folder);
          if (folder.subFolders && folder.subFolders.length > 0) {
            result.push(...flattenFolders(folder.subFolders));
          }
        });
        return result;
      };
      setAllFolders(flattenFolders(folders));
    } catch (err: any) {
      console.error('Error fetching folders:', err);
    } finally {
      setLoadingFolders(false);
    }
  };

  // Handle folder change
  const handleFolderChange = async (newFolderId: number | null) => {
    if (!doc?.id) return;
    
    setSavingFolder(true);
    try {
      await documentService.updateDocumentFolder(doc.id, newFolderId);
      setDocumentFolderId(newFolderId);
      // Reload document to get updated folder info
      const documentData = await documentService.getDocumentById(doc.id);
      if ((documentData as any).document?.folder) {
        setDocumentFolderId((documentData as any).document.folder.id);
      }
    } catch (error: any) {
      console.error('Failed to update folder:', error);
      alert('Failed to update folder: ' + (error.message || 'Unknown error'));
    } finally {
      setSavingFolder(false);
    }
  };

  // Poll for OCR text when processing
  useEffect(() => {
    if (!ocrProcessing || !doc?.id) return;

    let pollCount = 0;
    const maxPolls = 6; // Poll for up to 30 seconds (6 * 5 seconds)
    
    const pollInterval = setInterval(async () => {
      pollCount++;
      try {
        const ocrData = await documentService.getDocumentOCR(doc.id!);
        if (ocrData.ocrText && ocrData.ocrText.trim() !== '') {
          setOcrText(ocrData.ocrText);
          setOcrProcessing(false);
          setOcrConfidence(ocrData.ocrConfidence || null);
          setOcrError(null);
          clearInterval(pollInterval);
        } else {
          const ocrErrorMsg = (ocrData as { ocrError?: string }).ocrError;
          if (ocrErrorMsg) {
            // OCR failed, stop polling
            setOcrProcessing(false);
            setOcrError(ocrErrorMsg);
            clearInterval(pollInterval);
          } else if (pollCount >= maxPolls) {
            // Stop polling after max attempts
            setOcrProcessing(false);
            clearInterval(pollInterval);
          }
        }
      } catch (err) {
        if (pollCount >= maxPolls) {
          setOcrProcessing(false);
          clearInterval(pollInterval);
        }
      }
    }, 5000);

    return () => clearInterval(pollInterval);
  }, [ocrProcessing, doc?.id]);

  const loadDocumentPreview = async () => {
    if (!doc || !doc.id) return;

    setLoading(true);
    setError(null);

    try {
      // For now, we'll create a mock preview
      // In production, this would fetch from the backend
      const mockPreview = generateMockPreview(doc);
      setPreviewUrl(mockPreview);
      
      // Fetch document details including OCR text from backend
      try {
        const documentData = await documentService.getDocumentById(doc.id);
        
        // Set document folder ID
        if (doc.folder?.id) {
          setDocumentFolderId(doc.folder.id);
        } else if ((documentData as any).document?.folder?.id) {
          setDocumentFolderId((documentData as any).document.folder.id);
        } else {
          setDocumentFolderId(null);
        }
        
        // Load document type fields if available
        if ((documentData as any).typeFields) {
          const fields = (documentData as any).typeFields as DocumentTypeField[];
          setTypeFields(fields);
          const initialValues: Record<string, string> = {};
          fields.forEach(field => {
            initialValues[field.fieldKey] = field.value || field.defaultValue || '';
          });
          setMetadataValues(initialValues);
        }
        
        if (documentData.ocrText && documentData.ocrText.trim() !== '') {
          setOcrText(documentData.ocrText);
          setOcrProcessing(false);
          setOcrConfidence(documentData.ocrConfidence || null);
          setOcrError(null);
        } else {
          // Check if OCR is still processing or failed
          const isProcessing = (documentData as { ocrProcessing?: boolean }).ocrProcessing === true;
          const ocrErrorMsg = (documentData as { ocrError?: string }).ocrError;
          setOcrProcessing(isProcessing);
          setOcrError(ocrErrorMsg || null);
          
          // Try fetching OCR text specifically if not in main response
          try {
            const ocrData = await documentService.getDocumentOCR(doc.id);
            if (ocrData.ocrText && ocrData.ocrText.trim() !== '') {
              setOcrText(ocrData.ocrText);
              setOcrProcessing(false);
              setOcrConfidence(ocrData.ocrConfidence || null);
              setOcrError(null);
            } else {
              setOcrText(null);
              setOcrProcessing((ocrData as { ocrProcessing?: boolean }).ocrProcessing === true);
              setOcrError((ocrData as { ocrError?: string }).ocrError || null);
            }
          } catch (ocrErr) {
            // OCR endpoint might not exist or document not indexed yet
            // Fallback to extractedText if available
            setOcrText(doc.extractedText || null);
            setOcrProcessing(true);
            setOcrError(null);
          }
        }
      } catch (fetchErr) {
        // If fetching fails, fallback to extractedText if available
        setOcrText(doc.extractedText || null);
        setOcrProcessing(true);
      }
      
      setLoading(false);
    } catch (err: any) {
      setError(err.message || 'Failed to load document preview');
      setLoading(false);
    }
  };

  const generateMockPreview = (doc: Document): string => {
    // This is a placeholder - in production, you'd fetch actual document content
    return 'mock-preview-url';
  };

  const getFileType = (fileName: string): string => {
    const ext = fileName.split('.').pop()?.toLowerCase();
    return ext || 'unknown';
  };

  const renderPreview = () => {
    if (!doc) return null;

    const fileType = getFileType(doc.fileName);

    if (loading) {
      return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
          <CircularProgress />
        </Box>
      );
    }

    if (error) {
      return (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      );
    }

    switch (fileType) {
      case 'pdf':
        return renderPDFPreview();
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'gif':
        return renderImagePreview();
      case 'doc':
      case 'docx':
      case 'xls':
      case 'xlsx':
        return renderOfficePreview();
      default:
        return renderUnsupportedPreview();
    }
  };

  const renderPDFPreview = () => {
    return (
      <Box sx={{ minHeight: 500, backgroundColor: '#f5f5f5', borderRadius: 2, p: 2 }}>
        {/* PDF Viewer Controls */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <IconButton size="small" onClick={() => setCurrentPage(Math.max(1, currentPage - 1))}>
              <PrevIcon />
            </IconButton>
            <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', px: 2 }}>
              Page {currentPage} of {totalPages}
            </Typography>
            <IconButton size="small" onClick={() => setCurrentPage(Math.min(totalPages, currentPage + 1))}>
              <NextIcon />
            </IconButton>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <IconButton size="small" onClick={() => setZoom(Math.max(25, zoom - 25))}>
              <ZoomOutIcon />
            </IconButton>
            <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', px: 1 }}>
              {zoom}%
            </Typography>
            <IconButton size="small" onClick={() => setZoom(Math.min(200, zoom + 25))}>
              <ZoomInIcon />
            </IconButton>
          </Box>
        </Box>

        {/* PDF Canvas/Viewer */}
        <Box
          sx={{
            backgroundColor: '#fff',
            minHeight: 450,
            borderRadius: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '1px solid #e0e0e0',
          }}
        >
          <Typography variant="body2" color="textSecondary">
            PDF Preview (Integration with PDF.js)
            <br />
            <br />
            In production, this would display the actual PDF content using react-pdf or pdf.js
          </Typography>
        </Box>
      </Box>
    );
  };

  const renderImagePreview = () => {
    return (
      <Box sx={{ minHeight: 500, backgroundColor: '#f5f5f5', borderRadius: 2, p: 2, textAlign: 'center' }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2, gap: 1 }}>
          <IconButton size="small" onClick={() => setZoom(Math.max(25, zoom - 25))}>
            <ZoomOutIcon />
          </IconButton>
          <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', px: 1 }}>
            {zoom}%
          </Typography>
          <IconButton size="small" onClick={() => setZoom(Math.min(200, zoom + 25))}>
            <ZoomInIcon />
          </IconButton>
        </Box>
        <Box
          sx={{
            backgroundColor: '#fff',
            minHeight: 450,
            borderRadius: 1,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            border: '1px solid #e0e0e0',
          }}
        >
          <Typography variant="body2" color="textSecondary">
            Image Preview
            <br />
            <br />
            {doc?.fileName}
            <br />
            <br />
            In production, this would display the actual image with zoom and pan controls
          </Typography>
        </Box>
      </Box>
    );
  };

  const renderOfficePreview = () => {
    return (
      <Alert severity="info" sx={{ mt: 2 }}>
        Office document preview requires Microsoft Office Online integration or LibreOffice conversion.
        <br />
        <br />
        Download the document to view it in your local application.
      </Alert>
    );
  };

  const renderUnsupportedPreview = () => {
    return (
      <Alert severity="warning" sx={{ mt: 2 }}>
        Preview not available for this file type.
        <br />
        <br />
        Please download the document to view it.
      </Alert>
    );
  };

  const renderOCRText = () => {
    if (loading) {
      return (
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
          <CircularProgress />
        </Box>
      );
    }

    if (ocrProcessing) {
      return (
        <Alert severity="info">
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <CircularProgress size={20} />
            <Box>
              <Typography variant="body2" gutterBottom>
                <strong>OCR processing in progress...</strong>
              </Typography>
              <Typography variant="body2" sx={{ mt: 1, fontSize: '0.875rem' }}>
                Extracting text from document. This typically takes:
                <ul style={{ marginTop: '8px', marginBottom: '8px', paddingLeft: '20px' }}>
                  <li><strong>Small images (1-2 pages):</strong> 5-15 seconds</li>
                  <li><strong>Medium documents (3-10 pages):</strong> 15-45 seconds</li>
                  <li><strong>Large documents (10+ pages):</strong> 45 seconds - 2 minutes</li>
                </ul>
                The page will automatically update when OCR completes.
              </Typography>
            </Box>
          </Box>
        </Alert>
      );
    }

    if (!ocrText || ocrText.trim() === '') {
      return (
        <Alert severity={ocrError ? "warning" : "info"}>
          <Typography variant="body2" gutterBottom>
            <strong>
              {ocrError ? "OCR processing failed" : "No OCR text available for this document."}
            </strong>
          </Typography>
          {ocrError ? (
            <Typography variant="body2" sx={{ mt: 1 }}>
              {ocrError}
            </Typography>
          ) : (
            <>
              <Typography variant="body2" sx={{ mt: 1 }}>
                This document may not contain extractable text, or OCR processing may have failed.
                {ocrConfidence !== null && (
                  <span> OCR confidence: {(ocrConfidence * 100).toFixed(1)}%</span>
                )}
              </Typography>
              <Typography variant="body2" sx={{ mt: 1, fontSize: '0.875rem', color: 'text.secondary' }}>
                <strong>Note:</strong> If you just uploaded this document, please wait a few seconds and refresh.
                OCR processing runs asynchronously after upload.
              </Typography>
            </>
          )}
        </Alert>
      );
    }

    return (
      <Box
        sx={{
          backgroundColor: '#f9fafb',
          borderRadius: 2,
          p: 3,
          border: '1px solid #e5e7eb',
          minHeight: 400,
          maxHeight: 500,
          overflow: 'auto',
        }}
      >
        <Box>
          {ocrConfidence !== null && (
            <Typography variant="caption" color="text.secondary" sx={{ mb: 1, display: 'block' }}>
              OCR Confidence: {(ocrConfidence * 100).toFixed(1)}%
            </Typography>
          )}
          <Typography
            variant="body2"
            component="pre"
            sx={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-word',
              fontFamily: 'monospace',
              fontSize: '0.875rem',
              lineHeight: 1.6,
            }}
          >
            {ocrText}
          </Typography>
        </Box>
      </Box>
    );
  };

  const handleMetadataChange = (fieldKey: string, value: string) => {
    setMetadataValues(prev => ({
      ...prev,
      [fieldKey]: value
    }));
  };

  const handleSaveMetadata = async () => {
    if (!doc?.id) return;
    
    setSavingMetadata(true);
    try {
      await documentService.updateDocumentMetadata(doc.id, metadataValues);
      setEditingMetadata(false);
      // Reload document to get updated values
      const documentData = await documentService.getDocumentById(doc.id);
      if ((documentData as any).typeFields) {
        const fields = (documentData as any).typeFields as DocumentTypeField[];
        setTypeFields(fields);
        const updatedValues: Record<string, string> = {};
        fields.forEach(field => {
          updatedValues[field.fieldKey] = field.value || field.defaultValue || '';
        });
        setMetadataValues(updatedValues);
      }
    } catch (error: any) {
      console.error('Failed to save metadata:', error);
      alert('Failed to save metadata: ' + (error.message || 'Unknown error'));
    } finally {
      setSavingMetadata(false);
    }
  };

  const handleCancelEdit = () => {
    // Reset to original values
    const originalValues: Record<string, string> = {};
    typeFields.forEach(field => {
      originalValues[field.fieldKey] = field.value || field.defaultValue || '';
    });
    setMetadataValues(originalValues);
    setEditingMetadata(false);
  };

  const renderFieldInput = (field: DocumentTypeField) => {
    const value = metadataValues[field.fieldKey] || '';
    
    switch (field.fieldType) {
      case 'select':
        try {
          const options = field.fieldOptions ? JSON.parse(field.fieldOptions) : [];
          return (
            <FormControl fullWidth size="small">
              <InputLabel>{field.fieldLabel}</InputLabel>
              <Select
                value={value}
                label={field.fieldLabel}
                onChange={(e) => handleMetadataChange(field.fieldKey, e.target.value)}
                disabled={!editingMetadata}
              >
                {Array.isArray(options) && options.map((opt: string, idx: number) => (
                  <MenuItem key={idx} value={opt}>{opt}</MenuItem>
                ))}
              </Select>
            </FormControl>
          );
        } catch {
          return (
            <TextField
              fullWidth
              size="small"
              label={field.fieldLabel}
              value={value}
              onChange={(e) => handleMetadataChange(field.fieldKey, e.target.value)}
              disabled={!editingMetadata}
              required={field.isRequired}
            />
          );
        }
      case 'multiselect':
        try {
          const options = field.fieldOptions ? JSON.parse(field.fieldOptions) : [];
          const selectedValues = value ? value.split(',').map(v => v.trim()) : [];
          return (
            <FormControl fullWidth size="small">
              <InputLabel>{field.fieldLabel}</InputLabel>
              <Select
                multiple
                value={selectedValues}
                label={field.fieldLabel}
                onChange={(e) => handleMetadataChange(field.fieldKey, Array.isArray(e.target.value) ? e.target.value.join(',') : '')}
                disabled={!editingMetadata}
                renderValue={(selected) => (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {(selected as string[]).map((val) => (
                      <Chip key={val} label={val} size="small" />
                    ))}
                  </Box>
                )}
              >
                {Array.isArray(options) && options.map((opt: string, idx: number) => (
                  <MenuItem key={idx} value={opt}>{opt}</MenuItem>
                ))}
              </Select>
            </FormControl>
          );
        } catch {
          return (
            <TextField
              fullWidth
              size="small"
              label={field.fieldLabel}
              value={value}
              onChange={(e) => handleMetadataChange(field.fieldKey, e.target.value)}
              disabled={!editingMetadata}
              required={field.isRequired}
            />
          );
        }
      case 'number':
        return (
          <TextField
            fullWidth
            size="small"
            type="number"
            label={field.fieldLabel}
            value={value}
            onChange={(e) => handleMetadataChange(field.fieldKey, e.target.value)}
            disabled={!editingMetadata}
            required={field.isRequired}
          />
        );
      case 'date':
        return (
          <TextField
            fullWidth
            size="small"
            type="date"
            label={field.fieldLabel}
            value={value}
            onChange={(e) => handleMetadataChange(field.fieldKey, e.target.value)}
            disabled={!editingMetadata}
            required={field.isRequired}
            InputLabelProps={{ shrink: true }}
          />
        );
      default:
        return (
          <TextField
            fullWidth
            size="small"
            label={field.fieldLabel}
            value={value}
            onChange={(e) => handleMetadataChange(field.fieldKey, e.target.value)}
            disabled={!editingMetadata}
            required={field.isRequired}
            multiline={field.fieldType === 'textarea'}
            rows={field.fieldType === 'textarea' ? 3 : 1}
          />
        );
    }
  };

  const renderMetadata = () => {
    if (!doc) return null;

    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {/* Basic Document Information */}
        <Box>
          <Typography variant="h6" gutterBottom>Document Information</Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Box>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                File Name
              </Typography>
              <Typography variant="body1">{doc.fileName}</Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Document Type
              </Typography>
              <Typography variant="body1">{doc.documentType}</Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Department
              </Typography>
              <Typography variant="body1">{doc.department || 'N/A'}</Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Uploaded By
              </Typography>
              <Typography variant="body1">{typeof doc.uploadedBy === 'string' ? doc.uploadedBy : doc.uploadedBy?.username || 'Unknown'}</Typography>
            </Box>
            <Divider />
            <Box>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Upload Date
              </Typography>
              <Typography variant="body1">
                {doc.uploadedAt ? new Date(doc.uploadedAt).toLocaleString() : 'N/A'}
              </Typography>
            </Box>
            {doc.description && (
              <>
                <Divider />
                <Box>
                  <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                    Description
                  </Typography>
                  <Typography variant="body1">{doc.description}</Typography>
                </Box>
              </>
            )}
            {doc.tags && (
              <>
                <Divider />
                <Box>
                  <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                    Tags
                  </Typography>
                  <Typography variant="body1">{doc.tags}</Typography>
                </Box>
              </>
            )}
            <Divider />
            <Box>
              <Typography variant="subtitle2" color="textSecondary" gutterBottom>
                Folder
              </Typography>
              <FormControl fullWidth size="small" sx={{ mt: 1 }}>
                <InputLabel>Folder</InputLabel>
                <Select
                  value={documentFolderId || ''}
                  label="Folder"
                  onChange={(e) => handleFolderChange(e.target.value ? Number(e.target.value) : null)}
                  disabled={savingFolder || loadingFolders}
                >
                  <MenuItem value="">No Folder</MenuItem>
                  {allFolders.map((folder) => (
                    <MenuItem key={folder.id} value={folder.id}>
                      {folder.folderPath || folder.name}
                    </MenuItem>
                  ))}
                </Select>
                {savingFolder && (
                  <Typography variant="caption" sx={{ mt: 1, color: 'text.secondary' }}>
                    Saving...
                  </Typography>
                )}
              </FormControl>
            </Box>
          </Box>
        </Box>

        {/* Document Type Fields */}
        {typeFields.length > 0 && (
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="h6">Document Type Fields</Typography>
              {!editingMetadata ? (
                <Button
                  startIcon={<EditIcon />}
                  variant="outlined"
                  size="small"
                  onClick={() => setEditingMetadata(true)}
                >
                  Edit
                </Button>
              ) : (
                <Box sx={{ display: 'flex', gap: 1 }}>
                  <Button
                    startIcon={<SaveIcon />}
                    variant="contained"
                    size="small"
                    onClick={handleSaveMetadata}
                    disabled={savingMetadata}
                  >
                    Save
                  </Button>
                  <Button
                    startIcon={<CancelIcon />}
                    variant="outlined"
                    size="small"
                    onClick={handleCancelEdit}
                    disabled={savingMetadata}
                  >
                    Cancel
                  </Button>
                </Box>
              )}
            </Box>
            <Grid container spacing={2}>
              {typeFields
                .sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0))
                .map((field) => (
                  <Grid item xs={12} sm={6} key={field.id}>
                    {renderFieldInput(field)}
                    {field.description && (
                      <Typography variant="caption" color="textSecondary" sx={{ mt: 0.5, display: 'block' }}>
                        {field.description}
                      </Typography>
                    )}
                  </Grid>
                ))}
            </Grid>
          </Box>
        )}
      </Box>
    );
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="h6">{doc?.fileName || 'Document Viewer'}</Typography>
          <IconButton onClick={onClose}>
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent>
        <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)}>
            <Tab label="Preview" />
            <Tab label="OCR Text" />
            <Tab label="Metadata" />
            <Tab label="Relationships" />
          </Tabs>
        </Box>

        <TabPanel value={tabValue} index={0}>
          {renderPreview()}
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          {renderOCRText()}
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          {renderMetadata()}
        </TabPanel>

        <TabPanel value={tabValue} index={3}>
          {doc && <DocumentRelationships document={doc} />}
        </TabPanel>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        {onDownload && (
          <Button onClick={onDownload} variant="contained" startIcon={<DownloadIcon />}>
            Download
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

export default DocumentViewer;

