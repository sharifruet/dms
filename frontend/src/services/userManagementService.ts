import api from './api';
import { PaginatedResponse, Permission, Role, UserDetail } from '../types/userManagement';

export interface GetUsersParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
  search?: string;
}

export interface CreateUserPayload {
  username: string;
  email: string;
  password: string;
  firstName?: string;
  lastName?: string;
  department?: string;
  roleId: number;
}

export interface UpdateUserPayload {
  firstName?: string;
  lastName?: string;
  email?: string;
  department?: string;
  roleId?: number;
}

export interface ApiMessageResponse<T = unknown> {
  success: boolean;
  message?: string;
  user?: UserDetail;
  data?: T;
}

export interface UpdateRolePayload {
  displayName?: string;
  description?: string;
  isActive?: boolean;
}

const parsePaginatedUsers = (data: PaginatedResponse<UserDetail>): PaginatedResponse<UserDetail> => ({
  content: data.content ?? [],
  totalElements: data.totalElements ?? 0,
  totalPages: data.totalPages ?? 0,
  number: data.number ?? 0,
  size: data.size ?? 0,
});

export const userManagementService = {
  async getUsers(params: GetUsersParams = {}): Promise<PaginatedResponse<UserDetail>> {
    const response = await api.get<PaginatedResponse<UserDetail>>('/users', {
      params,
    });
    return parsePaginatedUsers(response.data);
  },

  async createUser(payload: CreateUserPayload): Promise<ApiMessageResponse<UserDetail>> {
    const response = await api.post<ApiMessageResponse<UserDetail>>('/users', payload);
    return response.data;
  },

  async updateUser(userId: number, payload: UpdateUserPayload): Promise<ApiMessageResponse<UserDetail>> {
    const response = await api.put<ApiMessageResponse<UserDetail>>(`/users/${userId}`, payload);
    return response.data;
  },

  async toggleUserStatus(userId: number): Promise<ApiMessageResponse> {
    const response = await api.put<ApiMessageResponse>(`/users/${userId}/toggle-status`);
    return response.data;
  },

  async getRoles(): Promise<Role[]> {
    const response = await api.get<Role[]>('/roles');
    return response.data;
  },

  async updateRole(roleId: number, payload: UpdateRolePayload): Promise<Role> {
    const response = await api.put<Role>(`/roles/${roleId}`, payload);
    return response.data;
  },

  async updateRolePermissions(roleId: number, permissionIds: number[]): Promise<Role> {
    const response = await api.put<Role>(`/roles/${roleId}/permissions`, {
      permissionIds,
    });
    return response.data;
  },

  async getPermissions(): Promise<Permission[]> {
    const response = await api.get<Permission[]>('/permissions');
    return response.data;
  },
};

