import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  CircularProgress,
  Box,
  Typography
} from '@mui/material';

interface LoadingDialogProps {
  open: boolean;
  title?: string;
  message?: string;
}

const LoadingDialog: React.FC<LoadingDialogProps> = ({ 
  open, 
  title = "Loading...", 
  message = "Please wait while we process your request." 
}) => {
  return (
    <Dialog open={open} maxWidth="sm" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Box display="flex" flexDirection="column" alignItems="center" py={2}>
          <CircularProgress size={40} />
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            {message}
          </Typography>
        </Box>
      </DialogContent>
    </Dialog>
  );
};

export default LoadingDialog;
