import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import type { Profile } from '@/types'

const mockProfileService = vi.hoisted(() => ({
  getProfile: vi.fn(),
  updateProfile: vi.fn(),
  updateAvatar: vi.fn(),
}))

vi.mock('@/services/profileService', () => ({
  profileService: mockProfileService,
}))

import { useProfileStore } from './profile.store'

const initialState = useProfileStore.getState()

function makeProfile(overrides = {}): Profile {
  return {
    userId: 'u1',
    fullName: 'John Doe',
    avatarUrl: null,
    phone: null,
    targetPosition: null,
    preferredLocation: null,
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
    ...overrides,
  }
}

describe('useProfileStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useProfileStore.setState({
      profile: null,
      loading: false,
      error: null,
    })
  })

  it('initializes with empty state', () => {
    const state = useProfileStore.getState()
    expect(state.profile).toBeNull()
    expect(state.loading).toBe(false)
    expect(state.error).toBeNull()
  })

  it('fetches profile successfully', async () => {
    const profile = makeProfile()
    mockProfileService.getProfile.mockResolvedValue(profile)

    await act(async () => {
      await useProfileStore.getState().fetchProfile()
    })

    const state = useProfileStore.getState()
    expect(state.profile).toEqual(profile)
    expect(state.loading).toBe(false)
    expect(state.error).toBeNull()
  })

  it('sets error on fetch failure', async () => {
    mockProfileService.getProfile.mockRejectedValue(new Error('Network error'))

    await act(async () => {
      try {
        await useProfileStore.getState().fetchProfile()
      } catch {
        // expected
      }
    })

    const state = useProfileStore.getState()
    expect(state.error).toBe('Network error')
    expect(state.loading).toBe(false)
    expect(state.profile).toBeNull()
  })

  it('updates profile and refreshes state', async () => {
    const updated = makeProfile({ fullName: 'Jane Doe' })
    mockProfileService.updateProfile.mockResolvedValue(updated)

    await act(async () => {
      await useProfileStore.getState().updateProfile({
        fullName: 'Jane Doe',
        phone: '1234567890',
        targetPosition: 'Developer',
        preferredLocation: 'NYC',
      })
    })

    expect(useProfileStore.getState().profile?.fullName).toBe('Jane Doe')
    expect(useProfileStore.getState().error).toBeNull()
  })

  it('throws and sets error on update failure', async () => {
    mockProfileService.updateProfile.mockRejectedValue(new Error('Update failed'))

    await expect(
      act(async () => {
        await useProfileStore.getState().updateProfile({
          fullName: 'Test',
          phone: '',
          targetPosition: '',
          preferredLocation: '',
        })
      })
    ).rejects.toThrow('Update failed')

    expect(useProfileStore.getState().error).toBe('Update failed')
  })

  it('updates avatar and refreshes state', async () => {
    const updated = makeProfile({ avatarUrl: 'https://example.com/avatar.png' })
    mockProfileService.updateAvatar.mockResolvedValue(updated)

    await act(async () => {
      await useProfileStore.getState().updateAvatar({ avatarUrl: 'https://example.com/avatar.png' })
    })

    expect(useProfileStore.getState().profile?.avatarUrl).toBe('https://example.com/avatar.png')
  })

  it('clears error state', () => {
    useProfileStore.setState({ error: 'Some error' })

    act(() => {
      useProfileStore.getState().clearError()
    })

    expect(useProfileStore.getState().error).toBeNull()
  })

  it('handles non-Error exceptions with fallback message', async () => {
    mockProfileService.getProfile.mockRejectedValue('String error')

    await act(async () => {
      try {
        await useProfileStore.getState().fetchProfile()
      } catch {
        // expected
      }
    })

    expect(useProfileStore.getState().error).toBe('Failed to load profile')
  })
})
