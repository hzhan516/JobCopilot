import apiClient from './api';
import type { ApiResponse, Job, PaginatedResponse } from '@/types';

// 职位服务
export const jobService = {
  // 获取职位列表
  getJobs: async (page = 1, size = 10): Promise<PaginatedResponse<Job>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Job>>>('/v1/jobs', {
      params: { page, size },
    });
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

  // 获取匹配的职位
  getMatchedJobs: async (resumeId: string, limit = 10): Promise<Job[]> => {
    const response = await apiClient.get<ApiResponse<Job[]>>(`/v1/jobs/match`, {
      params: { resumeId, limit },
    });
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default jobService;
