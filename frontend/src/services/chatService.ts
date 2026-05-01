import apiClient from './api';
import tokenStorage from './tokenStorage';
import type { ApiResponse, Conversation, Message, PaginatedResponse } from '@/types';

// 对话服务
export const chatService = {
  // 创建对话
  createConversation: async (title: string, resumeId?: string): Promise<Conversation> => {
    const response = await apiClient.post<ApiResponse<Conversation>>('/v1/conversations', {
      title,
      resumeId,
    });
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 获取用户的所有对话
  getConversations: async (): Promise<Conversation[]> => {
    const response = await apiClient.get<ApiResponse<Conversation[]>>('/v1/conversations');
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 获取对话详情
  getConversation: async (conversationId: string): Promise<Conversation> => {
    const response = await apiClient.get<ApiResponse<Conversation>>(
      `/v1/conversations/${conversationId}`
    );
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 删除对话
  deleteConversation: async (conversationId: string): Promise<void> => {
    const response = await apiClient.delete<ApiResponse<null>>(
      `/v1/conversations/${conversationId}`
    );
    if (response.data.code !== 200) {
      throw new Error(response.data.message);
    }
  },

  // 获取对话消息
  getMessages: async (
    conversationId: string,
    page = 1,
    size = 50
  ): Promise<PaginatedResponse<Message>> => {
    const response = await apiClient.get<ApiResponse<Conversation>>(
      `/v1/conversations/${conversationId}`,
      { params: { page, size } }
    );
    if (response.data.code === 200) {
      const list = response.data.data.messages ?? [];
      return {
        list,
        page,
        size,
        total: list.length,
        totalPages: 1,
      };
    }
    throw new Error(response.data.message);
  },

  // 发送消息
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

  // AI 回复（流式）
  streamAIResponse: async (
    conversationId: string,
    onMessage: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> => {
    try {
      const response = await fetch(
        `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'}/v1/conversations/${conversationId}/stream`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${tokenStorage.getAccessToken()}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error('Stream request failed');
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('No reader available');
      }

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          onComplete();
          break;
        }
        const chunk = decoder.decode(value, { stream: true });
        onMessage(chunk);
      }
    } catch (error) {
      onError(error as Error);
    }
  },
};

export default chatService;
