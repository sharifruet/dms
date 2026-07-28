import React, { useState, useEffect, useCallback } from 'react';
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
  Assignment as ContractIcon,
  EventAvailable as CalendarIcon,
  Work as BriefcaseIcon,
  CheckCircle as CheckIcon,
  AccountBalance as VaultIcon,
  Payments as MoneyIcon,
  Savings as HandIcon,
  Savings as PiggyIcon,
  PlaylistAddCheck as AppIcon,
  Groups as TenderIcon,
  ShoppingCart as CartIcon,
  Balance as BalanceIcon,
  Security as SecurityIcon,
  Description as DocIcon,
  Psychology as BrainIcon,
  Search as SearchIcon,
  AutoFixHigh as SparkleIcon,
  Memory as ChipIcon,
  ArrowForward as ArrowIcon,
  TrendingUp as TrendUpIcon,
  Refresh as RefreshIcon,
  Check as PsCheckIcon,
  Schedule as ClockIcon,
  HourglassBottom as HourglassIcon,
  Warning as WarnIcon,
  Close as CloseIcon,
  Autorenew as RenewIcon,
  ReportProblem as FileAlertIcon,
  Shield as ShieldIcon,
  GraphicEq as WaveformIcon,
  Notifications as BellIcon,
} from '@mui/icons-material';
import {
  EXECUTIVE_KPI_CARDS,
  APP_PROGRESS_ROWS,
  APP_COMPLETION,
  TENDER_STATS,
  PROCUREMENT_NATURE,
  PROCUREMENT_METHODS,
  BUDGET_MONITORING,
  PERFORMANCE_SECURITY,
  DOCUMENT_ANALYTICS,
  SEARCH_CHIPS,
  SEARCH_STATS,
  RECENT_ALERTS,
} from '../../constants/executiveDashboardData';
import { contractAgreementService } from '../../services/contractAgreementService';
import '../../styles/executive-dashboard.css';

interface ChartItem {
  label: string;
  value: number;
  color: string;
}

const KPI_ICONS: Record<string, React.ReactNode> = {
  contract: <ContractIcon fontSize="small" />,
  calendar: <CalendarIcon fontSize="small" />,
  briefcase: <BriefcaseIcon fontSize="small" />,
  check: <CheckIcon fontSize="small" />,
  vault: <VaultIcon fontSize="small" />,
  money: <MoneyIcon fontSize="small" />,
  hand: <HandIcon fontSize="small" />,
  piggy: <PiggyIcon fontSize="small" />,
};

const PS_ICONS: Record<string, React.ReactNode> = {
  good: <PsCheckIcon sx={{ fontSize: 14 }} />,
  warn: <ClockIcon sx={{ fontSize: 14 }} />,
  'warn-strong': <HourglassIcon sx={{ fontSize: 14 }} />,
  danger: <WarnIcon sx={{ fontSize: 14 }} />,
  expired: <CloseIcon sx={{ fontSize: 14 }} />,
};

const ALERT_ICONS: Record<string, React.ReactNode> = {
  file: <FileAlertIcon sx={{ fontSize: 18 }} />,
  shield: <ShieldIcon sx={{ fontSize: 18 }} />,
  waveform: <WaveformIcon sx={{ fontSize: 18 }} />,
};

function formatTimestamp(d: Date): string {
  return d
    .toLocaleString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
    .replace(',', ' •');
}

function useAnimatedCounter(target: number, active: boolean, duration = 1000): number {
  const [value, setValue] = useState(0);

  useEffect(() => {
    if (!active) return;
    let startTime: number | null = null;
    let frame: number;

    const step = (timestamp: number) => {
      if (!startTime) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(Math.round(target * eased));
      if (progress < 1) frame = requestAnimationFrame(step);
      else setValue(target);
    };

    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
  }, [target, active, duration]);

  return value;
}

function AnimatedNumber({
  value,
  prefix = '',
  suffix = '',
  animate,
  className,
}: {
  value: number;
  prefix?: string;
  suffix?: string;
  animate: boolean;
  className?: string;
}) {
  const animated = useAnimatedCounter(value, animate);
  return (
    <span className={className}>
      {prefix}
      {animated.toLocaleString('en-US')}
      {suffix}
    </span>
  );
}

