import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Container,
  TextField,
  Button,
  Typography,
  Paper,
  Grid,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Card,
  CardContent,
  CardActions,
  Pagination,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Alert,
  CircularProgress,
  Autocomplete,
  FormControlLabel,
  Switch,
  Divider,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  Search as SearchIcon,
  FilterList as FilterIcon,
  ExpandMore as ExpandMoreIcon,
  Download as DownloadIcon,
  Visibility as ViewIcon,
  HighlightOff as ClearIcon,
  Refresh as RefreshIcon,
  Analytics as AnalyticsIcon
} from '@mui/icons-material';
import { searchService, SearchFilters, SearchResult, SearchResultItem, SearchStatistics } from '../services/searchService';
import { documentService } from '../services/documentService';
import { DocumentCategory } from '../types/document';

interface SearchPageProps {}

const DEFAULT_CATEGORIES: DocumentCategory[] = [
  { id: -1, name: 'TENDER', displayName: 'Tender', description: 'Tender documents', isActive: true },
  { id: -2, name: 'BILL', displayName: 'Bill', description: 'Bills and invoices', isActive: true },
  { id: -3, name: 'CONTRACT', displayName: 'Contract', description: 'Contract documents', isActive: true },
  { id: -4, name: 'GENERAL', displayName: 'General', description: 'General purpose documents', isActive: true },
];

