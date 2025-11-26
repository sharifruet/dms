import React, { useState, useEffect } from 'react';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  CircularProgress,
  Typography,
  Box,
  Chip,
} from '@mui/material';
import { appEntryService, AppEntry } from '../services/appEntryService';

interface AppEntrySelectorProps {
  value: number | null;
  onChange: (appEntryId: number | null) => void;
  required?: boolean;
  disabled?: boolean;
  showDetails?: boolean;
}

/**
 * Component for selecting a yearly budget (APP entry)
 * Shows fiscal year and installment number in a readable format
 */
const AppEntrySelector: React.FC<AppEntrySelectorProps> = ({
  value,
  onChange,
  required = false,
  disabled = false,
  showDetails = false,
}) => {
  const [appEntries, setAppEntries] = useState<AppEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadAppEntries();
  }, []);

  const loadAppEntries = async () => {
    try {
      setLoading(true);
      setError(null);
      const entries = await appEntryService.getAppEntries();
      // Sort by fiscal year (descending) and installment number (descending)
      const sorted = entries.sort((a, b) => {
        if (a.fiscalYear !== b.fiscalYear) {
          return b.fiscalYear - a.fiscalYear;
        }
        return (b.releaseInstallmentNo || 0) - (a.releaseInstallmentNo || 0);
      });
      setAppEntries(sorted);
    } catch (err: any) {
      console.error('Failed to load yearly budgets:', err);
      setError('Failed to load yearly budgets');
    } finally {
      setLoading(false);
    }
  };

  const formatFiscalYear = (year: number): string => {
    const nextYear = (year + 1) % 100;
    return `${year}-${nextYear.toString().padStart(2, '0')}`;
  };

  const formatAppEntryLabel = (entry: AppEntry): string => {
    const fiscalYearStr = formatFiscalYear(entry.fiscalYear);
    const installment = entry.releaseInstallmentNo || 0;
    const amount = entry.allocationAmount || 0;
    
    if (showDetails && amount > 0) {
      // Format amount with BDT currency and commas
      const formattedAmount = new Intl.NumberFormat('en-BD', {
        style: 'currency',
        currency: 'BDT',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(amount);
      
      return `FY ${fiscalYearStr}, Installment ${installment} - ${formattedAmount}${entry.allocationType ? ` (${entry.allocationType})` : ''}`;
    }
    
    return `FY ${fiscalYearStr}, Installment ${installment}${entry.allocationType ? ` (${entry.allocationType})` : ''}`;
  };

  const selectedEntry = appEntries.find(entry => entry.id === value);

  if (loading) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <CircularProgress size={20} />
        <Typography variant="body2" color="text.secondary">
          Loading APP entries...
        </Typography>
      </Box>
    );
  }

  if (error) {
    return (
      <Typography variant="body2" color="error">
        {error}
      </Typography>
    );
  }

  return (
    <Box>
      <FormControl fullWidth required={required} disabled={disabled}>
        <InputLabel>Yearly Budget</InputLabel>
        <Select
          value={value || ''}
          label="Yearly Budget"
          onChange={(e) => {
            const newValue = e.target.value === '' ? null : Number(e.target.value);
            onChange(newValue);
          }}
        >
          <MenuItem value="">
            <em>None</em>
          </MenuItem>
          {appEntries.map((entry) => (
            <MenuItem key={entry.id} value={entry.id}>
              {formatAppEntryLabel(entry)}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      {selectedEntry && showDetails && (
        <Box sx={{ mt: 1, p: 1, bgcolor: 'grey.100', borderRadius: 1 }}>
          <Typography variant="caption" display="block" color="text.secondary">
            <strong>Fiscal Year:</strong> {formatFiscalYear(selectedEntry.fiscalYear)}
          </Typography>
          <Typography variant="caption" display="block" color="text.secondary">
            <strong>Installment:</strong> {selectedEntry.releaseInstallmentNo || 'N/A'}
          </Typography>
          {selectedEntry.allocationType && (
            <Typography variant="caption" display="block" color="text.secondary">
              <strong>Type:</strong> {selectedEntry.allocationType}
            </Typography>
          )}
          {selectedEntry.allocationAmount && (
            <Typography variant="caption" display="block" color="text.secondary">
              <strong>Amount:</strong>{' '}
              {new Intl.NumberFormat('en-BD', {
                style: 'currency',
                currency: 'BDT',
                minimumFractionDigits: 0,
                maximumFractionDigits: 0,
              }).format(selectedEntry.allocationAmount)}
            </Typography>
          )}
          {selectedEntry.budgetReleaseDate && (
            <Typography variant="caption" display="block" color="text.secondary">
              <strong>Release Date:</strong>{' '}
              {new Date(selectedEntry.budgetReleaseDate).toLocaleDateString()}
            </Typography>
          )}
          {selectedEntry.referenceMemoNumber && (
            <Typography variant="caption" display="block" color="text.secondary">
              <strong>Memo No:</strong> {selectedEntry.referenceMemoNumber}
            </Typography>
          )}
        </Box>
      )}
    </Box>
  );
};

export default AppEntrySelector;