function DonutChart({
  data,
  innerRadius = '70%',
  outerRadius = '100%',
}: {
  data: ChartItem[];
  innerRadius?: string | number;
  outerRadius?: string | number;
}) {
  const chartData = data.map((d) => ({ name: d.label, value: d.value, color: d.color }));

  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie
          data={chartData}
          cx="50%"
          cy="50%"
          innerRadius={innerRadius}
          outerRadius={outerRadius}
          dataKey="value"
          strokeWidth={0}
        >
          {chartData.map((entry, index) => (
            <Cell key={index} fill={entry.color} />
          ))}
        </Pie>
      </PieChart>
    </ResponsiveContainer>
  );
}

function ChartLegend({ data }: { data: ChartItem[] }) {
  const total = data.reduce((sum, d) => sum + d.value, 0);
  return (
    <ul className="legend-list">
      {data.map((d) => {
        const pct = total ? Math.round((d.value / total) * 100) : 0;
        return (
          <li key={d.label}>
            <span className="legend-name">
              <i className="dot" style={{ background: d.color }} />
              {d.label}
            </span>
            <span className="legend-value">
              {d.value} ({pct}%)
            </span>
          </li>
        );
      })}
    </ul>
  );
}

function CompletionRing({ pct }: { pct: number }) {
  const radius = 52;
  const circumference = 2 * Math.PI * radius;
  const [offset, setOffset] = useState(circumference);

  useEffect(() => {
    const timer = requestAnimationFrame(() => {
      setOffset(circumference * (1 - pct / 100));
    });
    return () => cancelAnimationFrame(timer);
  }, [pct, circumference]);

  return (
    <div className="completion-ring">
      <svg viewBox="0 0 120 120">
        <circle cx="60" cy="60" r={radius} className="ring-bg" />
        <circle
          cx="60"
          cy="60"
          r={radius}
          className="ring-fill"
          style={{
            strokeDasharray: circumference,
            strokeDashoffset: offset,
          }}
        />
      </svg>
      <div className="ring-center">
        <span className="ring-value">{pct}%</span>
        <span className="ring-label">Completion</span>
      </div>
    </div>
  );
}

