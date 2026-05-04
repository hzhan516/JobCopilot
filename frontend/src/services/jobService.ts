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

  // 提交职位 URL + 截图进行解析（Multipart）
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

  // 更新职位解析内容
  updateJob: async (jobId: string, data: UpdateJobRequest): Promise<Job> => {
    const response = await apiClient.put<ApiResponse<Job>>(`/v1/jobs/${jobId}`, data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 对单个职位进行简历评分
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

  // 发起异步职位匹配（保留，供历史功能使用）
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

  // 获取评分历史
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
