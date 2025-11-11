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
} from '@mui/material';
import {
  Close as CloseIcon,
  Download as DownloadIcon,
  ZoomIn as ZoomInIcon,
  ZoomOut as ZoomOutIcon,
  ChevronLeft as PrevIcon,
  ChevronRight as NextIcon,
} from '@mui/icons-material';
import { Document } from '../services/documentService';

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

  useEffect(() => {
    if (open && doc) {
      loadDocumentPreview();
    }
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [open, doc]);

  const loadDocumentPreview = async () => {
    if (!doc) return;

    setLoading(true);
    setError(null);

    try {
      // For now, we'll create a mock preview
      // In production, this would fetch from the backend
      const mockPreview = generateMockPreview(doc);
      setPreviewUrl(mockPreview);
      
      // Mock OCR text (in production, fetch from backend)
      // setOcrText(generateMockOCRText(doc));
      setOcrText(String(doc.extractedText ?? ""));
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

  const generateMockOCRText = (doc: Document): string => {
    return `Extracted text from ${doc.extractedText}\n\nThis is sample OCR text that would normally be extracted from the document using Tesseract OCR.\n\nThe system automatically processes uploaded documents and extracts text content for searching and indexing purposes.`;
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
    if (!ocrText) {
      return (
        <Alert severity="info">
          No OCR text available for this document.
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
    );
  };

  const renderMetadata = () => {
    if (!doc) return null;

    return (
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
          <Typography variant="body1">{(doc as any)?.uploadedBy?.username || (doc as any)?.uploadedBy || 'Unknown'}</Typography>
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

