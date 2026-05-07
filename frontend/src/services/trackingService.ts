import apiClient from './api';
import type {
  ApiResponse,
  Tracking,
  CreateTrackingRequest,
  UpdateTrackingRequest,
  TrackingStatsResponse,
} from '@/types';

type BackendTrackingStatsResponse = {
  totalApplications?: number;
  pendingCount?: number;
  appliedCount?: number;
  interviewingCount?: number;
  offerCount?: number;
  rejectedCount?: number;
  withdrawnCount?: number;
  successRate?: number;
};

export const trackingService = {
  getTrackings: async (status?: string): Promise<Tracking[]> => {
    const params = status ? { status } : {};
    const response = await apiClient.get<ApiResponse<Tracking[]>>('/v1/trackings', {
      params,
    });
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getTracking: async (trackingId: string): Promise<Tracking> => {
    const response = await apiClient.get<ApiResponse<Tracking>>(
      `/v1/trackings/${trackingId}`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  createTracking: async (data: CreateTrackingRequest): Promise<Tracking> => {
    const response = await apiClient.post<ApiResponse<Tracking>>('/v1/trackings', data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  updateTracking: async (
    trackingId: string,
    data: UpdateTrackingRequest
  ): Promise<Tracking> => {
    const response = await apiClient.put<ApiResponse<Tracking>>(
      `/v1/trackings/${trackingId}`,
      data
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  deleteTracking: async (trackingId: string): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<null>>(
      `/v1/trackings/${trackingId}`
    );
    if (response.data.code !== 200) {
      throw new Error(response.data.message);
    }
  },

  getTrackingStats: async (): Promise<TrackingStatsResponse> => {
    const response = await apiClient.get<ApiResponse<BackendTrackingStatsResponse>>(
      '/v1/trackings/stats'
    );
    if (response.data.code === 200) {
      const data = response.data.data;
      const successRate = data.successRate ?? 0;
      return {
        total: data.totalApplications ?? 0,
        pending: data.pendingCount ?? 0,
        applied: data.appliedCount ?? 0,
        screening: 0,
        interview: data.interviewingCount ?? 0,
        offer: data.offerCount ?? 0,
        rejected: data.rejectedCount ?? 0,
        withdrawn: data.withdrawnCount ?? 0,
        successRate: Number.isFinite(successRate) ? successRate : 0,
      };
    }
    throw new Error(response.data.message);
  },
};

export default trackingService;
