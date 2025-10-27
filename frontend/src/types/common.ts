// Common types used across the application

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  department?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}

export interface ApiResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface Notification {
  id: number;
  title: string;
  message: string;
  type: string;
  priority: string;
  isRead: boolean;
  createdAt: string;
  userId?: number;
}

export interface Workflow {
  id: number;
  name: string;
  description: string;
  type: string;
  status: string;
  isActive: boolean;
  createdAt: string;
  createdBy: string;
}

export interface WorkflowInstance {
  id: number;
  workflowId: number;
  status: string;
  initiatedBy: string;
  startedAt: string;
  completedAt?: string;
  currentStep: number;
  totalSteps: number;
}

export interface DocumentVersion {
  id: number;
  documentId: number;
  versionNumber: number;
  fileName: string;
  filePath: string;
  fileSize: number;
  uploadedBy: string;
  uploadedAt: string;
  changeDescription: string;
  isActive: boolean;
}

export interface DocumentComment {
  id: number;
  documentId: number;
  content: string;
  userId: number;
  userName: string;
  createdAt: string;
  updatedAt: string;
  isResolved: boolean;
}

export interface DocumentAnnotation {
  id: number;
  documentId: number;
  content: string;
  userId: number;
  userName: string;
  x: number;
  y: number;
  page: number;
  createdAt: string;
  updatedAt: string;
}

export interface Report {
  id: number;
  name: string;
  description: string;
  type: string;
  status: string;
  generatedBy: string;
  generatedAt: string;
  filePath: string;
  parameters: string;
}

export interface IntegrationConfig {
  id: number;
  integrationName: string;
  integrationType: string;
  endpointUrl: string;
  authenticationType: string;
  status: string;
  environment: string;
  isEnabled: boolean;
  lastSyncAt?: string;
  createdAt: string;
}

export interface MLModel {
  id: number;
  modelName: string;
  modelType: string;
  modelDescription: string;
  modelVersion: string;
  status: string;
  accuracy: number;
  deploymentStatus: string;
  isActive: boolean;
  createdAt: string;
}

export interface SystemHealthCheck {
  id: number;
  checkName: string;
  checkType: string;
  status: string;
  component: string;
  service: string;
  severity: string;
  responseTimeMs: number;
  executedAt: string;
  isEnabled: boolean;
}

export interface Tenant {
  id: number;
  tenantName: string;
  tenantCode: string;
  domain: string;
  subdomain: string;
  status: string;
  plan: string;
  maxUsers: number;
  currentUsers: number;
  maxStorageGb: number;
  currentStorageBytes: number;
  isActive: boolean;
  createdAt: string;
}

export interface BackupRecord {
  id: number;
  backupType: string;
  status: string;
  backupPath: string;
  sizeBytes: number;
  startedAt: string;
  completedAt: string;
  retentionUntil: string;
  createdAt: string;
}

export interface OptimizationTask {
  id: number;
  optimizationType: string;
  description: string;
  status: string;
  executionTimeMs: number;
  createdAt: string;
  startedAt: string;
  completedAt: string;
}

export interface DashboardStats {
  totalDocuments: number;
  totalUsers: number;
  totalStorageUsed: number;
  recentUploads: number;
  activeWorkflows: number;
  pendingNotifications: number;
  systemHealthScore: number;
}

export interface SearchResult {
  id: number;
  fileName: string;
  documentType: string;
  department: string;
  uploadedBy: string;
  uploadedAt: string;
  content: string;
  score: number;
  highlights: string[];
}

export interface FileUploadProgress {
  fileName: string;
  progress: number;
  status: 'uploading' | 'processing' | 'completed' | 'error';
  error?: string;
}

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

export interface FilterParams {
  searchTerm?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  type?: string;
  department?: string;
  userId?: number;
}

export interface ChartData {
  labels: string[];
  datasets: {
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string | string[];
    borderWidth?: number;
  }[];
}

export interface TableColumn {
  id: string;
  label: string;
  minWidth?: number;
  align?: 'right' | 'left' | 'center';
  format?: (value: any) => string;
}

export interface MenuItem {
  id: string;
  label: string;
  icon?: React.ReactNode;
  path?: string;
  children?: MenuItem[];
  permissions?: string[];
}

export interface BreadcrumbItem {
  label: string;
  path?: string;
  icon?: React.ReactNode;
}

export interface FormField {
  name: string;
  label: string;
  type: 'text' | 'email' | 'password' | 'number' | 'select' | 'multiselect' | 'textarea' | 'date' | 'file';
  required?: boolean;
  options?: { value: string; label: string }[];
  validation?: {
    min?: number;
    max?: number;
    pattern?: string;
    message?: string;
  };
}

export interface ErrorResponse {
  error: string;
  message: string;
  status: number;
  timestamp: string;
  path: string;
}

export interface SuccessResponse {
  message: string;
  data?: any;
  timestamp: string;
}
