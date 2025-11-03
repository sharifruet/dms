import React, { useState, useCallback } from 'react';
import { useDropzone } from 'react-dropzone';
import {
  Box,
  Paper,
  Typography,
  LinearProgress,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Button,
  Chip,
  Alert,
} from '@mui/material';
import {
  CloudUpload as UploadIcon,
  Description as FileIcon,
  Delete as DeleteIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import api from '../services/api';

interface FileWithProgress {
  file: File;
  progress: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
}

interface BulkFileUploadProps {
  onUploadComplete?: () => void;
  maxFiles?: number;
  maxFileSize?: number; // in MB
  acceptedFileTypes?: string[];
}

const BulkFileUpload: React.FC<BulkFileUploadProps> = ({
  onUploadComplete,
  maxFiles = 10,
  maxFileSize = 100,
  acceptedFileTypes = [],
}) => {
  const [files, setFiles] = useState<FileWithProgress[]>([]);
  const [uploading, setUploading] = useState(false);
  const [overallProgress, setOverallProgress] = useState(0);

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const newFiles: FileWithProgress[] = acceptedFiles.map((file) => ({
      file,
      progress: 0,
      status: 'pending' as const,
    }));
    setFiles((prev) => [...prev, ...newFiles].slice(0, maxFiles));
  }, [maxFiles]);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    maxFiles: maxFiles - files.length,
    maxSize: maxFileSize * 1024 * 1024,
    accept: acceptedFileTypes.length > 0 ? acceptedFileTypes.reduce((acc, type) => {
      acc[type] = [];
      return acc;
    }, {} as Record<string, string[]>) : undefined,
  });

  const removeFile = (index: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const uploadFiles = async () => {
    if (files.length === 0) return;

    setUploading(true);
    let completedFiles = 0;

    for (let i = 0; i < files.length; i++) {
      const fileItem = files[i];
      
      if (fileItem.status === 'success') {
        completedFiles++;
        continue;
      }

      try {
        // Update status to uploading
        setFiles((prev) => {
          const updated = [...prev];
          updated[i].status = 'uploading';
          return updated;
        });

        const formData = new FormData();
        formData.append('file', fileItem.file);
        formData.append('documentType', 'OTHER');
        formData.append('description', '');

        await api.post('/documents/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
          onUploadProgress: (progressEvent) => {
            const progress = progressEvent.total
              ? Math.round((progressEvent.loaded * 100) / progressEvent.total)
              : 0;
            
            setFiles((prev) => {
              const updated = [...prev];
              updated[i].progress = progress;
              return updated;
            });
          },
        });

        // Update status to success
        setFiles((prev) => {
          const updated = [...prev];
          updated[i].status = 'success';
          updated[i].progress = 100;
          return updated;
        });

        completedFiles++;
      } catch (error: any) {
        // Update status to error
        setFiles((prev) => {
          const updated = [...prev];
          updated[i].status = 'error';
          updated[i].error = error.response?.data?.message || 'Upload failed';
          return updated;
        });
      }

      // Update overall progress
      setOverallProgress(Math.round((completedFiles / files.length) * 100));
    }

    setUploading(false);
    if (onUploadComplete) {
      onUploadComplete();
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'success':
        return <SuccessIcon sx={{ color: '#10b981' }} />;
      case 'error':
        return <ErrorIcon sx={{ color: '#ef4444' }} />;
      default:
        return <FileIcon sx={{ color: '#6b7280' }} />;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'success':
        return 'success';
      case 'error':
        return 'error';
      case 'uploading':
        return 'primary';
      default:
        return 'default';
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  };

  const successCount = files.filter(f => f.status === 'success').length;
  const errorCount = files.filter(f => f.status === 'error').length;
  const pendingCount = files.filter(f => f.status === 'pending').length;

  return (
    <Box>
      {/* Drop Zone */}
      <Paper
        {...getRootProps()}
        elevation={0}
        sx={{
          p: 4,
          mb: 3,
          border: '2px dashed',
          borderColor: isDragActive ? '#3b82f6' : '#d1d5db',
          borderRadius: 3,
          backgroundColor: isDragActive ? '#eff6ff' : '#f9fafb',
          cursor: 'pointer',
          transition: 'all 0.2s ease',
          '&:hover': {
            borderColor: '#3b82f6',
            backgroundColor: '#f0f9ff',
          },
        }}
      >
        <input {...getInputProps()} />
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 2,
          }}
        >
          <UploadIcon sx={{ fontSize: '4rem', color: '#6b7280' }} />
          <Typography
            variant="h6"
            sx={{
              fontWeight: 600,
              color: '#1f2937',
              textAlign: 'center',
            }}
          >
            {isDragActive
              ? 'Drop files here...'
              : 'Drag & drop files here, or click to select'}
          </Typography>
          <Typography
            variant="body2"
            sx={{
              color: '#6b7280',
              textAlign: 'center',
            }}
          >
            Maximum {maxFiles} files, up to {maxFileSize}MB each
          </Typography>
        </Box>
      </Paper>

      {/* Summary */}
      {files.length > 0 && (
        <Box sx={{ mb: 3, display: 'flex', gap: 2, alignItems: 'center' }}>
          <Chip
            label={`${files.length} Total`}
            color="default"
            size="small"
          />
          {pendingCount > 0 && (
            <Chip
              label={`${pendingCount} Pending`}
              color="default"
              size="small"
            />
          )}
          {successCount > 0 && (
            <Chip
              label={`${successCount} Uploaded`}
              color="success"
              size="small"
            />
          )}
          {errorCount > 0 && (
            <Chip
              label={`${errorCount} Failed`}
              color="error"
              size="small"
            />
          )}
        </Box>
      )}

      {/* Overall Progress */}
      {uploading && (
        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
            <Typography variant="body2" sx={{ color: '#6b7280', fontWeight: 600 }}>
              Overall Progress
            </Typography>
            <Typography variant="body2" sx={{ color: '#6b7280', fontWeight: 600 }}>
              {overallProgress}%
            </Typography>
          </Box>
          <LinearProgress
            variant="determinate"
            value={overallProgress}
            sx={{
              height: 8,
              borderRadius: 4,
              backgroundColor: '#e5e7eb',
              '& .MuiLinearProgress-bar': {
                borderRadius: 4,
              },
            }}
          />
        </Box>
      )}

      {/* File List */}
      {files.length > 0 && (
        <Paper
          elevation={0}
          sx={{
            border: '1px solid #e5e7eb',
            borderRadius: 3,
            overflow: 'hidden',
            mb: 3,
          }}
        >
          <List sx={{ p: 0 }}>
            {files.map((fileItem, index) => (
              <ListItem
                key={index}
                sx={{
                  borderBottom: index < files.length - 1 ? '1px solid #e5e7eb' : 'none',
                  py: 2,
                }}
              >
                <ListItemIcon>
                  {getStatusIcon(fileItem.status)}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: 500,
                        color: '#1f2937',
                        mb: 0.5,
                      }}
                    >
                      {fileItem.file.name}
                    </Typography>
                  }
                  secondary={
                    <Box>
                      <Typography
                        variant="caption"
                        sx={{ color: '#6b7280', display: 'block', mb: 0.5 }}
                      >
                        {formatFileSize(fileItem.file.size)}
                      </Typography>
                      {fileItem.status === 'uploading' && (
                        <LinearProgress
                          variant="determinate"
                          value={fileItem.progress}
                          sx={{
                            height: 4,
                            borderRadius: 2,
                            backgroundColor: '#e5e7eb',
                          }}
                        />
                      )}
                      {fileItem.status === 'error' && fileItem.error && (
                        <Typography
                          variant="caption"
                          sx={{ color: '#ef4444' }}
                        >
                          {fileItem.error}
                        </Typography>
                      )}
                    </Box>
                  }
                />
                <ListItemSecondaryAction>
                  {fileItem.status !== 'uploading' && (
                    <IconButton
                      edge="end"
                      onClick={() => removeFile(index)}
                      size="small"
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  )}
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        </Paper>
      )}

      {/* Actions */}
      {files.length > 0 && (
        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
          <Button
            variant="outlined"
            onClick={() => setFiles([])}
            disabled={uploading}
          >
            Clear All
          </Button>
          <Button
            variant="contained"
            onClick={uploadFiles}
            disabled={uploading || files.length === 0}
            startIcon={<UploadIcon />}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
            }}
          >
            {uploading ? 'Uploading...' : `Upload ${files.length} File${files.length > 1 ? 's' : ''}`}
          </Button>
        </Box>
      )}
    </Box>
  );
};

export default BulkFileUpload;

