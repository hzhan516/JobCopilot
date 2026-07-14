import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import ResumesPage from '@/pages/resumes/ResumesPage';
import JobsPage from '@/pages/jobs/JobsPage';
import TrackingPage from '@/pages/tracking/TrackingPage';

const routeParams = vi.hoisted(() => ({ value: {} as Record<string, string> }));

vi.mock('react-router-dom', () => ({
  useParams: () => routeParams.value,
}));

vi.mock('@/pages/resumes/components/ResumeListPanel', () => ({
  default: () => <div data-testid="resume-list-panel" />,
}));
vi.mock('@/pages/resumes/components/ResumeDetailPanel', () => ({
  default: () => <div data-testid="resume-detail-panel" />,
}));
vi.mock('@/pages/jobs/components/JobListPanel', () => ({
  default: () => <div data-testid="job-list-panel" />,
}));
vi.mock('@/pages/jobs/components/JobDetailPanel', () => ({
  default: () => <div data-testid="job-detail-panel" />,
}));
vi.mock('@/pages/tracking/components/TrackingListPanel', () => ({
  default: () => <div data-testid="tracking-list-panel" />,
}));
vi.mock('@/pages/tracking/components/TrackingDetailPanel', () => ({
  default: () => <div data-testid="tracking-detail-panel" />,
}));

describe('central workspace route pages', () => {
  beforeEach(() => {
    routeParams.value = {};
  });

  it.each([
    ['resumes', ResumesPage, 'resume-list-panel'],
    ['jobs', JobsPage, 'job-list-panel'],
    ['applications', TrackingPage, 'tracking-list-panel'],
  ])('renders the %s list in the full workspace when no item is selected', (_name, Page, testId) => {
    render(<Page />);
    expect(screen.getByTestId(testId)).toBeInTheDocument();
  });

  it.each([
    ['resumes', ResumesPage, { groupId: 'resume-1' }, 'resume-detail-panel'],
    ['jobs', JobsPage, { jobId: 'job-1' }, 'job-detail-panel'],
    ['applications', TrackingPage, { trackingId: 'tracking-1' }, 'tracking-detail-panel'],
  ])('renders the %s detail in the same workspace route', (_name, Page, params, testId) => {
    routeParams.value = params;
    render(<Page />);
    expect(screen.getByTestId(testId)).toBeInTheDocument();
  });
});
