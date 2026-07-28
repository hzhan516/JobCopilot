import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { Conversation } from '@/types'

const apiMock = vi.hoisted(() => ({
  post: vi.fn(),
  get: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('./api', () => ({
  default: apiMock,
}))

import { chatService } from './chatService'

const mockConversation: Conversation = {
  conversationId: 'conv-1',
  userId: 'u1',
  title: 'Chat 1',
  status: 'ACTIVE',
  resumeVersionId: 'v1',
  jobId: 'j1',
  messages: [],
  createdAt: '2024-01-01',
  updatedAt: '2024-01-01',
}

describe('chatService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('creates conversation', async () => {
    apiMock.post.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: mockConversation } })

    const result = await chatService.createConversation('Chat 1', 'v1', 'j1')

    expect(apiMock.post).toHaveBeenCalledWith('/v1/conversations', {
      title: 'Chat 1',
      resumeVersionId: 'v1',
      jobId: 'j1',
    })
    expect(result).toEqual(mockConversation)
  })

  it('throws on non-200 create response', async () => {
    apiMock.post.mockResolvedValueOnce({ data: { code: 400, message: 'Invalid data', data: null } })

    await expect(chatService.createConversation('Chat', 'v1', 'j1')).rejects.toThrow('Invalid data')
  })

  it('gets all conversations', async () => {
    apiMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: [mockConversation] } })

    const result = await chatService.getConversations()

    expect(apiMock.get).toHaveBeenCalledWith('/v1/conversations')
    expect(result).toEqual([mockConversation])
  })

  it('gets single conversation', async () => {
    apiMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: mockConversation } })

    const result = await chatService.getConversation('conv-1')

    expect(apiMock.get).toHaveBeenCalledWith('/v1/conversations/conv-1')
    expect(result).toEqual(mockConversation)
  })

  it('deletes conversation', async () => {
    apiMock.delete.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: null } })

    await chatService.deleteConversation('conv-1')

    expect(apiMock.delete).toHaveBeenCalledWith('/v1/conversations/conv-1')
  })

  it('throws on delete failure', async () => {
    apiMock.delete.mockResolvedValueOnce({ data: { code: 500, message: 'Server error', data: null } })

    await expect(chatService.deleteConversation('conv-1')).rejects.toThrow('Server error')
  })

  it('sends message', async () => {
    apiMock.post.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: mockConversation } })

    const result = await chatService.sendMessage('conv-1', 'Hello AI')

    expect(apiMock.post).toHaveBeenCalledWith('/v1/conversations/conv-1/messages', {
      content: 'Hello AI',
      fileUrls: [],
    })
    expect(result).toEqual(mockConversation)
  })

  it('uploads and sends controlled attachment references', async () => {
    apiMock.post
      .mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: '/api/storage/download?key=conversations/conv-1/file.txt' } })
      .mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: mockConversation } })
    const file = new File(['notes'], 'notes.txt', { type: 'text/plain' })

    const url = await chatService.uploadAttachment('conv-1', file)
    await chatService.sendMessage('conv-1', 'Review this', [url])

    const form = apiMock.post.mock.calls[0][1] as FormData
    expect(apiMock.post.mock.calls[0][0]).toBe('/v1/conversations/conv-1/files')
    expect(form.get('file')).toBe(file)
    expect(apiMock.post.mock.calls[1]).toEqual([
      '/v1/conversations/conv-1/messages',
      { content: 'Review this', fileUrls: [url] },
    ])
  })

  it('retries the failed AI reply through the request-scoped endpoint', async () => {
    const pendingConversation: Conversation = {
      ...mockConversation,
      aiReply: {
        requestId: 'request-2',
        status: 'PENDING',
        errorCode: null,
        startedAt: '2026-07-18T12:00:00Z',
        completedAt: null,
        userMessageSequence: 1,
        assistantMessageSequence: null,
      },
    }
    apiMock.post.mockResolvedValueOnce({
      data: { code: 200, message: 'OK', data: pendingConversation },
    })

    const result = await chatService.retryAiReply('conv-1')

    expect(apiMock.post).toHaveBeenCalledWith('/v1/conversations/conv-1/ai-replies/retry')
    expect(result.aiReply?.requestId).toBe('request-2')
  })
})
