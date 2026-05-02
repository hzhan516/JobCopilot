import { create } from 'zustand';
import type { Profile, UpdateProfileRequest, UpdateAvatarRequest } from '../types';
import { profileService } from '../services/profileService';

/**
 * Profile Store 状态接口
 * Profile Store state interface
 */
interface ProfileStore {
  profile: Profile | null;
  loading: boolean;
  error: string | null;

  /**
   * 获取用户资料
   * Fetch user profile
   */
  fetchProfile: () => Promise<void>;

  /**
   * 更新用户资料
   * Update user profile
   */
  updateProfile: (data: UpdateProfileRequest) => Promise<void>;

  /**
   * 更新用户头像
   * Update user avatar
   */
  updateAvatar: (data: UpdateAvatarRequest) => Promise<void>;

  /**
   * 清除错误状态
   * Clear error state
   */
  clearError: () => void;
}

export const useProfileStore = create<ProfileStore>((set) => ({
  profile: null,
  loading: false,
  error: null,

  fetchProfile: async () => {
    set({ loading: true, error: null });
    try {
      const data = await profileService.getProfile();
      set({ profile: data });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to load profile';
      set({ error: message });
    } finally {
      set({ loading: false });
    }
  },

  updateProfile: async (data: UpdateProfileRequest) => {
    set({ loading: true, error: null });
    try {
      const updated = await profileService.updateProfile(data);
      set({ profile: updated });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to update profile';
      set({ error: message });
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  updateAvatar: async (data: UpdateAvatarRequest) => {
    set({ loading: true, error: null });
    try {
      const updated = await profileService.updateAvatar(data);
      set({ profile: updated });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to update avatar';
      set({ error: message });
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  clearError: () => set({ error: null }),
}));
