export interface Permission {
  id: number;
  name: string;
  displayName?: string;
  description?: string;
  resource?: string;
  action?: string;
}

export interface Role {
  id: number;
  name: string;
  displayName?: string;
  description?: string;
  isActive?: boolean;
  permissions: Permission[];
}

export interface UserDetail {
  id: number;
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role?: Role;
  department?: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

