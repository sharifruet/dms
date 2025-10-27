import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Chip,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Download as DownloadIcon,
  Visibility as ViewIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Share as ShareIcon
} from '@mui/icons-material';

interface DocumentCardProps {
  document: {
    id: number;
    fileName: string;
    documentType: string;
    uploadedBy: string;
    uploadedAt: string;
    department: string;
    size: string;
    status: string;
  };
  onView?: (document: any) => void;
  onDownload?: (document: any) => void;
  onEdit?: (document: any) => void;
  onDelete?: (document: any) => void;
  onShare?: (document: any) => void;
}

const DocumentCard: React.FC<DocumentCardProps> = ({
  document,
  onView,
  onDownload,
  onEdit,
  onDelete,
  onShare
}) => {
  const getDocumentTypeColor = (type: string) => {
    switch (type) {
      case 'PDF':
        return 'error';
      case 'DOCX':
      case 'DOC':
        return 'primary';
      case 'TXT':
        return 'default';
      case 'JPG':
      case 'PNG':
        return 'success';
      default:
        return 'default';
    }
  };

  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={1}>
          <Typography variant="h6" component="h3" noWrap sx={{ flexGrow: 1, mr: 1 }}>
            {document.fileName}
          </Typography>
          <Chip
            label={document.documentType}
            color={getDocumentTypeColor(document.documentType) as any}
            size="small"
          />
        </Box>
        
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Department: {document.department}
        </Typography>
        
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Uploaded by: {document.uploadedBy}
        </Typography>
        
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Size: {document.size}
        </Typography>
        
        <Typography variant="body2" color="text.secondary">
          {new Date(document.uploadedAt).toLocaleDateString()}
        </Typography>
      </CardContent>
      
      <Box sx={{ p: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Chip
          label={document.status}
          color="success"
          size="small"
        />
        
        <Box>
          {onView && (
            <Tooltip title="View">
              <IconButton size="small" onClick={() => onView(document)}>
                <ViewIcon />
              </IconButton>
            </Tooltip>
          )}
          {onDownload && (
            <Tooltip title="Download">
              <IconButton size="small" onClick={() => onDownload(document)}>
                <DownloadIcon />
              </IconButton>
            </Tooltip>
          )}
          {onShare && (
            <Tooltip title="Share">
              <IconButton size="small" onClick={() => onShare(document)}>
                <ShareIcon />
              </IconButton>
            </Tooltip>
          )}
          {onEdit && (
            <Tooltip title="Edit">
              <IconButton size="small" onClick={() => onEdit(document)}>
                <EditIcon />
              </IconButton>
            </Tooltip>
          )}
          {onDelete && (
            <Tooltip title="Delete">
              <IconButton size="small" onClick={() => onDelete(document)} color="error">
                <DeleteIcon />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      </Box>
    </Card>
  );
};

export default DocumentCard;
