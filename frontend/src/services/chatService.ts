import apiClient from './api';
import type { ApiResponse, Conversation } from '@/types';

export const chatService = {
  createConversation: async (
    title: string,
    resumeVersionId: string,
    jobId: string
  ): Promise<Conversation> => {
    const response = await apiClient.post<ApiResponse<Conversation>>('/v1/conversations', {
      title,
      resumeVersionId,
      jobId,
    });
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getConversations: async (): Promise<Conversation[]> => {
    const response = await apiClient.get<ApiResponse<Conversation[]>>('/v1/conversations');
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  getConversation: async (conversationId: string, signal?: AbortSignal): Promise<Conversation> => {
    const url = `/v1/conversations/${conversationId}`;
    const response = signal
      ? await apiClient.get<ApiResponse<Conversation>>(url, { signal })
      : await apiClient.get<ApiResponse<Conversation>>(url);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  deleteConversation: async (conversationId: string): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<null>>(
      `/v1/conversations/${conversationId}`
    );
    if (response.data.code !== 200) {
      throw new Error(response.data.message);
    }
  },

  sendMessage: async (
    conversationId: string,
    content: string,
    fileUrls: string[] = []
  ): Promise<Conversation> => {
    const response = await apiClient.post<ApiResponse<Conversation>>(
      `/v1/conversations/${conversationId}/messages`,
      { content, fileUrls }
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  uploadAttachment: async (conversationId: string, file: File): Promise<string> => {
    const form = new FormData();
    form.append('file', file);
    const response = await apiClient.post<ApiResponse<string>>(
      `/v1/conversations/${conversationId}/files`,
      form
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  retryAiReply: async (conversationId: string): Promise<Conversation> => {
    const response = await apiClient.post<ApiResponse<Conversation>>(
      `/v1/conversations/${conversationId}/ai-replies/retry`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  compactConversation: async (conversationId: string): Promise<Conversation> => {
    const response = await apiClient.post<ApiResponse<Conversation>>(
      `/v1/conversations/${conversationId}/compact`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default chatService;
