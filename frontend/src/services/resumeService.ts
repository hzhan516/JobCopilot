import apiClient from './api';
import type {
  ApiResponse,
  ResumeGroup,
  ResumeVersion,
  ResumeUploadResponse,
  ResumeEditRequest,
} from '@/types';

export const resumeService = {
  uploadResume: async (file: File, title?: string): Promise<ResumeUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (title) {
      formData.append('title', title);
    }

    const response = await apiClient.post<ApiResponse<ResumeUploadResponse>>(
      '/v1/resumes',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  downloadResume: async (versionId: string, format?: string): Promise<Blob> => {
    const params = format ? { format } : {};
    const response = await apiClient.get(`/v1/resumes/${versionId}/download`, {
      params,
      responseType: 'blob',
    });
    return response.data;
  },

  getResumeGroups: async (): Promise<ResumeGroup[]> => {
    const response = await apiClient.get<ApiResponse<ResumeGroup[]>>('/v1/resumes/groups');
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getResumeGroup: async (groupId: string): Promise<ResumeGroup> => {
    const response = await apiClient.get<ApiResponse<ResumeGroup>>(`/v1/resumes/groups/${groupId}`);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  deleteResumeGroup: async (groupId: string): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<null>>(`/v1/resumes/groups/${groupId}`);
    if (response.data.code !== 200) {
      throw new Error(response.data.message);
    }
  },

  getVersionsByGroup: async (groupId: string): Promise<ResumeVersion[]> => {
    const response = await apiClient.get<ApiResponse<ResumeVersion[]>>(
      `/v1/resumes/groups/${groupId}/versions`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getVersion: async (versionId: string): Promise<ResumeVersion> => {
    const response = await apiClient.get<ApiResponse<ResumeVersion>>(
      `/v1/resumes/versions/${versionId}`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  deleteVersion: async (versionId: string): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<null>>(`/v1/resumes/versions/${versionId}`);
    if (response.data.code !== 200) {
      throw new Error(response.data.message);
    }
  },

  editVersion: async (versionId: string, content: string): Promise<ResumeVersion> => {
    const response = await apiClient.put<ApiResponse<ResumeVersion>>(
      `/v1/resumes/versions/${versionId}`,
      { versionId, content } as ResumeEditRequest
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  createVersion: async (groupId: string, sourceVersionId?: string): Promise<ResumeVersion> => {
    const response = await apiClient.post<ApiResponse<ResumeVersion>>(
      `/v1/resumes/groups/${groupId}/versions`,
      { sourceVersionId: sourceVersionId || null }
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  activateVersion: async (versionId: string): Promise<ResumeVersion> => {
    const response = await apiClient.post<ApiResponse<ResumeVersion>>(
      `/v1/resumes/versions/${versionId}/activate`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default resumeService;
