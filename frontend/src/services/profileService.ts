import apiClient from './api';
import type { ApiResponse, Profile, UpdateProfileRequest, UpdateAvatarRequest } from '@/types';

export const profileService = {
  getProfile: async (): Promise<Profile> => {
    const response = await apiClient.get<ApiResponse<Profile>>('/v1/profile');
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  updateProfile: async (data: UpdateProfileRequest): Promise<Profile> => {
    const response = await apiClient.put<ApiResponse<Profile>>('/v1/profile', data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  updateAvatar: async (data: UpdateAvatarRequest): Promise<Profile> => {
    const response = await apiClient.put<ApiResponse<Profile>>('/v1/profile/avatar', data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default profileService;
