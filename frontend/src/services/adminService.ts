import apiClient from './api';
import type { ApiResponse } from '@/types';

export interface AdminUser {
  id: string; email: string; role: string; status: string;
  authProvider: string; emailVerified: boolean;
  resumeCount: number; jobCount: number; conversationCount: number;
  createdAt: string; updatedAt: string;
}

export interface SystemStats {
  userCount: number; resumeCount: number; jobCount: number;
  conversationCount: number; aiCallCount: number; applicationCount: number;
}

export interface ComponentHealth {
  postgres: boolean; redis: boolean; rabbitmq: boolean;
  aiService: boolean; minio: boolean;
}

export interface AuditLog {
  id: string; adminUserId: string; action: string;
  targetType: string; targetId: string; details: string;
  ipAddress: string; createdAt: string;
}

export interface PageResult<T> {
  content: T[]; page: number; size: number;
  totalElements: number; totalPages: number;
}

interface VersionInfo {
  version: string; component: string;
}

export const adminService = {
  // ─── Users ───
  listUsers: async (params: { role?: string; status?: string; email?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PageResult<AdminUser>>>('/api/admin/v1/users', { params });
    return res.data.data;
  },
  getUserDetail: async (id: string) => {
    const res = await apiClient.get<ApiResponse<AdminUser>>(`/api/admin/v1/users/${id}`);
    return res.data.data;
  },
  updateUserRole: async (id: string, role: string) => {
    const res = await apiClient.put<ApiResponse<AdminUser>>(`/api/admin/v1/users/${id}/role`, { role });
    return res.data.data;
  },
  updateUserStatus: async (id: string, status: string) => {
    const res = await apiClient.put<ApiResponse<AdminUser>>(`/api/admin/v1/users/${id}/status`, { status });
    return res.data.data;
  },
  deleteUser: async (id: string) => {
    await apiClient.delete(`/api/admin/v1/users/${id}`);
  },

  // ─── System ───
  getStats: async () => {
    const res = await apiClient.get<ApiResponse<SystemStats>>('/api/admin/v1/system/stats');
    return res.data.data;
  },
  getHealth: async () => {
    const res = await apiClient.get<ApiResponse<ComponentHealth>>('/api/admin/v1/system/health');
    return res.data.data;
  },
  getVersion: async () => {
    const res = await apiClient.get<ApiResponse<VersionInfo>>('/api/admin/v1/system/version');
    return res.data.data;
  },

  // ─── Audit Logs ───
  listAuditLogs: async (params: { adminUserId?: string; action?: string; page?: number; size?: number }) => {
    const res = await apiClient.get<ApiResponse<PageResult<AuditLog>>>('/api/admin/v1/audit-logs', { params });
    return res.data.data;
  },
};
