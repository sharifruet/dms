import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Grid,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  CircularProgress,
  Tooltip,
  Fab,
  Badge
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Visibility as ViewIcon,
  Upload as UploadIcon,
  Search as SearchIcon,
  FilterList as FilterIcon,
  Archive as ArchiveIcon,
  Unarchive as UnarchiveIcon
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import { documentService, Document as DmsDocument } from '../services/documentService';
import { folderService, Folder } from '../services/folderService';
import { DocumentCategory } from '../types/document';
import { DocumentType, getDocumentTypeLabel, requiresTenderWorkflow, getDocumentTypeColor, ALL_DOCUMENT_TYPES } from '../constants/documentTypes';

const DEFAULT_CATEGORIES: DocumentCategory[] = [];

const Documents: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  const [documents, setDocuments] = useState<DmsDocument[]>([]);
  const [categories, setCategories] = useState<DocumentCategory[]>(DEFAULT_CATEGORIES);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [openUploadDialog, setOpenUploadDialog] = useState(false);
  const [editingDocument, setEditingDocument] = useState<DmsDocument | null>(null);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    documentType: '',
    department: '',
    tags: ''
  });
  const [selectedFolderId, setSelectedFolderId] = useState<number | null>(null);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [folderHasWorkflow, setFolderHasWorkflow] = useState<boolean>(false);
  const [loadingFolders, setLoadingFolders] = useState(false);
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<string>('');
  const [duplicateDialogOpen, setDuplicateDialogOpen] = useState(false);
  const [duplicateInfo, setDuplicateInfo] = useState<import('../types/document').FileUploadResponse | null>(null);
  const [detectedDocumentType, setDetectedDocumentType] = useState<{ type: string; confidence: number } | null>(null);

  const departments = ['IT', 'HR', 'Finance', 'Operations', 'Legal', 'Marketing'];
  useEffect(() => {
    loadDocuments();
    loadCategories();
    loadFolders();
  }, []);

  useEffect(() => {
    // Check if selected folder has workflow when document type requires it
    if (selectedFolderId && requiresTenderWorkflow(formData.documentType)) {
      checkFolderWorkflow(selectedFolderId);
    } else {
      setFolderHasWorkflow(false);
    }
  }, [selectedFolderId, formData.documentType]);

  const loadFolders = async () => {
    try {
      setLoadingFolders(true);
      const folderTree = await folderService.getFolderTree();
      setFolders(folderTree);
    } catch (err: any) {
      console.error('Failed to load folders', err);
    } finally {
      setLoadingFolders(false);
    }
  };

  const checkFolderWorkflow = async (folderId: number) => {
    try {
      const hasWorkflow = await folderService.folderHasWorkflow(folderId);
      setFolderHasWorkflow(hasWorkflow);
    } catch (err: any) {
      console.error('Failed to check folder workflow', err);
      setFolderHasWorkflow(false);
    }
  };

  const flattenFolders = (folderTree: Folder[], result: Folder[] = []): Folder[] => {
    folderTree.forEach(folder => {
      result.push(folder);
      if (folder.subFolders && folder.subFolders.length > 0) {
        flattenFolders(folder.subFolders, result);
      }
    });
    return result;
  };

  const loadDocuments = async () => {
    try {
      setLoading(true);
      const resp = await documentService.getDocuments({ page: 0, size: 50 });
      setDocuments(resp.content || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  const loadCategories = async () => {
    try {
      const types = await documentService.getDocumentTypes();
      const allowed = new Set(ALL_DOCUMENT_TYPES);
      const filtered = types.filter((t: any) => allowed.has(t.value));
      const order: Record<string, number> = {
        [DocumentType.TENDER_NOTICE]: 1,
        [DocumentType.TENDER_DOCUMENT]: 2,
        [DocumentType.CONTRACT_AGREEMENT]: 3,
        [DocumentType.BANK_GUARANTEE_BG]: 4,
        [DocumentType.PERFORMANCE_SECURITY_PS]: 5,
        [DocumentType.PERFORMANCE_GUARANTEE_PG]: 6,
        [DocumentType.BILL]: 7,
        [DocumentType.STATIONERY_RECORD]: 8,
        [DocumentType.OTHER]: 99,
      };
      const mapped: DocumentCategory[] = filtered.map((t, idx) => ({
        id: -(idx + 1),
        name: t.value,
        displayName: t.label,
        description: t.label,
        isActive: true
      })).sort((a, b) => (order[a.name] ?? 100) - (order[b.name] ?? 100));
      setFormData((prev) => ({
        ...prev,
        documentType: prev.documentType || (mapped[0]?.name ?? '')
      }));
      setCategories(mapped);
    } catch (err: any) {
      console.error('Failed to load document types', err);
      setError(err.response?.data?.message || 'Failed to load document types');
      // Hard fallback using constants
      const fallback: DocumentCategory[] = ALL_DOCUMENT_TYPES.map((type, idx) => ({
        id: -(idx + 1),
        name: type,
        displayName: getDocumentTypeLabel(type),
        description: getDocumentTypeLabel(type),
        isActive: true
      }));
      setCategories(fallback);
      setFormData((prev) => ({
        ...prev,
        documentType: prev.documentType || fallback[0].name
      }));
    }
  };

  const handleUploadDocument = async () => {
    try {
      if (!uploadFile) {
        setError('Please select a file to upload');
        return;
      }

      if (!formData.documentType) {
        setError('Please select a document type');
        return;
      }

      // Phase 2: Folder validation
      if (formData.documentType === DocumentType.TENDER_NOTICE) {
        if (!selectedFolderId) {
          setError('Folder selection is required for Tender Notice uploads');
          return;
        }
      }

      if (requiresTenderWorkflow(formData.documentType)) {
        if (!selectedFolderId) {
          setError('Folder selection is required for this document type. Please select the folder used for the Tender Notice upload.');
          return;
        }
        if (!folderHasWorkflow) {
          setError('Selected folder does not have an associated workflow. Please select a folder that was used for a Tender Notice upload.');
          return;
        }
      }

      const response = await documentService.uploadDocument({
        file: uploadFile,
        title: formData.title || uploadFile.name,
        description: formData.description,
        documentType: formData.documentType,
        department: formData.department,
        tags: formData.tags,
        userId: user?.id || 0,
        folderId: selectedFolderId || undefined,
      });

      // Check if duplicate was detected
      if (response.isDuplicate) {
        setDuplicateInfo(response);
        setDuplicateDialogOpen(true);
        return;
      }

      // Success - close dialog and refresh
      setOpenUploadDialog(false);
      setUploadFile(null);
      setDetectedDocumentType(null);
      setFormData({
        title: '',
        description: '',
        documentType: categories[0]?.name || '',
        department: '',
        tags: ''
      });
      setSelectedFolderId(null);
      setFolderHasWorkflow(false);
      
      // Show detection result if available
      if (response.detectedDocumentType && response.detectionConfidence) {
        setDetectedDocumentType({
          type: response.detectedDocumentType,
          confidence: response.detectionConfidence
        });
      }
      
      await loadDocuments();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to upload document');
    }
  };

  const handleDuplicateAction = async (action: 'skip' | 'version' | 'replace') => {
    if (!uploadFile || !duplicateInfo || !duplicateInfo.duplicateDocumentId) {
      return;
    }

    try {
      const response = await documentService.handleDuplicateUpload(
        uploadFile,
        formData.documentType,
        duplicateInfo.duplicateDocumentId,
        action,
        formData.description,
        undefined, // metadata - no longer needed
        selectedFolderId || undefined // folderId
      );

      if (response.success) {
        setDuplicateDialogOpen(false);
        setDuplicateInfo(null);
        setOpenUploadDialog(false);
        setUploadFile(null);
        setFormData({
          title: '',
          description: '',
          documentType: categories[0]?.name || '',
          department: '',
          tags: ''
        });
        setSelectedFolderId(null);
        setFolderHasWorkflow(false);
        await loadDocuments();
      } else {
        setError(response.message || 'Failed to handle duplicate upload');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to handle duplicate upload');
    }
  };

  const handleEditDocument = (document: DmsDocument) => {
    setEditingDocument(document);
    setFormData({
      title: document.fileName,
      description: '',
      documentType: document.documentType,
      department: document.department ?? '',
      tags: ''
    });
    setOpenDialog(true);
  };

  const handleDeleteDocument = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this document?')) {
      try {
        await documentService.deleteDocument(id);
        await loadDocuments();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to delete document');
      }
    }
  };

  const handleArchiveDocument = async (id: number) => {
    if (window.confirm('Are you sure you want to archive this document?')) {
      try {
        await documentService.archiveDocument(id);
        await loadDocuments();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to archive document');
      }
    }
  };

  const handleDownloadDocument = (document: DmsDocument) => {
    console.log('Downloading document:', document.fileName);
  };

  const handleViewDocument = (document: DmsDocument) => {
    console.log('Viewing document:', document.fileName);
  };

  const formatFileSize = (value?: string | number): string => {
    if (value === undefined || value === null) return '-';
    const bytes = typeof value === 'string' ? parseInt(value, 10) : value;
    if (!Number.isFinite(bytes) || bytes <= 0) {
      return typeof value === 'string' ? value : `${bytes}`;
    }
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };


  const categoryCounts = categories.reduce<Record<string, number>>((acc, category) => {
    acc[category.name] = documents.filter((doc) => doc.documentType === category.name).length;
    return acc;
  }, {});

  const filteredDocuments = documents.filter(doc => {
    const matchesSearch = (doc.fileName || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
                         (doc.department || '').toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = !filterType || doc.documentType === filterType;
    return matchesSearch && matchesFilter;
  });

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
          Documents
        </Typography>
        <Button
          variant="contained"
          startIcon={<UploadIcon />}
          onClick={() => setOpenUploadDialog(true)}
        >
          Upload Document
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* Statistics Cards */}
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Total Documents
              </Typography>
              <Typography variant="h4">
                {documents.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        {categories.slice(0, 3).map((category) => (
          <Grid item xs={12} md={3} key={category.id}>
            <Card>
              <CardContent>
                <Typography color="textSecondary" gutterBottom>
                  {category.displayName || category.name}
                </Typography>
                <Typography variant="h4">
                  {categoryCounts[category.name] ?? 0}
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        ))}

        {/* Search and Filter */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Search documents"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    InputProps={{
                      startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />
                    }}
                  />
                </Grid>
                <Grid item xs={12} md={3}>
                  <FormControl fullWidth>
                    <InputLabel>Filter by Type</InputLabel>
                    <Select
                      value={filterType}
                      onChange={(e) => setFilterType(e.target.value as string)}
                    >
                      <MenuItem value="">All Types</MenuItem>
                      {categories.map((category) => (
                        <MenuItem key={category.id} value={category.name}>
                          {category.displayName || category.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>
                <Grid item xs={12} md={3}>
                  <Button
                    fullWidth
                    variant="outlined"
                    startIcon={<FilterIcon />}
                    onClick={() => {
                      setSearchTerm('');
                      setFilterType('');
                    }}
                  >
                    Clear Filters
                  </Button>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Documents Table */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Document List ({filteredDocuments.length} documents)
              </Typography>
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>File Name</TableCell>
                      <TableCell>Type</TableCell>
                      <TableCell>Department</TableCell>
                      <TableCell>Uploaded By</TableCell>
                      <TableCell>Upload Date</TableCell>
                      <TableCell>Size</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filteredDocuments.map((document) => (
                      <TableRow key={document.id}>
                        <TableCell>
                          <Typography variant="subtitle2">
                            {document.fileName}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={document.documentType}
                            color={getDocumentTypeColor(document.documentType)}
                            size="small"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {document.department || '-'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {typeof document.uploadedBy === 'string'
                              ? document.uploadedBy
                              : document.uploadedBy?.username || 'Unknown'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {document.createdAt
                              ? new Date(document.createdAt).toLocaleDateString()
                              : '-'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {formatFileSize(document.size ?? document.fileSize)}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {document.isActive ? 'Active' : 'Inactive'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Tooltip title="View Document">
                            <IconButton
                              size="small"
                              onClick={() => handleViewDocument(document)}
                            >
                              <ViewIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Download">
                            <IconButton
                              size="small"
                              onClick={() => handleDownloadDocument(document)}
                            >
                              <DownloadIcon />
                            </IconButton>
                          </Tooltip>
                          <Tooltip title="Edit">
                            <IconButton
                              size="small"
                              onClick={() => handleEditDocument(document)}
                            >
                              <EditIcon />
                            </IconButton>
                          </Tooltip>
                          {!document.isArchived && (
                            <Tooltip title="Archive">
                              <IconButton
                                size="small"
                                onClick={() => {
                                  if (!document.id) {
                                    setError('Unable to archive a document without an id');
                                    return;
                                  }
                                  handleArchiveDocument(document.id);
                                }}
                                color="warning"
                              >
                                <ArchiveIcon />
                              </IconButton>
                            </Tooltip>
                          )}
                          <Tooltip title="Delete">
                            <IconButton
                              size="small"
                              onClick={() => {
                                if (!document.id) {
                                  setError('Unable to delete a document without an id');
                                  return;
                                }
                                handleDeleteDocument(document.id);
                              }}
                              color="error"
                            >
                              <DeleteIcon />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Upload Document Dialog */}
      <Dialog open={openUploadDialog} onClose={() => {
        setOpenUploadDialog(false);
        setSelectedFolderId(null);
        setFolderHasWorkflow(false);
      }} maxWidth="md" fullWidth>
        <DialogTitle>Upload New Document</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <Button
                variant="outlined"
                component="label"
                fullWidth
                startIcon={<UploadIcon />}
              >
                {uploadFile ? uploadFile.name : 'Choose File'}
                <input
                  type="file"
                  hidden
                  onChange={(e) => {
                    const file = e.target.files?.[0] || null;
                    setUploadFile(file);
                    // Quick classification based on filename
                    if (file) {
                      const fileName = file.name.toLowerCase();
                      let detectedType: string | null = null;
                      let confidence = 0.5;
                      
                      if (fileName.includes('tender') && fileName.includes('notice')) {
                        detectedType = DocumentType.TENDER_NOTICE;
                        confidence = 0.7;
                      } else if (fileName.includes('tender') && (fileName.includes('doc') || fileName.includes('document'))) {
                        detectedType = DocumentType.TENDER_DOCUMENT;
                        confidence = 0.7;
                      } else if (fileName.includes('contract')) {
                        detectedType = DocumentType.CONTRACT_AGREEMENT;
                        confidence = 0.7;
                      } else if (fileName.includes('bank') && fileName.includes('guarantee')) {
                        detectedType = DocumentType.BANK_GUARANTEE_BG;
                        confidence = 0.7;
                      } else if (fileName.includes('performance') && fileName.includes('security')) {
                        detectedType = DocumentType.PERFORMANCE_SECURITY_PS;
                        confidence = 0.7;
                      } else if (fileName.includes('performance') && fileName.includes('guarantee')) {
                        detectedType = DocumentType.PERFORMANCE_GUARANTEE_PG;
                        confidence = 0.7;
                      } else if (fileName.includes('invoice') || fileName.includes('bill')) {
                        detectedType = DocumentType.BILL;
                        confidence = 0.7;
                      }
                      
                      if (detectedType) {
                        setDetectedDocumentType({ type: detectedType, confidence });
                        // Auto-select if confidence is high enough and no type is selected
                        if (confidence >= 0.6 && !formData.documentType) {
                          setFormData({ ...formData, documentType: detectedType });
                        }
                      } else {
                        setDetectedDocumentType(null);
                      }
                    } else {
                      setDetectedDocumentType(null);
                    }
                  }}
                  accept=".pdf,.doc,.docx,.txt,.jpg,.jpeg,.png"
                />
              </Button>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Title"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Document Type</InputLabel>
                <Select
                  value={formData.documentType}
                  label="Document Type"
                  onChange={(e) => {
                    setFormData({ ...formData, documentType: e.target.value });
                    setDetectedDocumentType(null); // Clear detection when manually changed
                  }}
                  disabled={categories.length === 0}
                >
                  {categories.map((category) => (
                    <MenuItem key={category.id} value={category.name}>
                      {category.displayName || category.name}
                    </MenuItem>
                  ))}
                </Select>
                {detectedDocumentType && detectedDocumentType.confidence >= 0.3 && (
                  <Box sx={{ mt: 1 }}>
                    <Alert severity={detectedDocumentType.confidence >= 0.6 ? "info" : "warning"} sx={{ py: 0.5 }}>
                      <Typography variant="caption">
                        Auto-detected: <strong>{getDocumentTypeLabel(detectedDocumentType.type)}</strong>
                        {' '}({Math.round(detectedDocumentType.confidence * 100)}% confidence)
                        {detectedDocumentType.confidence < 0.6 && ' - Please verify'}
                      </Typography>
                    </Alert>
                  </Box>
                )}
              </FormControl>
            </Grid>
            {/* Phase 2: Folder Selection - Required for Tender Notice and workflow documents */}
            <Grid item xs={12}>
              <FormControl fullWidth required={formData.documentType === DocumentType.TENDER_NOTICE || requiresTenderWorkflow(formData.documentType)}>
                <InputLabel>Folder *</InputLabel>
                <Select
                  value={selectedFolderId || ''}
                  label="Folder *"
                  onChange={(e) => {
                    const folderId = e.target.value as number | '';
                    setSelectedFolderId(folderId === '' ? null : folderId);
                  }}
                  disabled={loadingFolders}
                >
                  {flattenFolders(folders).map((folder) => (
                    <MenuItem key={folder.id} value={folder.id}>
                      {folder.folderPath || folder.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              {formData.documentType === DocumentType.TENDER_NOTICE && !selectedFolderId && (
                <Typography variant="caption" color="error" sx={{ mt: 0.5, display: 'block' }}>
                  Folder selection is required for Tender Notice uploads
                </Typography>
              )}
              {requiresTenderWorkflow(formData.documentType) && selectedFolderId && !folderHasWorkflow && (
                <Alert severity="warning" sx={{ mt: 1 }}>
                  Selected folder does not have an associated workflow. Please select a folder that was used for a Tender Notice upload.
                </Alert>
              )}
              {formData.documentType === DocumentType.TENDER_NOTICE && selectedFolderId && (
                <Alert severity="info" sx={{ mt: 1 }}>
                  A workflow will be automatically created for this folder using the folder name. Subsequent documents can be uploaded to this same folder.
                </Alert>
              )}
              {requiresTenderWorkflow(formData.documentType) && selectedFolderId && folderHasWorkflow && (
                <Alert severity="success" sx={{ mt: 1 }}>
                  Folder has an associated workflow. This document will be added to that workflow.
                </Alert>
              )}
            </Grid>
            {formData.documentType === DocumentType.TENDER_NOTICE && (
              <Grid item xs={12}>
                <Alert severity="info">
                  Selecting <strong>Tender Notice</strong> requires a folder selection. 
                  A workflow will be automatically created with the folder name. 
                  Upload subsequent documents (2–7) to the same folder.
                </Alert>
              </Grid>
            )}
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Department</InputLabel>
                <Select
                  value={formData.department}
                  onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                >
                  {departments.map((dept) => (
                    <MenuItem key={dept} value={dept}>
                      {dept}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                multiline
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Tags (comma-separated)"
                value={formData.tags}
                onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                placeholder="policy, important, confidential"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setOpenUploadDialog(false);
            setSelectedFolderId(null);
            setFolderHasWorkflow(false);
          }}>Cancel</Button>
          <Button onClick={handleUploadDocument} variant="contained">
            Upload
          </Button>
        </DialogActions>
      </Dialog>

      {/* Duplicate Detection Dialog */}
      <Dialog open={duplicateDialogOpen} onClose={() => setDuplicateDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Duplicate File Detected</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            A file with identical content already exists in the system.
          </Alert>
          
          {duplicateInfo && (
            <Box sx={{ mb: 3 }}>
              <Typography variant="subtitle2" gutterBottom>
                Existing Document:
              </Typography>
              <Box sx={{ pl: 2, borderLeft: 2, borderColor: 'divider' }}>
                <Typography variant="body2">
                  <strong>File:</strong> {duplicateInfo.duplicateOriginalName || duplicateInfo.duplicateFileName}
                </Typography>
                <Typography variant="body2">
                  <strong>Type:</strong> {duplicateInfo.duplicateDocumentType}
                </Typography>
                <Typography variant="body2">
                  <strong>Size:</strong> {formatFileSize(duplicateInfo.duplicateFileSize)}
                </Typography>
                <Typography variant="body2">
                  <strong>Uploaded by:</strong> {duplicateInfo.duplicateUploadedBy}
                </Typography>
                {duplicateInfo.duplicateCreatedAt && (
                  <Typography variant="body2">
                    <strong>Uploaded on:</strong> {new Date(duplicateInfo.duplicateCreatedAt).toLocaleString()}
                  </Typography>
                )}
              </Box>
            </Box>
          )}

          <Typography variant="subtitle2" gutterBottom>
            Choose an action:
          </Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            <Button
              variant="outlined"
              fullWidth
              onClick={() => handleDuplicateAction('skip')}
              sx={{ justifyContent: 'flex-start', textAlign: 'left' }}
            >
              <Box sx={{ textAlign: 'left' }}>
                <Typography variant="body1" fontWeight="bold">Skip Upload</Typography>
                <Typography variant="caption" color="text.secondary">
                  Cancel this upload. The existing document will remain unchanged.
                </Typography>
              </Box>
            </Button>
            <Button
              variant="outlined"
              fullWidth
              onClick={() => handleDuplicateAction('version')}
              sx={{ justifyContent: 'flex-start', textAlign: 'left' }}
            >
              <Box sx={{ textAlign: 'left' }}>
                <Typography variant="body1" fontWeight="bold">Upload as New Version</Typography>
                <Typography variant="caption" color="text.secondary">
                  Create a new version of the existing document. Version history will be maintained.
                </Typography>
              </Box>
            </Button>
            <Button
              variant="outlined"
              fullWidth
              onClick={() => handleDuplicateAction('replace')}
              sx={{ justifyContent: 'flex-start', textAlign: 'left' }}
            >
              <Box sx={{ textAlign: 'left' }}>
                <Typography variant="body1" fontWeight="bold">Replace Existing Document</Typography>
                <Typography variant="caption" color="text.secondary">
                  Replace the existing document with this file. The old file will be overwritten.
                </Typography>
              </Box>
            </Button>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => {
            setDuplicateDialogOpen(false);
            setDuplicateInfo(null);
          }}>
            Cancel
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Document Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Edit Document</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Title"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Document Type</InputLabel>
                <Select
                  value={formData.documentType}
                  label="Document Type"
                  onChange={(e) => setFormData({ ...formData, documentType: e.target.value })}
                  disabled={categories.length === 0}
                >
                  {categories.map((category) => (
                    <MenuItem key={category.id} value={category.name}>
                      {category.displayName || category.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Department</InputLabel>
                <Select
                  value={formData.department}
                  onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                >
                  {departments.map((dept) => (
                    <MenuItem key={dept} value={dept}>
                      {dept}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                multiline
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Tags (comma-separated)"
                value={formData.tags}
                onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                placeholder="policy, important, confidential"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={() => setOpenDialog(false)} variant="contained">
            Save Changes
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default Documents;