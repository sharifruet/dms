import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  TextField,
  Grid,
  Card,
  CardContent,
  Alert,
  Chip,
  Tooltip,
  IconButton,
  LinearProgress,
  Divider,
  Button,
} from '@mui/material';
import {
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Info as InfoIcon,
} from '@mui/icons-material';
import { documentService } from '../services/documentService';
import { DocumentType } from '../constants/documentTypes';

interface BillField {
  key: string;
  label: string;
  value: string;
  confidence?: number;
  source?: 'ocr' | 'manual';
  verified?: boolean;
}

interface BillFieldsEditorProps {
  documentId: number;
  documentType: string;
  metadata?: Record<string, string>;
  onSave?: (metadata: Record<string, string>) => Promise<void>;
  readOnly?: boolean;
}

const BILL_FIELD_KEYS = {
  vendorName: 'Vendor Name',
  invoiceNumber: 'Invoice Number',
  invoiceDate: 'Invoice Date',
  fiscalYear: 'Fiscal Year',
  totalAmount: 'Total Amount (BDT)',
  taxAmount: 'Tax Amount (BDT)',
  netAmount: 'Net Amount (BDT)',
  description: 'Description',
};

const BillFieldsEditor: React.FC<BillFieldsEditorProps> = ({
  documentId,
  documentType,
  metadata = {},
  onSave,
  readOnly = false,
}) => {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [fields, setFields] = useState<Record<string, BillField>>({});
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [saveSuccess, setSaveSuccess] = useState(false);

  useEffect(() => {
    if (documentType === DocumentType.BILL) {
      loadBillFields();
    }
  }, [documentId, documentType, metadata]);

  const loadBillFields = async () => {
    try {
      // Use provided metadata or load document to get metadata
      let metadataToUse = metadata;
      
      if (!metadataToUse || Object.keys(metadataToUse).length === 0) {
        const documentData = await documentService.getDocumentById(documentId);
        metadataToUse = (documentData as any).metadata || {};
      }
      
      // Extract bill fields from metadata
      const billFields: Record<string, BillField> = {};
      
      Object.keys(BILL_FIELD_KEYS).forEach((key) => {
        const value = metadataToUse[key] || '';
        const confidenceKey = `${key}_confidence`;
        const confidence = metadataToUse[confidenceKey] 
          ? parseFloat(metadataToUse[confidenceKey]) 
          : undefined;
        const source = confidence !== undefined ? 'ocr' : (value ? 'manual' : undefined);
        
        billFields[key] = {
          key,
          label: BILL_FIELD_KEYS[key as keyof typeof BILL_FIELD_KEYS],
          value,
          confidence,
          source,
          verified: !!value && confidence === undefined, // Consider verified if no confidence score (manually entered)
        };
      });
      
      setFields(billFields);
    } catch (error) {
      console.error('Failed to load bill fields:', error);
    }
  };

  const handleFieldChange = (key: string, value: string) => {
    setFields(prev => ({
      ...prev,
      [key]: {
        ...prev[key],
        value,
        verified: true, // Mark as verified when user edits
        source: 'manual',
      }
    }));
    
    // Clear error for this field
    if (errors[key]) {
      setErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[key];
        return newErrors;
      });
    }
  };

  const validateFields = (): boolean => {
    const newErrors: Record<string, string> = {};
    
    // Validate required fields
    if (!fields.vendorName?.value?.trim()) {
      newErrors.vendorName = 'Vendor name is required';
    }
    if (!fields.invoiceNumber?.value?.trim()) {
      newErrors.invoiceNumber = 'Invoice number is required';
    }
    if (!fields.invoiceDate?.value?.trim()) {
      newErrors.invoiceDate = 'Invoice date is required';
    }
    if (!fields.totalAmount?.value?.trim()) {
      newErrors.totalAmount = 'Total amount is required';
    }
    
    // Validate date format
    if (fields.invoiceDate?.value && !/^\d{4}-\d{2}-\d{2}$/.test(fields.invoiceDate.value)) {
      newErrors.invoiceDate = 'Date must be in YYYY-MM-DD format';
    }
    
    // Validate amounts are numbers
    if (fields.totalAmount?.value && isNaN(parseFloat(fields.totalAmount.value))) {
      newErrors.totalAmount = 'Total amount must be a valid number';
    }
    if (fields.taxAmount?.value && isNaN(parseFloat(fields.taxAmount.value))) {
      newErrors.taxAmount = 'Tax amount must be a valid number';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const calculateNetAmount = () => {
    const total = parseFloat(fields.totalAmount?.value || '0');
    const tax = parseFloat(fields.taxAmount?.value || '0');
    const net = total - tax;
    
    if (!isNaN(net) && (total > 0 || tax > 0)) {
      setFields(prev => ({
        ...prev,
        netAmount: {
          ...prev.netAmount || { key: 'netAmount', label: BILL_FIELD_KEYS.netAmount },
          value: net.toFixed(2),
          verified: true,
          source: 'manual',
        }
      }));
    }
  };

  useEffect(() => {
    if (fields.totalAmount?.value || fields.taxAmount?.value) {
      calculateNetAmount();
    }
  }, [fields.totalAmount?.value, fields.taxAmount?.value]);

  const handleSave = async () => {
    if (!validateFields()) {
      return;
    }
    
    setSaving(true);
    setSaveSuccess(false);
    
    try {
      // Prepare metadata object
      const metadataToSave: Record<string, string> = {};
      
      Object.keys(fields).forEach((key) => {
        if (fields[key]?.value) {
          metadataToSave[key] = fields[key].value;
        }
      });
      
      if (onSave) {
        await onSave(metadataToSave);
      } else {
        await documentService.updateDocumentMetadata(documentId, metadataToSave);
      }
      
      // Reload fields to get updated values
      await loadBillFields();
      
      setEditing(false);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (error: any) {
      console.error('Failed to save bill fields:', error);
      alert('Failed to save bill fields: ' + (error.message || 'Unknown error'));
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    loadBillFields();
    setEditing(false);
    setErrors({});
  };

  const getConfidenceColor = (confidence?: number): 'default' | 'success' | 'warning' | 'error' => {
    if (!confidence) return 'default';
    if (confidence >= 0.8) return 'success';
    if (confidence >= 0.6) return 'warning';
    return 'error';
  };

  const getConfidenceLabel = (confidence?: number): string => {
    if (!confidence) return 'Manual';
    return `${(confidence * 100).toFixed(0)}%`;
  };

  if (documentType !== DocumentType.BILL) {
    return null;
  }

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">Bill Information</Typography>
          {!readOnly && !editing && (
            <Button
              startIcon={<EditIcon />}
              variant="outlined"
              size="small"
              onClick={() => setEditing(true)}
            >
              Edit
            </Button>
          )}
          {editing && (
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                startIcon={<CancelIcon />}
                variant="outlined"
                size="small"
                onClick={handleCancel}
                disabled={saving}
              >
                Cancel
              </Button>
              <Button
                startIcon={<SaveIcon />}
                variant="contained"
                size="small"
                onClick={handleSave}
                disabled={saving}
              >
                Save
              </Button>
            </Box>
          )}
        </Box>

        {saveSuccess && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Bill fields saved successfully!
          </Alert>
        )}

        {saving && <LinearProgress sx={{ mb: 2 }} />}

        <Grid container spacing={2}>
          {/* Vendor Name */}
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={BILL_FIELD_KEYS.vendorName}
              value={fields.vendorName?.value || ''}
              onChange={(e) => handleFieldChange('vendorName', e.target.value)}
              disabled={!editing || readOnly}
              error={!!errors.vendorName}
              helperText={errors.vendorName}
              required
              InputProps={{
                endAdornment: fields.vendorName?.confidence !== undefined && (
                  <Tooltip title={`OCR Confidence: ${getConfidenceLabel(fields.vendorName.confidence)}`}>
                    <Chip
                      size="small"
                      label={getConfidenceLabel(fields.vendorName.confidence)}
                      color={getConfidenceColor(fields.vendorName.confidence)}
                      icon={
                        fields.vendorName.confidence && fields.vendorName.confidence < 0.8 ? (
                          <WarningIcon fontSize="small" />
                        ) : fields.vendorName.confidence ? (
                          <CheckCircleIcon fontSize="small" />
                        ) : (
                          <InfoIcon fontSize="small" />
                        )
                      }
                    />
                  </Tooltip>
                ),
              }}
            />
          </Grid>

          {/* Invoice Number */}
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={BILL_FIELD_KEYS.invoiceNumber}
              value={fields.invoiceNumber?.value || ''}
              onChange={(e) => handleFieldChange('invoiceNumber', e.target.value)}
              disabled={!editing || readOnly}
              error={!!errors.invoiceNumber}
              helperText={errors.invoiceNumber}
              required
              InputProps={{
                endAdornment: fields.invoiceNumber?.confidence !== undefined && (
                  <Tooltip title={`OCR Confidence: ${getConfidenceLabel(fields.invoiceNumber.confidence)}`}>
                    <Chip
                      size="small"
                      label={getConfidenceLabel(fields.invoiceNumber.confidence)}
                      color={getConfidenceColor(fields.invoiceNumber.confidence)}
                      icon={
                        fields.invoiceNumber.confidence && fields.invoiceNumber.confidence < 0.8 ? (
                          <WarningIcon fontSize="small" />
                        ) : fields.invoiceNumber.confidence ? (
                          <CheckCircleIcon fontSize="small" />
                        ) : (
                          <InfoIcon fontSize="small" />
                        )
                      }
                    />
                  </Tooltip>
                ),
              }}
            />
          </Grid>

          {/* Invoice Date */}
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              type="date"
              label={BILL_FIELD_KEYS.invoiceDate}
              value={fields.invoiceDate?.value || ''}
              onChange={(e) => handleFieldChange('invoiceDate', e.target.value)}
              disabled={!editing || readOnly}
              error={!!errors.invoiceDate}
              helperText={errors.invoiceDate}
              required
              InputLabelProps={{ shrink: true }}
              InputProps={{
                endAdornment: fields.invoiceDate?.confidence !== undefined && (
                  <Tooltip title={`OCR Confidence: ${getConfidenceLabel(fields.invoiceDate.confidence)}`}>
                    <Chip
                      size="small"
                      label={getConfidenceLabel(fields.invoiceDate.confidence)}
                      color={getConfidenceColor(fields.invoiceDate.confidence)}
                      icon={
                        fields.invoiceDate.confidence && fields.invoiceDate.confidence < 0.8 ? (
                          <WarningIcon fontSize="small" />
                        ) : fields.invoiceDate.confidence ? (
                          <CheckCircleIcon fontSize="small" />
                        ) : (
                          <InfoIcon fontSize="small" />
                        )
                      }
                    />
                  </Tooltip>
                ),
              }}
            />
          </Grid>

          {/* Fiscal Year */}
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              type="number"
              label={BILL_FIELD_KEYS.fiscalYear}
              value={fields.fiscalYear?.value || ''}
              onChange={(e) => handleFieldChange('fiscalYear', e.target.value)}
              disabled={!editing || readOnly}
              error={!!errors.fiscalYear}
              helperText={errors.fiscalYear}
              required
              InputProps={{
                endAdornment: fields.fiscalYear?.confidence !== undefined && (
                  <Tooltip title={`OCR Confidence: ${getConfidenceLabel(fields.fiscalYear.confidence)}`}>
                    <Chip
                      size="small"
                      label={getConfidenceLabel(fields.fiscalYear.confidence)}
                      color={getConfidenceColor(fields.fiscalYear.confidence)}
                    />
                  </Tooltip>
                ),
              }}
            />
          </Grid>

          <Divider sx={{ width: '100%', my: 1 }} />

          {/* Total Amount */}
          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              type="number"
              label={BILL_FIELD_KEYS.totalAmount}
              value={fields.totalAmount?.value || ''}
              onChange={(e) => handleFieldChange('totalAmount', e.target.value)}
              disabled={!editing || readOnly}
              error={!!errors.totalAmount}
              helperText={errors.totalAmount}
              required
              InputProps={{
                startAdornment: <Typography sx={{ mr: 1 }}>৳</Typography>,
                endAdornment: fields.totalAmount?.confidence !== undefined && (
                  <Tooltip title={`OCR Confidence: ${getConfidenceLabel(fields.totalAmount.confidence)}`}>
                    <Chip
                      size="small"
                      label={getConfidenceLabel(fields.totalAmount.confidence)}
                      color={getConfidenceColor(fields.totalAmount.confidence)}
                      sx={{ ml: 1 }}
                    />
                  </Tooltip>
                ),
              }}
            />
          </Grid>

          {/* Tax Amount */}
          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              type="number"
              label={BILL_FIELD_KEYS.taxAmount}
              value={fields.taxAmount?.value || ''}
              onChange={(e) => handleFieldChange('taxAmount', e.target.value)}
              disabled={!editing || readOnly}
              error={!!errors.taxAmount}
              helperText={errors.taxAmount}
              InputProps={{
                startAdornment: <Typography sx={{ mr: 1 }}>৳</Typography>,
                endAdornment: fields.taxAmount?.confidence !== undefined && (
                  <Tooltip title={`OCR Confidence: ${getConfidenceLabel(fields.taxAmount.confidence)}`}>
                    <Chip
                      size="small"
                      label={getConfidenceLabel(fields.taxAmount.confidence)}
                      color={getConfidenceColor(fields.taxAmount.confidence)}
                      sx={{ ml: 1 }}
                    />
                  </Tooltip>
                ),
              }}
            />
          </Grid>

          {/* Net Amount */}
          <Grid item xs={12} md={4}>
            <TextField
              fullWidth
              type="number"
              label={BILL_FIELD_KEYS.netAmount}
              value={fields.netAmount?.value || ''}
              disabled
              InputProps={{
                startAdornment: <Typography sx={{ mr: 1 }}>৳</Typography>,
              }}
              helperText="Calculated automatically (Total - Tax)"
            />
          </Grid>

          {/* Description */}
          <Grid item xs={12}>
            <TextField
              fullWidth
              multiline
              rows={3}
              label={BILL_FIELD_KEYS.description}
              value={fields.description?.value || ''}
              onChange={(e) => handleFieldChange('description', e.target.value)}
              disabled={!editing || readOnly}
              error={!!errors.description}
              helperText={errors.description}
            />
          </Grid>
        </Grid>

        {/* OCR Status Info */}
        {Object.values(fields).some(f => f.confidence !== undefined) && (
          <Alert severity="info" sx={{ mt: 2 }}>
            <Typography variant="body2">
              <strong>OCR Status:</strong> Some fields were automatically extracted from the invoice.
              Fields with confidence scores below 80% may need verification.
            </Typography>
          </Alert>
        )}
      </CardContent>
    </Card>
  );
};

export default BillFieldsEditor;

