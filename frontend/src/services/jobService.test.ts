import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Job } from '@/types';

const apiMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));

vi.mock('./api', () => ({
  default: apiMock,
}));

import { jobService } from './jobService';

const job: Job = {
  id: 'job-1',
  userId: 'user-1',
  originalUrl: 'https://example.com/job/1',
  status: 'PENDING',
  parsedContent: null,
  imageCheckEnabled: false,
  errorMessage: null,
  createdAt: '2026-05-07T08:00:00Z',
};

describe('jobService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits job URL and screenshot as multipart form data', async () => {
    apiMock.post.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: job } });
    const screenshot = new File(['image-bytes'], 'job.png', { type: 'image/png' });

    const result = await jobService.submitJob('https://example.com/job/1', screenshot);

    expect(apiMock.post).toHaveBeenCalledTimes(1);
    const [url, formData, config] = apiMock.post.mock.calls[0];
    expect(url).toBe('/v1/jobs');
    expect(formData).toBeInstanceOf(FormData);
    expect((formData as FormData).get('url')).toBe('https://example.com/job/1');
    expect((formData as FormData).get('screenshot')).toBe(screenshot);
    expect(config).toEqual({ headers: { 'Content-Type': 'multipart/form-data' } });
    expect(result).toBe(job);
  });

  it('deletes jobs through the soft-delete endpoint', async () => {
    apiMock.delete.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: null } });

    await expect(jobService.deleteJob('job-1')).resolves.toBeUndefined();

    expect(apiMock.delete).toHaveBeenCalledWith('/v1/jobs/job-1');
  });

  it('loads score history from the backend history endpoint', async () => {
    apiMock.get.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: [] } });

    await expect(jobService.getScoreHistory()).resolves.toEqual([]);

    expect(apiMock.get).toHaveBeenCalledWith('/v1/jobs/scores/history');
  });
});