const SearchPage: React.FC<SearchPageProps> = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<SearchResult | null>(null);
  const [searchStatistics, setSearchStatistics] = useState<SearchStatistics | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [showFilters, setShowFilters] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // Filter states
  const [filters, setFilters] = useState<SearchFilters>({});
  const [documentTypes, setDocumentTypes] = useState<string[]>([]);
  const [departments, setDepartments] = useState<string[]>([]);
  const [uploadedBy, setUploadedBy] = useState<string[]>([]);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [minOcrConfidence, setMinOcrConfidence] = useState<number | undefined>();
  const [isActive, setIsActive] = useState<boolean | undefined>(true);

  // Available options for filters
  const [categories, setCategories] = useState<DocumentCategory[]>(DEFAULT_CATEGORIES);
  const documentTypeOptions = categories.map((category) => category.name);
  const departmentOptions = ['Finance', 'HR', 'IT', 'Operations', 'Legal', 'Procurement'];
  const userOptions = ['admin', 'user1', 'user2', 'manager'];

  useEffect(() => {
    loadSearchStatistics();
    loadCategories();
  }, []);

  useEffect(() => {
    if (searchQuery.length > 2) {
      loadSuggestions();
    } else {
      setSuggestions([]);
      setShowSuggestions(false);
    }
  }, [searchQuery]);

  const loadSearchStatistics = async () => {
    try {
      const stats = await searchService.getSearchStatistics();
      setSearchStatistics(stats);
    } catch (err) {
      console.error('Failed to load search statistics:', err);
    }
  };

  const loadCategories = async () => {
    try {
      const data = await documentService.getDocumentCategories();
      if (!data || data.length === 0) {
        console.warn('[SearchPage] No categories returned from API; using defaults');
        setCategories(DEFAULT_CATEGORIES);
      } else {
        setCategories(data);
      }
    } catch (err) {
      console.error('Failed to load document categories:', err);
      setCategories(DEFAULT_CATEGORIES);
    }
  };

  const loadSuggestions = useCallback(async () => {
    try {
      const suggestions = await searchService.getSuggestions(searchQuery, 10);
      setSuggestions(suggestions);
      setShowSuggestions(true);
    } catch (err) {
      console.error('Failed to load suggestions:', err);
    }
  }, [searchQuery]);

  const performSearch = async (page: number = 0) => {
    setLoading(true);
    setError(null);

    try {
      const currentFilters: SearchFilters = {
        documentTypes: documentTypes.length > 0 ? documentTypes : undefined,
        departments: departments.length > 0 ? departments : undefined,
        uploadedBy: uploadedBy.length > 0 ? uploadedBy : undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        minOcrConfidence,
        isActive
      };

      const results = await searchService.searchDocuments(
        searchQuery || undefined,
        currentFilters,
        page,
        pageSize
      );

      setSearchResults(results);
      setCurrentPage(page);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Search failed');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    performSearch(0);
  };

  const handlePageChange = (event: React.ChangeEvent<unknown>, page: number) => {
    performSearch(page - 1);
  };

  const handleClearFilters = () => {
    setDocumentTypes([]);
    setDepartments([]);
    setUploadedBy([]);
    setStartDate('');
    setEndDate('');
    setMinOcrConfidence(undefined);
    setIsActive(true);
  };

  const handleSuggestionClick = (suggestion: string) => {
    setSearchQuery(suggestion);
    setShowSuggestions(false);
    performSearch(0);
  };

  const getCategoryDisplayName = (name?: string) => {
    if (!name) return 'Unknown';
    const category = categories.find((cat) => cat.name === name);
    return category ? category.displayName || category.name : name;
  };

  const getDocumentTypeColor = (type?: string) => {
    if (!type) return 'default';
    const normalized = type.toUpperCase();
    if (normalized.includes('BILL')) return 'warning';
    if (normalized.includes('TENDER')) return 'primary';
    if (normalized.includes('CONTRACT')) return 'success';
    return 'default';
  };

  const handleReindex = async () => {
    try {
      await searchService.reindexAllDocuments();
      alert('Reindex completed successfully');
      loadSearchStatistics();
    } catch (err: any) {
      alert('Reindex failed: ' + (err.response?.data?.error || err.message));
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const getConfidenceColor = (confidence: number) => {
    if (confidence >= 0.8) return 'success';
    if (confidence >= 0.6) return 'warning';
    return 'error';
  };

  const renderSearchResult = (item: SearchResultItem) => (
    <Card key={item.documentId} sx={{ mb: 2 }}>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="start" mb={1}>
          <Typography variant="h6" component="div">
            {item.originalName}
          </Typography>
          <Box display="flex" gap={1}>
            <Chip
              label={getCategoryDisplayName(item.documentType)}
              size="small"
              color={getDocumentTypeColor(item.documentType) as any}
              variant="outlined"
            />
            <Chip 
              label={`${(item.score * 100).toFixed(1)}%`} 
              size="small" 
              color="secondary"
            />
          </Box>
        </Box>

        <Typography variant="body2" color="text.secondary" gutterBottom>
          Department: {item.department} | Uploaded by: {item.uploadedBy} | 
          Created: {formatDate(item.createdAt)}
        </Typography>

        {item.description && (
          <Typography variant="body2" sx={{ mt: 1, mb: 1 }}>
            {item.description}
          </Typography>
        )}

        {item.highlights && Object.keys(item.highlights).length > 0 && (
          <Box sx={{ mt: 1 }}>
            <Typography variant="subtitle2" gutterBottom>
              Highlights:
            </Typography>
            {Object.entries(item.highlights).map(([field, highlights]) => (
              <Box key={field} sx={{ mb: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  {field}:
                </Typography>
                {highlights.map((highlight, index) => (
                  <Typography 
                    key={index} 
                    variant="body2" 
                    component="span"
                    dangerouslySetInnerHTML={{ __html: highlight }}
                    sx={{ ml: 1 }}
                  />
                ))}
              </Box>
            ))}
          </Box>
        )}

        <Box display="flex" gap={1} mt={2}>
          <Chip 
            label={`OCR: ${(item.ocrConfidence * 100).toFixed(1)}%`}
            size="small"
            color={getConfidenceColor(item.ocrConfidence)}
            variant="outlined"
          />
          <Chip 
            label={`Classification: ${(item.classificationConfidence * 100).toFixed(1)}%`}
            size="small"
            color={getConfidenceColor(item.classificationConfidence)}
            variant="outlined"
          />
        </Box>
      </CardContent>
      
      <CardActions>
        <Button size="small" startIcon={<ViewIcon />}>
          View
        </Button>
        <Button size="small" startIcon={<DownloadIcon />}>
          Download
        </Button>
      </CardActions>
    </Card>
  );

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>
        Document Search
      </Typography>

      {/* Search Statistics */}
      {searchStatistics && (
        <Paper sx={{ p: 2, mb: 3 }}>
          <Box display="flex" alignItems="center" gap={2}>
            <AnalyticsIcon color="primary" />
            <Typography variant="h6">Search Statistics</Typography>
            <Box display="flex" gap={3}>
              <Typography variant="body2">
                Total Documents: <strong>{searchStatistics.totalDocuments}</strong>
              </Typography>
              <Typography variant="body2">
                Active Documents: <strong>{searchStatistics.activeDocuments}</strong>
              </Typography>
            </Box>
            <Box flexGrow={1} />
            <Tooltip title="Reindex all documents">
              <IconButton onClick={handleReindex} color="primary">
                <RefreshIcon />
              </IconButton>
            </Tooltip>
          </Box>
        </Paper>
      )}

      {/* Search Form */}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Box display="flex" gap={2} alignItems="center" mb={2}>
          <TextField
            fullWidth
            label="Search documents"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
            InputProps={{
              startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />
            }}
          />
          <Button
            variant="contained"
            onClick={handleSearch}
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} /> : <SearchIcon />}
          >
            Search
          </Button>
          <Button
            variant="outlined"
            onClick={() => setShowFilters(!showFilters)}
            startIcon={<FilterIcon />}
          >
            Filters
          </Button>
        </Box>

        {/* Suggestions Dropdown */}
        {showSuggestions && suggestions.length > 0 && (
          <Paper sx={{ position: 'absolute', zIndex: 1000, width: '100%', maxHeight: 200, overflow: 'auto' }}>
            {suggestions.map((suggestion, index) => (
              <Box
                key={index}
                sx={{ p: 1, cursor: 'pointer', '&:hover': { bgcolor: 'action.hover' } }}
                onClick={() => handleSuggestionClick(suggestion)}
              >
                <Typography variant="body2">{suggestion}</Typography>
              </Box>
            ))}
          </Paper>
        )}

        {/* Advanced Filters */}
        {showFilters && (
          <Accordion expanded={showFilters}>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography>Advanced Filters</Typography>
            </AccordionSummary>
            <AccordionDetails>
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel>Document Types</InputLabel>
                    <Select
                      multiple
                      value={documentTypes}
                      onChange={(e) => setDocumentTypes(e.target.value as string[])}
                      renderValue={(selected) => (
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {(selected as string[]).map((value) => (
                            <Chip key={value} label={getCategoryDisplayName(value)} size="small" />
                          ))}
                        </Box>
                      )}
                    >
                      {documentTypeOptions.map((type) => (
                        <MenuItem key={type} value={type}>
                          {getCategoryDisplayName(type)}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel>Departments</InputLabel>
                    <Select
                      multiple
                      value={departments}
                      onChange={(e) => setDepartments(e.target.value as string[])}
                      renderValue={(selected) => (
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {(selected as string[]).map((value) => (
                            <Chip key={value} label={value} size="small" />
                          ))}
                        </Box>
                      )}
                    >
                      {departmentOptions.map((dept) => (
                        <MenuItem key={dept} value={dept}>
                          {dept}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} md={6}>
                  <FormControl fullWidth>
                    <InputLabel>Uploaded By</InputLabel>
                    <Select
                      multiple
                      value={uploadedBy}
                      onChange={(e) => setUploadedBy(e.target.value as string[])}
                      renderValue={(selected) => (
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {(selected as string[]).map((value) => (
                            <Chip key={value} label={value} size="small" />
                          ))}
                        </Box>
                      )}
                    >
                      {userOptions.map((user) => (
                        <MenuItem key={user} value={user}>
                          {user}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Grid>

                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Min OCR Confidence"
                    type="number"
                    value={minOcrConfidence || ''}
                    onChange={(e) => setMinOcrConfidence(e.target.value ? parseFloat(e.target.value) : undefined)}
                    inputProps={{ min: 0, max: 1, step: 0.1 }}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="Start Date"
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <TextField
                    fullWidth
                    label="End Date"
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    InputLabelProps={{ shrink: true }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <FormControlLabel
                    control={
                      <Switch
                        checked={isActive ?? true}
                        onChange={(e) => setIsActive(e.target.checked)}
                      />
                    }
                    label="Active Documents Only"
                  />
                </Grid>

                <Grid item xs={12}>
                  <Box display="flex" gap={2}>
                    <Button
                      variant="outlined"
                      onClick={handleClearFilters}
                      startIcon={<ClearIcon />}
                    >
                      Clear Filters
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            </AccordionDetails>
          </Accordion>
        )}
      </Paper>

      {/* Error Display */}
      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Search Results */}
      {searchResults && (
        <Paper sx={{ p: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">
              Search Results ({searchResults.totalHits} documents found)
            </Typography>
            <Box display="flex" gap={2} alignItems="center">
              <Typography variant="body2">
                Page {searchResults.pageNumber + 1} of {searchResults.totalPages}
              </Typography>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Page Size</InputLabel>
                <Select
                  value={pageSize}
                  onChange={(e) => setPageSize(e.target.value as number)}
                >
                  <MenuItem value={10}>10</MenuItem>
                  <MenuItem value={20}>20</MenuItem>
                  <MenuItem value={50}>50</MenuItem>
                  <MenuItem value={100}>100</MenuItem>
                </Select>
              </FormControl>
            </Box>
          </Box>

          {searchResults.items.length === 0 ? (
            <Typography variant="body1" color="text.secondary" textAlign="center" py={4}>
              No documents found matching your search criteria.
            </Typography>
          ) : (
            <>
              {searchResults.items.map(renderSearchResult)}
              
              <Box display="flex" justifyContent="center" mt={3}>
                <Pagination
                  count={searchResults.totalPages}
                  page={searchResults.pageNumber + 1}
                  onChange={handlePageChange}
                  color="primary"
                  size="large"
                />
              </Box>
            </>
          )}
        </Paper>
      )}
    </Container>
  );
};

export default SearchPage;
