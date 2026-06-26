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

  getConversation: async (conversationId: string): Promise<Conversation> => {
    const response = await apiClient.get<ApiResponse<Conversation>>(
      `/v1/conversations/${conversationId}`
    );
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

  sendMessage: async (conversationId: string, content: string): Promise<Conversation> => {
    const response = await apiClient.post<ApiResponse<Conversation>>(
      `/v1/conversations/${conversationId}/messages`,
      { content }
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },
};

export default chatService;
