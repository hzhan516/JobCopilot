import { create } from 'zustand';
import type { ResumeGroup, ResumeVersion, UploadResponse } from '../types/resume';
import { resumeService } from '../services/resumeService';
import type {
  ResumeGroup as ApiResumeGroup,
  ResumeVersion as ApiResumeVersion,
} from '../types';

function mapSummaryToVersion(
  summary: ApiResumeGroup['originalVersion'],
  groupId: string,
  versionType: ResumeVersion['versionType']
): ResumeVersion | null {
  if (!summary || !summary.exists || !summary.versionId) {
    return null;
  }

  return {
    versionId: summary.versionId,
    groupId,
    versionType,
    status: 'ACTIVE',
    storagePath: '',
    content: undefined,
    parsedContent: undefined,
    parseStatus: summary.status as ResumeVersion['parseStatus'],
    parseErrorMessage: undefined,
    createdAt: summary.createdAt,
  };
}

function adaptVersion(version: ApiResumeVersion): ResumeVersion {
  return {
    versionId: version.versionId,
    groupId: version.groupId,
    versionType: version.versionType as ResumeVersion['versionType'],
    status: 'ACTIVE',
    storagePath: '',
    content: version.content ?? undefined,
    parsedContent: undefined,
    parseStatus: version.status as ResumeVersion['parseStatus'],
    parseErrorMessage: undefined,
    createdAt: version.createdAt,
  };
}

function adaptGroup(group: ApiResumeGroup, versions: ResumeVersion[] = []): ResumeGroup {
  const fallbackVersions = [
    mapSummaryToVersion(group.originalVersion, group.groupId, 'ORIGINAL'),
    mapSummaryToVersion(group.convertedVersion, group.groupId, 'CONVERTED'),
    mapSummaryToVersion(group.aiOptimizedVersion, group.groupId, 'AI_OPTIMIZED'),
  ].filter((version): version is ResumeVersion => version !== null);

  return {
    groupId: group.groupId,
    title: group.title,
    isDefault: group.isDefault,
    createdAt: group.createdAt,
    updatedAt: group.updatedAt,
    versions: versions.length > 0 ? versions : fallbackVersions,
  };
}

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
  createVersion: (groupId: string, sourceVersionId?: string) => Promise<string>;
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
      const data = await resumeService.getResumeGroups();
      set({ groups: data.map((group) => adaptGroup(group)) });
    } finally {
      set({ loading: false });
    }
  },

  fetchGroupDetail: async (groupId: string) => {
    set({ loading: true });
    try {
      const [group, versions] = await Promise.all([
        resumeService.getResumeGroup(groupId),
        resumeService.getVersionsByGroup(groupId),
      ]);
      set({ currentGroup: adaptGroup(group, versions.map(adaptVersion)) });
    } finally {
      set({ loading: false });
    }
  },

  uploadResume: async (file: File, title?: string) => {
    set({ loading: true, uploadProgress: 0 });
    try {
      const data = await resumeService.uploadResume(file, title);
      set({ uploadProgress: 100 });
      return data;
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
          const data = await resumeService.getResumeGroup(groupId);
          const adaptedGroup = adaptGroup(data);
          set({ currentGroup: adaptedGroup });

          const originalVersion = adaptedGroup.versions.find((v) => v.versionType === 'ORIGINAL');
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
      await resumeService.editVersion(versionId, content);
      const { currentGroup } = get();
      if (currentGroup) {
        await get().fetchGroupDetail(currentGroup.groupId);
      }
    } finally {
      set({ loading: false });
    }
  },

  createVersion: async (groupId: string, sourceVersionId?: string) => {
    set({ loading: true });
    try {
      const newVersion = await resumeService.createVersion(groupId, sourceVersionId);
      await get().fetchGroupDetail(groupId);
      return newVersion.versionId;
    } finally {
      set({ loading: false });
    }
  },

  deleteGroup: async (groupId: string) => {
    set({ loading: true });
    try {
      await resumeService.deleteResumeGroup(groupId);
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
      await resumeService.deleteVersion(versionId);
      const { currentGroup } = get();
      if (currentGroup) {
        await get().fetchGroupDetail(currentGroup.groupId);
      }
    } finally {
      set({ loading: false });
    }
  }
}));
