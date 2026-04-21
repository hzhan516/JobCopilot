import apiClient from './api';
import type { ApiResponse, JobApplication, PaginatedResponse } from '@/types';

// 求职跟踪服务
export const trackingService = {
  // 获取所有投递记录
  getApplications: async (page = 1, size = 10): Promise<PaginatedResponse<JobApplication>> => {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<JobApplication>>>(
      '/v1/tracking/applications',
      {
        params: { page, size },
      }
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 获取投递详情
  getApplication: async (applicationId: string): Promise<JobApplication> => {
    const response = await apiClient.get<ApiResponse<JobApplication>>(
      `/v1/tracking/applications/${applicationId}`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 创建投递记录
  createApplication: async (data: {
    jobId: string;
    jobTitle: string;
    company: string;
    notes?: string;
  }): Promise<JobApplication> => {
    const response = await apiClient.post<ApiResponse<JobApplication>>(
      '/v1/tracking/applications',
      data
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 更新投递状态
  updateApplicationStatus: async (
    applicationId: string,
    status: JobApplication['status']
  ): Promise<JobApplication> => {
    const response = await apiClient.patch<ApiResponse<JobApplication>>(
      `/v1/tracking/applications/${applicationId}/status`,
      { status }
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 删除投递记录
  deleteApplication: async (applicationId: string): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<null>>(
      `/v1/tracking/applications/${applicationId}`
    );
    if (response.data.code !== 200) {
      throw new Error(response.data.message);
    }
  },
};

export default trackingService;
