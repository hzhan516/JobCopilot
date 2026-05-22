import apiClient from './api';
import type {
  ApiResponse,
  Job,
  JobMatchRequest,
  JobMatchResponse,
  JobMatchHistoryResponse,
  JobScoreRequest,
  JobScoreResponse,
  JobScoreHistoryResponse,
  UpdateJobRequest,
} from '@/types';

export const jobService = {
  getJobs: async (signal?: AbortSignal): Promise<Job[]> => {
    const response = await apiClient.get<ApiResponse<Job[]>>('/v1/jobs', {
      signal,
    });
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getJob: async (jobId: string): Promise<Job> => {
    const response = await apiClient.get<ApiResponse<Job>>(`/v1/jobs/${jobId}`);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  submitJob: async (url: string, screenshot?: File): Promise<Job> => {
    const formData = new FormData();
    formData.append('url', url);
    if (screenshot) {
      formData.append('screenshot', screenshot);
    }

    const response = await apiClient.post<ApiResponse<Job>>('/v1/jobs', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  updateJob: async (jobId: string, data: UpdateJobRequest): Promise<Job> => {
    const response = await apiClient.put<ApiResponse<Job>>(`/v1/jobs/${jobId}`, data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  deleteJob: async (jobId: string): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<null>>(`/v1/jobs/${jobId}`);
    if (response.data.code === 200) {
      return;
    }
    throw new Error(response.data.message);
  },

  scoreJob: async (jobId: string, request: JobScoreRequest): Promise<JobScoreResponse> => {
    const response = await apiClient.post<ApiResponse<JobScoreResponse>>(
      `/v1/jobs/${jobId}/score`,
      request
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  trackAction: async (jobId: string, action: 'CLICK' | 'APPLY' | 'REJECT', resumeVersionId?: string): Promise<void> => {
    let url = `/v1/jobs/${jobId}/track?action=${action}`;
    if (resumeVersionId) {
      url += `&resumeVersionId=${encodeURIComponent(resumeVersionId)}`;
    }
    const response = await apiClient.post<ApiResponse<null>>(url);
    if (response.data.code === 200) {
      return;
    }
    throw new Error(response.data.message);
  },

  startMatch: async (request: JobMatchRequest): Promise<JobMatchResponse> => {
    const response = await apiClient.post<ApiResponse<JobMatchResponse>>(
      '/v1/jobs/match',
      request
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getMatchResult: async (matchId: string): Promise<JobMatchResponse> => {
    const response = await apiClient.get<ApiResponse<JobMatchResponse>>(
      `/v1/jobs/match/${matchId}`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getMatchHistory: async (): Promise<JobMatchHistoryResponse[]> => {
    const response = await apiClient.get<ApiResponse<JobMatchHistoryResponse[]>>(
      '/v1/jobs/match/history'
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getScoreHistory: async (): Promise<JobScoreHistoryResponse[]> => {
    const response = await apiClient.get<ApiResponse<JobScoreHistoryResponse[]>>(
      '/v1/jobs/scores/history'
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default jobService;
