import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { Conversation, Message, PaginatedResponse } from '@/types'

const apiMock = vi.hoisted(() => ({
  post: vi.fn(),
  get: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('./api', () => ({
  default: apiMock,
}))

// 手动 mock tokenStorage，因为 streamAIResponse 使用它
const tokenStorageMock = vi.hoisted(() => ({
  getAccessToken: vi.fn().mockReturnValue('mock-token'),
}))

vi.mock('./tokenStorage', () => ({
  default: tokenStorageMock,
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

const mockMessage: Message = {
  messageId: 'msg-1',
  conversationId: 'conv-1',
  role: 'USER',
  content: 'Hello',
  sequence: 1,
  createdAt: '2024-01-01',
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

  it('gets paginated messages', async () => {
    const convWithMessages = { ...mockConversation, messages: [mockMessage] }
    apiMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: convWithMessages } })

    const result = await chatService.getMessages('conv-1', 1, 50)

    expect(apiMock.get).toHaveBeenCalledWith('/v1/conversations/conv-1', { params: { page: 1, size: 50 } })
    expect(result.list).toEqual([mockMessage])
    expect(result.page).toBe(1)
    expect(result.size).toBe(50)
  })

  it('handles empty messages', async () => {
    apiMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: mockConversation } })

    const result = await chatService.getMessages('conv-1')

    expect(result.list).toEqual([])
    expect(result.total).toBe(0)
  })

  it('sends message', async () => {
    apiMock.post.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: mockConversation } })

    const result = await chatService.sendMessage('conv-1', 'Hello AI')

    expect(apiMock.post).toHaveBeenCalledWith('/v1/conversations/conv-1/messages', { content: 'Hello AI' })
    expect(result).toEqual(mockConversation)
  })

  it('streams AI response via fetch', async () => {
    const mockReader = {
      read: vi.fn()
        .mockResolvedValueOnce({ done: false, value: new TextEncoder().encode('Hello') })
        .mockResolvedValueOnce({ done: false, value: new TextEncoder().encode(' World') })
        .mockResolvedValueOnce({ done: true, value: undefined }),
    }

    const mockResponse = {
      ok: true,
      body: {
        getReader: () => mockReader,
      },
    }

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(mockResponse))

    const onMessage = vi.fn()
    const onComplete = vi.fn()
    const onError = vi.fn()

    await chatService.streamAIResponse('conv-1', onMessage, onComplete, onError)

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining('/v1/conversations/conv-1/stream'),
      expect.objectContaining({
        method: 'GET',
        headers: { Authorization: 'Bearer mock-token' },
      })
    )
    expect(onMessage).toHaveBeenCalledTimes(2)
    expect(onComplete).toHaveBeenCalledTimes(1)
    expect(onError).not.toHaveBeenCalled()

    vi.unstubAllGlobals()
  })

  it('calls onError when stream request fails', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 500 }))

    const onMessage = vi.fn()
    const onComplete = vi.fn()
    const onError = vi.fn()

    await chatService.streamAIResponse('conv-1', onMessage, onComplete, onError)

    expect(onError).toHaveBeenCalledTimes(1)
    expect(onComplete).not.toHaveBeenCalled()

    vi.unstubAllGlobals()
  })

  it('calls onError when no reader available', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, body: null }))

    const onError = vi.fn()

    await chatService.streamAIResponse('conv-1', vi.fn(), vi.fn(), onError)

    expect(onError).toHaveBeenCalledTimes(1)

    vi.unstubAllGlobals()
  })

  it('calls onError when fetch throws', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('Network down')))

    const onError = vi.fn()

    await chatService.streamAIResponse('conv-1', vi.fn(), vi.fn(), onError)

    expect(onError).toHaveBeenCalledWith(expect.any(Error))

    vi.unstubAllGlobals()
  })
})
