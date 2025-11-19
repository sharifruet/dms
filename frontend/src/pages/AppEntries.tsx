import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert,
  CircularProgress,
  TextField,
  InputAdornment,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from '@mui/material';
import {
  Search as SearchIcon,
  Description as DocumentIcon,
  Download as DownloadIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { documentService, Document } from '../services/documentService';
import { getDocumentTypeLabel, getDocumentTypeColor } from '../constants/documentTypes';
import DocumentViewer from '../components/DocumentViewer';

interface AppEntry {
  id: number;
  entryDate: string;
  title: string;
  amount: number;
}

const AppEntries: React.FC = () => {
  const [appDocuments, setAppDocuments] = useState<Document[]>([]);
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [appEntries, setAppEntries] = useState<AppEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingEntries, setLoadingEntries] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [viewerOpen, setViewerOpen] = useState(false);

  useEffect(() => {
    loadAppDocuments();
  }, []);

  useEffect(() => {
    if (selectedDocument?.id) {
      loadAppEntries(selectedDocument.id);
    }
  }, [selectedDocument]);

  const loadAppDocuments = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await documentService.getDocuments({ size: 1000 });
      // Filter for APP documents
      const appDocs = (response.content || []).filter(
        (doc: Document) => doc.documentType === 'APP'
      );
      setAppDocuments(appDocs);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load APP documents');
    } finally {
      setLoading(false);
    }
  };

  const loadAppEntries = async (documentId: number) => {
    try {
      setLoadingEntries(true);
      const entries = await documentService.getAppEntries(documentId);
      setAppEntries(entries);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load APP entries');
      setAppEntries([]);
    } finally {
      setLoadingEntries(false);
    }
  };

  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatDate = (dateString: string): string => {
    if (!dateString) return '-';
    try {
      return new Date(dateString).toLocaleDateString();
    } catch {
      return dateString;
    }
  };

  const calculateTotal = (): number => {
    return appEntries.reduce((sum, entry) => sum + (entry.amount || 0), 0);
  };

  const filteredDocuments = appDocuments.filter((doc) =>
    (doc.originalName || doc.fileName || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4" component="h1">
          APP Entries
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* APP Documents List */}
        <Grid item xs={12} md={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                APP Documents ({appDocuments.length})
              </Typography>

              <TextField
                fullWidth
                size="small"
                placeholder="Search APP documents..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                sx={{ mb: 2 }}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
                }}
              />

              {filteredDocuments.length === 0 ? (
                <Alert severity="info">No APP documents found.</Alert>
              ) : (
                <Box sx={{ maxHeight: 600, overflow: 'auto' }}>
                  {filteredDocuments.map((doc) => (
                    <Card
                      key={doc.id}
                      sx={{
                        mb: 1,
                        cursor: 'pointer',
                        border: selectedDocument?.id === doc.id ? 2 : 1,
                        borderColor: selectedDocument?.id === doc.id ? 'primary.main' : 'divider',
                        '&:hover': {
                          backgroundColor: 'action.hover',
                        },
                      }}
                      onClick={() => setSelectedDocument(doc)}
                    >
                      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
                        <Box display="flex" alignItems="center" gap={1} mb={1}>
                          <DocumentIcon color="primary" />
                          <Typography variant="subtitle2" noWrap>
                            {doc.originalName || doc.fileName}
                          </Typography>
                        </Box>
                        <Box display="flex" gap={1} flexWrap="wrap">
                          <Chip
                            label={getDocumentTypeLabel(doc.documentType)}
                            size="small"
                            color={getDocumentTypeColor(doc.documentType) as any}
                          />
                          {doc.createdAt && (
                            <Typography variant="caption" color="text.secondary">
                              {new Date(doc.createdAt).toLocaleDateString()}
                            </Typography>
                          )}
                        </Box>
                      </CardContent>
                    </Card>
                  ))}
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* APP Entries Table */}
        <Grid item xs={12} md={8}>
          <Card>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
                <Typography variant="h6">
                  {selectedDocument
                    ? `Entries: ${selectedDocument.originalName || selectedDocument.fileName}`
                    : 'Select an APP document to view entries'}
                </Typography>
                {selectedDocument && (
                  <Box>
                    <IconButton
                      size="small"
                      onClick={() => setViewerOpen(true)}
                      title="View Document"
                    >
                      <ViewIcon />
                    </IconButton>
                  </Box>
                )}
              </Box>

              {!selectedDocument ? (
                <Alert severity="info">
                  Please select an APP document from the list to view its entries.
                </Alert>
              ) : loadingEntries ? (
                <Box display="flex" justifyContent="center" p={3}>
                  <CircularProgress />
                </Box>
              ) : appEntries.length === 0 ? (
                <Alert severity="warning">
                  No entries found for this APP document. The Excel file may not have been processed yet or may be empty.
                </Alert>
              ) : (
                <>
                  <TableContainer component={Paper} sx={{ maxHeight: 600 }}>
                    <Table stickyHeader>
                      <TableHead>
                        <TableRow>
                          <TableCell>Date</TableCell>
                          <TableCell>Title</TableCell>
                          <TableCell align="right">Amount</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {appEntries.map((entry) => (
                          <TableRow key={entry.id} hover>
                            <TableCell>{formatDate(entry.entryDate)}</TableCell>
                            <TableCell>{entry.title}</TableCell>
                            <TableCell align="right">
                              {formatCurrency(entry.amount || 0)}
                            </TableCell>
                          </TableRow>
                        ))}
                        <TableRow sx={{ backgroundColor: 'action.selected', fontWeight: 'bold' }}>
                          <TableCell colSpan={2}>
                            <Typography variant="subtitle2" fontWeight="bold">
                              Total
                            </Typography>
                          </TableCell>
                          <TableCell align="right">
                            <Typography variant="subtitle2" fontWeight="bold">
                              {formatCurrency(calculateTotal())}
                            </Typography>
                          </TableCell>
                        </TableRow>
                      </TableBody>
                    </Table>
                  </TableContainer>

                  <Box mt={2} display="flex" justifyContent="space-between" alignItems="center">
                    <Typography variant="body2" color="text.secondary">
                      {appEntries.length} entries
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total: {formatCurrency(calculateTotal())}
                    </Typography>
                  </Box>
                </>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Document Viewer */}
      {selectedDocument && (
        <DocumentViewer
          open={viewerOpen}
          onClose={() => setViewerOpen(false)}
          document={selectedDocument}
        />
      )}
    </Box>
  );
};

export default AppEntries;

