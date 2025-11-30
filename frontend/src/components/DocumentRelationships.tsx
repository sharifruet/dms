import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Card,
  CardContent,
  Button,
  List,
  ListItem,
  ListItemText,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Alert,
  CircularProgress,
  Divider,
  Link,
} from '@mui/material';
import {
  Link as LinkIcon,
  Delete as DeleteIcon,
  Add as AddIcon,
  Description as DocumentIcon,
  ArrowForward as ArrowForwardIcon,
} from '@mui/icons-material';
import {
  documentRelationshipService,
  DocumentRelationship,
  DocumentRelationshipType,
  CreateRelationshipRequest,
} from '../services/documentRelationshipService';
import { Document, documentService } from '../services/documentService';

interface DocumentRelationshipsProps {
  document: Document;
  onRelationshipChange?: () => void;
}

const DocumentRelationships: React.FC<DocumentRelationshipsProps> = ({
  document,
  onRelationshipChange,
}) => {
  const [relationships, setRelationships] = useState<DocumentRelationship[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openDialog, setOpenDialog] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [formData, setFormData] = useState<CreateRelationshipRequest>({
    targetDocumentId: 0,
    relationshipType: DocumentRelationshipType.OTHER,
    description: '',
  });
  const [availableDocuments, setAvailableDocuments] = useState<Document[]>([]);
  const [loadingDocuments, setLoadingDocuments] = useState(false);

  useEffect(() => {
    loadRelationships();
  }, [document.id]);

  const loadRelationships = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await documentRelationshipService.getRelationships(document.id!);
      setRelationships(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load relationships');
    } finally {
      setLoading(false);
    }
  };

  const loadAvailableDocuments = async () => {
    setLoadingDocuments(true);
    try {
      // Prefer documents from the same folder (same workflow) as the current document
      const folderId = document.folder?.id;
      const response = await documentService.getDocuments(
        folderId ? { folderId, size: 1000 } : { size: 1000 }
      );
      const filtered = (response.content || []).filter(
        (doc: Document) => doc.id !== document.id
      );
      setAvailableDocuments(filtered);
    } catch (err) {
      console.error('Failed to load documents:', err);
    } finally {
      setLoadingDocuments(false);
    }
  };

  const handleOpenDialog = () => {
    loadAvailableDocuments();
    setFormData({
      targetDocumentId: 0,
      relationshipType: DocumentRelationshipType.OTHER,
      description: '',
    });
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setFormData({
      targetDocumentId: 0,
      relationshipType: DocumentRelationshipType.OTHER,
      description: '',
    });
  };

  const handleCreateRelationship = async () => {
    if (!formData.targetDocumentId) {
      setError('Please select a target document');
      return;
    }

    try {
      await documentRelationshipService.createRelationship(document.id!, formData);
      handleCloseDialog();
      loadRelationships();
      if (onRelationshipChange) {
        onRelationshipChange();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create relationship');
    }
  };

  const handleDeleteRelationship = async (relationshipId: number) => {
    if (!window.confirm('Are you sure you want to delete this relationship?')) {
      return;
    }

    setDeletingId(relationshipId);
    try {
      await documentRelationshipService.deleteRelationship(document.id!, relationshipId);
      loadRelationships();
      if (onRelationshipChange) {
        onRelationshipChange();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete relationship');
    } finally {
      setDeletingId(null);
    }
  };

  const getRelationshipTypeLabel = (type: DocumentRelationshipType): string => {
    const labels: Record<DocumentRelationshipType, string> = {
      [DocumentRelationshipType.CONTRACT_TO_LC]: 'Contract → Letter of Credit',
      [DocumentRelationshipType.LC_TO_BG]: 'Letter of Credit → Bank Guarantee',
      [DocumentRelationshipType.BG_TO_PO]: 'Bank Guarantee → Purchase Order',
      [DocumentRelationshipType.PO_TO_CORRESPONDENCE]: 'Purchase Order → Correspondence',
      [DocumentRelationshipType.CONTRACT_TO_BG]: 'Contract → Bank Guarantee',
      [DocumentRelationshipType.CONTRACT_TO_PO]: 'Contract → Purchase Order',
      [DocumentRelationshipType.LC_TO_PO]: 'Letter of Credit → Purchase Order',
      [DocumentRelationshipType.BG_TO_CORRESPONDENCE]: 'Bank Guarantee → Correspondence',
      [DocumentRelationshipType.OTHER]: 'Other Relationship',
    };
    return labels[type] || type;
  };

  const getDocumentDisplayName = (doc: { fileName: string; originalName?: string }) => {
    return doc.originalName || doc.fileName;
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <LinkIcon /> Document Relationships ({relationships.length})
        </Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={handleOpenDialog}
          size="small"
        >
          Link Document
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {relationships.length === 0 ? (
        <Card>
          <CardContent>
            <Typography variant="body2" color="text.secondary" align="center">
              No relationships found. Click "Link Document" to create a relationship.
            </Typography>
          </CardContent>
        </Card>
      ) : (
        <List>
          {relationships.map((rel) => {
            const isSource = rel.sourceDocument.id === document.id;
            const relatedDoc = isSource ? rel.targetDocument : rel.sourceDocument;
            const isDeleting = deletingId === rel.id;

            return (
              <React.Fragment key={rel.id}>
                <ListItem
                  sx={{
                    border: '1px solid #e0e0e0',
                    borderRadius: 1,
                    mb: 1,
                    bgcolor: 'background.paper',
                  }}
                >
                  <DocumentIcon sx={{ mr: 2, color: 'primary.main' }} />
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, flexWrap: 'wrap' }}>
                        <Typography variant="body1" component="span" fontWeight="medium">
                          {getDocumentDisplayName(relatedDoc)}
                        </Typography>
                        <Chip
                          label={getRelationshipTypeLabel(rel.relationshipType)}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                        {rel.description && (
                          <Typography variant="caption" color="text.secondary">
                            {rel.description}
                          </Typography>
                        )}
                      </Box>
                    }
                    secondary={
                      <Box sx={{ mt: 0.5 }}>
                        <Typography variant="caption" color="text.secondary">
                          {relatedDoc.documentType} • Created {new Date(rel.createdAt).toLocaleDateString()}
                        </Typography>
                      </Box>
                    }
                  />
                  <IconButton
                    edge="end"
                    onClick={() => handleDeleteRelationship(rel.id)}
                    disabled={isDeleting}
                    color="error"
                    size="small"
                  >
                    {isDeleting ? <CircularProgress size={20} /> : <DeleteIcon />}
                  </IconButton>
                </ListItem>
              </React.Fragment>
            );
          })}
        </List>
      )}

      {/* Create Relationship Dialog */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Link Document</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2 }}>
            <FormControl fullWidth sx={{ mb: 2 }}>
              <InputLabel>Target Document</InputLabel>
              <Select
                value={formData.targetDocumentId}
                onChange={(e) =>
                  setFormData({ ...formData, targetDocumentId: e.target.value as number })
                }
                label="Target Document"
                disabled={loadingDocuments}
              >
                {loadingDocuments ? (
                  <MenuItem disabled>
                    <CircularProgress size={20} sx={{ mr: 1 }} />
                    Loading documents...
                  </MenuItem>
                ) : (
                  availableDocuments.map((doc) => (
                    <MenuItem key={doc.id} value={doc.id}>
                      {doc.originalName || doc.fileName} ({doc.documentType})
                    </MenuItem>
                  ))
                )}
              </Select>
            </FormControl>

            <FormControl fullWidth sx={{ mb: 2 }}>
              <InputLabel>Relationship Type</InputLabel>
              <Select
                value={formData.relationshipType}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    relationshipType: e.target.value as DocumentRelationshipType,
                  })
                }
                label="Relationship Type"
              >
                {Object.values(DocumentRelationshipType).map((type) => (
                  <MenuItem key={type} value={type}>
                    {getRelationshipTypeLabel(type)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              fullWidth
              label="Description (Optional)"
              multiline
              rows={3}
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              sx={{ mb: 2 }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button onClick={handleCreateRelationship} variant="contained" disabled={!formData.targetDocumentId}>
            Create Link
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default DocumentRelationships;

