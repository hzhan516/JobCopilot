import { create } from 'zustand';
import type { ResumeGroup, UploadResponse } from '../types/resume';
import { resumeApi } from '../services/resume.api';

interface ResumeStore {
  groups: ResumeGroup[];
  currentGroup: ResumeGroup | null;
  loading: boolean;
  uploadProgress: number;
  
  fetchGroups: () => Promise<void>;
  fetchGroupDetail: (groupId: string) => Promise<void>;
  uploadResume: (file: File, title?: string) => Promise<UploadResponse['data']>;
  pollParseStatus: (groupId: string) => Promise<'COMPLETED' | 'FAILED' | 'TIMEOUT'>;
  saveVersion: (versionId: string, content: string) => Promise<void>;
  deleteGroup: (groupId: string) => Promise<void>;
  deleteVersion: (versionId: string) => Promise<void>;
}

export const useResumeStore = create<ResumeStore>((set, get) => ({
  groups: [],
  currentGroup: null,
  loading: false,
  uploadProgress: 0,

  fetchGroups: async () => {
    set({ loading: true });
    try {
      const { data } = await resumeApi.getGroups();
      set({ groups: data });
    } finally {
      set({ loading: false });
    }
  },

  fetchGroupDetail: async (groupId: string) => {
    set({ loading: true });
    try {
      const { data } = await resumeApi.getGroupDetail(groupId);
      set({ currentGroup: data });
    } finally {
      set({ loading: false });
    }
  },

  uploadResume: async (file: File, title?: string) => {
    set({ loading: true, uploadProgress: 0 });
    try {
      const data = await resumeApi.uploadResume(file, title);
      set({ uploadProgress: 100 });
      return data.data;
    } finally {
      set({ loading: false });
      setTimeout(() => set({ uploadProgress: 0 }), 1000);
    }
  },

  pollParseStatus: async (groupId: string) => {
    const maxRetries = 30;
    const interval = 2000;
    let attempts = 0;

    return new Promise((resolve) => {
      const poll = async () => {
        attempts++;
        try {
          const { data } = await resumeApi.getGroupDetail(groupId);
          set({ currentGroup: data });
          
          const originalVersion = data.versions.find(v => v.versionType === 'ORIGINAL');
          
          const status = originalVersion?.parseStatus;
          
          if (status === 'COMPLETED') {
            resolve('COMPLETED');
            return;
          } else if (status === 'FAILED') {
            resolve('FAILED');
            return;
          }
        } catch (error) {
          console.error(error);
        }

        if (attempts >= maxRetries) {
          resolve('TIMEOUT');
          return;
        }

        setTimeout(poll, interval);
      };

      poll();
    });
  },

  saveVersion: async (versionId: string, content: string) => {
    set({ loading: true });
    try {
      await resumeApi.updateVersion(versionId, content);
      const { currentGroup } = get();
      if (currentGroup) {
        await get().fetchGroupDetail(currentGroup.groupId);
      }
    } finally {
      set({ loading: false });
    }
  },

  deleteGroup: async (groupId: string) => {
    set({ loading: true });
    try {
      await resumeApi.deleteGroup(groupId);
      const { groups, currentGroup } = get();
      set({ 
        groups: groups.filter(g => g.groupId !== groupId),
        currentGroup: currentGroup?.groupId === groupId ? null : currentGroup
      });
    } finally {
      set({ loading: false });
    }
  },

  deleteVersion: async (versionId: string) => {
    set({ loading: true });
    try {
      await resumeApi.deleteVersion(versionId);
      const { currentGroup } = get();
      if (currentGroup) {
        await get().fetchGroupDetail(currentGroup.groupId);
      }
    } finally {
      set({ loading: false });
    }
  }
}));
