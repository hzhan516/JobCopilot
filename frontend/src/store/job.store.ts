import { create } from 'zustand';
import type { Job, JobScoreResponse, JobScoreHistoryResponse } from '@/types';
import { jobService } from '@/services/jobService';
import i18n from '@/i18n';
import { toast } from 'sonner';

interface JobFilters {
  searchQuery: string;
  sortBy: 'date' | 'status';
}

interface JobStore {
  jobs: Job[];
  filteredJobs: Job[];
  loading: boolean;
  error: string | null;

  filters: JobFilters;

  // Composite key format: `${jobId}_${resumeVersionId}`
  // 复合键格式：${jobId}_${resumeVersionId}
  scoreResults: Record<string, JobScoreResponse>;
  scoringState: Record<string, boolean>;
  selectedResumes: Record<string, string>;

  fetchJobs: () => Promise<void>;
  submitJob: (url: string, screenshot: File) => Promise<void>;
  deleteJob: (jobId: string) => Promise<void>;
  scoreJob: (jobId: string, resumeVersionId: string) => Promise<void>;
  loadScoreHistory: () => Promise<void>;
  setSearchQuery: (query: string) => void;
  setSortBy: (sortBy: 'date' | 'status') => void;
  setSelectedResume: (jobId: string, versionId: string) => void;
  resetError: () => void;
}

/**
 * Derives filtered and sorted results from the raw job list.
 * Performs case-insensitive matching across title, company, and description.
 *
 * 从原始职位列表派生筛选与排序结果，支持标题、公司、描述的不区分大小写匹配
 */
function applyFilters(jobs: Job[], filters: JobFilters): Job[] {
  let result = [...jobs];

  if (filters.searchQuery) {
    const query = filters.searchQuery.toLowerCase();
    result = result.filter((job) => {
      const title = job.parsedContent?.title?.toLowerCase() ?? '';
      const company = job.parsedContent?.company?.toLowerCase() ?? '';
      const desc = job.parsedContent?.description?.toLowerCase() ?? '';
      return title.includes(query) || company.includes(query) || desc.includes(query);
    });
  }

  switch (filters.sortBy) {
    case 'date':
      result.sort((a, b) => {
        const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return dateB - dateA;
      });
      break;
    case 'status':
      result.sort((a, b) => a.status.localeCompare(b.status));
      break;
  }

  return result;
}

/**
 * Hydrates scoreResults and selectedResumes from persisted history.
 * History is pre-sorted by createdAt descending, so the first record per jobId
 * represents the most recent scoring.
 *
 * 从历史记录恢复评分结果与选中简历。历史已按时间降序排列，
 * 每个职位的首条记录即为最新评分。
 */
function buildScoreStateFromHistory(
  history: JobScoreHistoryResponse[]
): Pick<JobStore, 'scoreResults' | 'selectedResumes'> {
  const scoreResults: Record<string, JobScoreResponse> = {};
  const selectedResumes: Record<string, string> = {};

  for (const record of history) {
    const key = `${record.jobId}_${record.resumeVersionId}`;
    scoreResults[key] = {
      suitable: record.suitable,
      summary: record.summary,
      finalScore: record.finalScore,
      breakdown: {
        skillScore: record.skillScore,
        experienceScore: record.experienceScore,
        overallScore: record.overallScore,
      },
    };
    // Use first-seen record per job as the default selected resume since history is sorted desc
    // 每个职位只设置一次：取该职位最新的评分简历（history 已按 createdAt 降序）
    if (!selectedResumes[record.jobId]) {
      selectedResumes[record.jobId] = record.resumeVersionId;
    }
  }

  return { scoreResults, selectedResumes };
}

function omitJobScoreState<T>(record: Record<string, T>, jobId: string): Record<string, T> {
  return Object.fromEntries(
    Object.entries(record).filter(([key]) => !key.startsWith(`${jobId}_`))
  ) as Record<string, T>;
}

