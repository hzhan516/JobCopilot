import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';

const mockStore = vi.hoisted(() => ({
  stats: null,
  health: null,
  queueStats: null,
  statsLoading: false,
  fetchStats: vi.fn(),
  fetchQueueStats: vi.fn(),
  purgeQueue: vi.fn(),
  retryDlq: vi.fn(),
}));

vi.mock('@/store/admin.store', () => ({
  useAdminStore: (selector?: (state: typeof mockStore) => unknown) =>
    selector ? selector(mockStore) : mockStore,
}));

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}));

import AdminMonitoring from './AdminMonitoring';

describe('AdminMonitoring', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockStore.stats = null;
    mockStore.health = null;
    mockStore.queueStats = null;
    mockStore.statsLoading = false;
  });

  it('renders title and refreshes on mount', async () => {
    render(<AdminMonitoring />);

    expect(screen.getByText('admin.monitoring.title')).toBeInTheDocument();
    await waitFor(() => {
      expect(mockStore.fetchStats).toHaveBeenCalled();
      expect(mockStore.fetchQueueStats).toHaveBeenCalled();
    });
  });

  it('renders health status grid', async () => {
    mockStore.health = {
      postgres: true,
      redis: false,
      rabbitmq: true,
      aiService: true,
      minio: false,
    };

    render(<AdminMonitoring />);

    await waitFor(() => {
      expect(screen.getByText('PostgreSQL')).toBeInTheDocument();
      expect(screen.getByText('Redis')).toBeInTheDocument();
    });
  });

  it('renders queue table with actions', async () => {
    mockStore.queueStats = {
      queues: {
        'ai.queue.job.parse': { depth: 3, consumers: 1 },
        'ai.queue.feedback': { depth: 0, consumers: 1 },
      },
    };

    render(<AdminMonitoring />);

    await waitFor(() => {
      expect(screen.getByText('ai.queue.job.parse')).toBeInTheDocument();
      expect(screen.getByText('3')).toBeInTheDocument();
      expect(screen.getAllByText('admin.monitoring.purge').length).toBe(2);
    });
  });

  it('opens confirm dialog when purge is clicked', async () => {
    mockStore.queueStats = {
      queues: {
        'ai.queue.job.parse': { depth: 1, consumers: 1 },
      },
    };

    render(<AdminMonitoring />);

    await waitFor(() => screen.getByText('ai.queue.job.parse'));
    fireEvent.click(screen.getByText('admin.monitoring.purge'));

    await waitFor(() => {
      expect(screen.getByText('common.confirm')).toBeInTheDocument();
    });
  });
});
