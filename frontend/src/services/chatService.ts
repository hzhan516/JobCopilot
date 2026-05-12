import apiClient from './api';
import tokenStorage from './tokenStorage';
import type { ApiResponse, Conversation, Message, PaginatedResponse } from '@/types';

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

  /**
   * Streams AI response chunks via raw fetch to handle Server-Sent Events.
   * Bypasses axios because streaming responses require manual reader control.
   *
   * 通过原生 fetch 流式读取 AI 回复片段，处理 Server-Sent Events。
   * 绕过 axios，因为流式响应需要手动控制 reader。
   */
  streamAIResponse: async (
    conversationId: string,
    onMessage: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<void> => {
    try {
      const response = await fetch(
        `${import.meta.env.VITE_API_BASE_URL || '/api'}/v1/conversations/${conversationId}/stream`,
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
