import { useEffect, useState, useMemo, useCallback, useRef } from 'react';
import type { Tracking, TrackingStatsResponse } from '@/types';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { formatDate, formatDateTime } from '@/utils/i18n';
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
  Clock,
  MoreHorizontal,
  Pencil,
  Trash2,
} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';

const getTodayDateInputValue = () => {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const clampAppliedDate = (value: string) => {
  const today = getTodayDateInputValue();
  return value && value > today ? today : value;
};

const getAppliedDateForStatus = (status: Tracking['status'], appliedAt: string) => {
  const clampedAppliedAt = clampAppliedDate(appliedAt);
  return status === 'APPLIED' && !clampedAppliedAt ? getTodayDateInputValue() : clampedAppliedAt;
};

export default function TrackingPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const dismissedEditTrackingIdRef = useRef<string | null>(null);
  const [trackings, setTrackings] = useState<Tracking[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingTrackingId, setEditingTrackingId] = useState<string | null>(null);
  const [newTracking, setNewTracking] = useState({
    jobTitle: '',
    companyName: '',
    status: 'APPLIED' as Tracking['status'],
    appliedAt: getTodayDateInputValue(),
    notes: '',
  });
  const [editTracking, setEditTracking] = useState({
    jobTitle: '',
    companyName: '',
    status: 'APPLIED' as Tracking['status'],
    appliedAt: '',
    notes: '',
  });
  const [stats, setStats] = useState<TrackingStatsResponse | null>(null);
  const todayDateInputValue = getTodayDateInputValue();

  const statusConfig: Record<string, { labelKey: string; color: string }> = useMemo(
    () => ({
      PENDING: { labelKey: 'tracking.status.PENDING', color: 'bg-gray-100 text-gray-500' },
      APPLIED: { labelKey: 'tracking.status.APPLIED', color: 'bg-blue-100 text-blue-700' },
      SCREENING: { labelKey: 'tracking.status.SCREENING', color: 'bg-yellow-100 text-yellow-700' },
      INTERVIEWING: { labelKey: 'tracking.status.INTERVIEWING', color: 'bg-purple-100 text-purple-700' },
      OFFER: { labelKey: 'tracking.status.OFFER', color: 'bg-green-100 text-green-700' },
      ACCEPTED: { labelKey: 'tracking.status.ACCEPTED', color: 'bg-emerald-100 text-emerald-700' },
      REJECTED: { labelKey: 'tracking.status.REJECTED', color: 'bg-red-100 text-red-700' },
      WITHDRAWN: { labelKey: 'tracking.status.WITHDRAWN', color: 'bg-gray-100 text-gray-700' },
    }),
    []
  );

  useEffect(() => {
    let ignored = false;

    void (async () => {
      try {
        setIsLoading(true);
        const data = await trackingService.getTrackings();
        if (ignored) return;
        setTrackings(data);
      } catch {
        if (!ignored) toast.error(t('tracking.loadError'));
      } finally {
        if (!ignored) setIsLoading(false);
      }

      try {
        const data = await trackingService.getTrackingStats();
        if (ignored) return;
        setStats(data);
      } catch {
        if (!ignored) setStats(null);
      }
    })();

    return () => {
      ignored = true;
    };
  }, [t]);

  const updateEditSearchParam = useCallback((trackingId: string) => {
    dismissedEditTrackingIdRef.current = null;
    setSearchParams((params) => {
      const next = new URLSearchParams(params);
      next.set('edit', trackingId);
      return next;
    }, { replace: true });
  }, [setSearchParams]);

  const clearEditSearchParam = useCallback(() => {
    setSearchParams((params) => {
      const next = new URLSearchParams(params);
      next.delete('edit');
      return next;
    }, { replace: true });
  }, [setSearchParams]);

  const closeEditDialog = useCallback(() => {
    dismissedEditTrackingIdRef.current = editingTrackingId ?? searchParams.get('edit');
    setEditDialogOpen(false);
    setEditingTrackingId(null);
    clearEditSearchParam();
  }, [clearEditSearchParam, editingTrackingId, searchParams]);

  const handleAddTracking = async () => {
    if (!newTracking.jobTitle || !newTracking.companyName) {
      toast.error(t('tracking.fillRequired'));
      return;
    }
    try {
      await trackingService.createTracking({
        jobTitle: newTracking.jobTitle,
        companyName: newTracking.companyName,
        status: newTracking.status,
        appliedAt: getAppliedDateForStatus(newTracking.status, newTracking.appliedAt) || undefined,
        notes: newTracking.notes || undefined,
      });
      await Promise.all([
        trackingService.getTrackings().then(setTrackings),
        trackingService.getTrackingStats().then(setStats).catch(() => setStats(null)),
      ]);
      setNewTracking({ jobTitle: '', companyName: '', status: 'APPLIED', appliedAt: getTodayDateInputValue(), notes: '' });
      setAddDialogOpen(false);
      toast.success(t('tracking.addSuccess'));
    } catch {
      toast.error(t('tracking.addFailed'));
    }
  };

  const openEditDialog = useCallback((tracking: Tracking) => {
    setEditingTrackingId(tracking.trackingId);
    setEditTracking({
      jobTitle: tracking.jobTitle,
      companyName: tracking.companyName,
      status: tracking.status,
      appliedAt: clampAppliedDate(tracking.appliedAt ?? ''),
      notes: tracking.notes ?? '',
    });
    updateEditSearchParam(tracking.trackingId);
    setEditDialogOpen(true);
  }, [updateEditSearchParam]);

  const handleEditTracking = async () => {
    if (!editingTrackingId) return;

    const jobTitle = editTracking.jobTitle.trim();
    const companyName = editTracking.companyName.trim();
    if (!jobTitle || !companyName) {
      toast.error(t('tracking.fillRequired'));
      return;
    }

    try {
      await trackingService.updateTracking(editingTrackingId, {
        jobTitle,
        companyName,
        status: editTracking.status,
        appliedAt: getAppliedDateForStatus(editTracking.status, editTracking.appliedAt) || undefined,
        notes: editTracking.notes,
      });
      await Promise.all([
        trackingService.getTrackings().then(setTrackings),
        trackingService.getTrackingStats().then(setStats).catch(() => setStats(null)),
      ]);
      closeEditDialog();
      toast.success(t('tracking.editSuccess'));
    } catch {
      toast.error(t('tracking.editFailed'));
    }
  };

  const handleUpdateStatus = async (trackingId: string, status: Tracking['status']) => {
    try {
      const tracking = trackings.find((item) => item.trackingId === trackingId);
      await trackingService.updateTracking(trackingId, {
        status,
        appliedAt: getAppliedDateForStatus(status, tracking?.appliedAt ?? '') || undefined,
      });
      await Promise.all([
        trackingService.getTrackings().then(setTrackings),
        trackingService.getTrackingStats().then(setStats).catch(() => setStats(null)),
      ]);
      toast.success(t('tracking.updateSuccess'));
    } catch {
      toast.error(t('tracking.updateFailed'));
    }
  };

  const handleDeleteTracking = async (trackingId: string) => {
    try {
      await trackingService.deleteTracking(trackingId);
      await Promise.all([
        trackingService.getTrackings().then(setTrackings),
        trackingService.getTrackingStats().then(setStats).catch(() => setStats(null)),
      ]);
      toast.success(t('tracking.deleteSuccess'));
    } catch {
      toast.error(t('tracking.deleteFailed'));
    }
  };

  useEffect(() => {
    const editTrackingId = searchParams.get('edit');
    if (!editTrackingId) {
      dismissedEditTrackingIdRef.current = null;
      return;
    }
    if (dismissedEditTrackingIdRef.current === editTrackingId || isLoading || editDialogOpen) return;

    const tracking = trackings.find((item) => item.trackingId === editTrackingId);
    if (tracking) {
      requestAnimationFrame(() => openEditDialog(tracking));
    }
  }, [editDialogOpen, isLoading, openEditDialog, searchParams, trackings]);

  // Derive local counts as fallback when stats API fails
  // 统计各状态数量（作为 stats API 失败时的回退）
  const statusCounts = trackings.reduce((acc, t) => {
    acc[t.status] = (acc[t.status] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  const successRate = useMemo(() => {
    const value = stats?.successRate ?? 0;
    return Number.isFinite(value) ? Math.max(0, Math.min(100, value)) : 0;
  }, [stats]);

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

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-gray-500">{t('tracking.stats.applied')}</p>
                <p className="text-2xl font-bold text-blue-600">
                  {stats?.applied ?? statusCounts.APPLIED ?? 0}
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
                  {stats?.interview ?? statusCounts.INTERVIEWING ?? 0}
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
                  {stats?.offer ?? statusCounts.OFFER ?? 0}
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
                <p className="text-2xl font-bold text-gray-900">
                  {stats?.total ?? trackings.length}
                </p>
              </div>
              <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center">
                <Calendar className="w-5 h-5 text-gray-600" />
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="mt-6">
        <div className="flex justify-between text-sm mb-2">
          <span className="font-medium text-gray-700">{t('tracking.successRate')}</span>
          <span className="text-gray-500">{Math.round(successRate)}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-4">
          <div
            className="bg-green-500 h-4 rounded-full transition-all duration-500"
            style={{ width: `${successRate}%` }}
          />
        </div>
      </div>

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
                      {(() => {
                        const config = statusConfig[tracking.status] || { labelKey: 'tracking.status.UNKNOWN', color: 'bg-gray-100 text-gray-500' };
                        return (
                          <Badge className={config.color}>
                            {t(config.labelKey)}
                          </Badge>
                        );
                      })()}
                    </div>
                    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-sm text-gray-500 mb-2">
                      <span className="flex items-center">
                        <Building2 className="w-4 h-4 mr-1" />
                        {tracking.companyName}
                      </span>
                      {tracking.appliedAt && (
                        <span className="flex items-center">
                          <Calendar className="w-4 h-4 mr-1" />
                          {formatDate(tracking.appliedAt)}
                        </span>
                      )}
                      <span className="flex items-center">
                        <Clock className="w-4 h-4 mr-1" />
                        {t('tracking.lastEditedAt', {
                          time: formatDateTime(tracking.updatedAt || tracking.createdAt),
                        })}
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
                        <DropdownMenuItem onClick={() => openEditDialog(tracking)}>
                          <Pencil className="w-4 h-4 mr-2" />
                          {t('common.edit')}
                        </DropdownMenuItem>
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
                    appliedAt: getAppliedDateForStatus(value as Tracking['status'], newTracking.appliedAt),
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
              <Label>{t('tracking.appliedAt')}</Label>
              <Input
                type="date"
                max={todayDateInputValue}
                value={newTracking.appliedAt}
                onChange={(e) =>
                  setNewTracking({ ...newTracking, appliedAt: clampAppliedDate(e.target.value) })
                }
              />
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

      <Dialog
        open={editDialogOpen}
        onOpenChange={(open) => {
          if (open) setEditDialogOpen(true);
          else closeEditDialog();
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('tracking.editRecordTitle')}</DialogTitle>
            <DialogDescription>{t('tracking.editRecordDesc')}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>{t('tracking.jobTitle')}</Label>
              <Input
                placeholder={t('tracking.jobTitlePlaceholder')}
                value={editTracking.jobTitle}
                onChange={(e) =>
                  setEditTracking({ ...editTracking, jobTitle: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>{t('tracking.company')}</Label>
              <Input
                placeholder={t('tracking.companyPlaceholder')}
                value={editTracking.companyName}
                onChange={(e) =>
                  setEditTracking({ ...editTracking, companyName: e.target.value })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>{t('tracking.currentStatus')}</Label>
              <Select
                value={editTracking.status}
                onValueChange={(value) =>
                  setEditTracking({
                    ...editTracking,
                    status: value as Tracking['status'],
                    appliedAt: getAppliedDateForStatus(value as Tracking['status'], editTracking.appliedAt),
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
              <Label>{t('tracking.appliedAt')}</Label>
              <Input
                type="date"
                max={todayDateInputValue}
                value={editTracking.appliedAt}
                onChange={(e) =>
                  setEditTracking({ ...editTracking, appliedAt: clampAppliedDate(e.target.value) })
                }
              />
            </div>
            <div className="space-y-2">
              <Label>{t('tracking.notes')}</Label>
              <Input
                placeholder={t('tracking.notesPlaceholder')}
                value={editTracking.notes}
                onChange={(e) =>
                  setEditTracking({ ...editTracking, notes: e.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeEditDialog}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleEditTracking}>{t('common.save')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
