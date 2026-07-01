import { create } from 'zustand';
import { adminService, type AdminUser, type SystemStats, type ComponentHealth, type AuditLog } from '@/services/adminService';

interface AdminState {
  users: AdminUser[];
  userPage: { totalElements: number; totalPages: number };
  userFilters: { page: number; size: number; role?: string; status?: string };
  stats: SystemStats | null;
  health: ComponentHealth | null;
  auditLogs: AuditLog[];
  loading: boolean;
  statsLoading: boolean;

  fetchUsers: () => Promise<void>;
  fetchStats: () => Promise<void>;
  fetchHealth: () => Promise<void>;
  setUserFilters: (f: Partial<AdminState['userFilters']>) => void;
  updateUserRole: (id: string, role: string) => Promise<void>;
  updateUserStatus: (id: string, status: string) => Promise<void>;
  deleteUser: (id: string) => Promise<void>;
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
}));
