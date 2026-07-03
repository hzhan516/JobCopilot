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

export interface DynamicConfig {
  key: string;
  value: string;
  defaultValue: string;
  description: string;
  category: string;
  valueType: 'BOOLEAN' | 'NUMBER' | 'STRING' | 'JSON';
  sensitive: boolean;
  readOnly: boolean;
  updatedBy: string | null;
  updatedAt: string | null;
}

export interface QueueStats {
  queues: Record<string, { depth: number | string; consumers: number; error?: string }>;
}

export interface AIStatus {
  service: string;
  version: string;
  uptime_seconds: number;
  model_version?: string;
  model_trained_at?: string;
  mq_connected: boolean;
}

export interface ModelInfo {
  loaded: boolean;
  version?: string;
  trained_at?: string;
  metrics?: Record<string, number>;
  filename?: string;
  message?: string;
}

export interface ModelVersion {
  key: string;
  size: number;
  last_modified: string;
  version: string;
}

export interface RetrainResult {
  status: string;
  new_version?: string;
  message?: string;
  metrics?: Record<string, number>;
  samples_used?: number;
}

export interface PurgeResult {
  status: string;
  queue: string;
  messages_removed: number;
}

export interface RetryResult {
  status: string;
  queue: string;
  messages_retried: number;
}

interface VersionInfo {
  version: string; component: string;
}

function toConfigValue(value: string | number | boolean): string {
  if (typeof value === 'boolean') return value ? 'true' : 'false';
  return String(value);
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

  // ─── Config ───
  getConfigs: async () => {
    const res = await apiClient.get<ApiResponse<DynamicConfig[]>>('/api/admin/v1/config');
    return res.data.data;
  },
  getConfig: async (key: string) => {
    const res = await apiClient.get<ApiResponse<DynamicConfig>>(`/api/admin/v1/config/${key}`);
    return res.data.data;
  },
  updateConfig: async (key: string, value: string | number | boolean) => {
    const res = await apiClient.put<ApiResponse<DynamicConfig>>(`/api/admin/v1/config/${key}`, { value: toConfigValue(value) });
    return res.data.data;
  },
  resetConfig: async (key: string) => {
    const res = await apiClient.post<ApiResponse<DynamicConfig>>(`/api/admin/v1/config/${key}/reset`);
    return res.data.data;
  },

  // ─── Monitoring (proxied to AI service) ───
  getQueueStats: async () => {
    const res = await apiClient.get<QueueStats>('/api/admin/v1/monitoring/queues');
    return res.data;
  },
  purgeQueue: async (queueName: string) => {
    const res = await apiClient.post<PurgeResult>(`/api/admin/v1/monitoring/queues/${queueName}/purge`);
    return res.data;
  },
  retryDlq: async (queueName: string) => {
    const res = await apiClient.post<RetryResult>(`/api/admin/v1/monitoring/queues/${queueName}/retry-dlq`);
    return res.data;
  },

  // ─── AI Service (proxied to AI service) ───
  getAIStatus: async () => {
    const res = await apiClient.get<AIStatus>('/api/admin/v1/ai/status');
    return res.data;
  },
  getModelInfo: async () => {
    const res = await apiClient.get<ModelInfo>('/api/admin/v1/ai/model/info');
    return res.data;
  },
  getModelHistory: async () => {
    const res = await apiClient.get<{ versions: ModelVersion[] }>('/api/admin/v1/ai/model/history');
    return res.data;
  },
  triggerRetrain: async () => {
    const res = await apiClient.post<RetrainResult>('/api/admin/v1/ai/model/retrain');
    return res.data;
  },
  rollbackModel: async (version: string) => {
    const res = await apiClient.post<{ status: string; version: string; message?: string }>('/api/admin/v1/ai/model/rollback', { version });
    return res.data;
  },
  flushAICache: async () => {
    const res = await apiClient.post<{ status: string; keys_deleted: number }>('/api/admin/v1/ai/cache/flush');
    return res.data;
  },
};
