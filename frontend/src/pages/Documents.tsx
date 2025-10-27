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
import { DocumentType } from '../types/document';

const Documents: React.FC = () => {
  const { user } = useAppSelector((state) => state.auth);
  const [documents, setDocuments] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [openUploadDialog, setOpenUploadDialog] = useState(false);
  const [editingDocument, setEditingDocument] = useState<any>(null);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    documentType: DocumentType.OTHER,
    department: '',
    tags: ''
  });
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<DocumentType | 'ALL'>('ALL');

  const departments = ['IT', 'HR', 'Finance', 'Operations', 'Legal', 'Marketing'];
  const documentTypes = Object.values(DocumentType);

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      setLoading(true);
      // Simulate API call
      const mockDocuments = [
        {
          id: 1,
          fileName: 'Company Policy.pdf',
          documentType: DocumentType.OTHER,
          uploadedBy: 'John Doe',
          uploadedAt: '2024-12-01T10:00:00Z',
          department: 'HR',
          size: '2.5 MB',
          status: 'Active'
        },
        {
          id: 2,
          fileName: 'Financial Report Q4.docx',
          documentType: DocumentType.OTHER,
          uploadedBy: 'Jane Smith',
          uploadedAt: '2024-12-02T14:30:00Z',
          department: 'Finance',
          size: '1.8 MB',
          status: 'Active'
        },
        {
          id: 3,
          fileName: 'Technical Specification.pdf',
          documentType: DocumentType.OTHER,
          uploadedBy: 'Mike Johnson',
          uploadedAt: '2024-12-03T09:15:00Z',
          department: 'IT',
          size: '3.2 MB',
          status: 'Active'
        }
      ];
      setDocuments(mockDocuments);
    } catch (err) {
      setError('Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  const handleUploadDocument = async () => {
    try {
      if (!uploadFile) {
        setError('Please select a file to upload');
        return;
      }

      // Simulate file upload
      const newDocument = {
        id: documents.length + 1,
        fileName: uploadFile.name,
        documentType: getFileType(uploadFile.name),
        uploadedBy: user?.username || 'Current User',
        uploadedAt: new Date().toISOString(),
        department: formData.department,
        size: formatFileSize(uploadFile.size),
        status: 'Active'
      };

      setDocuments([newDocument, ...documents]);
      setOpenUploadDialog(false);
      setUploadFile(null);
      setFormData({
        title: '',
        description: '',
        documentType: DocumentType.OTHER,
        department: '',
        tags: ''
      });
    } catch (err) {
      setError('Failed to upload document');
    }
  };

  const handleEditDocument = (document: any) => {
    setEditingDocument(document);
    setFormData({
      title: document.fileName,
      description: '',
      documentType: document.documentType,
      department: document.department,
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

  const handleDownloadDocument = (document: any) => {
    // Simulate download
    console.log('Downloading document:', document.fileName);
  };

  const handleViewDocument = (document: any) => {
    // Simulate document viewing
    console.log('Viewing document:', document.fileName);
  };

  const getFileType = (fileName: string): DocumentType => {
    const extension = fileName.split('.').pop()?.toLowerCase();
    switch (extension) {
      case 'pdf':
        return DocumentType.OTHER;
      case 'docx':
        return DocumentType.OTHER;
      case 'doc':
        return DocumentType.OTHER;
      case 'txt':
        return DocumentType.OTHER;
      case 'jpg':
      case 'jpeg':
        return DocumentType.OTHER;
      case 'png':
        return DocumentType.OTHER;
      default:
        return DocumentType.OTHER;
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const getDocumentTypeColor = (type: DocumentType) => {
    switch (type) {
      case DocumentType.OTHER:
        return 'error';
      case DocumentType.OTHER:
        return 'primary';
      case DocumentType.OTHER:
        return 'primary';
      case DocumentType.OTHER:
        return 'default';
      case DocumentType.OTHER:
        return 'success';
      case DocumentType.OTHER:
        return 'success';
      default:
        return 'default';
    }
  };

  const filteredDocuments = documents.filter(doc => {
    const matchesSearch = doc.fileName.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         doc.department.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filterType === 'ALL' || doc.documentType === filterType;
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
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                PDF Documents
              </Typography>
              <Typography variant="h4" color="error.main">
                {documents.filter(d => d.documentType === DocumentType.OTHER).length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Word Documents
              </Typography>
              <Typography variant="h4" color="primary.main">
                {documents.filter(d => d.documentType === DocumentType.OTHER || d.documentType === DocumentType.OTHER).length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Images
              </Typography>
              <Typography variant="h4" color="success.main">
                {documents.filter(d => d.documentType === DocumentType.OTHER || d.documentType === DocumentType.OTHER).length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

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
                      onChange={(e) => setFilterType(e.target.value as DocumentType | 'ALL')}
                    >
                      <MenuItem value="ALL">All Types</MenuItem>
                      {documentTypes.map((type) => (
                        <MenuItem key={type} value={type}>
                          {type}
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
                      setFilterType('ALL');
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
                            {document.department}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {document.uploadedBy}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {new Date(document.uploadedAt).toLocaleDateString()}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {document.size}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={document.status}
                            color="success"
                            size="small"
                          />
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
                              onClick={() => handleDeleteDocument(document.id)}
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