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
  Analytics as AnalyticsIcon,
  GetApp as GetAppIcon,
  PictureAsPdf as PdfIcon,
  TableChart as ExcelIcon,
  HelpOutline as HelpIcon
} from '@mui/icons-material';
import { searchService, SearchFilters, SearchResult, SearchResultItem, SearchStatistics } from '../services/searchService';
import { documentService } from '../services/documentService';
import { DocumentCategory } from '../types/document';
import { ALL_DOCUMENT_TYPES, getDocumentTypeLabel, getDocumentTypeColor } from '../constants/documentTypes';
import SearchSuggestions from '../components/SearchSuggestions';

interface SearchPageProps {}

const DEFAULT_CATEGORIES: DocumentCategory[] = ALL_DOCUMENT_TYPES.map((type, idx) => ({
  id: -(idx + 1),
  name: type,
  displayName: getDocumentTypeLabel(type),
  description: getDocumentTypeLabel(type),
  isActive: true,
}));

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
  const [showBooleanHelp, setShowBooleanHelp] = useState(false);
  const [exporting, setExporting] = useState<'excel' | 'pdf' | null>(null);
  const searchInputRef = React.useRef<HTMLInputElement>(null);

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

  const handleExportExcel = async () => {
    setExporting('excel');
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

      const blob = await searchService.exportToExcel(
        searchQuery || undefined,
        currentFilters,
        0,
        1000 // Export up to 1000 results
      );

      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `search_results_${new Date().toISOString().slice(0, 10)}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Export to Excel failed');
    } finally {
      setExporting(null);
    }
  };

  const handleExportPdf = async () => {
    setExporting('pdf');
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

      const blob = await searchService.exportToPdf(
        searchQuery || undefined,
        currentFilters,
        0,
        100 // Export up to 100 results for PDF
      );

      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `search_results_${new Date().toISOString().slice(0, 10)}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Export to PDF failed');
    } finally {
      setExporting(null);
    }
  };

  const getCategoryDisplayName = (name?: string) => {
    if (!name) return 'Unknown';
    const category = categories.find((cat) => cat.name === name);
    return category ? category.displayName || category.name : name;
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

  // Enhanced highlighting function
  const highlightSearchTerms = (text: string, query: string): React.ReactNode => {
    if (!query || !text) return text;
    
    // Extract search terms (remove operators)
    const terms = query
      .split(/\s+(AND|OR|NOT)\s+/gi)
      .filter(term => term && !/^(AND|OR|NOT)$/i.test(term.trim()))
      .map(term => term.trim().replace(/"/g, ''))
      .filter(term => term.length > 0);
    
    if (terms.length === 0) return text;
    
    // Create regex pattern from all terms
    const pattern = new RegExp(`(${terms.map(term => term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('|')})`, 'gi');
    const parts = text.split(pattern);
    
    return parts.map((part, index) => {
      const isMatch = terms.some(term => part.toLowerCase().includes(term.toLowerCase()));
      if (isMatch) {
        return (
          <Box
            key={index}
            component="span"
            sx={{
              backgroundColor: 'yellow',
              fontWeight: 'bold',
              padding: '0 2px',
              borderRadius: '2px'
            }}
          >
            {part}
          </Box>
        );
      }
      return <span key={index}>{part}</span>;
    });
  };

  const renderSearchResult = (item: SearchResultItem) => (
    <Card key={item.documentId} sx={{ mb: 2, '&:hover': { boxShadow: 4 } }}>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="start" mb={1}>
          <Box sx={{ flex: 1 }}>
            <Typography 
              variant="h6" 
              component="div"
              sx={{ 
                mb: 0.5,
                wordBreak: 'break-word'
              }}
            >
              {highlightSearchTerms(item.originalName || item.fileName, searchQuery)}
            </Typography>
            {item.originalName !== item.fileName && (
              <Typography variant="caption" color="text.secondary">
                File: {item.fileName}
              </Typography>
            )}
          </Box>
          <Box display="flex" gap={1} flexWrap="wrap">
            <Chip
              label={getCategoryDisplayName(item.documentType)}
              size="small"
              color={getDocumentTypeColor(item.documentType)}
              variant="outlined"
            />
            <Chip 
              label={`Score: ${(item.score * 100).toFixed(0)}%`} 
              size="small" 
              color={item.score >= 0.8 ? 'success' : item.score >= 0.5 ? 'warning' : 'default'}
              variant="outlined"
            />
          </Box>
        </Box>

        <Typography variant="body2" color="text.secondary" gutterBottom>
          Department: {item.department || 'N/A'} | Uploaded by: {item.uploadedBy || 'Unknown'} | 
          Created: {formatDate(item.createdAt)}
        </Typography>

        {item.description && (
          <Box sx={{ mt: 1, mb: 1, p: 1, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Typography variant="body2">
              {highlightSearchTerms(item.description, searchQuery)}
            </Typography>
          </Box>
        )}

        {/* Enhanced Highlights Display */}
        {item.highlights && Object.keys(item.highlights).length > 0 && (
          <Box sx={{ mt: 2, p: 1.5, bgcolor: 'info.light', borderRadius: 1, border: '1px solid', borderColor: 'info.main' }}>
            <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 'bold' }}>
              Search Matches:
            </Typography>
            {Object.entries(item.highlights).map(([field, highlights]) => (
              <Box key={field} sx={{ mb: 1 }}>
                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 'bold' }}>
                  {field.charAt(0).toUpperCase() + field.slice(1)}:
                </Typography>
                <Box sx={{ mt: 0.5, ml: 1 }}>
                  {highlights.map((highlight, index) => (
                    <Typography 
                      key={index} 
                      variant="body2" 
                      component="div"
                      dangerouslySetInnerHTML={{ __html: highlight }}
                      sx={{ 
                        mb: 0.5,
                        '& mark': {
                          backgroundColor: 'yellow',
                          fontWeight: 'bold',
                          padding: '0 2px',
                          borderRadius: '2px'
                        }
                      }}
                    />
                  ))}
                </Box>
              </Box>
            ))}
          </Box>
        )}

        <Box display="flex" gap={1} mt={2} flexWrap="wrap">
          <Chip 
            label={`OCR: ${((item.ocrConfidence || 0) * 100).toFixed(1)}%`}
            size="small"
            color={getConfidenceColor(item.ocrConfidence || 0)}
            variant="outlined"
          />
          <Chip 
            label={`Classification: ${((item.classificationConfidence || 0) * 100).toFixed(1)}%`}
            size="small"
            color={getConfidenceColor(item.classificationConfidence || 0)}
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
        <Box sx={{ position: 'relative' }}>
          <Box display="flex" gap={2} alignItems="center" mb={2}>
            <Box ref={searchInputRef} sx={{ flex: 1, position: 'relative' }}>
              <TextField
                fullWidth
                label="Search documents"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  if (e.target.value.length > 2) {
                    loadSuggestions();
                  } else {
                    setSuggestions([]);
                    setShowSuggestions(false);
                  }
                }}
                onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
                onFocus={() => {
                  if (suggestions.length > 0) {
                    setShowSuggestions(true);
                  }
                }}
                onBlur={() => {
                  // Delay hiding suggestions to allow click
                  setTimeout(() => setShowSuggestions(false), 200);
                }}
                placeholder="Enter search terms... Use AND, OR, NOT for Boolean search"
                InputProps={{
                  startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />,
                  endAdornment: (
                    <Tooltip title="Boolean Search Help">
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          setShowBooleanHelp(!showBooleanHelp);
                        }}
                      >
                        <HelpIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  )
                }}
              />
              {/* Enhanced Suggestions Component */}
              {showSuggestions && suggestions.length > 0 && (
                <SearchSuggestions
                  suggestions={suggestions}
                  onSuggestionSelect={handleSuggestionClick}
                  searchQuery={searchQuery}
                />
              )}
            </Box>
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

          {/* Boolean Search Help */}
          {showBooleanHelp && (
            <Alert severity="info" sx={{ mb: 2 }} onClose={() => setShowBooleanHelp(false)}>
              <Typography variant="subtitle2" gutterBottom>
                <strong>Boolean Search Operators:</strong>
              </Typography>
              <Typography variant="body2" component="div">
                <strong>AND</strong> - Find documents containing all terms: <code>tender AND contract</code>
                <br />
                <strong>OR</strong> - Find documents containing any term: <code>invoice OR bill</code>
                <br />
                <strong>NOT</strong> - Exclude documents with term: <code>contract NOT agreement</code>
                <br />
                <strong>Quotes</strong> - Exact phrase: <code>&quot;tender notice&quot;</code>
              </Typography>
            </Alert>
          )}

          {/* Boolean Operator Quick Insert */}
          <Box display="flex" gap={1} mb={2} flexWrap="wrap" alignItems="center">
            <Typography variant="caption" color="text.secondary" sx={{ mr: 1 }}>
              Quick insert:
            </Typography>
            <Chip
              label="AND"
              size="small"
              onClick={() => {
                const input = searchInputRef.current?.querySelector('input') as HTMLInputElement;
                const cursorPos = input?.selectionStart ?? searchQuery.length;
                const newQuery = searchQuery.slice(0, cursorPos) + ' AND ' + searchQuery.slice(cursorPos);
                setSearchQuery(newQuery);
                setTimeout(() => {
                  if (input) {
                    input.focus();
                    input.setSelectionRange(cursorPos + 5, cursorPos + 5);
                  }
                }, 0);
              }}
              sx={{ cursor: 'pointer' }}
            />
            <Chip
              label="OR"
              size="small"
              onClick={() => {
                const input = searchInputRef.current?.querySelector('input') as HTMLInputElement;
                const cursorPos = input?.selectionStart ?? searchQuery.length;
                const newQuery = searchQuery.slice(0, cursorPos) + ' OR ' + searchQuery.slice(cursorPos);
                setSearchQuery(newQuery);
                setTimeout(() => {
                  if (input) {
                    input.focus();
                    input.setSelectionRange(cursorPos + 4, cursorPos + 4);
                  }
                }, 0);
              }}
              sx={{ cursor: 'pointer' }}
            />
            <Chip
              label="NOT"
              size="small"
              onClick={() => {
                const input = searchInputRef.current?.querySelector('input') as HTMLInputElement;
                const cursorPos = input?.selectionStart ?? searchQuery.length;
                const newQuery = searchQuery.slice(0, cursorPos) + ' NOT ' + searchQuery.slice(cursorPos);
                setSearchQuery(newQuery);
                setTimeout(() => {
                  if (input) {
                    input.focus();
                    input.setSelectionRange(cursorPos + 5, cursorPos + 5);
                  }
                }, 0);
              }}
              sx={{ cursor: 'pointer' }}
            />
            <Chip
              label='"" Quotes'
              size="small"
              onClick={() => {
                const input = searchInputRef.current?.querySelector('input') as HTMLInputElement;
                const cursorPos = input?.selectionStart ?? searchQuery.length;
                const newQuery = searchQuery.slice(0, cursorPos) + '""' + searchQuery.slice(cursorPos);
                setSearchQuery(newQuery);
                setTimeout(() => {
                  if (input) {
                    input.focus();
                    input.setSelectionRange(cursorPos + 1, cursorPos + 1);
                  }
                }, 0);
              }}
              sx={{ cursor: 'pointer' }}
            />
          </Box>
        </Box>

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
              {/* Export Buttons */}
              <Box display="flex" gap={1}>
                <Tooltip title="Export to Excel">
                  <IconButton
                    onClick={handleExportExcel}
                    disabled={exporting === 'excel' || searchResults.totalHits === 0}
                    color="success"
                  >
                    {exporting === 'excel' ? <CircularProgress size={20} /> : <ExcelIcon />}
                  </IconButton>
                </Tooltip>
                <Tooltip title="Export to PDF">
                  <IconButton
                    onClick={handleExportPdf}
                    disabled={exporting === 'pdf' || searchResults.totalHits === 0}
                    color="error"
                  >
                    {exporting === 'pdf' ? <CircularProgress size={20} /> : <PdfIcon />}
                  </IconButton>
                </Tooltip>
              </Box>
              <Typography variant="body2">
                Page {searchResults.pageNumber + 1} of {searchResults.totalPages}
              </Typography>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Page Size</InputLabel>
                <Select
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(e.target.value as number);
                    performSearch(0);
                  }}
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
