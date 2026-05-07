import axios from 'axios';
import tokenStorage from './tokenStorage';
import type { ResumeGroup, ResumeVersion, UploadResponse, DownloadFormat } from '../types/resume';

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
});

api.interceptors.request.use(config => {
  const token = tokenStorage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const resumeApi = {
  uploadResume: async (file: File, title?: string): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    if (title) formData.append('title', title);
    
    const response = await api.post<UploadResponse>('/resumes', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  getGroups: async (): Promise<{ data: ResumeGroup[] }> => {
    const response = await api.get<{ data: ResumeGroup[] }>('/resumes/groups');
    return response.data;
  },

  getGroupDetail: async (groupId: string): Promise<{ data: ResumeGroup }> => {
    const response = await api.get<{ data: ResumeGroup }>(`/resumes/groups/${groupId}`);
    return response.data;
  },

  getVersions: async (groupId: string): Promise<{ data: ResumeVersion[] }> => {
    const response = await api.get<{ data: ResumeVersion[] }>(`/resumes/groups/${groupId}/versions`);
    return response.data;
  },

  getVersionDetail: async (versionId: string): Promise<{ data: ResumeVersion }> => {
    const response = await api.get<{ data: ResumeVersion }>(`/resumes/versions/${versionId}`);
    return response.data;
  },

  updateVersion: async (versionId: string, content: string): Promise<void> => {
    await api.put(`/resumes/versions/${versionId}`, { versionId, content });
  },

  downloadVersion: async (versionId: string, format: DownloadFormat): Promise<Blob> => {
    const response = await api.get(`/resumes/${versionId}/download`, {
      params: { format },
      responseType: 'blob',
    });
    return response.data;
  },

  deleteGroup: async (groupId: string): Promise<void> => {
    await api.delete(`/resumes/groups/${groupId}`);
  },

  deleteVersion: async (versionId: string): Promise<void> => {
    await api.delete(`/resumes/versions/${versionId}`);
  },
};
