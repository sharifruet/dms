// Application constants and configuration

export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

export const ROUTES = {
  LOGIN: '/login',
  DASHBOARD: '/dashboard',
  DOCUMENTS: '/documents',
  USERS: '/users',
  SEARCH: '/search',
  NOTIFICATIONS: '/notifications',
  EXPIRY_TRACKING: '/expiry-tracking',
  REPORTS: '/reports',
  ANALYTICS: '/analytics',
  WORKFLOWS: '/workflows',
  VERSIONING: '/versioning',
  INTEGRATIONS: '/integrations',
  ML_MODELS: '/ml',
  HEALTH: '/health'
} as const;

export const USER_ROLES = {
  ADMIN: 'ADMIN',
  OFFICER: 'OFFICER',
  VIEWER: 'VIEWER',
  AUDITOR: 'AUDITOR'
} as const;

export const DOCUMENT_TYPES = {
  PDF: 'PDF',
  DOCX: 'DOCX',
  DOC: 'DOC',
  TXT: 'TXT',
  JPG: 'JPG',
  PNG: 'PNG'
} as const;

export const NOTIFICATION_TYPES = {
  SYSTEM_ALERT: 'SYSTEM_ALERT',
  DOCUMENT_UPLOAD: 'DOCUMENT_UPLOAD',
  DOCUMENT_EXPIRY: 'DOCUMENT_EXPIRY',
  WORKFLOW_UPDATE: 'WORKFLOW_UPDATE',
  USER_MENTION: 'USER_MENTION',
  COMMENT_REPLY: 'COMMENT_REPLY'
} as const;

export const NOTIFICATION_PRIORITIES = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  CRITICAL: 'CRITICAL'
} as const;

export const WORKFLOW_STATUS = {
  PENDING: 'PENDING',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  REJECTED: 'REJECTED',
  CANCELLED: 'CANCELLED'
} as const;

export const INTEGRATION_TYPES = {
  LDAP: 'LDAP',
  SAP: 'SAP',
  SALESFORCE: 'SALESFORCE',
  MICROSOFT_GRAPH: 'MICROSOFT_GRAPH',
  SLACK: 'SLACK',
  WEBHOOK: 'WEBHOOK',
  REST_API: 'REST_API',
  SOAP: 'SOAP',
  DATABASE: 'DATABASE',
  FILE_SYSTEM: 'FILE_SYSTEM'
} as const;

export const ML_MODEL_TYPES = {
  CLASSIFICATION: 'CLASSIFICATION',
  REGRESSION: 'REGRESSION',
  CLUSTERING: 'CLUSTERING',
  ANOMALY_DETECTION: 'ANOMALY_DETECTION',
  RECOMMENDATION: 'RECOMMENDATION',
  NATURAL_LANGUAGE_PROCESSING: 'NATURAL_LANGUAGE_PROCESSING',
  COMPUTER_VISION: 'COMPUTER_VISION',
  TIME_SERIES: 'TIME_SERIES',
  DEEP_LEARNING: 'DEEP_LEARNING',
  ENSEMBLE: 'ENSEMBLE'
} as const;

export const HEALTH_CHECK_TYPES = {
  DATABASE_CONNECTION: 'DATABASE_CONNECTION',
  REDIS_CONNECTION: 'REDIS_CONNECTION',
  ELASTICSEARCH_CONNECTION: 'ELASTICSEARCH_CONNECTION',
  FILE_SYSTEM: 'FILE_SYSTEM',
  MEMORY_USAGE: 'MEMORY_USAGE',
  CPU_USAGE: 'CPU_USAGE',
  DISK_SPACE: 'DISK_SPACE',
  NETWORK_CONNECTIVITY: 'NETWORK_CONNECTIVITY',
  API_RESPONSE_TIME: 'API_RESPONSE_TIME',
  SERVICE_AVAILABILITY: 'SERVICE_AVAILABILITY'
} as const;

export const TENANT_PLANS = {
  FREE: 'FREE',
  BASIC: 'BASIC',
  PROFESSIONAL: 'PROFESSIONAL',
  ENTERPRISE: 'ENTERPRISE',
  CUSTOM: 'CUSTOM'
} as const;

export const TENANT_STATUS = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
  SUSPENDED: 'SUSPENDED',
  TRIAL: 'TRIAL',
  EXPIRED: 'EXPIRED',
  CANCELLED: 'CANCELLED'
} as const;

export const BACKUP_TYPES = {
  FULL: 'FULL',
  INCREMENTAL: 'INCREMENTAL',
  DIFFERENTIAL: 'DIFFERENTIAL',
  SNAPSHOT: 'SNAPSHOT'
} as const;

