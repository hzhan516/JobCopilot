import { useEffect, useState } from 'react';
import {
  Activity,
  AlertCircle,
  CheckCircle2,
  FileText,
  Briefcase,
  MessageSquare,
  Server,
  Users,
  RefreshCw,
  Trash2,
  RotateCcw,
  Loader2,
} from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';

import { useAdminStore } from '@/store/admin.store';
import StatCard from '@/components/admin/StatCard';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

import type { ComponentHealth } from '@/services/adminService';

const AUTO_REFRESH_INTERVAL_MS = 30_000;

export default function AdminMonitoring() {
  const { t } = useTranslation();
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [confirmAction, setConfirmAction] = useState<
    { type: 'purge' | 'retry'; queue: string } | null
  >(null);

  const {
    stats,
    health,
    statsLoading,
    queueStats,
    fetchStats,
    fetchQueueStats,
    purgeQueue,
    retryDlq,
  } = useAdminStore();

  const refresh = async () => {
    setRefreshing(true);
    try {
      await Promise.all([fetchStats(), fetchQueueStats()]);
    } catch {
      toast.error(t('admin.monitoring.loadError'));
    } finally {
      setRefreshing(false);
    }
  };

  useEffect(() => {
    const init = async () => {
      setRefreshing(true);
      try {
        await Promise.all([fetchStats(), fetchQueueStats()]);
      } catch {
        toast.error(t('admin.monitoring.loadError'));
      } finally {
        setRefreshing(false);
      }
    };
    init();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!autoRefresh) return undefined;
    const id = setInterval(refresh, AUTO_REFRESH_INTERVAL_MS);
    return () => clearInterval(id);
  }, [autoRefresh]); // eslint-disable-line react-hooks/exhaustive-deps

  const healthItems: { key: keyof ComponentHealth; label: string }[] = [
    { key: 'postgres', label: 'PostgreSQL' },
    { key: 'redis', label: 'Redis' },
    { key: 'rabbitmq', label: 'RabbitMQ' },
    { key: 'aiService', label: 'AI Service' },
    { key: 'minio', label: 'MinIO' },
  ];

  const handleConfirm = async () => {
    if (!confirmAction) return;
    const { type, queue } = confirmAction;
    try {
      if (type === 'purge') {
        await purgeQueue(queue);
        toast.success(t('admin.monitoring.purgeSuccess'));
      } else {
        await retryDlq(queue);
        toast.success(t('admin.monitoring.retryDlqSuccess'));
      }
    } catch {
      toast.error(type === 'purge' ? t('admin.monitoring.purgeError') : t('admin.monitoring.retryDlqError'));
    } finally {
      setConfirmAction(null);
    }
  };

  const queues = queueStats ? Object.entries(queueStats.queues) : [];

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-2xl font-bold">{t('admin.monitoring.title')}</h1>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
            <Checkbox
              checked={autoRefresh}
              onCheckedChange={(checked) => setAutoRefresh(checked === true)}
            />
            {t('admin.monitoring.autoRefresh')}
          </label>
          <Button
            variant="outline"
            size="sm"
            onClick={refresh}
            disabled={refreshing || statsLoading}
          >
            {refreshing || statsLoading ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <RefreshCw className="w-4 h-4" />
            )}
            {t('admin.monitoring.refresh')}
          </Button>
        </div>
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">{t('admin.monitoring.componentHealth')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            {healthItems.map(({ key, label }) => {
              const ok = health?.[key] ?? false;
              return (
                <div
                  key={key}
                  className={`flex items-center gap-2 rounded-lg border px-4 py-3 ${
                    ok
                      ? 'bg-green-50 border-green-200 text-green-800'
                      : 'bg-red-50 border-red-200 text-red-800'
                  }`}
                >
                  {ok ? (
                    <CheckCircle2 className="w-5 h-5" />
                  ) : (
                    <AlertCircle className="w-5 h-5" />
                  )}
                  <span className="text-sm font-medium">{label}</span>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">{t('admin.monitoring.systemStats')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
            <StatCard
              title={t('dashboard.stats.resumes')}
              value={stats?.resumeCount ?? '-'}
              icon={<FileText className="w-5 h-5" />}
            />
            <StatCard
              title={t('dashboard.stats.matches')}
              value={stats?.jobCount ?? '-'}
              icon={<Briefcase className="w-5 h-5" />}
            />
            <StatCard
              title={t('dashboard.stats.chats')}
              value={stats?.conversationCount ?? '-'}
              icon={<MessageSquare className="w-5 h-5" />}
            />
            <StatCard
              title={t('tracking.stats.applied')}
              value={stats?.applicationCount ?? '-'}
              icon={<Activity className="w-5 h-5" />}
            />
            <StatCard
              title={t('dashboard.stats.applications')}
              value={stats?.aiCallCount ?? '-'}
              icon={<Server className="w-5 h-5" />}
            />
            <StatCard
              title={t('admin.monitoring.queueStatus')}
              value={stats?.userCount ?? '-'}
              icon={<Users className="w-5 h-5" />}
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('admin.monitoring.queueStatus')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="text-left p-3 font-medium">{t('admin.monitoring.queueName')}</th>
                  <th className="text-left p-3 font-medium">{t('admin.monitoring.depth')}</th>
                  <th className="text-left p-3 font-medium">{t('admin.monitoring.consumers')}</th>
                  <th className="text-right p-3 font-medium">{t('admin.monitoring.actions')}</th>
                </tr>
              </thead>
              <tbody>
                {queues.length === 0 && (
                  <tr>
                    <td colSpan={4} className="p-6 text-center text-gray-400">
                      {t('admin.monitoring.noQueues')}
                    </td>
                  </tr>
                )}
                {queues.map(([name, stat]) => (
                  <tr key={name} className="border-b last:border-none hover:bg-gray-50">
                    <td className="p-3 font-mono text-xs">{name}</td>
                    <td className="p-3">{typeof stat.depth === 'number' ? stat.depth : stat.depth}</td>
                    <td className="p-3">{stat.consumers}</td>
                    <td className="p-3 text-right space-x-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setConfirmAction({ type: 'purge', queue: name })}
                      >
                        <Trash2 className="w-3.5 h-3.5 mr-1" />
                        {t('admin.monitoring.purge')}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setConfirmAction({ type: 'retry', queue: name })}
                      >
                        <RotateCcw className="w-3.5 h-3.5 mr-1" />
                        {t('admin.monitoring.retryDlq')}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      <Dialog open={!!confirmAction} onOpenChange={(open) => !open && setConfirmAction(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('common.confirm')}</DialogTitle>
            <DialogDescription>
              {confirmAction?.type === 'purge'
                ? t('admin.monitoring.purgeConfirm', { queue: confirmAction.queue })
                : t('admin.monitoring.retryDlqConfirm', { queue: confirmAction?.queue ?? '' })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmAction(null)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant={confirmAction?.type === 'purge' ? 'destructive' : 'default'}
              onClick={handleConfirm}
            >
              {t('common.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
