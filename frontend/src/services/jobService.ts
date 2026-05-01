import apiClient from './api';
import type {
  ApiResponse,
  Job,
  JobMatchRequest,
  JobMatchResponse,
  JobMatchHistoryResponse,
} from '@/types';

// 职位服务
export const jobService = {
  // 获取职位列表
  getJobs: async (): Promise<Job[]> => {
    const response = await apiClient.get<ApiResponse<Job[]>>('/v1/jobs');
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 获取职位详情
  getJob: async (jobId: string): Promise<Job> => {
    const response = await apiClient.get<ApiResponse<Job>>(`/v1/jobs/${jobId}`);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 提交职位 URL 进行解析
  submitJob: async (url: string, imageCheckEnabled = false): Promise<Job> => {
    const response = await apiClient.post<ApiResponse<Job>>('/v1/jobs', {
      url,
      imageCheckEnabled,
    });
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 发起异步职位匹配
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

  // 获取匹配结果（轮询用）
  getMatchResult: async (matchId: string): Promise<JobMatchResponse> => {
    const response = await apiClient.get<ApiResponse<JobMatchResponse>>(
      `/v1/jobs/match/${matchId}`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 获取匹配历史
  getMatchHistory: async (): Promise<JobMatchHistoryResponse[]> => {
    const response = await apiClient.get<ApiResponse<JobMatchHistoryResponse[]>>(
      '/v1/jobs/match/history'
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default jobService;
