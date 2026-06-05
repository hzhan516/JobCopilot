import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act, waitFor } from '@testing-library/react'
import type { ResumeGroup as ApiResumeGroup, ResumeVersion as ApiResumeVersion } from '@/types'

const mockResumeService = vi.hoisted(() => ({
  getResumeGroups: vi.fn(),
  getResumeGroup: vi.fn(),
  getVersionsByGroup: vi.fn(),
  uploadResume: vi.fn(),
  editVersion: vi.fn(),
  createVersion: vi.fn(),
  deleteResumeGroup: vi.fn(),
  activateVersion: vi.fn(),
  deleteVersion: vi.fn(),
}))

vi.mock('@/services/resumeService', () => ({
  resumeService: mockResumeService,
}))

import { useResumeStore } from './resume.store'

const initialState = useResumeStore.getState()

function makeApiGroup(id: string, title: string): ApiResumeGroup {
  return {
    groupId: id,
    title,
    isDefault: false,
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
    originalVersion: {
      versionId: `${id}-v1`,
      status: 'ACTIVE',
      parseStatus: 'COMPLETED',
      createdAt: '2024-01-01',
      exists: true,
    },
    convertedVersion: null,
    aiOptimizedVersion: null,
  }
}

function makeApiVersion(id: string, groupId: string, type: 'ORIGINAL' | 'CONVERTED' = 'ORIGINAL'): ApiResumeVersion {
  return {
    versionId: id,
    groupId,
    versionType: type,
    status: 'ACTIVE',
    parseStatus: 'COMPLETED',
    originalFileName: 'resume.pdf',
    fileType: 'PDF',
    fileSize: 1024,
    content: null,
    editable: true,
    createdAt: '2024-01-01',
    updatedAt: '2024-01-01',
  }
}

describe('useResumeStore', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useResumeStore.setState({
      groups: [],
      currentGroup: null,
      loading: false,
      uploadProgress: 0,
    })
  })

  it('initializes with empty state', () => {
    const state = useResumeStore.getState()
    expect(state.groups).toEqual([])
    expect(state.currentGroup).toBeNull()
    expect(state.loading).toBe(false)
    expect(state.uploadProgress).toBe(0)
  })

  it('fetches groups and maps them', async () => {
    mockResumeService.getResumeGroups.mockResolvedValue([makeApiGroup('g1', 'Resume 1')])

    await act(async () => {
      await useResumeStore.getState().fetchGroups()
    })

    const state = useResumeStore.getState()
    expect(state.groups).toHaveLength(1)
    expect(state.groups[0].groupId).toBe('g1')
    expect(state.groups[0].versions).toHaveLength(1)
    expect(state.groups[0].versions[0].versionType).toBe('ORIGINAL')
    expect(state.loading).toBe(false)
  })

  it('fetches group detail with versions', async () => {
    mockResumeService.getResumeGroup.mockResolvedValue(makeApiGroup('g1', 'Resume 1'))
    mockResumeService.getVersionsByGroup.mockResolvedValue([makeApiVersion('v1', 'g1', 'CONVERTED')])

    await act(async () => {
      await useResumeStore.getState().fetchGroupDetail('g1')
    })

    const state = useResumeStore.getState()
    expect(state.currentGroup?.groupId).toBe('g1')
    expect(state.currentGroup?.versions).toHaveLength(1)
    expect(state.currentGroup?.versions[0].versionType).toBe('CONVERTED')
  })

  it('uploads resume and tracks progress', async () => {
    mockResumeService.uploadResume.mockResolvedValue({
      groupId: 'g1',
      originalVersionId: 'v1',
      title: 'My Resume',
      createdAt: '2024-01-01',
    })

    let progressDuringUpload = 0
    const file = new File(['content'], 'resume.pdf')

    const uploadPromise = act(async () => {
      const promise = useResumeStore.getState().uploadResume(file, 'My Resume')
      // Check loading state during upload
      progressDuringUpload = useResumeStore.getState().uploadProgress
      return promise
    })

    const result = await uploadPromise

    expect(result.groupId).toBe('g1')
    expect(useResumeStore.getState().uploadProgress).toBe(100)
    expect(useResumeStore.getState().loading).toBe(false)
  })

  it('deletes group and updates state', async () => {
    useResumeStore.setState({
      groups: [
        { groupId: 'g1', title: 'R1', isDefault: false, createdAt: '', updatedAt: '', versions: [] },
        { groupId: 'g2', title: 'R2', isDefault: false, createdAt: '', updatedAt: '', versions: [] },
      ],
      currentGroup: { groupId: 'g1', title: 'R1', isDefault: false, createdAt: '', updatedAt: '', versions: [] },
    })
    mockResumeService.deleteResumeGroup.mockResolvedValue(undefined)

    await act(async () => {
      await useResumeStore.getState().deleteGroup('g1')
    })

    const state = useResumeStore.getState()
    expect(state.groups.map((g) => g.groupId)).toEqual(['g2'])
    expect(state.currentGroup).toBeNull()
  })

  it('deletes group without affecting currentGroup if different', async () => {
    useResumeStore.setState({
      groups: [
        { groupId: 'g1', title: 'R1', isDefault: false, createdAt: '', updatedAt: '', versions: [] },
        { groupId: 'g2', title: 'R2', isDefault: false, createdAt: '', updatedAt: '', versions: [] },
      ],
      currentGroup: { groupId: 'g2', title: 'R2', isDefault: false, createdAt: '', updatedAt: '', versions: [] },
    })
    mockResumeService.deleteResumeGroup.mockResolvedValue(undefined)

    await act(async () => {
      await useResumeStore.getState().deleteGroup('g1')
    })

    expect(useResumeStore.getState().currentGroup?.groupId).toBe('g2')
  })

  it('handles empty groups response', async () => {
    mockResumeService.getResumeGroups.mockResolvedValue([])

    await act(async () => {
      await useResumeStore.getState().fetchGroups()
    })

    expect(useResumeStore.getState().groups).toEqual([])
  })

  it('sets loading to false even when fetch fails', async () => {
    mockResumeService.getResumeGroups.mockRejectedValue(new Error('Network error'))

    await act(async () => {
      try {
        await useResumeStore.getState().fetchGroups()
      } catch {
        // expected
      }
    })

    expect(useResumeStore.getState().loading).toBe(false)
  })

  it('skips saveVersion when content unchanged', async () => {
    useResumeStore.setState({
      currentGroup: {
        groupId: 'g1',
        title: 'R1',
        isDefault: false,
        createdAt: '',
        updatedAt: '',
        versions: [
          {
            versionId: 'v1',
            groupId: 'g1',
            versionType: 'CONVERTED',
            status: 'ACTIVE',
            storagePath: '',
            content: 'same content',
            parseStatus: 'COMPLETED',
            createdAt: '',
          },
        ],
      },
    })

    await act(async () => {
      await useResumeStore.getState().saveVersion('v1', 'same content')
    })

    expect(mockResumeService.editVersion).not.toHaveBeenCalled()
    expect(useResumeStore.getState().loading).toBe(false)
  })
})
