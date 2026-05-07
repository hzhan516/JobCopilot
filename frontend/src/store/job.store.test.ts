import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Job } from '@/types';

const jobServiceMock = vi.hoisted(() => ({
  getJobs: vi.fn(),
  submitJob: vi.fn(),
  deleteJob: vi.fn(),
  scoreJob: vi.fn(),
  getScoreHistory: vi.fn(),
}));

vi.mock('@/services/jobService', () => ({
  jobService: jobServiceMock,
  default: jobServiceMock,
}));

vi.mock('@/i18n', () => ({
  default: {
    language: 'en',
    t: (key: string) => key,
    changeLanguage: vi.fn(),
  },
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

import { useJobStore } from './job.store';

function makeJob(id: string, title: string, company: string, createdAt: string, status: Job['status'] = 'COMPLETED'): Job {
  return {
    id,
    userId: 'user-1',
    originalUrl: `https://example.com/${id}`,
    status,
    parsedContent: {
      title,
      company,
      salary: '',
      location: '',
      description: `${title} at ${company}`,
      requirements: [],
    },
    imageCheckEnabled: false,
    errorMessage: null,
    createdAt,
  };
}

const initialState = useJobStore.getState();

describe('useJobStore', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useJobStore.setState({
      jobs: [],
      filteredJobs: [],
      loading: false,
      error: null,
      filters: { searchQuery: '', sortBy: 'date' },
      scoreResults: {},
      scoringState: {},
      selectedResumes: {},
    });
  });

  it('fetches jobs and sorts newest first by default', async () => {
    const older = makeJob('job-1', 'Backend Engineer', 'Old Co', '2026-05-01T00:00:00Z');
    const newer = makeJob('job-2', 'Frontend Engineer', 'New Co', '2026-05-07T00:00:00Z');
    jobServiceMock.getJobs.mockResolvedValueOnce([older, newer]);

    await initialState.fetchJobs();

    expect(useJobStore.getState().jobs).toEqual([older, newer]);
    expect(useJobStore.getState().filteredJobs.map((job) => job.id)).toEqual(['job-2', 'job-1']);
    expect(useJobStore.getState().loading).toBe(false);
  });

  it('filters jobs by title, company, and description text', async () => {
    const jobs = [
      makeJob('job-1', 'Backend Engineer', 'Desert Cloud', '2026-05-01T00:00:00Z'),
      makeJob('job-2', 'Frontend Engineer', 'PixelWorks Labs', '2026-05-07T00:00:00Z'),
    ];
    useJobStore.setState({ jobs, filteredJobs: jobs });

    useJobStore.getState().setSearchQuery('pixelworks');

    expect(useJobStore.getState().filteredJobs.map((job) => job.id)).toEqual(['job-2']);
  });

  it('removes a deleted job and clears related score state', async () => {
    const jobs = [
      makeJob('job-1', 'Backend Engineer', 'Desert Cloud', '2026-05-01T00:00:00Z'),
      makeJob('job-2', 'Frontend Engineer', 'PixelWorks Labs', '2026-05-07T00:00:00Z'),
    ];
    useJobStore.setState({
      jobs,
      filteredJobs: jobs,
      selectedResumes: { 'job-1': 'resume-1', 'job-2': 'resume-2' },
      scoreResults: {
        'job-1_resume-1': {
          suitable: true,
          summary: 'Good match',
          finalScore: 90,
          breakdown: { skillScore: 90, experienceScore: 90, overallScore: 90 },
        },
      },
      scoringState: { 'job-1_resume-1': false },
    });
    jobServiceMock.deleteJob.mockResolvedValueOnce(undefined);

    await useJobStore.getState().deleteJob('job-1');

    const state = useJobStore.getState();
    expect(state.jobs.map((job) => job.id)).toEqual(['job-2']);
    expect(state.filteredJobs.map((job) => job.id)).toEqual(['job-2']);
    expect(state.selectedResumes).toEqual({ 'job-2': 'resume-2' });
    expect(state.scoreResults).toEqual({});
    expect(state.scoringState).toEqual({});
  });
});
