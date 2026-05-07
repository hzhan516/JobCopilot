import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Tracking } from '@/types';

const apiMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));

vi.mock('./api', () => ({
  default: apiMock,
}));

import { trackingService } from './trackingService';

const tracking: Tracking = {
  trackingId: 'tracking-1',
  userId: 'user-1',
  job: null,
  companyName: 'Example Inc',
  jobTitle: 'Frontend Engineer',
  status: 'APPLIED',
  appliedAt: '2026-05-07',
  createdAt: '2026-05-07T08:00:00Z',
  updatedAt: '2026-05-07T08:00:00Z',
  notes: 'Applied through company site',
  events: [],
};

describe('trackingService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('creates tracking records with the current editable fields', async () => {
    apiMock.post.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: tracking } });

    const result = await trackingService.createTracking({
      companyName: 'Example Inc',
      jobTitle: 'Frontend Engineer',
      status: 'APPLIED',
      appliedAt: '2026-05-07',
      notes: 'Applied through company site',
    });

    expect(apiMock.post).toHaveBeenCalledWith('/v1/trackings', {
      companyName: 'Example Inc',
      jobTitle: 'Frontend Engineer',
      status: 'APPLIED',
      appliedAt: '2026-05-07',
      notes: 'Applied through company site',
    });
    expect(result).toBe(tracking);
  });

  it('updates tracking records through the tracking id endpoint', async () => {
    apiMock.put.mockResolvedValueOnce({ data: { code: 200, message: 'OK', data: tracking } });

    await trackingService.updateTracking('tracking-1', {
      status: 'INTERVIEWING',
      notes: 'Technical interview scheduled',
    });

    expect(apiMock.put).toHaveBeenCalledWith('/v1/trackings/tracking-1', {
      status: 'INTERVIEWING',
      notes: 'Technical interview scheduled',
    });
  });

  it('maps backend stats to frontend stats and guards invalid success rate', async () => {
    apiMock.get.mockResolvedValueOnce({
      data: {
        code: 200,
        message: 'OK',
        data: {
          totalApplications: 3,
          pendingCount: 1,
          appliedCount: 1,
          interviewingCount: 0,
          offerCount: 1,
          rejectedCount: 0,
          withdrawnCount: 0,
          successRate: Number.NaN,
        },
      },
    });

    await expect(trackingService.getTrackingStats()).resolves.toEqual({
      total: 3,
      pending: 1,
      applied: 1,
      screening: 0,
      interview: 0,
      offer: 1,
      rejected: 0,
      withdrawn: 0,
      successRate: 0,
    });
  });
});