const ExecutiveDashboard: React.FC = () => {
  const [lastUpdated, setLastUpdated] = useState(() => formatTimestamp(new Date()));
  const [spinning, setSpinning] = useState(false);
  const [animateCounters, setAnimateCounters] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [totalContracts, setTotalContracts] = useState<number | null>(null);

  const loadTotalContracts = useCallback(async () => {
    try {
      const count = await contractAgreementService.getTotalCount();
      setTotalContracts(count);
    } catch (error) {
      console.error('Failed to load total contracts count:', error);
    }
  }, []);

  useEffect(() => {
    loadTotalContracts();
  }, [loadTotalContracts]);

  const handleRefresh = useCallback(() => {
    setSpinning(true);
    setLastUpdated(formatTimestamp(new Date()));
    setAnimateCounters(false);
    loadTotalContracts().finally(() => {
      requestAnimationFrame(() => setAnimateCounters(true));
      setTimeout(() => setSpinning(false), 650);
    });
  }, [loadTotalContracts]);

  const kpiCards = EXECUTIVE_KPI_CARDS.map((kpi) =>
    kpi.label === 'Total Contracts' && totalContracts !== null
      ? { ...kpi, value: totalContracts }
      : kpi
  );

  const tenderTotal = TENDER_STATS.reduce((s, d) => s + d.value, 0);
  const procurementTotal = PROCUREMENT_METHODS.reduce((s, d) => s + d.value, 0);

  const methodBarData = PROCUREMENT_METHODS.map((d) => ({
    name: d.label,
    value: d.value,
    fill: d.color,
  }));

  const budgetDonutData: ChartItem[] = [
    { label: 'Utilized', value: BUDGET_MONITORING.utilizedPct, color: '#16a34a' },
    { label: 'Remaining', value: BUDGET_MONITORING.remainingPct, color: '#e2e8f0' },
  ];

  return (
    <div className="exec-dashboard">
      <div className="status-strip">
        <span className="status-dot" />
        <span className="status-text">System Status: All Systems Operational</span>
        <span className="status-divider" />
        <span className="last-updated">
          Last Updated: <strong>{lastUpdated}</strong>
        </span>
        <button
          type="button"
          className={`refresh-btn${spinning ? ' spinning' : ''}`}
          onClick={handleRefresh}
          aria-label="Refresh dashboard"
        >
          <RefreshIcon sx={{ fontSize: 14 }} />
        </button>
      </div>

      <main className="content">
        {/* KPI Row */}
        <section className="kpi-grid">
          {kpiCards.map((kpi) => (
            <div key={kpi.label} className="kpi-card" style={{ '--accent': kpi.accent } as React.CSSProperties}>
              <div className="kpi-icon">{KPI_ICONS[kpi.icon]}</div>
              <div className="kpi-info">
                <span className="kpi-label">{kpi.label}</span>
                <AnimatedNumber
                  value={kpi.value}
                  prefix={'prefix' in kpi ? kpi.prefix : ''}
                  suffix={'suffix' in kpi ? kpi.suffix : ''}
                  animate={animateCounters}
                  className="kpi-value"
                />
                <span className="kpi-sub">{kpi.sub}</span>
              </div>
            </div>
          ))}
        </section>

        {/* Row 2 */}
        <section className="grid-3">
          <div className="panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><AppIcon /></span>
                APP Progress
                <span className="panel-tag">Annual Procurement Plan</span>
              </h2>
              <button type="button" className="view-all">
                View All <ArrowIcon sx={{ fontSize: 12 }} />
              </button>
            </div>
            <div className="app-progress-list">
              {APP_PROGRESS_ROWS.map((row) => (
                <div key={row.label} className="progress-row">
                  <span className="progress-label">{row.label}</span>
                  <div className="progress-track">
                    <div
                      className="progress-fill"
                      style={{ width: `${row.pct}%`, background: row.color }}
                    />
                  </div>
                  <span className="progress-value">{row.value}</span>
                </div>
              ))}
            </div>
            <div className="app-completion">
              <CompletionRing pct={APP_COMPLETION.pct} />
              <div className="completion-meta">
                <span className="trend-up">
                  <TrendUpIcon sx={{ fontSize: 14 }} />
                  {APP_COMPLETION.trend}
                </span>
                <span className="target-text">Target: {APP_COMPLETION.target}</span>
              </div>
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><TenderIcon /></span>
                Tender Statistics
              </h2>
              <button type="button" className="view-all">
                View All <ArrowIcon sx={{ fontSize: 12 }} />
              </button>
            </div>
            <div className="donut-wrap">
              <DonutChart data={[...TENDER_STATS]} />
              <div className="donut-center">
                <span className="donut-value">{tenderTotal}</span>
                <span className="donut-label">Total Tenders</span>
              </div>
            </div>
            <ChartLegend data={[...TENDER_STATS]} />
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><CartIcon /></span>
                Procurement Analytics
              </h2>
              <button type="button" className="view-all">
                View All <ArrowIcon sx={{ fontSize: 12 }} />
              </button>
            </div>
            <p className="chart-subtitle">Procurement Nature</p>
            <div className="dual-chart-top">
              <div className="mini-donut-wrap">
                <DonutChart data={[...PROCUREMENT_NATURE]} innerRadius="65%" />
              </div>
              <ul className="legend-list compact">
                {[...PROCUREMENT_NATURE].map((d) => (
                  <li key={d.label}>
                    <span className="legend-name">
                      <i className="dot" style={{ background: d.color }} />
                      {d.label}
                    </span>
                    <span className="legend-value">{d.value}</span>
                  </li>
                ))}
              </ul>
            </div>
            <p className="chart-subtitle">Procurement Method / Type</p>
            <div className="bar-chart-wrap">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={methodBarData} margin={{ top: 4, right: 4, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="0" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                  <YAxis tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                  <Tooltip />
                  <Bar dataKey="value" radius={[6, 6, 0, 0]} maxBarSize={34} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="total-line">
              Total: <strong>{procurementTotal}</strong>
            </div>
          </div>
        </section>

        {/* Row 3 */}
        <section className="grid-3">
          <div className="panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><BalanceIcon /></span>
                Budget Monitoring
              </h2>
            </div>
            <div className="split-panel">
              <div className="metric-list">
                <div className="metric-row">
                  <span>Total Allocation</span>
                  <strong>{BUDGET_MONITORING.totalAllocation}</strong>
                </div>
                <div className="metric-row">
                  <span>Released Budget</span>
                  <strong className="text-blue">{BUDGET_MONITORING.releasedBudget}</strong>
                </div>
                <div className="metric-row">
                  <span>Expenditure</span>
                  <strong className="text-red">{BUDGET_MONITORING.expenditure}</strong>
                </div>
                <div className="metric-row">
                  <span>Remaining Budget</span>
                  <strong className="text-green">{BUDGET_MONITORING.remainingBudget}</strong>
                </div>
              </div>
              <div className="mini-donut-wrap small">
                <DonutChart data={budgetDonutData} innerRadius="78%" />
                <div className="donut-center small">
                  <span className="donut-value">{BUDGET_MONITORING.utilizationPct}%</span>
                  <span className="donut-label">Utilization</span>
                </div>
              </div>
            </div>
            <div className="legend-inline">
              <span>
                <i className="dot" style={{ background: '#16a34a' }} />
                Utilized ({BUDGET_MONITORING.utilizedPct}%)
              </span>
              <span>
                <i className="dot" style={{ background: '#e2e8f0' }} />
                Remaining ({BUDGET_MONITORING.remainingPct}%)
              </span>
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><SecurityIcon /></span>
                Performance Security
              </h2>
            </div>
            <div className="status-list">
              {PERFORMANCE_SECURITY.map((row) => (
                <div key={row.name} className="status-row">
                  <span className={`status-icon ${row.variant}`}>
                    {row.variant === 'good' && row.name === 'Renewed'
                      ? <RenewIcon sx={{ fontSize: 14 }} />
                      : PS_ICONS[row.variant]}
                  </span>
                  <span className="status-name">{row.name}</span>
                  <span className="status-count">{row.count.toLocaleString()}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><DocIcon /></span>
                Document Analytics
              </h2>
              <button type="button" className="view-all">
                View All <ArrowIcon sx={{ fontSize: 12 }} />
              </button>
            </div>
            <div className="metric-list roomy">
              {DOCUMENT_ANALYTICS.map((row) => (
                <div key={row.label} className="metric-row">
                  <span>{row.label}</span>
                  <strong className={row.className || undefined}>
                    <AnimatedNumber value={row.value} animate={animateCounters} />
                  </strong>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Row 4 */}
        <section className="grid-2">
          <div className="panel search-panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><BrainIcon /></span>
                Enterprise Search
              </h2>
            </div>
            <div className="ai-search-box">
              <SearchIcon sx={{ color: '#64748b', fontSize: 18 }} />
              <input
                type="text"
                placeholder="Search any word inside millions of procurement documents..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              <button type="button" className="ai-search-btn" aria-label="AI search">
                <SparkleIcon sx={{ fontSize: 16 }} />
              </button>
            </div>
            <p className="ai-powered-tag">
              <ChipIcon sx={{ fontSize: 14 }} />
              Powered by AI OCR + Elasticsearch
            </p>
            <div className="search-chips">
              {SEARCH_CHIPS.map((chip) => (
                <button
                  key={chip}
                  type="button"
                  className="chip"
                  onClick={() => setSearchQuery(chip)}
                >
                  {chip}
                </button>
              ))}
            </div>
            <div className="search-stat-row">
              {SEARCH_STATS.map((stat) => (
                <div key={stat.label} className="search-stat">
                  <span>{stat.value}</span>
                  <small>{stat.label}</small>
                </div>
              ))}
            </div>
          </div>

          <div className="panel">
            <div className="panel-head">
              <h2>
                <span className="panel-icon"><BellIcon /></span>
                Recent Alerts
              </h2>
              <button type="button" className="view-all">
                View All Alerts <ArrowIcon sx={{ fontSize: 12 }} />
              </button>
            </div>
            <div className="alert-list">
              {RECENT_ALERTS.map((alert) => (
                <div key={alert.title} className={`alert-card ${alert.variant}`}>
                  <span className="alert-icon">{ALERT_ICONS[alert.icon]}</span>
                  <div className="alert-body">
                    <p className="alert-title">{alert.title}</p>
                    <p className="alert-desc">{alert.desc}</p>
                  </div>
                  <span className="alert-count">{alert.count}</span>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
    </div>
  );
};

export default ExecutiveDashboard;
