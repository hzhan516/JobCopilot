import apiClient from './api';
import type { ApiResponse, Profile, UpdateProfileRequest, UpdateAvatarRequest } from '@/types';

/**
 * 用户资料服务
 * User profile service
 */
export const profileService = {
  /**
   * 获取当前用户资料
   * Get current user profile
   */
  getProfile: async (): Promise<Profile> => {
    const response = await apiClient.get<ApiResponse<Profile>>('/v1/profile');
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  /**
   * 更新用户资料
   * Update user profile
   */
  updateProfile: async (data: UpdateProfileRequest): Promise<Profile> => {
    const response = await apiClient.put<ApiResponse<Profile>>('/v1/profile', data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  /**
   * 更新用户头像
   * Update user avatar
   */
  updateAvatar: async (data: UpdateAvatarRequest): Promise<Profile> => {
    const response = await apiClient.put<ApiResponse<Profile>>('/v1/profile/avatar', data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default profileService;
