import { useEffect, useState, useMemo } from 'react';
import type { Tracking } from '@/types';
import { useTranslation } from 'react-i18next';
import { formatDate } from '@/utils/i18n';
import { trackingService } from '@/services/trackingService';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  ClipboardList,
  Plus,
  Building2,
  Briefcase,
  Calendar,
  MoreHorizontal,
  Trash2,
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';

export default function TrackingPage() {
  const { t } = useTranslation();
  const [trackings, setTrackings] = useState<Tracking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [newTracking, setNewTracking] = useState({
    jobTitle: '',
    companyName: '',
    status: 'APPLIED' as Tracking['status'],
    notes: '',
  });

  const statusConfig: Record<string, { labelKey: string; color: string }> = useMemo(
    () => ({
      APPLIED: { labelKey: 'tracking.status.APPLIED', color: 'bg-blue-100 text-blue-700' },
      SCREENING: { labelKey: 'tracking.status.SCREENING', color: 'bg-yellow-100 text-yellow-700' },
      INTERVIEW: { labelKey: 'tracking.status.INTERVIEW', color: 'bg-purple-100 text-purple-700' },
      OFFER: { labelKey: 'tracking.status.OFFER', color: 'bg-green-100 text-green-700' },
      REJECTED: { labelKey: 'tracking.status.REJECTED', color: 'bg-red-100 text-red-700' },
      WITHDRAWN: { labelKey: 'tracking.status.WITHDRAWN', color: 'bg-gray-100 text-gray-700' },
    }),
    []
  );

  // 加载投递记录
  useEffect(() => {
    loadTrackings();
  }, []);

  const loadTrackings = async () => {
    try {
      setIsLoading(true);
      const data = await trackingService.getTrackings();
      setTrackings(data);
    } catch {
      toast.error(t('tracking.loadError'));
    } finally {
      setIsLoading(false);
    }
  };

  // 添加投递记录
  const handleAddTracking = async () => {
    if (!newTracking.jobTitle || !newTracking.companyName) {
      toast.error(t('tracking.fillRequired'));
      return;
    }
    try {
      await trackingService.createTracking({
        jobTitle: newTracking.jobTitle,
        companyName: newTracking.companyName,
        notes: newTracking.notes || undefined,
      });
      await loadTrackings();
      setNewTracking({ jobTitle: '', companyName: '', status: 'APPLIED', notes: '' });
      setAddDialogOpen(false);
      toast.success(t('tracking.addSuccess'));
    } catch {
      toast.error(t('tracking.addFailed'));
    }
  };

  // 更新状态
  const handleUpdateStatus = async (trackingId: string, status: Tracking['status']) => {
    try {
      await trackingService.updateTracking(trackingId, { status });
      await loadTrackings();
      toast.success(t('tracking.updateSuccess'));
    } catch {
      toast.error(t('tracking.updateFailed'));
    }
  };

  // 删除投递记录
  const handleDeleteTracking = async (trackingId: string) => {
    try {
      await trackingService.deleteTracking(trackingId);
      await loadTrackings();
      toast.success(t('tracking.deleteSuccess'));
    } catch {
      toast.error(t('tracking.deleteFailed'));
    }
  };

  // 统计各状态数量
  const statusCounts = trackings.reduce((acc, t) => {
    acc[t.status] = (acc[t.status] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  // 渲染加载状态
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900">{t('tracking.title')}</h1>
            <p className="text-gray-500 mt-1">{t('tracking.subtitle')}</p>
          </div>
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
        <Skeleton className="h-[400px]" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{t('tracking.title')}</h1>
          <p className="text-gray-500 mt-1">{t('tracking.subtitle')}</p>
        </div>
        <Button onClick={() => setAddDialogOpen(true)}>
          <Plus className="w-4 h-4 mr-2" />
          {t('tracking.addRecord')}
        </Button>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{t('tracking.stats.applied')}</p>
                <p className="text-2xl font-bold text-blue-600">
                  {statusCounts.APPLIED || 0}
                </p>
              </div>
              <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                <ClipboardList className="w-5 h-5 text-blue-600" />
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{t('tracking.stats.interview')}</p>
                <p className="text-2xl font-bold text-purple-600">
                  {statusCounts.INTERVIEW || 0}
                </p>
              </div>
              <div className="w-10 h-10 bg-purple-100 rounded-full flex items-center justify-center">
                <Briefcase className="w-5 h-5 text-purple-600" />
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{t('tracking.stats.offered')}</p>
                <p className="text-2xl font-bold text-green-600">
                  {statusCounts.OFFER || 0}
                </p>
              </div>
              <div className="w-10 h-10 bg-green-100 rounded-full flex items-center justify-center">
                <Building2 className="w-5 h-5 text-green-600" />
              </div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{t('tracking.stats.total')}</p>
                <p className="text-2xl font-bold text-gray-900">{trackings.length}</p>
              </div>
              <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center">
                <Calendar className="w-5 h-5 text-gray-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 投递记录列表 */}
      <Card>
        <CardHeader className="pb-4">
          <h3 className="text-lg font-semibold">{t('tracking.recordList')}</h3>
        </CardHeader>
        <CardContent>
          {trackings.length === 0 ? (
            <div className="text-center py-12">
              <ClipboardList className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">{t('tracking.emptyTitle')}</h3>
              <p className="text-gray-500 mb-4">{t('tracking.emptyDesc')}</p>
              <Button onClick={() => setAddDialogOpen(true)}>
                <Plus className="w-4 h-4 mr-2" />
                {t('tracking.addRecord')}
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              {trackings.map((tracking) => (
                <div
                  key={tracking.trackingId}
                  className="flex items-start justify-between p-4 border rounded-lg hover:bg-gray-50 transition-colors"
                >
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <h4 className="font-semibold text-gray-900">{tracking.jobTitle}</h4>
                      <Badge className={statusConfig[tracking.status].color}>
                        {t(statusConfig[tracking.status].labelKey)}
                      </Badge>
                    </div>
                    <div className="flex items-center space-x-4 text-sm text-gray-500 mb-2">
                      <span className="flex items-center">
                        <Building2 className="w-4 h-4 mr-1" />
                        {tracking.companyName}
                      </span>
                      <span className="flex items-center">
                        <Calendar className="w-4 h-4 mr-1" />
                        {tracking.appliedAt ? formatDate(tracking.appliedAt) : '-'}
                      </span>
                    </div>
                    {tracking.notes && (
                      <p className="text-sm text-gray-600">{tracking.notes}</p>
                    )}
                  </div>
                  <div className="flex items-center space-x-2">
                    <Select
                      value={tracking.status}
                      onValueChange={(value) =>
                        handleUpdateStatus(tracking.trackingId, value as Tracking['status'])
                      }
                    >
                      <SelectTrigger className="w-32">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {Object.entries(statusConfig).map(([key, { labelKey }]) => (
                          <SelectItem key={key} value={key}>
                            {t(labelKey)}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreHorizontal className="w-4 h-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => handleDeleteTracking(tracking.trackingId)}
                          className="text-red-600"
                        >
                          <Trash2 className="w-4 h-4 mr-2" />
                          {t('common.delete')}
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* 添加记录对话框 */}
      <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('tracking.addRecordTitle')}</DialogTitle>
            <DialogDescription>{t('tracking.addRecordDesc')}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>{t('tracking.jobTitle')}</Label>
              <Input
                placeholder={t('tracking.jobTitlePlaceholder')}
                value={newTracking.jobTitle}
                onChange={(e) =>
                  setNewTracking({ ...newTracking, jobTitle: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>{t('tracking.company')}</Label>
              <Input
                placeholder={t('tracking.companyPlaceholder')}
                value={newTracking.companyName}
                onChange={(e) =>
                  setNewTracking({ ...newTracking, companyName: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>{t('tracking.currentStatus')}</Label>
              <Select
                value={newTracking.status}
                onValueChange={(value) =>
                  setNewTracking({
                    ...newTracking,
                    status: value as Tracking['status'],
                  })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(statusConfig).map(([key, { labelKey }]) => (
                    <SelectItem key={key} value={key}>
                      {t(labelKey)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>{t('tracking.notes')}</Label>
              <Input
                placeholder={t('tracking.notesPlaceholder')}
                value={newTracking.notes}
                onChange={(e) =>
                  setNewTracking({ ...newTracking, notes: e.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleAddTracking}>{t('tracking.add')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
