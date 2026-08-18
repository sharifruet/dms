import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import {
  ArrowForward,
  AutoAwesome,
  AccessTime,
  Autorenew,
  Balance,
  CalendarTodayOutlined,
  Check,
  Checklist,
  CheckCircle,
  Close,
  DescriptionOutlined,
  DocumentScannerOutlined,
  EventAvailable,
  ExpandMore,
  Groups,
  HourglassBottom,
  Memory,
  NotificationsNone,
  PaymentsOutlined,
  Psychology,
  Refresh,
  SavingsOutlined,
  Search as SearchIcon,
  ShieldOutlined,
  ShoppingCart,
  AccountBalance,
  TrendingDown,
  TrendingFlat,
  TrendingUp,
  WarningAmber,
  WorkOutline,
} from '@mui/icons-material';
import { useAppSelector } from '../hooks/redux';
import procurementService from '../services/procurementService';
import { ChartBucket, ExecutiveSnapshot } from '../types/procurement';
import './ExecutiveDashboard.css';

/* =========================================================
   BPDB Executive Dashboard

   The layout and theme are the signed-off mock-up in `dboard/index.html`;
   the figures are live, from GET /api/procurement/executive-dashboard.
   Nothing on this page is a stored dashboard number — every count is
   derived from the lifecycle records by ExecutiveDashboardService, and
   what each one means is documented there rather than here.

   The mock-up's Chart.js canvases are drawn with recharts (already a
   dependency) and its Font Awesome glyphs with the MUI icon set,
   because the app loads neither of those CDNs.
   ========================================================= */

const COLORS = {
  blue: '#0e7cc4',
  blueLight: '#1594e0',
  green: '#16a34a',
  amber: '#f5a623',
  purple: '#7c3aed',
  teal: '#00b4a6',
  red: '#e11d3c',
  gray200: '#e2e8f0',
};

/** Series colours, in the mock-up's order, cycled when a chart has more slices. */
const SERIES = [COLORS.blue, COLORS.green, COLORS.amber, COLORS.purple, COLORS.red, COLORS.teal];

interface LegendDatum {
  label: string;
  value: number;
  color: string;
  // recharts v3 types its `data` prop as an indexable record, so a plain
  // interface is rejected without this.
  [key: string]: string | number;
}

const withColors = (buckets: ChartBucket[]): LegendDatum[] =>
  buckets.map((b, i) => ({ label: b.label, value: b.value, color: SERIES[i % SERIES.length] }));

/** "FY 2025-2026", the form the client's papers use. */
const fyLabel = (year: number): string => `FY ${year}-${year + 1}`;

const formatTimestamp = (value: string | Date): string => {
  const d = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return '—';
  return d
    .toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
    .replace(',', ' •');
};

const crore = (value: number | null | undefined): string =>
  value === null || value === undefined
    ? '—'
    : `BDT ${value.toLocaleString('en-US', { maximumFractionDigits: 2 })} Cr`;

