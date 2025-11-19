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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  IconButton,
  Alert,
  Chip,
  CircularProgress,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import { DocumentType } from '../constants/documentTypes';
import documentTypeFieldService, { DocumentTypeField, FieldOption } from '../services/documentTypeFieldService';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

const DocumentTypeFields: React.FC = () => {
  const [selectedDocumentType, setSelectedDocumentType] = useState<string>(DocumentType.TENDER_NOTICE);
  const [fields, setFields] = useState<DocumentTypeField[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [editingField, setEditingField] = useState<DocumentTypeField | null>(null);
  const [formData, setFormData] = useState<Partial<DocumentTypeField>>({
    documentType: DocumentType.TENDER_NOTICE,
    fieldKey: '',
    fieldLabel: '',
    fieldType: 'text',
    isRequired: false,
    isOcrMappable: false,
    ocrPattern: '',
    defaultValue: '',
    fieldOptions: '',
    displayOrder: 0,
    isActive: true,
    description: '',
  });

  const fieldTypes = ['text', 'number', 'date', 'select', 'multiselect'];

  useEffect(() => {
    loadFields();
  }, [selectedDocumentType]);

  const loadFields = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await documentTypeFieldService.getAllFieldsForDocumentType(selectedDocumentType);
      setFields(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load fields');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = (field?: DocumentTypeField) => {
    if (field) {
      setEditingField(field);
      setFormData({
        ...field,
        documentType: selectedDocumentType,
      });
    } else {
      setEditingField(null);
      setFormData({
        documentType: selectedDocumentType,
        fieldKey: '',
        fieldLabel: '',
        fieldType: 'text',
        isRequired: false,
        isOcrMappable: false,
        ocrPattern: '',
        defaultValue: '',
        fieldOptions: '',
        displayOrder: fields.length,
        isActive: true,
        description: '',
      });
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setEditingField(null);
    setFormData({
      documentType: selectedDocumentType,
      fieldKey: '',
      fieldLabel: '',
      fieldType: 'text',
      isRequired: false,
      isOcrMappable: false,
      ocrPattern: '',
      defaultValue: '',
      fieldOptions: '',
      displayOrder: 0,
      isActive: true,
      description: '',
    });
  };

  const handleSave = async () => {
    try {
      if (editingField?.id) {
        await documentTypeFieldService.updateField(editingField.id, formData as DocumentTypeField);
      } else {
        await documentTypeFieldService.createField(formData as DocumentTypeField);
      }
      handleCloseDialog();
      loadFields();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save field');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Are you sure you want to delete this field?')) {
      try {
        await documentTypeFieldService.deleteField(id);
        loadFields();
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to delete field');
      }
    }
  };

  const handleDeactivate = async (id: number) => {
    try {
      await documentTypeFieldService.deactivateField(id);
      loadFields();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to deactivate field');
    }
  };

  const documentTypes = Object.values(DocumentType);

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" sx={{ fontWeight: 600 }}>
          Document Type Fields Configuration
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpenDialog()}
        >
          Add Field
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          <FormControl fullWidth sx={{ mb: 3 }}>
            <InputLabel>Document Type</InputLabel>
            <Select
              value={selectedDocumentType}
              label="Document Type"
              onChange={(e) => setSelectedDocumentType(e.target.value)}
            >
              {documentTypes.map((type) => (
                <MenuItem key={type} value={type}>
                  {type.replace(/_/g, ' ')}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Field Key</TableCell>
                    <TableCell>Label</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Required</TableCell>
                    <TableCell>OCR Mappable</TableCell>
                    <TableCell>Display Order</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {fields.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={8} align="center">
                        No fields configured for this document type
                      </TableCell>
                    </TableRow>
                  ) : (
                    fields.map((field) => (
                      <TableRow key={field.id}>
                        <TableCell>{field.fieldKey}</TableCell>
                        <TableCell>{field.fieldLabel}</TableCell>
                        <TableCell>
                          <Chip label={field.fieldType} size="small" />
                        </TableCell>
                        <TableCell>
                          {field.isRequired ? (
                            <Chip label="Yes" color="error" size="small" />
                          ) : (
                            <Chip label="No" size="small" />
                          )}
                        </TableCell>
                        <TableCell>
                          {field.isOcrMappable ? (
                            <Chip label="Yes" color="success" size="small" />
                          ) : (
                            <Chip label="No" size="small" />
                          )}
                        </TableCell>
                        <TableCell>{field.displayOrder}</TableCell>
                        <TableCell>
                          {field.isActive ? (
                            <Chip label="Active" color="success" size="small" />
                          ) : (
                            <Chip label="Inactive" size="small" />
                          )}
                        </TableCell>
                        <TableCell>
                          <IconButton
                            size="small"
                            onClick={() => handleOpenDialog(field)}
                            color="primary"
                          >
                            <EditIcon />
                          </IconButton>
                          {field.isActive && (
                            <IconButton
                              size="small"
                              onClick={() => field.id && handleDeactivate(field.id)}
                              color="warning"
                            >
                              <CancelIcon />
                            </IconButton>
                          )}
                          <IconButton
                            size="small"
                            onClick={() => field.id && handleDelete(field.id)}
                            color="error"
                          >
                            <DeleteIcon />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {/* Add/Edit Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingField ? 'Edit Field' : 'Add New Field'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Field Key"
                value={formData.fieldKey}
                onChange={(e) => setFormData({ ...formData, fieldKey: e.target.value })}
                required
                disabled={!!editingField}
                helperText="Unique identifier (e.g., tenderId, contractNumber)"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Field Label"
                value={formData.fieldLabel}
                onChange={(e) => setFormData({ ...formData, fieldLabel: e.target.value })}
                required
                helperText="Display name for the field"
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Field Type</InputLabel>
                <Select
                  value={formData.fieldType}
                  label="Field Type"
                  onChange={(e) => setFormData({ ...formData, fieldType: e.target.value as any })}
                >
                  {fieldTypes.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Display Order"
                type="number"
                value={formData.displayOrder}
                onChange={(e) => setFormData({ ...formData, displayOrder: parseInt(e.target.value) || 0 })}
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isRequired || false}
                    onChange={(e) => setFormData({ ...formData, isRequired: e.target.checked })}
                  />
                }
                label="Required Field"
              />
            </Grid>
            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.isOcrMappable || false}
                    onChange={(e) => setFormData({ ...formData, isOcrMappable: e.target.checked })}
                  />
                }
                label="OCR Mappable (can be auto-populated from OCR)"
              />
            </Grid>
            {formData.isOcrMappable && (
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="OCR Pattern (Regex)"
                  value={formData.ocrPattern || ''}
                  onChange={(e) => setFormData({ ...formData, ocrPattern: e.target.value })}
                  helperText="Regular expression pattern to extract value from OCR text. Use capturing groups."
                  multiline
                  rows={2}
                />
              </Grid>
            )}
            {(formData.fieldType === 'select' || formData.fieldType === 'multiselect') && (
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Field Options (JSON)"
                  value={formData.fieldOptions || ''}
                  onChange={(e) => setFormData({ ...formData, fieldOptions: e.target.value })}
                  helperText='JSON array: [{"value":"option1","label":"Option 1"},{"value":"option2","label":"Option 2"}]'
                  multiline
                  rows={4}
                />
              </Grid>
            )}
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Default Value"
                value={formData.defaultValue || ''}
                onChange={(e) => setFormData({ ...formData, defaultValue: e.target.value })}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Description"
                value={formData.description || ''}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                multiline
                rows={2}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleSave} variant="contained" startIcon={<SaveIcon />}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DocumentTypeFields;

