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
  FilterList as FilterIcon
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import { documentService, Document as DmsDocument } from '../services/documentService';
import { DocumentCategory } from '../types/document';

const DEFAULT_CATEGORIES: DocumentCategory[] = [
  { id: -1, name: 'TENDER', displayName: 'Tender', description: 'Tender documents', isActive: true },
  { id: -2, name: 'BILL', displayName: 'Bill', description: 'Bills and invoices', isActive: true },
  { id: -3, name: 'CONTRACT', displayName: 'Contract', description: 'Contract documents', isActive: true },
  { id: -4, name: 'GENERAL', displayName: 'General', description: 'General purpose documents', isActive: true }
];

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
    documentType: DEFAULT_CATEGORIES[0].name,
    department: '',
    tags: ''
  });
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<string>('');

  const departments = ['IT', 'HR', 'Finance', 'Operations', 'Legal', 'Marketing'];
  useEffect(() => {
    loadDocuments();
    loadCategories();
  }, []);

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
      const data = await documentService.getDocumentCategories();
      const normalized = (data && data.length > 0) ? data : DEFAULT_CATEGORIES;
      if (!data || data.length === 0) {
        console.warn('[Documents] No categories returned from API; using defaults');
      }
      setCategories(normalized);
      setFormData((prev) => ({
        ...prev,
        documentType: prev.documentType || (normalized[0]?.name ?? '')
      }));
    } catch (err: any) {
      console.error('Failed to load document categories', err);
      setCategories(DEFAULT_CATEGORIES);
      setFormData((prev) => ({
        ...prev,
        documentType: prev.documentType || DEFAULT_CATEGORIES[0].name
      }));
      setError(err.response?.data?.message || 'Failed to load document categories; using defaults');
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

      await documentService.uploadDocument({
        file: uploadFile,
        title: formData.title || uploadFile.name,
        description: formData.description,
        documentType: formData.documentType,
        department: formData.department,
        tags: formData.tags,
        userId: (user as any)?.id || 0,
      });

      setOpenUploadDialog(false);
      setUploadFile(null);
      setFormData({
        title: '',
        description: '',
        documentType: categories[0]?.name || '',
        department: '',
        tags: ''
      });
      await loadDocuments();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to upload document');
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
        setDocuments(documents.filter(doc => doc.id !== id));
      } catch (err) {
        setError('Failed to delete document');
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

  const getDocumentTypeColor = (type: string) => {
    if (!type) return 'default';
    const normalized = type.toUpperCase();
    if (normalized.includes('BILL')) return 'warning';
    if (normalized.includes('TENDER')) return 'primary';
    if (normalized.includes('CONTRACT')) return 'success';
    return 'default';
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
                            color={getDocumentTypeColor(document.documentType) as any}
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
      <Dialog open={openUploadDialog} onClose={() => setOpenUploadDialog(false)} maxWidth="md" fullWidth>
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
                  onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
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
          <Button onClick={() => setOpenUploadDialog(false)}>Cancel</Button>
          <Button onClick={handleUploadDocument} variant="contained">
            Upload
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