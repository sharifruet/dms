import React, { useState, useEffect } from 'react';
import {
  Box,
  Typography,
  Button,
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
  Breadcrumbs,
  Link,
  IconButton,
  Menu,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import {
  CreateNewFolder as CreateFolderIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  DriveFileMove as MoveIcon,
  Folder as FolderIcon,
  NavigateNext as NavigateNextIcon,
} from '@mui/icons-material';
import { Folder, folderService, CreateFolderRequest, UpdateFolderRequest } from '../services/folderService';
import FolderTree from './FolderTree';
import FolderSummary from './FolderSummary';

interface FolderExplorerProps {
  onFolderSelect?: (folder: Folder | null) => void;
  selectedFolderId?: number | null;
}

const FolderExplorer: React.FC<FolderExplorerProps> = ({
  onFolderSelect,
  selectedFolderId,
}) => {
  const [selectedFolder, setSelectedFolder] = useState<Folder | null>(null);
  const [breadcrumbs, setBreadcrumbs] = useState<Folder[]>([]);
  const [openCreateDialog, setOpenCreateDialog] = useState(false);
  const [openEditDialog, setOpenEditDialog] = useState(false);
  const [openMoveDialog, setOpenMoveDialog] = useState(false);
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const [menuFolder, setMenuFolder] = useState<Folder | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  const [createForm, setCreateForm] = useState<CreateFolderRequest>({
    name: '',
    description: '',
    parentFolderId: null,
    department: '',
  });
  
  const [editForm, setEditForm] = useState<UpdateFolderRequest>({
    name: '',
    description: '',
    department: '',
  });

  useEffect(() => {
    if (selectedFolderId) {
      loadFolder(selectedFolderId);
    }
  }, [selectedFolderId]);

  const loadFolder = async (folderId: number) => {
    try {
      const folder = await folderService.getFolder(folderId);
      setSelectedFolder(folder);
      buildBreadcrumbs(folder);
      if (onFolderSelect) {
        onFolderSelect(folder);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load folder');
    }
  };

  const buildBreadcrumbs = (folder: Folder) => {
    const crumbs: Folder[] = [];
    let current: Folder | null = folder;
    
    while (current) {
      crumbs.unshift(current);
      current = current.parentFolder || null;
    }
    
    setBreadcrumbs(crumbs);
  };

  const handleFolderSelect = (folder: Folder | null) => {
    setSelectedFolder(folder);
    if (folder) {
      buildBreadcrumbs(folder);
    } else {
      setBreadcrumbs([]);
    }
    if (onFolderSelect) {
      onFolderSelect(folder);
    }
  };

  const handleCreateFolder = async () => {
    if (!createForm.name.trim()) {
      setError('Folder name is required');
      return;
    }

    try {
      await folderService.createFolder({
        ...createForm,
        parentFolderId: selectedFolder?.id || null,
      });
      setSuccess('Folder created successfully');
      setOpenCreateDialog(false);
      setCreateForm({ name: '', description: '', parentFolderId: null, department: '' });
      // Reload folder tree would be handled by parent component
      window.location.reload(); // Simple refresh for now
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create folder');
    }
  };

  const handleEditFolder = async () => {
    if (!menuFolder || !editForm.name.trim()) {
      return;
    }

    try {
      await folderService.updateFolder(menuFolder.id, editForm);
      setSuccess('Folder updated successfully');
      setOpenEditDialog(false);
      setMenuAnchor(null);
      if (menuFolder.id === selectedFolder?.id) {
        loadFolder(menuFolder.id);
      }
      window.location.reload();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update folder');
    }
  };

  const handleDeleteFolder = async () => {
    if (!menuFolder) return;
    
    if (!window.confirm(`Are you sure you want to delete folder "${menuFolder.name}"?`)) {
      return;
    }

    try {
      await folderService.deleteFolder(menuFolder.id);
      setSuccess('Folder deleted successfully');
      setMenuAnchor(null);
      if (menuFolder.id === selectedFolder?.id) {
        setSelectedFolder(null);
        setBreadcrumbs([]);
        if (onFolderSelect) {
          onFolderSelect(null);
        }
      }
      window.location.reload();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete folder');
    }
  };

  const handleMoveFolder = async (newParentId: number | null) => {
    if (!menuFolder) return;

    try {
      await folderService.moveFolder(menuFolder.id, { newParentFolderId: newParentId });
      setSuccess('Folder moved successfully');
      setOpenMoveDialog(false);
      setMenuAnchor(null);
      window.location.reload();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to move folder');
    }
  };

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>, folder: Folder) => {
    setMenuAnchor(event.currentTarget);
    setMenuFolder(folder);
    setEditForm({
      name: folder.name,
      description: folder.description || '',
      department: folder.department || '',
    });
  };

  const handleMenuClose = () => {
    setMenuAnchor(null);
    setMenuFolder(null);
  };

  return (
    <Box>
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      <Box sx={{ display: 'flex', gap: 2 }}>
        {/* Folder Tree Sidebar */}
        <Box sx={{ width: 300, borderRight: 1, borderColor: 'divider', minHeight: 600 }}>
          <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Typography variant="h6" sx={{ fontSize: '1rem' }}>
              Folders
            </Typography>
            <Button
              size="small"
              startIcon={<CreateFolderIcon />}
              onClick={() => setOpenCreateDialog(true)}
              variant="outlined"
            >
              New
            </Button>
          </Box>
          <FolderTree
            onFolderSelect={handleFolderSelect}
            selectedFolderId={selectedFolder?.id}
            showCreateButton={true}
            onCreateFolder={(parentId) => {
              setCreateForm({ ...createForm, parentFolderId: parentId || null });
              setOpenCreateDialog(true);
            }}
          />
        </Box>

        {/* Main Content Area */}
        <Box sx={{ flex: 1 }}>
          {/* Breadcrumbs */}
          {breadcrumbs.length > 0 && (
            <Box sx={{ mb: 2 }}>
              <Breadcrumbs separator={<NavigateNextIcon fontSize="small" />}>
                <Link
                  component="button"
                  variant="body1"
                  onClick={() => handleFolderSelect(null)}
                  sx={{ cursor: 'pointer' }}
                >
                  Root
                </Link>
                {breadcrumbs.map((folder, index) => (
                  <Link
                    key={folder.id}
                    component="button"
                    variant="body1"
                    onClick={() => handleFolderSelect(folder)}
                    sx={{
                      cursor: 'pointer',
                      fontWeight: index === breadcrumbs.length - 1 ? 600 : 400,
                    }}
                  >
                    {folder.name}
                  </Link>
                ))}
              </Breadcrumbs>
            </Box>
          )}

          {/* Selected Folder Info */}
          {selectedFolder && (
            <Box sx={{ mb: 2 }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
                <Typography variant="h5" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <FolderIcon /> {selectedFolder.name}
                </Typography>
                <IconButton onClick={(e) => handleMenuOpen(e, selectedFolder)}>
                  <EditIcon />
                </IconButton>
              </Box>
              {selectedFolder.description && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  {selectedFolder.description}
                </Typography>
              )}
              <FolderSummary folderId={selectedFolder.id} />
            </Box>
          )}

          {!selectedFolder && (
            <Box sx={{ textAlign: 'center', py: 8 }}>
              <FolderIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                Select a folder to view details
              </Typography>
            </Box>
          )}
        </Box>
      </Box>

      {/* Context Menu */}
      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={handleMenuClose}>
        <MenuItem onClick={() => { setOpenEditDialog(true); handleMenuClose(); }}>
          <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Edit</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => { setOpenMoveDialog(true); handleMenuClose(); }}>
          <ListItemIcon><MoveIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Move</ListItemText>
        </MenuItem>
        {menuFolder && !menuFolder.isSystemFolder && (
          <MenuItem onClick={() => { handleDeleteFolder(); handleMenuClose(); }}>
            <ListItemIcon><DeleteIcon fontSize="small" color="error" /></ListItemIcon>
            <ListItemText>Delete</ListItemText>
          </MenuItem>
        )}
      </Menu>

      {/* Create Folder Dialog */}
      <Dialog open={openCreateDialog} onClose={() => setOpenCreateDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Folder</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Folder Name"
            value={createForm.name}
            onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Description"
            multiline
            rows={3}
            value={createForm.description}
            onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
            margin="normal"
          />
          <TextField
            fullWidth
            label="Department"
            value={createForm.department}
            onChange={(e) => setCreateForm({ ...createForm, department: e.target.value })}
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCreateDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateFolder} variant="contained">Create</Button>
        </DialogActions>
      </Dialog>

      {/* Edit Folder Dialog */}
      <Dialog open={openEditDialog} onClose={() => setOpenEditDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Folder</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Folder Name"
            value={editForm.name}
            onChange={(e) => setEditForm({ ...editForm, name: e.target.value })}
            margin="normal"
            required
          />
          <TextField
            fullWidth
            label="Description"
            multiline
            rows={3}
            value={editForm.description}
            onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
            margin="normal"
          />
          <TextField
            fullWidth
            label="Department"
            value={editForm.department}
            onChange={(e) => setEditForm({ ...editForm, department: e.target.value })}
            margin="normal"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenEditDialog(false)}>Cancel</Button>
          <Button onClick={handleEditFolder} variant="contained">Save</Button>
        </DialogActions>
      </Dialog>

      {/* Move Folder Dialog */}
      <Dialog open={openMoveDialog} onClose={() => setOpenMoveDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Move Folder</DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 2 }}>
            Select new parent folder for "{menuFolder?.name}"
          </Typography>
          <Button onClick={() => handleMoveFolder(null)} variant="outlined" fullWidth sx={{ mb: 1 }}>
            Move to Root
          </Button>
          {/* In a real implementation, you'd show a folder picker here */}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenMoveDialog(false)}>Cancel</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default FolderExplorer;

