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
  const [treeRefreshTrigger, setTreeRefreshTrigger] = useState(0);
  
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

  const loadingFolderRef = React.useRef<number | null>(null);
  const currentFolderIdRef = React.useRef<number | null>(null);

  const loadFolder = React.useCallback(async (folderId: number) => {
    // Prevent concurrent loads of the same folder
    if (loadingFolderRef.current === folderId) {
      return;
    }
    
    // Prevent loading if already selected
    if (currentFolderIdRef.current === folderId) {
      return;
    }

    loadingFolderRef.current = folderId;
    try {
      const folder = await folderService.getFolder(folderId);
      setSelectedFolder(folder);
      currentFolderIdRef.current = folderId;
      buildBreadcrumbs(folder);
      // Don't call onFolderSelect here - it should only be called from user interactions
      // to prevent infinite loops
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load folder');
    } finally {
      loadingFolderRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (selectedFolderId && selectedFolderId !== currentFolderIdRef.current) {
      loadFolder(selectedFolderId);
    }
  }, [selectedFolderId, loadFolder]);

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
    // Update ref to prevent reload
    if (folder) {
      currentFolderIdRef.current = folder.id;
      setSelectedFolder(folder);
      buildBreadcrumbs(folder);
    } else {
      currentFolderIdRef.current = null;
      setSelectedFolder(null);
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
        // parentFolderId is already set in createForm when dialog opens
      });
      setSuccess('Folder created successfully');
      setOpenCreateDialog(false);
      setCreateForm({ name: '', description: '', parentFolderId: null, department: '' });
      // Trigger tree refresh
      setTreeRefreshTrigger(prev => prev + 1);
      // Expand parent folder if subfolder was created
      if (createForm.parentFolderId && selectedFolder?.id === createForm.parentFolderId) {
        // Keep parent selected and expanded
      }
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
      setTreeRefreshTrigger(prev => prev + 1);
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
      setTreeRefreshTrigger(prev => prev + 1);
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
      setTreeRefreshTrigger(prev => prev + 1);
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
              onClick={() => {
                setCreateForm({ 
                  name: '', 
                  description: '', 
                  parentFolderId: selectedFolder?.id || null, 
                  department: '' 
                });
                setOpenCreateDialog(true);
              }}
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
              setCreateForm({ 
                name: '', 
                description: '', 
                parentFolderId: parentId || null, 
                department: '' 
              });
              setOpenCreateDialog(true);
            }}
            refreshTrigger={treeRefreshTrigger}
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
      <Dialog 
        open={openCreateDialog} 
        onClose={() => {
          setOpenCreateDialog(false);
          setCreateForm({ name: '', description: '', parentFolderId: null, department: '' });
        }} 
        maxWidth="sm" 
        fullWidth
      >
        <DialogTitle>
          {createForm.parentFolderId 
            ? 'Create Subfolder' 
            : 'Create New Folder'}
        </DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Folder Name"
            value={createForm.name}
            onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
            margin="normal"
            required
            autoFocus
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
          <Button onClick={() => {
            setOpenCreateDialog(false);
            setCreateForm({ name: '', description: '', parentFolderId: null, department: '' });
          }}>Cancel</Button>
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