export const useJobStore = create<JobStore>((set, get) => ({
  jobs: [],
  filteredJobs: [],
  loading: false,
  error: null,

  filters: {
    searchQuery: '',
    sortBy: 'date',
  },

  scoreResults: {},
  scoringState: {},
  selectedResumes: {},

  fetchJobs: async () => {
    set({ loading: true, error: null });
    try {
      const data = await jobService.getJobs();
      const { filters } = get();
      set({ jobs: data, filteredJobs: applyFilters(data, filters) });
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      set({ error: message });
      toast.error(i18n.t('jobList.loadError') ?? 'Failed to load jobs');
    } finally {
      set({ loading: false });
    }
  },

  submitJob: async (url: string, screenshot: File) => {
    set({ loading: true, error: null });
    try {
      await jobService.submitJob(url, screenshot);
      toast.success(i18n.t('jobList.submitSuccess') ?? 'Job submitted successfully');
      await get().fetchJobs();
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      set({ error: message });
      toast.error(i18n.t('jobList.submitError') ?? 'Failed to submit job');
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  deleteJob: async (jobId: string) => {
    set({ error: null });
    try {
      await jobService.deleteJob(jobId);
      const { filters } = get();
      set((state) => {
        const jobs = state.jobs.filter((job) => job.id !== jobId);
        const selectedResumes = { ...state.selectedResumes };
        delete selectedResumes[jobId];
        return {
          jobs,
          filteredJobs: applyFilters(jobs, filters),
          selectedResumes,
          scoreResults: omitJobScoreState(state.scoreResults, jobId),
          scoringState: omitJobScoreState(state.scoringState, jobId),
        };
      });
      toast.success(i18n.t('jobList.deleteSuccess') ?? 'Job deleted successfully');
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      set({ error: message });
      toast.error(i18n.t('jobList.deleteError') ?? 'Failed to delete job');
      throw error;
    }
  },

  scoreJob: async (jobId: string, resumeVersionId: string) => {
    const stateKey = `${jobId}_${resumeVersionId}`;
    set((state) => ({
      scoringState: { ...state.scoringState, [stateKey]: true },
    }));

    try {
      const result = await jobService.scoreJob(jobId, { resumeVersionId });
      set((state) => ({
        scoreResults: { ...state.scoreResults, [stateKey]: result },
      }));
      toast.success(i18n.t('jobList.scoreSuccess') ?? 'Score completed');
    } catch (error) {
      const err = error as { response?: { status?: number; data?: { message?: string } } };
      if (err.response?.status === 503) {
        toast.warning(
          err.response?.data?.message ?? (i18n.t('jobList.aiServiceUnavailable') ?? 'AI service unavailable')
        );
      } else if (err.response?.status === 422) {
        toast.warning(
          err.response?.data?.message ?? (i18n.t('jobList.scoringNotReady') ?? 'Scoring not ready')
        );
      } else {
        toast.error(
          err.response?.data?.message ?? (i18n.t('jobList.scoreError') ?? 'Failed to score job')
        );
      }
      throw error;
    } finally {
      set((state) => ({
        scoringState: { ...state.scoringState, [stateKey]: false },
      }));
    }
  },

  loadScoreHistory: async () => {
    try {
      const history = await jobService.getScoreHistory();
      const { scoreResults, selectedResumes } = buildScoreStateFromHistory(history);
      set((state) => ({
        scoreResults: { ...state.scoreResults, ...scoreResults },
        selectedResumes: { ...state.selectedResumes, ...selectedResumes },
      }));
    } catch {
      // Score history is non-critical; silently degrade to avoid blocking UI
      // 评分历史非关键数据，静默降级以避免阻塞界面
    }
  },

  setSearchQuery: (query: string) => {
    const filters = { ...get().filters, searchQuery: query };
    set({
      filters,
      filteredJobs: applyFilters(get().jobs, filters),
    });
  },

  setSortBy: (sortBy: 'date' | 'status') => {
    const filters = { ...get().filters, sortBy };
    set({
      filters,
      filteredJobs: applyFilters(get().jobs, filters),
    });
  },

  setSelectedResume: (jobId: string, versionId: string) => {
    set((state) => ({
      selectedResumes: { ...state.selectedResumes, [jobId]: versionId },
    }));
  },

  resetError: () => set({ error: null }),
}));

// ponytail: inline selectors used directly; no selector exports needed
