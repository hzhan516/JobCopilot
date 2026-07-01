import { useEffect, useState } from 'react';
import {
  Bot,
  RefreshCw,
  Play,
  RotateCcw,
  Trash2,
  Loader2,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';

import { useAdminStore } from '@/store/admin.store';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import type { ModelVersion } from '@/services/adminService';

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${(bytes / k ** i).toFixed(1)} ${sizes[i]}`;
}

function formatDuration(totalSeconds: number): string {
  const days = Math.floor(totalSeconds / 86_400);
  const hours = Math.floor((totalSeconds % 86_400) / 3_600);
  const minutes = Math.floor((totalSeconds % 3_600) / 60);
  if (days > 0) return `${days}d ${hours}h ${minutes}m`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

export default function AdminAIService() {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [rollbackTarget, setRollbackTarget] = useState<ModelVersion | null>(null);

  const {
    aiStatus,
    modelInfo,
    modelHistory,
    modelActionLoading,
    cacheFlushLoading,
    fetchAIStatus,
    fetchModelInfo,
    fetchModelHistory,
    triggerRetrain,
    rollbackModel,
    flushAICache,
  } = useAdminStore();

  const refresh = async () => {
    setLoading(true);
    try {
      await Promise.all([fetchAIStatus(), fetchModelInfo(), fetchModelHistory()]);
    } catch {
      toast.error(t('admin.ai.loadError'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refresh();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleRetrain = async () => {
    try {
      const result = await triggerRetrain();
      if (result.status === 'completed') {
        toast.success(t('admin.ai.retrainSuccess'));
      } else {
        toast.warning(result.message ?? t('admin.ai.retrainError'));
      }
    } catch {
      toast.error(t('admin.ai.retrainError'));
    }
  };

  const handleRollback = async () => {
    if (!rollbackTarget) return;
    try {
      await rollbackModel(rollbackTarget.version);
      toast.success(t('admin.ai.rollbackSuccess'));
    } catch {
      toast.error(t('admin.ai.rollbackError'));
    } finally {
      setRollbackTarget(null);
    }
  };

  const handleFlushCache = async () => {
    try {
      await flushAICache();
      toast.success(t('admin.ai.flushSuccess'));
    } catch {
      toast.error(t('admin.ai.flushError'));
    }
  };

  const auc = modelInfo?.metrics?.auc ?? modelInfo?.metrics?.AUC;

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">{t('admin.ai.title')}</h1>
        <Button variant="outline" size="sm" onClick={refresh} disabled={loading}>
          {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
          {t('admin.ai.refreshStatus')}
        </Button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-base flex items-center gap-2">
              <Bot className="w-4 h-4" />
              {t('admin.ai.serviceStatus')}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="flex justify-between text-sm">
              <span className="text-gray-500">{t('admin.ai.version')}</span>
              <span className="font-medium">{aiStatus?.version ?? '-'}</span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-500">{t('admin.ai.uptime')}</span>
              <span className="font-medium">
                {aiStatus ? formatDuration(aiStatus.uptime_seconds) : '-'}
              </span>
            </div>
            <div className="flex justify-between text-sm items-center">
              <span className="text-gray-500">{t('admin.ai.mqConnected')}</span>
              {aiStatus?.mq_connected ? (
                <Badge className="bg-green-100 text-green-800 hover:bg-green-100 flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  {t('common.yes')}
                </Badge>
              ) : (
                <Badge variant="destructive" className="flex items-center gap-1">
                  <XCircle className="w-3.5 h-3.5" />
                  {t('common.no')}
                </Badge>
              )}
            </div>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-base">{t('admin.ai.currentModel')}</CardTitle>
          </CardHeader>
          <CardContent>
            {!modelInfo?.loaded ? (
              <div className="text-gray-500">{t('admin.ai.noModel')}</div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
                <div>
                  <div className="text-xs text-gray-500">{t('admin.ai.modelVersion')}</div>
                  <div className="text-lg font-semibold">{modelInfo.version ?? '-'}</div>
                </div>
                <div>
                  <div className="text-xs text-gray-500">{t('admin.ai.trainedAt')}</div>
                  <div className="text-lg font-semibold">
                    {modelInfo.trained_at
                      ? new Date(modelInfo.trained_at).toLocaleString()
                      : '-'}
                  </div>
                </div>
                <div>
                  <div className="text-xs text-gray-500">{t('admin.ai.metricAuc')}</div>
                  <div className="text-lg font-semibold">
                    {typeof auc === 'number' ? auc.toFixed(3) : '-'}
                  </div>
                </div>
              </div>
            )}
            <div className="flex flex-wrap gap-2">
              <Button onClick={handleRetrain} disabled={modelActionLoading}>
                {modelActionLoading ? (
                  <Loader2 className="w-4 h-4 animate-spin mr-1" />
                ) : (
                  <Play className="w-4 h-4 mr-1" />
                )}
                {modelActionLoading ? t('admin.ai.retraining') : t('admin.ai.manualRetrain')}
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">{t('admin.ai.cacheManagement')}</CardTitle>
        </CardHeader>
        <CardContent className="flex items-center gap-4">
          <Button variant="outline" onClick={handleFlushCache} disabled={cacheFlushLoading}>
            {cacheFlushLoading ? (
              <Loader2 className="w-4 h-4 animate-spin mr-1" />
            ) : (
              <Trash2 className="w-4 h-4 mr-1" />
            )}
            {cacheFlushLoading ? t('admin.ai.flushing') : t('admin.ai.flushCache')}
          </Button>
          <span className="text-sm text-gray-500">{t('common.warning')}: {t('admin.ai.flushCache')}</span>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('admin.ai.modelHistory')}</CardTitle>
        </CardHeader>
        <CardContent>
          {modelHistory.length === 0 ? (
            <div className="text-gray-500">{t('admin.ai.noHistory')}</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    <th className="text-left p-3 font-medium">{t('admin.ai.modelVersion')}</th>
                    <th className="text-left p-3 font-medium">{t('admin.ai.size')}</th>
                    <th className="text-left p-3 font-medium">{t('admin.ai.created')}</th>
                    <th className="text-right p-3 font-medium">{t('admin.ai.action')}</th>
                  </tr>
                </thead>
                <tbody>
                  {modelHistory.map((v) => (
                    <tr key={v.key} className="border-b last:border-none hover:bg-gray-50">
                      <td className="p-3 font-mono">{v.version}</td>
                      <td className="p-3">{formatBytes(v.size)}</td>
                      <td className="p-3">
                        {v.last_modified
                          ? new Date(v.last_modified).toLocaleString()
                          : '-'}
                      </td>
                      <td className="p-3 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={modelActionLoading}
                          onClick={() => setRollbackTarget(v)}
                        >
                          <RotateCcw className="w-3.5 h-3.5 mr-1" />
                          {t('admin.ai.rollback')}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={!!rollbackTarget} onOpenChange={(open) => !open && setRollbackTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('common.confirm')}</DialogTitle>
            <DialogDescription>
              {rollbackTarget &&
                t('admin.ai.rollbackConfirm', { version: rollbackTarget.version })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRollbackTarget(null)}>{t('common.cancel')}</Button>
            <Button onClick={handleRollback} disabled={modelActionLoading}>
              {modelActionLoading && <Loader2 className="w-4 h-4 animate-spin mr-1" />}
              {t('admin.ai.rollback')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
