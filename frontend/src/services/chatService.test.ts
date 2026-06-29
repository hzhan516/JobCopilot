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

    expect(apiMock.post).toHaveBeenCalledWith('/v1/conversations/conv-1/messages', { content: 'Hello AI' })
    expect(result).toEqual(mockConversation)
  })
})
