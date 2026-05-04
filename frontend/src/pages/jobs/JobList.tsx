import { useEffect, useState, useCallback } from 'react';
import type { Job, JobScoreResponse, ResumeGroup } from '@/types';
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
  Sparkles,
  Loader2,
  Plus,
  Link as LinkIcon,
  Image,
  Star,
} from 'lucide-react';
import { toast } from 'sonner';

const MAX_SCREENSHOT_SIZE = 5 * 1024 * 1024; // 5MB

export default function JobList() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [filteredJobs, setFilteredJobs] = useState<Job[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('date');

  // 简历列表
  const [resumes, setResumes] = useState<ResumeGroup[]>([]);

  // 新加职位对话框
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [jobUrl, setJobUrl] = useState('');
  const [screenshotFile, setScreenshotFile] = useState<File | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 评分状态（按 jobId + resumeVersionId 记录）
  const [scoringState, setScoringState] = useState<Record<string, boolean>>({});
  const [scoreResults, setScoreResults] = useState<Record<string, JobScoreResponse>>({});

  // 每个职位当前选中的简历版本
  const [selectedResumes, setSelectedResumes] = useState<Record<string, string>>({});

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

  useEffect(() => {
    loadJobs();
    loadResumes();
  }, [loadJobs, loadResumes]);

  // 智能轮询：有简历正在解析时，每3秒刷新一次简历列表
  useEffect(() => {
    const hasPendingResume = resumes.some((group) =>
      [group.originalVersion, group.convertedVersion, group.aiOptimizedVersion]
        .filter((v): v is NonNullable<typeof v> => !!v)
        .some((v) => v.parseStatus === 'PENDING' || v.parseStatus === 'PARSING')
    );
    if (!hasPendingResume) return;

    const interval = setInterval(() => {
      loadResumes();
    }, 3000);

    return () => clearInterval(interval);
  }, [resumes, loadResumes]);

  // 筛选和排序
  useEffect(() => {
    let result = [...jobs];

    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter((job) => {
        const title = job.parsedContent?.title?.toLowerCase() || '';
        const company = job.parsedContent?.company?.toLowerCase() || '';
        const desc = job.parsedContent?.description?.toLowerCase() || '';
        return title.includes(query) || company.includes(query) || desc.includes(query);
      });
    }

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

  // 获取所有可用的简历版本列表（排除原版简历，仅保留转换版和 AI 优化版）
  const availableResumeVersions = resumes.flatMap((group) =>
    [group.convertedVersion, group.aiOptimizedVersion]
      .filter((v): v is NonNullable<typeof v> => !!v && v.exists)
      .map((version) => ({
        versionId: version.versionId,
        label: `${group.title} - ${version.versionId.slice(0, 8)}`,
        parseStatus: version.parseStatus,
      }))
  );

  // 提交新职位
  const handleSubmitJob = async () => {
    if (!jobUrl.trim()) {
      toast.error(t('jobList.urlRequired'));
      return;
    }
    if (!screenshotFile) {
      toast.error(t('jobList.screenshotRequired'));
      return;
    }
    if (screenshotFile.size > MAX_SCREENSHOT_SIZE) {
      toast.error(t('jobList.screenshotTooLarge'));
      return;
    }

    try {
      setIsSubmitting(true);
      await jobService.submitJob(jobUrl.trim(), screenshotFile);
      toast.success(t('jobList.submitSuccess'));
      setAddDialogOpen(false);
      setJobUrl('');
      setScreenshotFile(null);
      loadJobs();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || t('jobList.submitError'));
    } finally {
      setIsSubmitting(false);
    }
  };

  // 开始评分
  const handleScoreJob = async (jobId: string) => {
    const resumeVersionId = selectedResumes[jobId];
    if (!resumeVersionId) {
      toast.error(t('jobList.selectResumeForScore'));
      return;
    }

    const stateKey = `${jobId}_${resumeVersionId}`;
    try {
      setScoringState((prev) => ({ ...prev, [stateKey]: true }));
      const result = await jobService.scoreJob(jobId, { resumeVersionId });
      setScoreResults((prev) => ({ ...prev, [stateKey]: result }));
      toast.success(t('jobList.scoreSuccess'));
    } catch (error: unknown) {
      const err = error as { response?: { status?: number; data?: { message?: string } } };
      if (err.response?.status === 503) {
        toast.warning(err.response?.data?.message || t('jobList.aiServiceUnavailable'));
      } else if (err.response?.status === 422) {
        toast.warning(err.response?.data?.message || t('jobList.scoringNotReady'));
      } else {
        toast.error(err.response?.data?.message || t('jobList.scoreError'));
      }
    } finally {
      setScoringState((prev) => ({ ...prev, [stateKey]: false }));
    }
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
        <Button onClick={() => setAddDialogOpen(true)}>
          <Plus className="w-4 h-4 mr-2" />
          {t('jobList.addJob')}
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
          filteredJobs.map((job) => {
            const currentResumeId = selectedResumes[job.id] || '';
            const currentResume = availableResumeVersions.find((v) => v.versionId === currentResumeId);
            const scoreKey = `${job.id}_${currentResumeId}`;
            const isScoring = scoringState[scoreKey] || false;
            const scoreResult = scoreResults[scoreKey];

            return (
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
                        {job.parsedContent?.location && (
                          <span className="flex items-center">
                            <span className="w-4 h-4 mr-1">📍</span>
                            {job.parsedContent.location}
                          </span>
                        )}
                        {job.parsedContent?.salary && (
                          <span className="flex items-center">
                            <span className="w-4 h-4 mr-1">💰</span>
                            {job.parsedContent.salary}
                          </span>
                        )}
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
                <CardContent className="space-y-4">
                  <p className="text-gray-600 line-clamp-2">
                    {job.parsedContent?.description || t('jobDetail.noDescription')}
                  </p>

                  {/* 评分区域 */}
                  {job.status === 'COMPLETED' && availableResumeVersions.length > 0 && (
                    <div className="border rounded-lg p-4 bg-gray-50/50 space-y-3">
                      <div className="flex flex-col sm:flex-row sm:items-center gap-3">
                        <Select
                          value={currentResumeId}
                          onValueChange={(val) =>
                            setSelectedResumes((prev) => ({ ...prev, [job.id]: val }))
                          }
                        >
                          <SelectTrigger className="w-full sm:w-64">
                            <SelectValue placeholder={t('jobList.selectResumeForScore')} />
                          </SelectTrigger>
                          <SelectContent>
                            {availableResumeVersions.map((rv) => (
                              <SelectItem key={rv.versionId} value={rv.versionId}>
                                {rv.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        <Button
                          size="sm"
                          onClick={() => handleScoreJob(job.id)}
                          disabled={!currentResumeId || isScoring || job.status !== 'COMPLETED' || (currentResume?.parseStatus !== 'COMPLETED')}
                          title={job.status !== 'COMPLETED' ? t('jobList.scoringNotReady') : currentResume?.parseStatus !== 'COMPLETED' ? t('jobList.resumeParsing') : undefined}
                        >
                          {isScoring ? (
                            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                          ) : (
                            <Sparkles className="w-4 h-4 mr-2" />
                          )}
                          {t('jobList.startScore')}
                        </Button>
                      </div>

                      {/* 评分结果 */}
                      {scoreResult && (
                        <div className="rounded-md border bg-white p-3 space-y-2">
                          <div className="flex items-center gap-3">
                            <Badge
                              className={
                                scoreResult.finalScore >= 0.7
                                  ? 'bg-green-100 text-green-700'
                                  : scoreResult.finalScore >= 0.4
                                  ? 'bg-blue-100 text-blue-700'
                                  : 'bg-gray-100 text-gray-700'
                              }
                            >
                              <Star className="w-3 h-3 mr-1" />
                              {t('jobList.matchScore', {
                                score: Math.round(scoreResult.finalScore * 100),
                              })}
                            </Badge>
                            <span className="text-sm text-gray-500">
                              {scoreResult.suitable
                                ? t('jobList.suitable')
                                : t('jobList.notSuitable')}
                            </span>
                          </div>
                          <p className="text-sm text-gray-700">{scoreResult.summary}</p>
                          <div className="flex flex-wrap gap-2 text-xs text-gray-500">
                            <span>
                              {t('jobList.skillScore')}: {Math.round(scoreResult.breakdown.skillScore * 100)}%
                            </span>
                            <span>
                              {t('jobList.experienceScore')}: {Math.round(scoreResult.breakdown.experienceScore * 100)}%
                            </span>
                            <span>
                              {t('jobList.overallScore')}: {Math.round(scoreResult.breakdown.overallScore * 100)}%
                            </span>
                          </div>
                        </div>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })
        )}
      </div>

      {/* 新加职位对话框 */}
      <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('jobList.addJobDialogTitle')}</DialogTitle>
            <DialogDescription>{t('jobList.addJobDialogDesc')}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium mb-2 block flex items-center">
                <LinkIcon className="w-4 h-4 mr-1" />
                {t('jobList.jobUrl')}
              </label>
              <Input
                placeholder={t('jobList.jobUrlPlaceholder')}
                value={jobUrl}
                onChange={(e) => setJobUrl(e.target.value)}
              />
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block flex items-center">
                <Image className="w-4 h-4 mr-1" />
                {t('jobList.jobScreenshot')}
              </label>
              <Input
                type="file"
                accept="image/*"
                onChange={(e) => {
                  const file = e.target.files?.[0] || null;
                  if (file && file.size > MAX_SCREENSHOT_SIZE) {
                    toast.error(t('jobList.screenshotTooLarge'));
                    setScreenshotFile(null);
                    e.target.value = '';
                    return;
                  }
                  setScreenshotFile(file);
                }}
              />
              <p className="text-xs text-gray-500 mt-1">{t('jobList.screenshotHint')}</p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleSubmitJob} disabled={isSubmitting}>
              {isSubmitting ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Plus className="w-4 h-4 mr-2" />
              )}
              {t('jobList.submitJob')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
