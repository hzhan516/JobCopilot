import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ResumeListPanel from '@/pages/resumes/components/ResumeListPanel';
import JobListPanel from '@/pages/jobs/components/JobListPanel';
import TrackingListPanel from '@/pages/tracking/components/TrackingListPanel';

vi.mock('@/pages/resumes/ResumeList', () => ({
  default: () => <div data-testid="resume-list-content" />,
}));

vi.mock('@/pages/jobs/JobList', () => ({
  default: () => <div data-testid="job-list-content" />,
}));

vi.mock('@/pages/tracking/Tracking', () => ({
  default: () => <div data-testid="tracking-list-content" />,
}));

describe('master-detail list panels', () => {
  it.each([
    ['resume', ResumeListPanel, 'resume-list-content'],
    ['job', JobListPanel, 'job-list-content'],
    ['tracking', TrackingListPanel, 'tracking-list-content'],
  ])('%s list establishes a container and blocks horizontal scrolling', (_, Panel, contentId) => {
    render(<Panel />);

    const content = screen.getByTestId(contentId);
    const panel = content.parentElement;

    expect(panel).toHaveClass('master-detail-list-container');
    expect(panel).toHaveClass('overflow-x-hidden');
    expect(panel).toHaveClass('overflow-y-auto');
  });
});