export const OPTIMIZATION_TYPES = {
  DATABASE: 'DATABASE',
  CACHE: 'CACHE',
  FILE_SYSTEM: 'FILE_SYSTEM',
  MEMORY: 'MEMORY',
  SEARCH: 'SEARCH',
  NETWORK: 'NETWORK',
  CPU: 'CPU',
  STORAGE: 'STORAGE'
} as const;

export const DEPARTMENTS = [
  'IT',
  'HR',
  'Finance',
  'Operations',
  'Legal',
  'Marketing',
  'Sales',
  'Customer Service',
  'Research & Development',
  'Quality Assurance'
] as const;

export const FILE_UPLOAD_LIMITS = {
  MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
  ALLOWED_TYPES: [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain',
    'image/jpeg',
    'image/png',
    'image/gif'
  ],
  ALLOWED_EXTENSIONS: ['.pdf', '.doc', '.docx', '.txt', '.jpg', '.jpeg', '.png', '.gif']
} as const;

export const PAGINATION_DEFAULTS = {
  PAGE_SIZE: 10,
  PAGE_SIZE_OPTIONS: [5, 10, 25, 50, 100]
} as const;

export const DATE_FORMATS = {
  DISPLAY: 'MMM dd, yyyy',
  DISPLAY_WITH_TIME: 'MMM dd, yyyy HH:mm',
  API: 'yyyy-MM-dd',
  API_WITH_TIME: 'yyyy-MM-dd HH:mm:ss'
} as const;

export const COLORS = {
  PRIMARY: '#1976d2',
  SECONDARY: '#dc004e',
  SUCCESS: '#2e7d32',
  WARNING: '#ed6c02',
  ERROR: '#d32f2f',
  INFO: '#0288d1',
  BACKGROUND: '#f5f5f5',
  SURFACE: '#ffffff',
  TEXT_PRIMARY: '#212121',
  TEXT_SECONDARY: '#757575'
} as const;

export const BREAKPOINTS = {
  XS: 0,
  SM: 600,
  MD: 900,
  LG: 1200,
  XL: 1536
} as const;

export const ANIMATION_DURATIONS = {
  SHORT: 150,
  MEDIUM: 300,
  LONG: 500
} as const;

export const STORAGE_KEYS = {
  TOKEN: 'token',
  USER: 'user',
  THEME: 'theme',
  LANGUAGE: 'language',
  SIDEBAR_COLLAPSED: 'sidebarCollapsed'
} as const;

export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  INTERNAL_SERVER_ERROR: 500,
  SERVICE_UNAVAILABLE: 503
} as const;

export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Network error. Please check your connection.',
  UNAUTHORIZED: 'You are not authorized to perform this action.',
  FORBIDDEN: 'Access denied.',
  NOT_FOUND: 'The requested resource was not found.',
  SERVER_ERROR: 'An internal server error occurred.',
  VALIDATION_ERROR: 'Please check your input and try again.',
  FILE_TOO_LARGE: 'File size exceeds the maximum allowed limit.',
  INVALID_FILE_TYPE: 'Invalid file type. Please select a supported file format.',
  UPLOAD_FAILED: 'File upload failed. Please try again.',
  GENERIC_ERROR: 'An unexpected error occurred. Please try again.'
} as const;

export const SUCCESS_MESSAGES = {
  UPLOAD_SUCCESS: 'File uploaded successfully.',
  UPDATE_SUCCESS: 'Updated successfully.',
  DELETE_SUCCESS: 'Deleted successfully.',
  SAVE_SUCCESS: 'Saved successfully.',
  COPY_SUCCESS: 'Copied to clipboard.',
  DOWNLOAD_SUCCESS: 'Download started.',
  EXPORT_SUCCESS: 'Export completed.',
  IMPORT_SUCCESS: 'Import completed.'
} as const;

export const VALIDATION_RULES = {
  USERNAME: {
    MIN_LENGTH: 3,
    MAX_LENGTH: 50,
    PATTERN: /^[a-zA-Z0-9_]+$/
  },
  PASSWORD: {
    MIN_LENGTH: 8,
    MAX_LENGTH: 128,
    REQUIRE_UPPERCASE: true,
    REQUIRE_LOWERCASE: true,
    REQUIRE_NUMBER: true,
    REQUIRE_SPECIAL_CHAR: true
  },
  EMAIL: {
    PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  },
  PHONE: {
    PATTERN: /^[\+]?[1-9][\d]{0,15}$/
  }
} as const;

export const FEATURE_FLAGS = {
  ENABLE_NOTIFICATIONS: true,
  ENABLE_ANALYTICS: true,
  ENABLE_ML_MODELS: true,
  ENABLE_INTEGRATIONS: true,
  ENABLE_HEALTH_MONITORING: true,
  ENABLE_MULTI_TENANCY: true,
  ENABLE_OPTIMIZATION: true,
  ENABLE_DISASTER_RECOVERY: true
} as const;