/** Cubic ease-out count-up, the mock-up's `animateCounters()`. */
const useCountUp = (target: number, runKey: number, duration = 1000): number => {
  const [value, setValue] = useState(0);

  useEffect(() => {
    let frame = 0;
    let start: number | null = null;

    const step = (timestamp: number) => {
      if (start === null) start = timestamp;
      const progress = Math.min((timestamp - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(target * eased);
      if (progress < 1) frame = window.requestAnimationFrame(step);
      else setValue(target);
    };

    frame = window.requestAnimationFrame(step);
    return () => window.cancelAnimationFrame(frame);
  }, [target, runKey, duration]);

  return value;
};

const Counter: React.FC<{
  value: number;
  runKey: number;
  prefix?: string;
  suffix?: string;
  decimals?: number;
  className?: string;
}> = ({ value, runKey, prefix = '', suffix = '', decimals = 0, className }) => {
  const current = useCountUp(value, runKey);
  return (
    <span className={className}>
      {prefix}
      {current.toLocaleString('en-US', {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
      })}
      {suffix}
    </span>
  );
};

const Legend: React.FC<{ data: LegendDatum[]; compact?: boolean }> = ({ data, compact }) => {
  const total = data.reduce((sum, d) => sum + d.value, 0);
  if (!data.length) {
    return <p className="empty-note">Nothing captured yet.</p>;
  }
  return (
    <ul className={compact ? 'legend-list compact' : 'legend-list'}>
      {data.map((d) => (
        <li key={d.label}>
          <span className="legend-name">
            <i className="dot" style={{ background: d.color }} />
            {d.label}
          </span>
          <span className="legend-value">
            {d.value} ({total ? Math.round((d.value / total) * 100) : 0}%)
          </span>
        </li>
      ))}
    </ul>
  );
};

const ChartTooltip: React.FC<{ active?: boolean; payload?: any[]; suffix?: string }> = ({
  active,
  payload,
  suffix = '',
}) => {
  if (!active || !payload || !payload.length) return null;
  const point = payload[0];
  const label = point.payload?.label ?? point.name;
  return (
    <div className="chart-tooltip">
      {label}:{' '}
      <strong>
        {point.value}
        {suffix}
      </strong>
    </div>
  );
};

/** Donut sized to fill its wrapper, matching Chart.js `cutout`. */
const Donut: React.FC<{ data: LegendDatum[]; cutout: string; suffix?: string }> = ({
  data,
  cutout,
  suffix,
}) => {
  // An all-zero dataset renders as nothing at all in recharts, which reads as a broken
  // chart rather than an empty one. A single grey ring says "no data" unambiguously.
  const empty = !data.length || data.every((d) => d.value === 0);
  const slices: LegendDatum[] = empty
    ? [{ label: 'No data', value: 1, color: COLORS.gray200 }]
    : data;
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie
          data={slices}
          dataKey="value"
          nameKey="label"
          innerRadius={cutout}
          outerRadius="100%"
          startAngle={90}
          endAngle={-270}
          stroke="none"
          isAnimationActive
        >
          {slices.map((d) => (
            <Cell key={d.label} fill={d.color} />
          ))}
        </Pie>
        {!empty && <Tooltip content={<ChartTooltip suffix={suffix} />} />}
      </PieChart>
    </ResponsiveContainer>
  );
};

const ExecutiveDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAppSelector((state) => state.auth);

  const [snapshot, setSnapshot] = useState<ExecutiveSnapshot | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [fiscalYear, setFiscalYear] = useState<number | undefined>(undefined);
  const [refreshKey, setRefreshKey] = useState(0);
  const [spinning, setSpinning] = useState(false);
  const [ringOffset, setRingOffset] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const spinTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const RING_RADIUS = 52;
  const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

  const load = useCallback(async (year: number | undefined) => {
    setLoading(true);
    try {
      const data = await procurementService.getExecutiveDashboard(year);
      setSnapshot(data);
      setError(null);
    } catch (e: any) {
      setError(e?.response?.data?.error || 'Could not load the executive dashboard');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load(fiscalYear);
  }, [load, fiscalYear, refreshKey]);

  // Release the ring on the frame after it is drawn, so the CSS stroke-dashoffset
  // transition actually runs (the mock-up does the same with a double rAF).
  const completionPct = snapshot?.appProgress.completionPct ?? 0;
  useEffect(() => {
    setRingOffset(RING_CIRCUMFERENCE);
    const frame = window.requestAnimationFrame(() => {
      setRingOffset(RING_CIRCUMFERENCE * (1 - completionPct / 100));
    });
    return () => window.cancelAnimationFrame(frame);
  }, [RING_CIRCUMFERENCE, completionPct]);

  useEffect(
    () => () => {
      if (spinTimer.current) clearTimeout(spinTimer.current);
    },
    []
  );

  const handleRefresh = useCallback(() => {
    setSpinning(true);
    setRefreshKey((k) => k + 1);
    if (spinTimer.current) clearTimeout(spinTimer.current);
    spinTimer.current = setTimeout(() => setSpinning(false), 650);
  }, []);

  const runSearch = useCallback(
    (term: string) => {
      const trimmed = term.trim();
      navigate(trimmed ? `/search?q=${encodeURIComponent(trimmed)}` : '/search');
    },
    [navigate]
  );

  const tenderData = useMemo(
    () => withColors(snapshot?.tenderStatistics ?? []),
    [snapshot]
  );
  const natureData = useMemo(() => withColors(snapshot?.procurementNature ?? []), [snapshot]);
  const methodData = useMemo(() => withColors(snapshot?.procurementMethod ?? []), [snapshot]);

  const year = new Date().getFullYear();
  const initials = (user?.username ?? 'User').slice(0, 2).toUpperCase();

  const kpis = snapshot?.kpis;
  const app = snapshot?.appProgress;
  const budget = snapshot?.budget;
  const ps = snapshot?.performanceSecurity;
  const docs = snapshot?.documents;
  const alerts = snapshot?.alerts ?? [];

  const tenderTotal = tenderData.reduce((sum, d) => sum + d.value, 0);
  const methodTotal = methodData.reduce((sum, d) => sum + d.value, 0);
  const utilization = budget?.utilizationPct ?? 0;
  const budgetDonut: LegendDatum[] = [
    { label: 'Utilized', value: utilization, color: COLORS.green },
    { label: 'Remaining', value: Math.max(0, 100 - utilization), color: COLORS.gray200 },
  ];

  const alertCount = alerts.reduce((sum, a) => sum + a.count, 0);

  /** APP funnel rows: share of the year's packages that have reached each gate. */
  const share = (value?: number) =>
    !app || !app.totalPackages ? 0 : Math.round(((value ?? 0) / app.totalPackages) * 100);

  const progressRows = [
    { label: 'Total APP Package', pct: 100, color: COLORS.blue, value: `${app?.totalPackages ?? 0}` },
    { label: 'Planned Budget', pct: 100, color: COLORS.green, value: crore(app?.plannedBudgetCrore) },
    {
      label: 'Published Tender',
      pct: share(app?.publishedTenders),
      color: COLORS.blueLight,
      value: `${app?.publishedTenders ?? 0} (${share(app?.publishedTenders)}%)`,
    },
    {
      label: 'Awarded',
      pct: share(app?.awarded),
      color: COLORS.amber,
      value: `${app?.awarded ?? 0} (${share(app?.awarded)}%)`,
    },
    {
      label: 'Under Execution',
      pct: share(app?.underExecution),
      color: COLORS.purple,
      value: `${app?.underExecution ?? 0} (${share(app?.underExecution)}%)`,
    },
    {
      label: 'Completed',
      pct: share(app?.completed),
      color: COLORS.teal,
      value: `${app?.completed ?? 0} (${share(app?.completed)}%)`,
    },
    {
      label: 'Delayed',
      pct: share(app?.delayed),
      color: COLORS.red,
      value: `${app?.delayed ?? 0} (${share(app?.delayed)}%)`,
    },
  ];

  const kpiCards = [
    {
      label: 'Total Contracts',
      value: kpis?.totalContracts ?? 0,
      sub: 'Signed',
      accent: COLORS.blue,
      icon: <DescriptionOutlined />,
    },
    {
      label: 'Live Tender',
      value: kpis?.liveTenders ?? 0,
      sub: 'Currently Live',
      accent: COLORS.green,
      icon: <EventAvailable />,
    },
    {
      label: 'Running Contracts',
      value: kpis?.runningContracts ?? 0,
      sub: 'In Progress',
      accent: COLORS.amber,
      icon: <WorkOutline />,
    },
    {
      label: 'Completed Contracts',
      value: kpis?.completedContracts ?? 0,
      sub: 'Closed',
      accent: COLORS.purple,
      icon: <CheckCircle />,
    },
    {
      label: 'Approved Budget',
      value: budget?.approvedCrore ?? 0,
      sub: 'FY Total',
      accent: COLORS.blue,
      icon: <AccountBalance />,
      prefix: 'BDT ',
      suffix: ' Cr',
      decimals: 2,
    },
    {
      label: 'Allocated Budget',
      value: budget?.releasedCrore ?? 0,
      sub: 'Released',
      accent: COLORS.teal,
      icon: <PaymentsOutlined />,
      prefix: 'BDT ',
      suffix: ' Cr',
      decimals: 2,
    },
    {
      label: 'Expenditure',
      value: budget?.expenditureCrore ?? 0,
      sub: 'Utilized',
      accent: COLORS.red,
      icon: <TrendingDown />,
      prefix: 'BDT ',
      suffix: ' Cr',
      decimals: 2,
    },
    {
      label: 'Remaining Budget',
      value: budget?.remainingCrore ?? 0,
      sub: 'Available',
      accent: COLORS.green,
      icon: <SavingsOutlined />,
      prefix: 'BDT ',
      suffix: ' Cr',
      decimals: 2,
    },
  ];

  const psRows = [
    { tone: 'good', icon: <Check />, name: 'Active PS/BG', count: ps?.active ?? 0 },
    { tone: 'warn', icon: <AccessTime />, name: 'Expiring in 30 Days', count: ps?.expiringIn30Days ?? 0 },
    {
      tone: 'warn-strong',
      icon: <HourglassBottom />,
      name: 'Expiring in 15 Days',
      count: ps?.expiringIn15Days ?? 0,
    },
    { tone: 'danger', icon: <WarningAmber />, name: 'Expiring in 7 Days', count: ps?.expiringIn7Days ?? 0 },
    { tone: 'expired', icon: <Close />, name: 'Expired', count: ps?.expired ?? 0 },
    { tone: 'good', icon: <Autorenew />, name: 'Renewed', count: ps?.renewed ?? 0 },
  ];

  const documentRows = [
    { label: 'Total Documents', value: docs?.total ?? 0, tone: '' },
    { label: "Today's Upload", value: docs?.todayUploads ?? 0, tone: 'text-blue' },
    { label: 'OCR Processed', value: docs?.ocrProcessed ?? 0, tone: 'text-orange' },
    { label: 'Pending OCR', value: docs?.ocrPending ?? 0, tone: '' },
    { label: 'Failed OCR', value: docs?.ocrFailed ?? 0, tone: docs?.ocrFailed ? 'text-red' : '' },
    { label: 'Archived Documents', value: docs?.archived ?? 0, tone: '' },
  ];

  const alertIcon = (key: string) => {
    if (key.startsWith('PS_')) return <ShieldOutlined />;
    if (key.startsWith('OCR')) return <DocumentScannerOutlined />;
    return <DescriptionOutlined />;
  };

  const trend = app?.completionTrendPct;

  return (
    <div className="exec-dash">
      <header className="topbar">
        <div className="topbar-left">
          <div className="header-title">
            <h1>Executive Dashboard</h1>
            <p>Digital Document Management &amp; Procurement Governance System</p>
          </div>
        </div>
        <div className="topbar-right">
          <form
            className="global-search"
            onSubmit={(e) => {
              e.preventDefault();
              runSearch(searchTerm);
            }}
          >
            <SearchIcon />
            <input
              type="text"
              placeholder="Search documents, tenders, contracts, suppliers..."
              aria-label="Global search"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </form>
          <div className="fy-selector">
            <CalendarTodayOutlined style={{ fontSize: 14 }} />
            <select
              value={fiscalYear === undefined ? 'all' : String(fiscalYear)}
              onChange={(e) =>
                setFiscalYear(e.target.value === 'all' ? undefined : Number(e.target.value))
              }
              aria-label="Financial year"
            >
              <option value="all">All Years</option>
              {(snapshot?.fiscalYears ?? []).map((y) => (
                <option key={y} value={y}>
                  {fyLabel(y)}
                </option>
              ))}
            </select>
          </div>
          <button
            type="button"
            className="icon-btn"
            aria-label="Notifications"
            onClick={() => navigate('/notifications')}
          >
            <NotificationsNone />
            {alertCount > 0 && <span className="notif-count">{alertCount}</span>}
          </button>
          <div className="profile-chip">
            <div className="avatar">{initials}</div>
            <div className="profile-text">
              <span className="profile-name">{user?.username ?? 'User'}</span>
              <span className="profile-role">{user?.role?.toLowerCase() ?? '—'}</span>
            </div>
            <ExpandMore />
          </div>
        </div>
      </header>

      <div className={error ? 'status-strip is-error' : 'status-strip'}>
        <span className="status-dot" />
        <span className="status-text">
          {error
            ? `Data unavailable: ${error}`
            : loading
              ? 'Refreshing lifecycle data…'
              : 'System Status: All Systems Operational'}
        </span>
        <span className="status-divider" />
        <span className="last-updated">
          Last Updated:{' '}
          <strong>{snapshot ? formatTimestamp(snapshot.generatedAt) : '—'}</strong>
        </span>
        <button
          type="button"
          className={spinning ? 'refresh-btn spinning' : 'refresh-btn'}
          onClick={handleRefresh}
          aria-label="Refresh dashboard"
        >
          <Refresh />
        </button>
      </div>

      <main className="content">
        {/* KPI ROW */}
        <section className="kpi-grid">
          {kpiCards.map((kpi) => (
            <div
              key={kpi.label}
              className="kpi-card"
              style={{ '--accent': kpi.accent } as React.CSSProperties}
            >
              <div className="kpi-icon">{kpi.icon}</div>
              <div className="kpi-info">
                <span className="kpi-label">{kpi.label}</span>
                <span className="kpi-value">
                  <Counter
                    value={kpi.value}
                    runKey={refreshKey}
                    prefix={kpi.prefix}
                    suffix={kpi.suffix}
                    decimals={kpi.decimals}
                  />
                </span>
                <span className="kpi-sub">{kpi.sub}</span>
              </div>
            </div>
          ))}
        </section>

        {/* ROW 2: APP Progress / Tender Statistics / Procurement Analytics */}
        <section className="grid-3">
          <div className="panel">
            <div className="panel-head">
              <h2>
                <Checklist /> APP Progress <span className="panel-tag">Annual Procurement Plan</span>
              </h2>
              <button
                type="button"
                className="view-all"
                onClick={() => navigate('/procurement/packages')}
              >
                View All <ArrowForward />
              </button>
            </div>
            <div className="app-progress-list">
              {progressRows.map((row) => (
                <div className="progress-row" key={row.label}>
                  <span className="progress-label">{row.label}</span>
                  <div className="progress-track">
                    <div
                      className="progress-fill"
                      style={
                        { '--pct': `${row.pct}%`, background: row.color } as React.CSSProperties
                      }
                    />
                  </div>
                  <span className="progress-value">{row.value}</span>
                </div>
              ))}
            </div>
            <div className="app-completion">
              <div className="completion-ring">
                <svg viewBox="0 0 120 120">
                  <circle cx="60" cy="60" r={RING_RADIUS} className="ring-bg" />
                  <circle
                    cx="60"
                    cy="60"
                    r={RING_RADIUS}
                    className="ring-fill"
                    style={{
                      strokeDasharray: RING_CIRCUMFERENCE,
                      strokeDashoffset: ringOffset,
                    }}
                  />
                </svg>
                <div className="ring-center">
                  <span className="ring-value">{Math.round(completionPct)}%</span>
                  <span className="ring-label">Completion</span>
                </div>
              </div>
              <div className="completion-meta">
                {trend === null || trend === undefined ? (
                  <span className="trend-flat">
                    <TrendingFlat /> No prior year to compare
                  </span>
                ) : (
                  <span className={trend < 0 ? 'trend-down' : 'trend-up'}>
                    {trend < 0 ? <TrendingDown /> : <TrendingUp />}{' '}
                    {Math.abs(trend).toFixed(1)}% vs Last Year
                  </span>
                )}
                <span className="target-text">Target: 100%</span>
              </div>
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <Groups /> Tender Statistics
              </h2>
              <button
                type="button"
                className="view-all"
                onClick={() => navigate('/procurement/packages')}
              >
                View All <ArrowForward />
              </button>
            </div>
            <div className="donut-wrap">
              <Donut data={tenderData} cutout="70%" />
              <div className="donut-center">
                <span className="donut-value">{tenderTotal}</span>
                <span className="donut-label">Total Tenders</span>
              </div>
            </div>
            <Legend data={tenderData} />
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <ShoppingCart /> Procurement Analytics
              </h2>
              <button type="button" className="view-all" onClick={() => navigate('/reports')}>
                View All <ArrowForward />
              </button>
            </div>
            <p className="chart-subtitle">Procurement Nature</p>
            <div className="dual-chart-top">
              <div className="mini-donut-wrap">
                <Donut data={natureData} cutout="65%" />
              </div>
              <Legend data={natureData} compact />
            </div>
            <p className="chart-subtitle">Procurement Method / Type</p>
            <div className="bar-chart-wrap">
              {methodData.length === 0 ? (
                <p className="empty-note">No procurement method captured yet.</p>
              ) : (
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={methodData} margin={{ top: 4, right: 4, bottom: 0, left: -18 }}>
                    <CartesianGrid stroke="#f1f5f9" vertical={false} />
                    <XAxis
                      dataKey="label"
                      tickLine={false}
                      axisLine={{ stroke: '#e2e8f0' }}
                      tick={{ fill: '#64748b', fontSize: 11 }}
                    />
                    <YAxis
                      tickLine={false}
                      axisLine={false}
                      tick={{ fill: '#64748b', fontSize: 11 }}
                      allowDecimals={false}
                    />
                    <Tooltip content={<ChartTooltip />} cursor={{ fill: 'rgba(14,124,196,0.06)' }} />
                    <Bar dataKey="value" radius={[6, 6, 0, 0]} maxBarSize={34}>
                      {methodData.map((d) => (
                        <Cell key={d.label} fill={d.color} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </div>
            <div className="total-line">
              Total: <strong>{methodTotal}</strong>
            </div>
          </div>
        </section>

        {/* ROW 3: Budget Monitoring / Performance Security / Document Analytics */}
        <section className="grid-3">
          <div className="panel">
            <div className="panel-head">
              <h2>
                <Balance /> Budget Monitoring
              </h2>
            </div>
            <div className="split-panel">
              <div className="metric-list">
                <div className="metric-row">
                  <span>Total Allocation</span>
                  <strong>{crore(budget?.approvedCrore)}</strong>
                </div>
                <div className="metric-row">
                  <span>Released Budget</span>
                  <strong className="text-blue">{crore(budget?.releasedCrore)}</strong>
                </div>
                <div className="metric-row">
                  <span>Expenditure</span>
                  <strong className="text-red">{crore(budget?.expenditureCrore)}</strong>
                </div>
                <div className="metric-row">
                  <span>Remaining Budget</span>
                  <strong className="text-green">{crore(budget?.remainingCrore)}</strong>
                </div>
              </div>
              <div className="mini-donut-wrap small">
                <Donut data={budgetDonut} cutout="78%" suffix="%" />
                <div className="donut-center small">
                  <span className="donut-value">{utilization.toFixed(2)}%</span>
                  <span className="donut-label">Utilization</span>
                </div>
              </div>
            </div>
            <div className="legend-inline">
              <span>
                <i className="dot" style={{ background: COLORS.green }} /> Utilized (
                {utilization.toFixed(2)}%)
              </span>
              <span>
                <i className="dot" style={{ background: COLORS.gray200 }} /> Remaining (
                {Math.max(0, 100 - utilization).toFixed(2)}%)
              </span>
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <ShieldOutlined /> Performance Security
              </h2>
              <button
                type="button"
                className="view-all"
                onClick={() => navigate('/procurement/expiries')}
              >
                View All <ArrowForward />
              </button>
            </div>
            <div className="status-list">
              {psRows.map((row) => (
                <div className="status-row" key={row.name}>
                  <span className={`status-icon ${row.tone}`}>{row.icon}</span>
                  <span className="status-name">{row.name}</span>
                  <span className="status-count">{row.count.toLocaleString('en-US')}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <DescriptionOutlined /> Document Analytics
              </h2>
              <button type="button" className="view-all" onClick={() => navigate('/reports')}>
                View All <ArrowForward />
              </button>
            </div>
            <div className="metric-list roomy">
              {documentRows.map((m) => (
                <div className="metric-row" key={m.label}>
                  <span>{m.label}</span>
                  <strong className={m.tone}>
                    <Counter value={m.value} runKey={refreshKey} />
                  </strong>
                </div>
              ))}
              <div className="metric-row">
                <span>Indexed (Elasticsearch)</span>
                <strong className="text-green">
                  {docs?.indexed === null || docs?.indexed === undefined ? (
                    <span title="Elasticsearch is not reachable">—</span>
                  ) : (
                    <Counter value={docs.indexed} runKey={refreshKey} />
                  )}
                </strong>
              </div>
            </div>
          </div>
        </section>

        {/* ROW 4: Enterprise Search / Recent Alerts */}
        <section className="grid-2">
          <div className="panel search-panel">
            <div className="panel-head">
              <h2>
                <Psychology /> Enterprise Search
              </h2>
            </div>
            <form
              className="ai-search-box"
              onSubmit={(e) => {
                e.preventDefault();
                runSearch(searchTerm);
              }}
            >
              <SearchIcon />
              <input
                type="text"
                placeholder="Search any word inside millions of procurement documents..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                aria-label="Enterprise search"
              />
              <button className="ai-search-btn" type="submit" aria-label="Search">
                <AutoAwesome />
              </button>
            </form>
            <p className="ai-powered-tag">
              <Memory /> Powered by OCR + Elasticsearch
            </p>
            <div className="search-chips">
              {['Performance Security', 'Contract Amendment', 'Notification of Award', 'Delivery Challan'].map(
                (chip) => (
                  <button type="button" className="chip" key={chip} onClick={() => runSearch(chip)}>
                    {chip}
                  </button>
                )
              )}
            </div>
            <div className="search-stat-row">
              <div className="search-stat">
                <span>{(docs?.total ?? 0).toLocaleString('en-US')}</span>
                <small>Total Documents</small>
              </div>
              <div className="search-stat">
                <span>
                  {docs?.indexed === null || docs?.indexed === undefined
                    ? '—'
                    : docs.indexed.toLocaleString('en-US')}
                </span>
                <small>Indexed</small>
              </div>
              <div className="search-stat">
                <span>{(docs?.ocrProcessed ?? 0).toLocaleString('en-US')}</span>
                <small>OCR Processed</small>
              </div>
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <NotificationsNone /> Recent Alerts
              </h2>
              <button
                type="button"
                className="view-all"
                onClick={() => navigate('/procurement/expiries')}
              >
                View All Alerts <ArrowForward />
              </button>
            </div>
            <div className="alert-list">
              {alerts.map((alert) => (
                <div className={`alert-card ${alert.tone}`} key={alert.key}>
                  {alertIcon(alert.key)}
                  <div className="alert-body">
                    <p className="alert-title">{alert.title}</p>
                    <p className="alert-desc">{alert.description}</p>
                  </div>
                  <span className="alert-count">{alert.count}</span>
                </div>
              ))}
              {alerts.length === 0 && (
                <p className="empty-note">Nothing outstanding — no expiry or OCR backlog.</p>
              )}
            </div>
          </div>
        </section>
      </main>

      <footer className="page-footer">
        <span>&copy; {year} BPDB. All Rights Reserved.</span>
        <div className="footer-links">
          <a href="/help">Help</a>
          <a href="/privacy">Privacy Policy</a>
          <a href="/terms">Terms of Use</a>
        </div>
      </footer>
    </div>
  );
};

export default ExecutiveDashboard;
