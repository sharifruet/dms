import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  Folder as FolderIcon,
  Description as DocumentIcon,
  Upload as UploadIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { FolderSummary as FolderSummaryType, folderService } from '../services/folderService';

interface FolderSummaryProps {
  folderId: number | null;
}

const FolderSummary: React.FC<FolderSummaryProps> = ({ folderId }) => {
  const [summary, setSummary] = useState<FolderSummaryType | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (folderId) {
      loadSummary();
    } else {
      setSummary(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [folderId]);

  const loadSummary = async () => {
    if (!folderId) return;
    
    setLoading(true);
    setError(null);
    try {
      const data = await folderService.getFolderSummary(folderId);
      setSummary(data);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load folder summary');
    } finally {
      setLoading(false);
    }
  };

  if (!folderId) {
    return (
      <Card>
        <CardContent>
          <Typography variant="body2" color="text.secondary" align="center">
            Select a folder to view summary
          </Typography>
        </CardContent>
      </Card>
    );
  }

  if (loading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 2 }}>
            <CircularProgress size={24} />
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent>
          <Alert severity="error">{error}</Alert>
        </CardContent>
      </Card>
    );
  }

  if (!summary) {
    return null;
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <FolderIcon /> {summary.folderName}
        </Typography>
        <Grid container spacing={2} sx={{ mt: 1 }}>
          <Grid item xs={4}>
            <Box sx={{ textAlign: 'center' }}>
              <DocumentIcon sx={{ fontSize: 32, color: 'primary.main', mb: 1 }} />
              <Typography variant="h4" color="primary">
                {summary.totalFiles}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Total Files
              </Typography>
            </Box>
          </Grid>
          <Grid item xs={4}>
            <Box sx={{ textAlign: 'center' }}>
              <CheckCircleIcon sx={{ fontSize: 32, color: 'success.main', mb: 1 }} />
              <Typography variant="h4" color="success.main">
                {summary.uploadedFiles}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Uploaded Files
              </Typography>
            </Box>
          </Grid>
          {/* Remaining in folder metric hidden as requested */}
        </Grid>
      </CardContent>
    </Card>
  );
};

export default FolderSummary;

