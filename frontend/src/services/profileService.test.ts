import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { Profile } from '@/types'

const apiMock = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
}))

vi.mock('./api', () => ({
  default: apiMock,
}))

import { profileService } from './profileService'

const mockProfile: Profile = {
  userId: 'u1',
  fullName: 'John Doe',
  avatarUrl: 'https://example.com/avatar.png',
  phone: '+1-234-567-8900',
  targetPosition: 'Senior Developer',
  preferredLocation: 'Phoenix, AZ',
  createdAt: '2024-01-01',
  updatedAt: '2024-01-01',
}

describe('profileService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('gets profile successfully', async () => {
    apiMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: mockProfile } })

    const result = await profileService.getProfile()

    expect(apiMock.get).toHaveBeenCalledWith('/v1/profile')
    expect(result).toEqual(mockProfile)
  })

  it('throws on getProfile failure', async () => {
    apiMock.get.mockResolvedValueOnce({ data: { code: 500, message: 'Server error', data: null } })

    await expect(profileService.getProfile()).rejects.toThrow('Server error')
  })

  it('updates profile successfully', async () => {
    const updated = { ...mockProfile, fullName: 'Jane Doe' }
    apiMock.put.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: updated } })

    const result = await profileService.updateProfile({
      fullName: 'Jane Doe',
      phone: '+1-234-567-8900',
      targetPosition: 'Senior Developer',
      preferredLocation: 'Phoenix, AZ',
    })

    expect(apiMock.put).toHaveBeenCalledWith('/v1/profile', {
      fullName: 'Jane Doe',
      phone: '+1-234-567-8900',
      targetPosition: 'Senior Developer',
      preferredLocation: 'Phoenix, AZ',
    })
    expect(result.fullName).toBe('Jane Doe')
  })

  it('throws on updateProfile failure', async () => {
    apiMock.put.mockResolvedValueOnce({ data: { code: 400, message: 'Invalid input', data: null } })

    await expect(
      profileService.updateProfile({
        fullName: '',
        phone: '',
        targetPosition: '',
        preferredLocation: '',
      })
    ).rejects.toThrow('Invalid input')
  })

  it('updates avatar successfully', async () => {
    const updated = { ...mockProfile, avatarUrl: 'https://example.com/new-avatar.png' }
    apiMock.put.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: updated } })

    const result = await profileService.updateAvatar({ avatarUrl: 'https://example.com/new-avatar.png' })

    expect(apiMock.put).toHaveBeenCalledWith('/v1/profile/avatar', {
      avatarUrl: 'https://example.com/new-avatar.png',
    })
    expect(result.avatarUrl).toBe('https://example.com/new-avatar.png')
  })

  it('throws on updateAvatar failure', async () => {
    apiMock.put.mockResolvedValueOnce({ data: { code: 413, message: 'File too large', data: null } })

    await expect(
      profileService.updateAvatar({ avatarUrl: 'https://example.com/huge.png' })
    ).rejects.toThrow('File too large')
  })
})
