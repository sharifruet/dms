import React from 'react';
import {
  Paper,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Typography,
  Box,
  Chip
} from '@mui/material';
import { Search as SearchIcon } from '@mui/icons-material';

interface SearchSuggestionsProps {
  suggestions: string[];
  onSuggestionSelect: (suggestion: string) => void;
  searchQuery: string;
}

const SearchSuggestions: React.FC<SearchSuggestionsProps> = ({
  suggestions,
  onSuggestionSelect,
  searchQuery
}) => {
  if (suggestions.length === 0) {
    return null;
  }

  // Highlight matching text in suggestion
  const highlightMatch = (text: string, query: string) => {
    if (!query) return text;
    
    const parts = text.split(new RegExp(`(${query})`, 'gi'));
    return parts.map((part, index) =>
      part.toLowerCase() === query.toLowerCase() ? (
        <Box component="span" key={index} sx={{ fontWeight: 'bold', backgroundColor: 'yellow' }}>
          {part}
        </Box>
      ) : (
        <Box component="span" key={index}>{part}</Box>
      )
    );
  };

  return (
    <Paper
      elevation={3}
      sx={{
        position: 'absolute',
        zIndex: 1000,
        width: '100%',
        maxHeight: 300,
        overflow: 'auto',
        mt: 0.5
      }}
    >
      <Box sx={{ p: 1, borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="caption" color="text.secondary">
          Suggestions ({suggestions.length})
        </Typography>
      </Box>
      <List dense>
        {suggestions.map((suggestion, index) => (
          <ListItem key={index} disablePadding>
            <ListItemButton
              onClick={() => onSuggestionSelect(suggestion)}
              sx={{
                '&:hover': {
                  backgroundColor: 'action.hover'
                }
              }}
            >
              <SearchIcon sx={{ mr: 1, fontSize: 16, color: 'text.secondary' }} />
              <ListItemText
                primary={highlightMatch(suggestion, searchQuery)}
                primaryTypographyProps={{
                  variant: 'body2'
                }}
              />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </Paper>
  );
};

export default SearchSuggestions;

