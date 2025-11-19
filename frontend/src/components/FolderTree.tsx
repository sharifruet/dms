import React, { useState, useEffect } from 'react';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Collapse,
  Typography,
  IconButton,
  CircularProgress,
} from '@mui/material';
import {
  Folder as FolderIcon,
  FolderOpen as FolderOpenIcon,
  ExpandMore as ExpandMoreIcon,
  ChevronRight as ChevronRightIcon,
  Add as AddIcon,
} from '@mui/icons-material';
import { Folder, folderService } from '../services/folderService';

interface FolderTreeProps {
  onFolderSelect?: (folder: Folder | null) => void;
  selectedFolderId?: number | null;
  showCreateButton?: boolean;
  onCreateFolder?: (parentFolderId?: number) => void;
}

const FolderTree: React.FC<FolderTreeProps> = ({
  onFolderSelect,
  selectedFolderId,
  showCreateButton = false,
  onCreateFolder,
}) => {
  const [folders, setFolders] = useState<Folder[]>([]);
  const [expandedFolders, setExpandedFolders] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadFolders();
  }, []);

  const loadFolders = async () => {
    setLoading(true);
    try {
      const tree = await folderService.getFolderTree();
      setFolders(tree);
      // Expand root folders by default
      const rootIds = tree.map(f => f.id);
      setExpandedFolders(new Set(rootIds));
    } catch (error) {
      console.error('Failed to load folders:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleToggle = (folderId: number) => {
    const newExpanded = new Set(expandedFolders);
    if (newExpanded.has(folderId)) {
      newExpanded.delete(folderId);
    } else {
      newExpanded.add(folderId);
    }
    setExpandedFolders(newExpanded);
  };

  const handleFolderClick = (folder: Folder) => {
    if (onFolderSelect) {
      onFolderSelect(folder);
    }
  };

  const renderFolder = (folder: Folder, level: number = 0) => {
    const isExpanded = expandedFolders.has(folder.id);
    const isSelected = selectedFolderId === folder.id;
    const hasSubFolders = folder.subFolders && folder.subFolders.length > 0;

    return (
      <React.Fragment key={folder.id}>
        <ListItem
          disablePadding
          sx={{
            pl: level * 2,
            bgcolor: isSelected ? 'action.selected' : 'transparent',
            '&:hover': {
              bgcolor: 'action.hover',
            },
          }}
        >
          <ListItemButton
            onClick={() => {
              if (hasSubFolders) {
                handleToggle(folder.id);
              }
              handleFolderClick(folder);
            }}
            sx={{ py: 0.5 }}
          >
            <ListItemIcon sx={{ minWidth: 36 }}>
              {hasSubFolders ? (
                isExpanded ? (
                  <ExpandMoreIcon fontSize="small" />
                ) : (
                  <ChevronRightIcon fontSize="small" />
                )
              ) : (
                <Box sx={{ width: 24 }} />
              )}
            </ListItemIcon>
            <ListItemIcon sx={{ minWidth: 36 }}>
              {isExpanded ? (
                <FolderOpenIcon color="primary" />
              ) : (
                <FolderIcon color="primary" />
              )}
            </ListItemIcon>
            <ListItemText
              primary={folder.name}
              secondary={folder.description}
              primaryTypographyProps={{
                variant: 'body2',
                fontWeight: isSelected ? 600 : 400,
              }}
            />
            {showCreateButton && onCreateFolder && (
              <IconButton
                size="small"
                onClick={(e) => {
                  e.stopPropagation();
                  onCreateFolder(folder.id);
                }}
                sx={{ ml: 1 }}
              >
                <AddIcon fontSize="small" />
              </IconButton>
            )}
          </ListItemButton>
        </ListItem>
        {hasSubFolders && (
          <Collapse in={isExpanded} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>
              {folder.subFolders?.map((subFolder) => renderFolder(subFolder, level + 1))}
            </List>
          </Collapse>
        )}
      </React.Fragment>
    );
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ p: 2, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6" sx={{ fontSize: '1rem', fontWeight: 600 }}>
          Folders
        </Typography>
      </Box>
      <List dense>
        {folders.length === 0 ? (
          <ListItem>
            <ListItemText
              primary="No folders"
              secondary="Create a folder to organize your documents"
            />
          </ListItem>
        ) : (
          folders.map((folder) => renderFolder(folder))
        )}
      </List>
    </Box>
  );
};

export default FolderTree;

