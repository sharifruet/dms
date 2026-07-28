export const EXECUTIVE_KPI_CARDS = [
  { label: 'Total Contracts', value: 0, sub: 'All Time', accent: '#0e7cc4', icon: 'contract' },
  { label: 'Live Tender', value: 0, sub: 'Currently Live', accent: '#16a34a', icon: 'calendar' },
  { label: 'Running Contracts', value: 0, sub: 'In Progress', accent: '#f5a623', icon: 'briefcase' },
  { label: 'Completed Contracts', value: 963, sub: 'Completed', accent: '#7c3aed', icon: 'check' },
  { label: 'Approved Budget', value: 5250, prefix: 'BDT ', suffix: ' Cr', sub: 'FY Total', accent: '#0e7cc4', icon: 'vault' },
  { label: 'Allocated Budget', value: 4820, prefix: 'BDT ', suffix: ' Cr', sub: 'Released', accent: '#00b4a6', icon: 'money' },
  { label: 'Expenditure', value: 3940, prefix: 'BDT ', suffix: ' Cr', sub: 'Utilized', accent: '#e11d3c', icon: 'hand' },
  { label: 'Remaining Budget', value: 880, prefix: 'BDT ', suffix: ' Cr', sub: 'Available', accent: '#16a34a', icon: 'piggy' },
] as const;

export const APP_PROGRESS_ROWS = [
  { label: 'Total APP Package', value: '240', pct: 100, color: '#0e7cc4' },
  { label: 'Planned Budget', value: 'BDT 5,250 Cr', pct: 100, color: '#16a34a' },
  { label: 'Published Tender', value: '195 (81%)', pct: 81, color: '#1594e0' },
  { label: 'Awarded', value: '178 (74%)', pct: 74, color: '#f5a623' },
  { label: 'Under Execution', value: '152 (63%)', pct: 63, color: '#7c3aed' },
  { label: 'Completed', value: '124 (52%)', pct: 52, color: '#00b4a6' },
  { label: 'Delayed', value: '16 (7%)', pct: 7, color: '#e11d3c' },
] as const;

export const APP_COMPLETION = {
  pct: 72,
  trend: '8% vs Last Year',
  target: '100%',
} as const;

export const TENDER_STATS = [
  { label: 'Awarded', value: 178, color: '#0e7cc4' },
  { label: 'Live Tender', value: 48, color: '#16a34a' },
  { label: 'Under Evaluation', value: 52, color: '#f5a623' },
  { label: 'Cancelled', value: 23, color: '#e11d3c' },
  { label: 'Retendered', value: 24, color: '#7c3aed' },
] as const;

export const PROCUREMENT_NATURE = [
  { label: 'Goods', value: 168, color: '#0e7cc4' },
  { label: 'Works', value: 96, color: '#16a34a' },
  { label: 'Services', value: 61, color: '#f5a623' },
] as const;

export const PROCUREMENT_METHODS = [
  { label: 'OTM', value: 142, color: '#0e7cc4' },
  { label: 'LTM', value: 88, color: '#16a34a' },
  { label: 'RFQ', value: 54, color: '#f5a623' },
  { label: 'DPM', value: 41, color: '#7c3aed' },
] as const;

export const BUDGET_MONITORING = {
  totalAllocation: 'BDT 5,250 Cr',
  releasedBudget: 'BDT 4,820 Cr',
  expenditure: 'BDT 3,940 Cr',
  remainingBudget: 'BDT 880 Cr',
  utilizationPct: 68.45,
  utilizedPct: 68.45,
  remainingPct: 31.55,
} as const;

export const PERFORMANCE_SECURITY = [
  { name: 'Active PS/BG', count: 1125, variant: 'good' as const },
  { name: 'Expiring in 30 Days', count: 17, variant: 'warn' as const },
  { name: 'Expiring in 15 Days', count: 9, variant: 'warn-strong' as const },
  { name: 'Expiring in 7 Days', count: 3, variant: 'danger' as const },
  { name: 'Expired', count: 3, variant: 'expired' as const },
  { name: 'Renewed', count: 214, variant: 'good' as const },
] as const;

export const DOCUMENT_ANALYTICS = [
  { label: 'Total Documents', value: 18245311, className: '' },
  { label: "Today's Upload", value: 2314, className: 'text-blue' },
  { label: 'OCR Processed', value: 17880, className: 'text-orange' },
  { label: 'Pending OCR', value: 213, className: '' },
  { label: 'Indexed (Elasticsearch)', value: 18240121, className: 'text-green' },
  { label: 'Archived Documents', value: 9520115, className: '' },
] as const;

export const SEARCH_CHIPS = [
  'Tender No. TN-2026-114',
  'PS/BG Renewal',
  'Contract Amendment',
  'Grid Substation',
] as const;

export const SEARCH_STATS = [
  { value: '18,245,311', label: 'Total Documents' },
  { value: '18,240,121', label: 'Indexed' },
  { value: '< 400ms', label: 'Avg. Query Time' },
] as const;

export const RECENT_ALERTS = [
  {
    title: 'Tender Validity Expiring (7 Days)',
    desc: '3 tenders require immediate action before validity lapses.',
    count: 3,
    variant: 'danger' as const,
    icon: 'file',
  },
  {
    title: 'Tender Validity Expiring (15 Days)',
    desc: '2 tenders approaching validity deadline.',
    count: 2,
    variant: 'warn' as const,
    icon: 'file',
  },
  {
    title: 'Tender Validity Expiring (30 Days)',
    desc: '4 tenders to review within the month.',
    count: 4,
    variant: 'info' as const,
    icon: 'file',
  },
  {
    title: 'Performance Security Expiring (7 Days)',
    desc: '5 PS/BG instruments require renewal urgently.',
    count: 5,
    variant: 'danger' as const,
    icon: 'shield',
  },
  {
    title: 'Performance Security Expiring (30 Days)',
    desc: '12 PS/BG instruments to review this month.',
    count: 12,
    variant: 'warn' as const,
    icon: 'shield',
  },
  {
    title: 'Documents Pending for OCR',
    desc: '213 uploaded documents awaiting OCR processing.',
    count: 213,
    variant: 'neutral' as const,
    icon: 'waveform',
  },
] as const;

export const FISCAL_YEARS = ['FY 2026-2027', 'FY 2025-2026', 'FY 2024-2025'] as const;
