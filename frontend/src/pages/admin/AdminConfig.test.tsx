import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';

const mockStore = vi.hoisted(() => ({
  configs: [],
  configsLoading: false,
  fetchConfigs: vi.fn().mockResolvedValue(undefined),
  updateConfig: vi.fn().mockResolvedValue(undefined),
  resetConfig: vi.fn().mockResolvedValue(undefined),
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
  },
}));

import AdminConfig from './AdminConfig';

describe('AdminConfig', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockStore.configs = [];
    mockStore.configsLoading = false;
  });

  it('renders title and fetches configs', async () => {
    render(<AdminConfig />);

    expect(screen.getByText('admin.config.title')).toBeInTheDocument();
    await waitFor(() => expect(mockStore.fetchConfigs).toHaveBeenCalled());
  });

  it('groups configs by category', async () => {
    mockStore.configs = [
      {
        key: 'captcha.enabled',
        value: 'true',
        defaultValue: 'false',
        description: '',
        category: 'Security',
        valueType: 'BOOLEAN',
        sensitive: false,
        readOnly: false,
        updatedBy: null,
        updatedAt: null,
      },
      {
        key: 'ai.textModel',
        value: 'gemini/model',
        defaultValue: '',
        description: '',
        category: 'AI',
        valueType: 'STRING',
        sensitive: false,
        readOnly: false,
        updatedBy: null,
        updatedAt: null,
      },
    ];

    render(<AdminConfig />);

    await waitFor(() => {
      expect(screen.getByText('Security')).toBeInTheDocument();
      expect(screen.getByText('AI')).toBeInTheDocument();
      expect(screen.getByText('captcha.enabled')).toBeInTheDocument();
      expect(screen.getByText('ai.textModel')).toBeInTheDocument();
    });
  });

  it('filters configs by search term', async () => {
    mockStore.configs = [
      {
        key: 'captcha.enabled',
        value: 'true',
        defaultValue: 'false',
        description: '',
        category: 'Security',
        valueType: 'BOOLEAN',
        sensitive: false,
        readOnly: false,
        updatedBy: null,
        updatedAt: null,
      },
    ];

    render(<AdminConfig />);

    await waitFor(() => screen.getByText('captcha.enabled'));
    const input = screen.getByPlaceholderText('admin.config.searchPlaceholder');
    fireEvent.change(input, { target: { value: 'nothing' } });

    await waitFor(() => {
      expect(screen.queryByText('captcha.enabled')).not.toBeInTheDocument();
    });
  });

  it('opens edit dialog for a config', async () => {
    mockStore.configs = [
      {
        key: 'captcha.enabled',
        value: 'true',
        defaultValue: 'false',
        description: '',
        category: 'Security',
        valueType: 'BOOLEAN',
        sensitive: false,
        readOnly: false,
        updatedBy: null,
        updatedAt: null,
      },
    ];

    render(<AdminConfig />);

    await waitFor(() => screen.getByText('captcha.enabled'));
    fireEvent.click(screen.getByText('common.edit'));

    await waitFor(() => {
      expect(screen.getByText('admin.config.editTitle')).toBeInTheDocument();
    });
  });
});
