import { useEffect, useState, useCallback } from 'react';
import type { Job, MatchItem, ResumeGroup } from '@/types';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { jobService } from '@/services/jobService';
import { resumeService } from '@/services/resumeService';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Briefcase,
  Building2,
  Search,
  Filter,
  ExternalLink,
  Star,
  Sparkles,
  Loader2,
  TrendingUp,
  Target,
  MapPin,
} from 'lucide-react';
import { toast } from 'sonner';

export default function JobList() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [filteredJobs, setFilteredJobs] = useState<Job[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('date');

  // 匹配相关状态
  const [resumes, setResumes] = useState<ResumeGroup[]>([]);
  const [matchDialogOpen, setMatchDialogOpen] = useState(false);
  const [selectedResumeVersionId, setSelectedResumeVersionId] = useState('');
  const [isMatching, setIsMatching] = useState(false);
  const [matchResults, setMatchResults] = useState<MatchItem[]>([]);
  const [matchStatus, setMatchStatus] = useState<'idle' | 'processing' | 'completed' | 'failed'>('idle');
  const [, setActiveMatchId] = useState<string | null>(null);

  const loadJobs = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await jobService.getJobs();
      setJobs(data);
      setFilteredJobs(data);
    } catch {
      toast.error(t('jobList.loadError'));
    } finally {
      setIsLoading(false);
    }
  }, [t]);

  const loadResumes = useCallback(async () => {
    try {
      const data = await resumeService.getResumeGroups();
      setResumes(data);
    } catch {
      // 静默处理
    }
  }, []);

  // 加载职位数据
  useEffect(() => {
    loadJobs();
    loadResumes();
  }, [loadJobs, loadResumes]);

  // 筛选和排序
  useEffect(() => {
    let result = [...jobs];

    // 搜索筛选
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter((job) => {
        const title = job.parsedContent?.title?.toLowerCase() || '';
        const company = job.parsedContent?.company?.toLowerCase() || '';
        const desc = job.parsedContent?.description?.toLowerCase() || '';
        return title.includes(query) || company.includes(query) || desc.includes(query);
      });
    }

    // 排序
    switch (sortBy) {
      case 'date':
        result.sort((a, b) => {
          const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return dateB - dateA;
        });
        break;
      case 'status':
        result.sort((a, b) => a.status.localeCompare(b.status));
        break;
    }

    setFilteredJobs(result);
  }, [jobs, searchQuery, sortBy]);

  // 轮询匹配结果
  const pollMatchResult = useCallback(async (matchId: string) => {
    const maxAttempts = 30;
    let attempts = 0;

    const poll = async () => {
      attempts++;
      try {
        const result = await jobService.getMatchResult(matchId);
        if (result.status === 'COMPLETED') {
          setMatchResults(result.matches);
          setMatchStatus('completed');
          setIsMatching(false);
          return;
        } else if (result.status === 'FAILED') {
          setMatchStatus('failed');
          setIsMatching(false);
          toast.error(t('jobMatch.failed'));
          return;
        }
      } catch {
        // 轮询出错继续尝试
      }

      if (attempts >= maxAttempts) {
        setMatchStatus('failed');
        setIsMatching(false);
        toast.error(t('jobMatch.timeout'));
        return;
      }

      setTimeout(poll, 2000);
    };

    poll();
  }, [t]);

  // 发起匹配
  const handleStartMatch = async () => {
    if (!selectedResumeVersionId) {
      toast.error(t('jobMatch.selectResume'));
      return;
    }

    try {
      setIsMatching(true);
      setMatchStatus('processing');
      setMatchResults([]);

      const result = await jobService.startMatch({
        resumeVersionId: selectedResumeVersionId,
        topK: 10,
      });

      setActiveMatchId(result.matchId);
      pollMatchResult(result.matchId);
    } catch (error: unknown) {
      setIsMatching(false);
      setMatchStatus('failed');

      // 区分向量未就绪(422)与其他错误
      // Distinguish vector not ready (422) from other errors
      const axiosError = error as { response?: { status: number; data?: { message?: string } } };
      if (axiosError.response?.status === 422) {
        toast.error(axiosError.response.data?.message || t('jobMatch.vectorNotReady'));
      } else {
        toast.error(t('jobMatch.startError'));
      }
    }
  };

  // 渲染匹配因子徽章
  const renderMatchFactors = (factors: MatchItem['matchFactors']) => {
    if (!factors) return null;
    const items = [
      { key: 'skillMatch', label: t('jobMatch.skillMatch'), icon: Target, value: factors.skillMatch },
      { key: 'experienceMatch', label: t('jobMatch.experienceMatch'), icon: TrendingUp, value: factors.experienceMatch },
      { key: 'locationMatch', label: t('jobMatch.locationMatch'), icon: MapPin, value: factors.locationMatch },
    ];

    return (
      <div className="flex flex-wrap gap-2 mt-2">
        {items.map((item) => (
          <Badge
            key={item.key}
            variant="secondary"
            className={`text-xs ${
              item.value >= 0.8
                ? 'bg-green-100 text-green-700'
                : item.value >= 0.5
                ? 'bg-blue-100 text-blue-700'
                : 'bg-gray-100 text-gray-700'
            }`}
          >
            <item.icon className="w-3 h-3 mr-1" />
            {item.label}: {Math.round(item.value * 100)}%
          </Badge>
        ))}
      </div>
    );
  };

  // 渲染骨架屏
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{t('jobList.title')}</h1>
          <p className="text-gray-500 mt-1">{t('jobList.subtitle')}</p>
        </div>
        <div className="flex space-x-4">
          <Skeleton className="h-10 flex-1" />
          <Skeleton className="h-10 w-32" />
          <Skeleton className="h-10 w-40" />
        </div>
        <div className="grid gap-4">
          {[1, 2, 3, 4, 5].map((i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 页面标题 */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">{t('jobList.title')}</h1>
          <p className="text-gray-500 mt-1">{t('jobList.subtitle')}</p>
        </div>
        <Button onClick={() => setMatchDialogOpen(true)} disabled={isMatching}>
          {isMatching ? (
            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
          ) : (
            <Sparkles className="w-4 h-4 mr-2" />
          )}
          {isMatching ? t('jobMatch.processing') : t('jobMatch.startMatch')}
        </Button>
      </div>

      {/* 筛选栏 */}
      <div className="flex flex-col lg:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <Input
            placeholder={t('jobList.searchPlaceholder')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
        <div className="flex gap-4">
          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-40">
              <Filter className="w-4 h-4 mr-2" />
              <SelectValue placeholder={t('jobList.sortBy')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="date">{t('jobList.sortDate')}</SelectItem>
              <SelectItem value="status">{t('jobList.sortStatus')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* 匹配结果区域 */}
      {matchStatus === 'completed' && matchResults.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-gray-900 flex items-center">
              <Sparkles className="w-5 h-5 mr-2 text-amber-500" />
              {t('jobMatch.resultTitle')}
            </h2>
            <Button variant="outline" size="sm" onClick={() => { setMatchStatus('idle'); setMatchResults([]); }}>
              {t('common.close')}
            </Button>
          </div>
          <div className="grid gap-4">
            {matchResults.map((match) => (
              <Card key={match.jobId} className="hover:shadow-md transition-shadow border-amber-200">
                <CardHeader className="pb-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-2">
                        <h3 className="text-xl font-semibold text-gray-900">{match.title}</h3>
                        <Badge
                          className={`${
                            match.matchScore >= 80
                              ? 'bg-green-100 text-green-700'
                              : match.matchScore >= 60
                              ? 'bg-blue-100 text-blue-700'
                              : 'bg-gray-100 text-gray-700'
                          }`}
                        >
                          <Star className="w-3 h-3 mr-1" />
                          {t('jobList.matchScore', { score: match.matchScore })}
                        </Badge>
                      </div>
                      <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                        <span className="flex items-center">
                          <Building2 className="w-4 h-4 mr-1" />
                          {match.company}
                        </span>
                      </div>
                      {renderMatchFactors(match.matchFactors)}
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/jobs/${match.jobId}`)}
                    >
                      <ExternalLink className="w-4 h-4 mr-2" />
                      {t('jobList.viewDetails')}
                    </Button>
                  </div>
                </CardHeader>
                <CardContent className="space-y-3">
                  {match.matchReason && (
                    <div className="rounded-md border border-amber-200 bg-amber-50 p-3">
                      <div className="mb-1 flex items-center text-sm font-medium text-amber-900">
                        <Sparkles className="mr-2 h-4 w-4 text-amber-600" />
                        {t('jobMatch.matchReason')}
                      </div>
                      <p className="text-sm leading-6 text-amber-950">{match.matchReason}</p>
                    </div>
                  )}
                  <p className="text-gray-600 line-clamp-2">{match.description}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      )}

      {matchStatus === 'processing' && (
        <div className="flex flex-col items-center justify-center py-12 border rounded-lg bg-blue-50/50">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600 mb-4" />
          <h3 className="text-lg font-medium text-gray-900">{t('jobMatch.processing')}</h3>
          <p className="text-gray-500">{t('jobMatch.processingDesc')}</p>
        </div>
      )}

      {/* 职位列表 */}
      <div className="grid gap-4">
        {filteredJobs.length === 0 ? (
          <Card className="border-dashed">
            <CardContent className="flex flex-col items-center justify-center py-16">
              <Briefcase className="w-16 h-16 text-gray-300 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">{t('jobList.emptyTitle')}</h3>
              <p className="text-gray-500">{t('jobList.emptyDesc')}</p>
            </CardContent>
          </Card>
        ) : (
          filteredJobs.map((job) => (
            <Card key={job.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <h3 className="text-xl font-semibold text-gray-900">
                        {job.parsedContent?.title || t('jobDetail.unknownTitle')}
                      </h3>
                      <Badge variant={job.status === 'COMPLETED' ? 'default' : 'secondary'}>
                        {t(`jobDetail.status.${job.status}`)}
                      </Badge>
                    </div>
                    <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                      <span className="flex items-center">
                        <Building2 className="w-4 h-4 mr-1" />
                        {job.parsedContent?.company || t('jobDetail.unknownCompany')}
                      </span>
                    </div>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => navigate(`/jobs/${job.id}`)}
                  >
                    <ExternalLink className="w-4 h-4 mr-2" />
                    {t('jobList.viewDetails')}
                  </Button>
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-gray-600 line-clamp-2">
                  {job.parsedContent?.description || t('jobDetail.noDescription')}
                </p>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* 发起匹配对话框 */}
      <Dialog open={matchDialogOpen} onOpenChange={setMatchDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('jobMatch.dialogTitle')}</DialogTitle>
            <DialogDescription>{t('jobMatch.dialogDesc')}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block">{t('jobMatch.selectResume')}</label>
              <Select value={selectedResumeVersionId} onValueChange={setSelectedResumeVersionId}>
                <SelectTrigger>
                  <SelectValue placeholder={t('jobMatch.selectResumePlaceholder')} />
                </SelectTrigger>
                <SelectContent>
                  {resumes.map((group) =>
                    [group.originalVersion, group.convertedVersion, group.aiOptimizedVersion]
                      .filter((v): v is NonNullable<typeof v> => !!v && v.exists)
                      .map((version) => {
                        // original 版本需要解析完成(COMPLETED)才允许匹配
                        // original version must be COMPLETED to be selectable for matching
                        const isOriginalNotReady =
                          version === group.originalVersion && version.status !== 'COMPLETED';
                        const label = `${group.title} - ${version.versionId.slice(0, 8)} (${version.status})`;
                        return (
                          <SelectItem
                            key={version.versionId}
                            value={version.versionId}
                            disabled={isOriginalNotReady}
                          >
                            {label}
                            {isOriginalNotReady ? ' - ' + t('jobMatch.notReady') : ''}
                          </SelectItem>
                        );
                      })
                  )}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setMatchDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleStartMatch} disabled={!selectedResumeVersionId || isMatching}>
              {isMatching ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Sparkles className="w-4 h-4 mr-2" />
              )}
              {t('jobMatch.startMatch')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
