import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';

const mockStore = vi.hoisted(() => ({
  aiStatus: null,
  modelInfo: null,
  modelHistory: [],
  modelActionLoading: false,
  cacheFlushLoading: false,
  fetchAIStatus: vi.fn(),
  fetchModelInfo: vi.fn(),
  fetchModelHistory: vi.fn(),
  triggerRetrain: vi.fn(),
  rollbackModel: vi.fn(),
  flushAICache: vi.fn(),
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

import AdminAIService from './AdminAIService';

describe('AdminAIService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockStore.aiStatus = null;
    mockStore.modelInfo = null;
    mockStore.modelHistory = [];
    mockStore.modelActionLoading = false;
    mockStore.cacheFlushLoading = false;
  });

  it('renders title and fetches data on mount', async () => {
    render(<AdminAIService />);

    expect(screen.getByText('admin.ai.title')).toBeInTheDocument();
    await waitFor(() => {
      expect(mockStore.fetchAIStatus).toHaveBeenCalled();
      expect(mockStore.fetchModelInfo).toHaveBeenCalled();
      expect(mockStore.fetchModelHistory).toHaveBeenCalled();
    });
  });

  it('renders service status', async () => {
    mockStore.aiStatus = {
      service: 'jobcopilot-ai-service',
      version: '0.2.0',
      uptime_seconds: 3600,
      mq_connected: true,
    };

    render(<AdminAIService />);

    await waitFor(() => {
      expect(screen.getByText('0.2.0')).toBeInTheDocument();
      expect(screen.getByText('common.yes')).toBeInTheDocument();
    });
  });

  it('renders current model info', async () => {
    mockStore.modelInfo = {
      loaded: true,
      version: 'v42',
      trained_at: '2026-06-30T10:00:00Z',
      metrics: { auc: 0.847 },
    };

    render(<AdminAIService />);

    await waitFor(() => {
      expect(screen.getByText('v42')).toBeInTheDocument();
      expect(screen.getByText('0.847')).toBeInTheDocument();
    });
  });

  it('renders model history and rollback action', async () => {
    mockStore.modelHistory = [
      { key: 'ranker_model_v42.txt', size: 1024, last_modified: '2026-06-30T10:00:00Z', version: 'v42' },
    ];

    render(<AdminAIService />);

    await waitFor(() => {
      expect(screen.getByText('v42')).toBeInTheDocument();
      expect(screen.getByText('admin.ai.rollback')).toBeInTheDocument();
    });
  });

  it('triggers manual retrain on click', async () => {
    mockStore.modelInfo = { loaded: true, version: 'v42' };
    mockStore.triggerRetrain.mockResolvedValueOnce({ status: 'completed', new_version: 'v43' });

    render(<AdminAIService />);

    await waitFor(() => screen.getByText('admin.ai.manualRetrain'));
    fireEvent.click(screen.getByText('admin.ai.manualRetrain'));

    await waitFor(() => {
      expect(mockStore.triggerRetrain).toHaveBeenCalled();
    });
  });
});
