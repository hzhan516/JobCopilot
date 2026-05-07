import { create } from 'zustand';
import type { Job, JobScoreResponse, JobScoreHistoryResponse } from '@/types';
import { jobService } from '@/services/jobService';
import i18n from '@/i18n';
import { toast } from 'sonner';

/**
 * Job 筛选条件类型
 * Job filter conditions type
 */
interface JobFilters {
  searchQuery: string;
  sortBy: 'date' | 'status';
}

/**
 * Job Store 状态接口
 * Job Store state interface
 */
interface JobStore {
  /** 原始职位列表 / Raw job list */
  jobs: Job[];
  /** 筛选后的职位列表 / Filtered job list */
  filteredJobs: Job[];
  /** 加载状态 / Loading state */
  loading: boolean;
  /** 错误信息 / Error message */
  error: string | null;

  /** 当前筛选条件 / Current filters */
  filters: JobFilters;

  /** 评分结果缓存（key: `${jobId}_${resumeVersionId}`） */
  scoreResults: Record<string, JobScoreResponse>;
  /** 评分进行中状态（key: `${jobId}_${resumeVersionId}`） */
  scoringState: Record<string, boolean>;
  /** 每个职位当前选中的简历版本 / Selected resume version per job */
  selectedResumes: Record<string, string>;

  /**
   * 获取职位列表
   * Fetch job list
   */
  fetchJobs: () => Promise<void>;

  /**
   * 提交新职位
   * Submit a new job
   */
  submitJob: (url: string, screenshot: File) => Promise<void>;

  /**
   * 隐藏职位
   * Hide a job from user-facing lists
   */
  deleteJob: (jobId: string) => Promise<void>;

  /**
   * 对职位进行简历评分
   * Score a job against a resume
   */
  scoreJob: (jobId: string, resumeVersionId: string) => Promise<void>;

  /**
   * 加载评分历史
   * Load score history
   */
  loadScoreHistory: () => Promise<void>;

  /**
   * 设置搜索关键词（自动触发筛选）
   * Set search query (auto triggers filtering)
   */
  setSearchQuery: (query: string) => void;

  /**
   * 设置排序方式（自动触发筛选）
   * Set sort criteria (auto triggers filtering)
   */
  setSortBy: (sortBy: 'date' | 'status') => void;

  /**
   * 设置职位对应的选中简历版本
   * Set selected resume version for a job
   */
  setSelectedResume: (jobId: string, versionId: string) => void;

  /**
   * 清除错误状态
   * Clear error state
   */
  resetError: () => void;
}

/**
 * 应用筛选和排序逻辑
 * Apply filter and sort logic
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
 * 从评分历史记录构建 scoreResults 和 selectedResumes 初始值
 * Build scoreResults and selectedResumes from history records
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
    // 只设置一次：取该职位最新的评分简历（history 已按 createdAt 降序）
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
      // 提交成功后刷新列表
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
      // 评分历史非关键数据，静默处理
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

// ====== Selector helpers for performance optimization ======

/** 选择职位列表数据（派生状态） */
export const selectJobs = (state: JobStore): Job[] => state.filteredJobs;

/** 选择加载状态 */
export const selectLoading = (state: JobStore): boolean => state.loading;

/** 选择错误状态 */
export const selectError = (state: JobStore): string | null => state.error;

/** 选择筛选条件 */
export const selectFilters = (state: JobStore): JobFilters => state.filters;

/** 选择评分结果 */
export const selectScoreResults = (state: JobStore): Record<string, JobScoreResponse> =>
  state.scoreResults;

/** 选择评分进行中状态 */
export const selectScoringState = (state: JobStore): Record<string, boolean> =>
  state.scoringState;

/** 选择每个职位对应的简历版本 */
export const selectSelectedResumes = (state: JobStore): Record<string, string> =>
  state.selectedResumes;
