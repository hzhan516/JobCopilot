import { create } from 'zustand';
import {
  adminService,
  type AdminUser,
  type SystemStats,
  type ComponentHealth,
  type AuditLog,
  type DynamicConfig,
  type QueueStats,
  type AIStatus,
  type ModelInfo,
  type ModelVersion,
  type RetrainResult,
  type PurgeResult,
  type RetryResult,
} from '@/services/adminService';

interface AdminState {
  users: AdminUser[];
  userPage: { totalElements: number; totalPages: number };
  userFilters: { page: number; size: number; role?: string; status?: string };
  stats: SystemStats | null;
  health: ComponentHealth | null;
  auditLogs: AuditLog[];
  loading: boolean;
  statsLoading: boolean;

  // Config
  configs: DynamicConfig[];
  configsLoading: boolean;

  // Monitoring / AI Service
  queueStats: QueueStats | null;
  aiStatus: AIStatus | null;
  modelInfo: ModelInfo | null;
  modelHistory: ModelVersion[];
  modelActionLoading: boolean;
  cacheFlushLoading: boolean;

  fetchUsers: () => Promise<void>;
  fetchStats: () => Promise<void>;
  fetchHealth: () => Promise<void>;
  setUserFilters: (f: Partial<AdminState['userFilters']>) => void;
  updateUserRole: (id: string, role: string) => Promise<void>;
  updateUserStatus: (id: string, status: string) => Promise<void>;
  deleteUser: (id: string) => Promise<void>;

  fetchConfigs: () => Promise<void>;
  updateConfig: (key: string, value: string | number | boolean) => Promise<void>;
  resetConfig: (key: string) => Promise<void>;

  fetchQueueStats: () => Promise<void>;
  fetchAIStatus: () => Promise<void>;
  fetchModelInfo: () => Promise<void>;
  fetchModelHistory: () => Promise<void>;
  triggerRetrain: () => Promise<RetrainResult>;
  rollbackModel: (version: string) => Promise<void>;
  purgeQueue: (name: string) => Promise<PurgeResult>;
  retryDlq: (name: string) => Promise<RetryResult>;
  flushAICache: () => Promise<void>;
}

export const useAdminStore = create<AdminState>((set, get) => ({
  users: [],
  userPage: { totalElements: 0, totalPages: 0 },
  userFilters: { page: 0, size: 20 },
  stats: null,
  health: null,
  auditLogs: [],
  loading: false,
  statsLoading: false,

  configs: [],
  configsLoading: false,

  queueStats: null,
  aiStatus: null,
  modelInfo: null,
  modelHistory: [],
  modelActionLoading: false,
  cacheFlushLoading: false,

  fetchUsers: async () => {
    set({ loading: true });
    try {
      const { userFilters } = get();
      const result = await adminService.listUsers(userFilters);
      set({ users: result.content, userPage: { totalElements: result.totalElements, totalPages: result.totalPages } });
    } finally {
      set({ loading: false });
    }
  },

  fetchStats: async () => {
    set({ statsLoading: true });
    try {
      const [stats, health] = await Promise.all([adminService.getStats(), adminService.getHealth()]);
      set({ stats, health });
    } finally {
      set({ statsLoading: false });
    }
  },

  fetchHealth: async () => {
    const health = await adminService.getHealth();
    set({ health });
  },

  setUserFilters: (f) => {
    set((s) => ({ userFilters: { ...s.userFilters, ...f } }));
  },

  updateUserRole: async (id, role) => {
    await adminService.updateUserRole(id, role);
    get().fetchUsers();
  },

  updateUserStatus: async (id, status) => {
    await adminService.updateUserStatus(id, status);
    get().fetchUsers();
  },

  deleteUser: async (id) => {
    await adminService.deleteUser(id);
    get().fetchUsers();
  },

  fetchConfigs: async () => {
    set({ configsLoading: true });
    try {
      const configs = await adminService.getConfigs();
      set({ configs });
    } finally {
      set({ configsLoading: false });
    }
  },

  updateConfig: async (key, value) => {
    await adminService.updateConfig(key, value);
    await get().fetchConfigs();
  },

  resetConfig: async (key) => {
    await adminService.resetConfig(key);
    await get().fetchConfigs();
  },

  fetchQueueStats: async () => {
    const queueStats = await adminService.getQueueStats();
    set({ queueStats });
  },

  fetchAIStatus: async () => {
    const aiStatus = await adminService.getAIStatus();
    set({ aiStatus });
  },

  fetchModelInfo: async () => {
    const modelInfo = await adminService.getModelInfo();
    set({ modelInfo });
  },

  fetchModelHistory: async () => {
    const { versions } = await adminService.getModelHistory();
    set({ modelHistory: versions });
  },

  triggerRetrain: async () => {
    set({ modelActionLoading: true });
    try {
      const result = await adminService.triggerRetrain();
      await get().fetchModelInfo();
      await get().fetchModelHistory();
      return result;
    } finally {
      set({ modelActionLoading: false });
    }
  },

  rollbackModel: async (version) => {
    set({ modelActionLoading: true });
    try {
      await adminService.rollbackModel(version);
      await get().fetchModelInfo();
      await get().fetchModelHistory();
    } finally {
      set({ modelActionLoading: false });
    }
  },

  purgeQueue: async (name) => {
    const result = await adminService.purgeQueue(name);
    await get().fetchQueueStats();
    return result;
  },

  retryDlq: async (name) => {
    const result = await adminService.retryDlq(name);
    await get().fetchQueueStats();
    return result;
  },

  flushAICache: async () => {
    set({ cacheFlushLoading: true });
    try {
      await adminService.flushAICache();
    } finally {
      set({ cacheFlushLoading: false });
    }
  },
}));
