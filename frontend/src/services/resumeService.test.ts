import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ApiResponse, ResumeGroup, ResumeVersion, ResumeUploadResponse } from '@/types'

vi.mock('@/services/api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
    put: vi.fn(),
  },
}))

import apiClient from '@/services/api'
import { resumeService } from './resumeService'

const mockPost = vi.mocked(apiClient.post)
const mockGet = vi.mocked(apiClient.get)
const mockDelete = vi.mocked(apiClient.delete)
const mockPut = vi.mocked(apiClient.put)

describe('resumeService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('uploadResume', () => {
    it('uploads a file with optional title', async () => {
      const mockResponse: ApiResponse<ResumeUploadResponse> = {
        code: 200,
        message: 'ok',
        data: { groupId: 'g1', originalVersionId: 'v1', title: 'My Resume', createdAt: '2024-01-01' },
      }
      mockPost.mockResolvedValue({ data: mockResponse })

      const file = new File(['content'], 'resume.pdf', { type: 'application/pdf' })
      const result = await resumeService.uploadResume(file, 'My Resume')

      expect(mockPost).toHaveBeenCalledWith(
        '/v1/resumes',
        expect.any(FormData),
        expect.objectContaining({
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      )
      expect(result.groupId).toBe('g1')
    })

    it('throws when response code is not 200', async () => {
      mockPost.mockResolvedValue({ data: { code: 400, message: 'Invalid file', data: null } })
      const file = new File(['content'], 'resume.pdf')
      await expect(resumeService.uploadResume(file)).rejects.toThrow('Invalid file')
    })
  })

  describe('getResumeGroups', () => {
    it('returns resume groups on success', async () => {
      const groups: ResumeGroup[] = [
        {
          groupId: 'g1',
          title: 'Resume 1',
          isDefault: true,
          createdAt: '2024-01-01',
          updatedAt: '2024-01-01',
          originalVersion: null,
          convertedVersion: null,
          aiOptimizedVersion: null,
        },
      ]
      mockGet.mockResolvedValue({ data: { code: 200, message: 'ok', data: groups } })

      const result = await resumeService.getResumeGroups()
      expect(result).toEqual(groups)
      expect(mockGet).toHaveBeenCalledWith('/v1/resumes/groups')
    })

    it('throws on non-200 response', async () => {
      mockGet.mockResolvedValue({ data: { code: 500, message: 'Server error', data: null } })
      await expect(resumeService.getResumeGroups()).rejects.toThrow('Server error')
    })
  })

  describe('deleteResumeGroup', () => {
    it('deletes group successfully', async () => {
      mockDelete.mockResolvedValue({ data: { code: 200, message: 'ok', data: null } })
      await resumeService.deleteResumeGroup('g1')
      expect(mockDelete).toHaveBeenCalledWith('/v1/resumes/groups/g1')
    })

    it('throws on error response', async () => {
      mockDelete.mockResolvedValue({ data: { code: 404, message: 'Not found', data: null } })
      await expect(resumeService.deleteResumeGroup('g1')).rejects.toThrow('Not found')
    })
  })

  describe('editVersion', () => {
    it('edits version content successfully', async () => {
      const version: ResumeVersion = {
        versionId: 'v1',
        groupId: 'g1',
        versionType: 'ORIGINAL',
        status: 'ACTIVE',
        parseStatus: 'COMPLETED',
        originalFileName: 'resume.pdf',
        fileType: 'PDF',
        fileSize: 1024,
        content: 'Updated content',
        editable: true,
        createdAt: '2024-01-01',
        updatedAt: '2024-01-02',
      }
      mockPut.mockResolvedValue({ data: { code: 200, message: 'ok', data: version } })

      const result = await resumeService.editVersion('v1', 'Updated content')
      expect(result.content).toBe('Updated content')
      expect(mockPut).toHaveBeenCalledWith(
        '/v1/resumes/versions/v1',
        { versionId: 'v1', content: 'Updated content' }
      )
    })
  })

  describe('downloadResume', () => {
    it('returns blob for download', async () => {
      const blob = new Blob(['pdf content'])
      mockGet.mockResolvedValue({ data: blob })

      const result = await resumeService.downloadResume('v1', 'pdf')
      expect(result).toBeInstanceOf(Blob)
      expect(mockGet).toHaveBeenCalledWith('/v1/resumes/v1/download', {
        params: { format: 'pdf' },
        responseType: 'blob',
      })
    })

    it('uses no format param when undefined', async () => {
      const blob = new Blob(['content'])
      mockGet.mockResolvedValue({ data: blob })

      await resumeService.downloadResume('v1')
      expect(mockGet).toHaveBeenCalledWith('/v1/resumes/v1/download', {
        params: {},
        responseType: 'blob',
      })
    })
  })
})
