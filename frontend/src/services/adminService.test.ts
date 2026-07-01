import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiMock = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: apiMock,
}));

import { adminService } from './adminService';

describe('adminService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('gets configs', async () => {
    const configs = [{ key: 'captcha.enabled', value: 'true', valueType: 'BOOLEAN' }];
    apiMock.get.mockResolvedValueOnce({ data: { code: 200, data: configs } });

    const result = await adminService.getConfigs();

    expect(apiMock.get).toHaveBeenCalledWith('/api/admin/v1/config');
    expect(result).toEqual(configs);
  });

  it('updates config', async () => {
    const updated = { key: 'captcha.enabled', value: 'false', valueType: 'BOOLEAN' };
    apiMock.put.mockResolvedValueOnce({ data: { code: 200, data: updated } });

    const result = await adminService.updateConfig('captcha.enabled', false);

    expect(apiMock.put).toHaveBeenCalledWith('/api/admin/v1/config/captcha.enabled', { value: 'false' });
    expect(result).toEqual(updated);
  });

  it('resets config', async () => {
    const reset = { key: 'captcha.enabled', value: 'true', valueType: 'BOOLEAN' };
    apiMock.post.mockResolvedValueOnce({ data: { code: 200, data: reset } });

    const result = await adminService.resetConfig('captcha.enabled');

    expect(apiMock.post).toHaveBeenCalledWith('/api/admin/v1/config/captcha.enabled/reset');
    expect(result).toEqual(reset);
  });

  it('gets queue stats', async () => {
    const stats = { queues: { q1: { depth: 3, consumers: 1 } } };
    apiMock.get.mockResolvedValueOnce({ data: stats });

    const result = await adminService.getQueueStats();

    expect(apiMock.get).toHaveBeenCalledWith('/api/admin/v1/monitoring/queues');
    expect(result).toEqual(stats);
  });

  it('purges queue', async () => {
    const response = { status: 'purged', queue: 'q1', messages_removed: 3 };
    apiMock.post.mockResolvedValueOnce({ data: response });

    const result = await adminService.purgeQueue('q1');

    expect(apiMock.post).toHaveBeenCalledWith('/api/admin/v1/monitoring/queues/q1/purge');
    expect(result).toEqual(response);
  });

  it('retries dlq', async () => {
    const response = { status: 'completed', queue: 'q1', messages_retried: 2 };
    apiMock.post.mockResolvedValueOnce({ data: response });

    const result = await adminService.retryDlq('q1');

    expect(apiMock.post).toHaveBeenCalledWith('/api/admin/v1/monitoring/queues/q1/retry-dlq');
    expect(result).toEqual(response);
  });

  it('gets AI status', async () => {
    const status = { service: 'ai', version: '0.2.0', uptime_seconds: 120, mq_connected: true };
    apiMock.get.mockResolvedValueOnce({ data: status });

    const result = await adminService.getAIStatus();

    expect(apiMock.get).toHaveBeenCalledWith('/api/admin/v1/ai/status');
    expect(result).toEqual(status);
  });

  it('gets model info', async () => {
    const info = { loaded: true, version: 'v42' };
    apiMock.get.mockResolvedValueOnce({ data: info });

    const result = await adminService.getModelInfo();

    expect(apiMock.get).toHaveBeenCalledWith('/api/admin/v1/ai/model/info');
    expect(result).toEqual(info);
  });

  it('gets model history', async () => {
    const history = { versions: [{ key: 'ranker_model_v42.txt', size: 1024, last_modified: '2026-06-30', version: 'v42' }] };
    apiMock.get.mockResolvedValueOnce({ data: history });

    const result = await adminService.getModelHistory();

    expect(apiMock.get).toHaveBeenCalledWith('/api/admin/v1/ai/model/history');
    expect(result).toEqual(history);
  });

  it('triggers retrain', async () => {
    const response = { status: 'completed', new_version: 'v43' };
    apiMock.post.mockResolvedValueOnce({ data: response });

    const result = await adminService.triggerRetrain();

    expect(apiMock.post).toHaveBeenCalledWith('/api/admin/v1/ai/model/retrain');
    expect(result).toEqual(response);
  });

  it('rolls back model', async () => {
    const response = { status: 'rolled_back', version: 'v42' };
    apiMock.post.mockResolvedValueOnce({ data: response });

    const result = await adminService.rollbackModel('v42');

    expect(apiMock.post).toHaveBeenCalledWith('/api/admin/v1/ai/model/rollback', { version: 'v42' });
    expect(result).toEqual(response);
  });

  it('flushes AI cache', async () => {
    const response = { status: 'flushed', keys_deleted: 5 };
    apiMock.post.mockResolvedValueOnce({ data: response });

    const result = await adminService.flushAICache();

    expect(apiMock.post).toHaveBeenCalledWith('/api/admin/v1/ai/cache/flush');
    expect(result).toEqual(response);
  });
});
