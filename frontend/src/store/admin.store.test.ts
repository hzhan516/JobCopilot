import { beforeEach, describe, expect, it, vi } from 'vitest';

const adminServiceMock = vi.hoisted(() => ({
  getConfigs: vi.fn(),
  updateConfig: vi.fn(),
  resetConfig: vi.fn(),
  getQueueStats: vi.fn(),
  getAIStatus: vi.fn(),
  getModelInfo: vi.fn(),
  getModelHistory: vi.fn(),
  triggerRetrain: vi.fn(),
  rollbackModel: vi.fn(),
  purgeQueue: vi.fn(),
  retryDlq: vi.fn(),
  flushAICache: vi.fn(),
  listUsers: vi.fn(),
  getStats: vi.fn(),
  getHealth: vi.fn(),
}));

vi.mock('@/services/adminService', () => ({
  adminService: adminServiceMock,
}));

import { useAdminStore } from './admin.store';

const initialState = useAdminStore.getState();

describe('useAdminStore', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAdminStore.setState({
      ...initialState,
      configs: [],
      queueStats: null,
      aiStatus: null,
      modelInfo: null,
      modelHistory: [],
    });
  });

  it('fetches configs', async () => {
    const configs = [{ key: 'k', value: 'v', valueType: 'STRING' }];
    adminServiceMock.getConfigs.mockResolvedValueOnce(configs);

    await useAdminStore.getState().fetchConfigs();

    expect(useAdminStore.getState().configs).toEqual(configs);
    expect(useAdminStore.getState().configsLoading).toBe(false);
  });

  it('updates config and refreshes', async () => {
    adminServiceMock.updateConfig.mockResolvedValueOnce({});
    adminServiceMock.getConfigs.mockResolvedValueOnce([]);

    await useAdminStore.getState().updateConfig('k', 'v');

    expect(adminServiceMock.updateConfig).toHaveBeenCalledWith('k', 'v');
    expect(adminServiceMock.getConfigs).toHaveBeenCalled();
  });

  it('resets config and refreshes', async () => {
    adminServiceMock.resetConfig.mockResolvedValueOnce({});
    adminServiceMock.getConfigs.mockResolvedValueOnce([]);

    await useAdminStore.getState().resetConfig('k');

    expect(adminServiceMock.resetConfig).toHaveBeenCalledWith('k');
    expect(adminServiceMock.getConfigs).toHaveBeenCalled();
  });

  it('fetches queue stats', async () => {
    const stats = { queues: { q1: { depth: 1, consumers: 1 } } };
    adminServiceMock.getQueueStats.mockResolvedValueOnce(stats);

    await useAdminStore.getState().fetchQueueStats();

    expect(useAdminStore.getState().queueStats).toEqual(stats);
  });

  it('fetches AI status', async () => {
    const status = { service: 'ai', version: '1', uptime_seconds: 10, mq_connected: true };
    adminServiceMock.getAIStatus.mockResolvedValueOnce(status);

    await useAdminStore.getState().fetchAIStatus();

    expect(useAdminStore.getState().aiStatus).toEqual(status);
  });

  it('fetches model info and history', async () => {
    const info = { loaded: true, version: 'v1' };
    const history = { versions: [{ key: 'k', size: 1, last_modified: 't', version: 'v1' }] };
    adminServiceMock.getModelInfo.mockResolvedValueOnce(info);
    adminServiceMock.getModelHistory.mockResolvedValueOnce(history);

    await useAdminStore.getState().fetchModelInfo();
    await useAdminStore.getState().fetchModelHistory();

    expect(useAdminStore.getState().modelInfo).toEqual(info);
    expect(useAdminStore.getState().modelHistory).toEqual(history.versions);
  });

  it('triggers retrain and refreshes model data', async () => {
    const result = { status: 'completed', new_version: 'v2' };
    adminServiceMock.triggerRetrain.mockResolvedValueOnce(result);
    adminServiceMock.getModelInfo.mockResolvedValueOnce({ loaded: true, version: 'v2' });
    adminServiceMock.getModelHistory.mockResolvedValueOnce({ versions: [] });

    const returned = await useAdminStore.getState().triggerRetrain();

    expect(returned).toEqual(result);
    expect(adminServiceMock.getModelInfo).toHaveBeenCalled();
    expect(adminServiceMock.getModelHistory).toHaveBeenCalled();
  });

  it('rolls back model and refreshes', async () => {
    adminServiceMock.rollbackModel.mockResolvedValueOnce({});
    adminServiceMock.getModelInfo.mockResolvedValueOnce({ loaded: true, version: 'v1' });
    adminServiceMock.getModelHistory.mockResolvedValueOnce({ versions: [] });

    await useAdminStore.getState().rollbackModel('v1');

    expect(adminServiceMock.rollbackModel).toHaveBeenCalledWith('v1');
  });

  it('purges queue and refreshes stats', async () => {
    const result = { status: 'purged', queue: 'q1', messages_removed: 1 };
    adminServiceMock.purgeQueue.mockResolvedValueOnce(result);
    adminServiceMock.getQueueStats.mockResolvedValueOnce({ queues: {} });

    const returned = await useAdminStore.getState().purgeQueue('q1');

    expect(returned).toEqual(result);
    expect(adminServiceMock.getQueueStats).toHaveBeenCalled();
  });

  it('retries dlq and refreshes stats', async () => {
    const result = { status: 'completed', queue: 'q1', messages_retried: 1 };
    adminServiceMock.retryDlq.mockResolvedValueOnce(result);
    adminServiceMock.getQueueStats.mockResolvedValueOnce({ queues: {} });

    const returned = await useAdminStore.getState().retryDlq('q1');

    expect(returned).toEqual(result);
    expect(adminServiceMock.getQueueStats).toHaveBeenCalled();
  });

  it('flushes AI cache', async () => {
    adminServiceMock.flushAICache.mockResolvedValueOnce({});

    await useAdminStore.getState().flushAICache();

    expect(adminServiceMock.flushAICache).toHaveBeenCalled();
    expect(useAdminStore.getState().cacheFlushLoading).toBe(false);
  });
});
