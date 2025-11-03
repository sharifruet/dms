import React, { useState } from 'react';
import {
  Box,
  Button,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Checkbox,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Typography,
  CircularProgress,
} from '@mui/material';
import {
  MoreVert as MoreIcon,
  Download as DownloadIcon,
  Delete as DeleteIcon,
  Archive as ArchiveIcon,
  Share as ShareIcon,
  DriveFileMove as MoveIcon,
  Label as LabelIcon,
} from '@mui/icons-material';

interface BatchActionsProps {
  selectedItems: number[];
  onClearSelection: () => void;
  onBatchAction: (action: string, items: number[]) => Promise<void>;
  totalItems?: number;
}

const BatchActions: React.FC<BatchActionsProps> = ({
  selectedItems,
  onClearSelection,
  onBatchAction,
  totalItems,
}) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [confirmDialogOpen, setConfirmDialogOpen] = useState(false);
  const [currentAction, setCurrentAction] = useState<string>('');
  const [processing, setProcessing] = useState(false);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleActionClick = (action: string) => {
    setCurrentAction(action);
    if (action === 'delete' || action === 'archive') {
      setConfirmDialogOpen(true);
    } else {
      executeBatchAction(action);
    }
    handleMenuClose();
  };

  const executeBatchAction = async (action: string) => {
    setProcessing(true);
    try {
      await onBatchAction(action, selectedItems);
      onClearSelection();
    } catch (error) {
      console.error('Batch action failed:', error);
    } finally {
      setProcessing(false);
      setConfirmDialogOpen(false);
    }
  };

  const handleConfirm = () => {
    executeBatchAction(currentAction);
  };

  const handleCancel = () => {
    setConfirmDialogOpen(false);
    setCurrentAction('');
  };

  const getActionLabel = (action: string) => {
    const labels: Record<string, string> = {
      delete: 'Delete',
      archive: 'Archive',
      download: 'Download',
      share: 'Share',
      move: 'Move',
      label: 'Add Label',
    };
    return labels[action] || action;
  };

  if (selectedItems.length === 0) {
    return null;
  }

  return (
    <>
      <Box
        sx={{
          position: 'sticky',
          top: 0,
          zIndex: 100,
          backgroundColor: '#f0f9ff',
          borderBottom: '1px solid #bfdbfe',
          px: 3,
          py: 2,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 2,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Checkbox
            checked={selectedItems.length === totalItems && totalItems > 0}
            indeterminate={selectedItems.length > 0 && selectedItems.length < (totalItems || 0)}
            onChange={onClearSelection}
          />
          <Chip
            label={`${selectedItems.length} selected`}
            color="primary"
            size="small"
            sx={{
              fontWeight: 600,
            }}
          />
        </Box>

        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Button
            size="small"
            startIcon={<DownloadIcon />}
            onClick={() => handleActionClick('download')}
            disabled={processing}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
            }}
          >
            Download
          </Button>
          <Button
            size="small"
            startIcon={<ShareIcon />}
            onClick={() => handleActionClick('share')}
            disabled={processing}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
            }}
          >
            Share
          </Button>
          <Button
            size="small"
            startIcon={<ArchiveIcon />}
            onClick={() => handleActionClick('archive')}
            disabled={processing}
            sx={{
              textTransform: 'none',
              fontWeight: 600,
            }}
          >
            Archive
          </Button>
          <Button
            size="small"
            startIcon={<DeleteIcon />}
            onClick={() => handleActionClick('delete')}
            disabled={processing}
            color="error"
            sx={{
              textTransform: 'none',
              fontWeight: 600,
            }}
          >
            Delete
          </Button>
          <Button
            size="small"
            onClick={handleMenuOpen}
            disabled={processing}
            sx={{
              minWidth: 'auto',
              px: 1,
            }}
          >
            <MoreIcon />
          </Button>
        </Box>
      </Box>

      {/* More Actions Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => handleActionClick('move')}>
          <ListItemIcon>
            <MoveIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Move to Department</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleActionClick('label')}>
          <ListItemIcon>
            <LabelIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Add Label</ListItemText>
        </MenuItem>
      </Menu>

      {/* Confirmation Dialog */}
      <Dialog
        open={confirmDialogOpen}
        onClose={handleCancel}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>
          Confirm {getActionLabel(currentAction)}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body1">
            Are you sure you want to {currentAction} {selectedItems.length} document
            {selectedItems.length > 1 ? 's' : ''}?
          </Typography>
          {currentAction === 'delete' && (
            <Typography
              variant="body2"
              sx={{ mt: 2, color: '#ef4444', fontWeight: 600 }}
            >
              This action cannot be undone.
            </Typography>
          )}
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={handleCancel} disabled={processing}>
            Cancel
          </Button>
          <Button
            onClick={handleConfirm}
            variant="contained"
            color={currentAction === 'delete' ? 'error' : 'primary'}
            disabled={processing}
            startIcon={processing ? <CircularProgress size={16} /> : null}
          >
            {processing ? 'Processing...' : getActionLabel(currentAction)}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default BatchActions;

