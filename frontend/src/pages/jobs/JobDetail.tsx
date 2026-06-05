import { useEffect, useState, useCallback, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { jobService } from '@/services/jobService';
import { resumeService } from '@/services/resumeService';
import type { Job, JobScoreResponse, ResumeGroup } from '@/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import {
  ArrowLeft,
  Building2,
  ExternalLink,
  List,
  FileText,
  Sparkles,
  Loader2,
  Save,
  X,
  Pencil,
  Star,
  MapPin,
  DollarSign,
  Trash2,
  Plus,
} from 'lucide-react';
import { toast } from 'sonner';

export default function JobDetail() {
  const { t } = useTranslation();
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  const hasTrackedClick = useRef(false);
  const [job, setJob] = useState<Job | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [resumes, setResumes] = useState<ResumeGroup[]>([]);

  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    title: '',
    company: '',
    salary: '',
    location: '',
    description: '',
    requirements: [] as string[],
  });

  const [selectedResumeVersionId, setSelectedResumeVersionId] = useState<string>('');
  const [isScoring, setIsScoring] = useState(false);
  const [scoreResult, setScoreResult] = useState<JobScoreResponse | null>(null);

  const loadJob = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await jobService.getJob(jobId!);
      setJob(data);
      if (data.parsedContent) {
        setEditForm({
          title: data.parsedContent.title || '',
          company: data.parsedContent.company || '',
          salary: data.parsedContent.salary || '',
          location: data.parsedContent.location || '',
          description: data.parsedContent.description || '',
          requirements: data.parsedContent.requirements || [],
        });
      }
    } catch {
      toast.error(t('jobDetail.loadError'));
    } finally {
      setIsLoading(false);
    }
  }, [jobId, t]);

  const loadResumes = useCallback(async () => {
    try {
      const data = await resumeService.getResumeGroups();
      setResumes(data);
      // Prefer converted over aiOptimized for scoring; original version is fallback-only
      // 默认选择第一个可用的非原版简历（优先 converted，其次 aiOptimized）
      setSelectedResumeVersionId((prev) => {
        if (prev) return prev;
        if (data.length > 0) {
          const firstAvailable =
            data[0].convertedVersion ?? data[0].aiOptimizedVersion ?? null;
          if (firstAvailable) {
            return firstAvailable.versionId;
          }
        }
        return prev;
      });
    } catch {
      // Silently degrade
      // 静默降级
    }
  }, []);

  const loadScoreHistory = useCallback(async () => {
    if (!jobId) return;
    try {
      const history = await jobService.getScoreHistory();
      // History is sorted by createdAt desc; take the first match for this job
      // 过滤当前职位的评分记录，取最新的一条（history 已按 createdAt 降序）
      const jobScores = history.filter((r) => r.jobId === jobId);
      if (jobScores.length > 0) {
        const latest = jobScores[0];
        setScoreResult({
          suitable: latest.suitable,
          summary: latest.summary,
          finalScore: latest.finalScore,
          breakdown: {
            skillScore: latest.skillScore,
            experienceScore: latest.experienceScore,
            overallScore: latest.overallScore,
          },
        });
        setSelectedResumeVersionId(latest.resumeVersionId);
      }
    } catch {
      // Silently degrade
      // 静默降级
    }
  }, [jobId]);

  useEffect(() => {
    if (jobId) {
      loadJob();
      loadResumes();
      loadScoreHistory();
      // Track view action securely (Strict Mode safe)
      // 安全记录职位浏览行为，并兼容 React Strict Mode 的重复执行
      if (!hasTrackedClick.current) {
        hasTrackedClick.current = true;
        jobService.trackAction(jobId, 'CLICK').catch(err => {
          console.warn('Failed to track job click', err);
        });
      }
    }
  }, [jobId, loadJob, loadResumes, loadScoreHistory]);

  // Poll resume parse status every 3s when any resume is pending
  // 有简历正在解析时，每 3 秒刷新一次简历列表
  const hasPendingResume = resumes.some((group) =>
    [group.originalVersion, group.convertedVersion, group.aiOptimizedVersion]
      .filter((v): v is NonNullable<typeof v> => !!v)
      .some((v) => v.parseStatus === 'PENDING' || v.parseStatus === 'PARSING')
  );

  useEffect(() => {
    if (!hasPendingResume) return;

    const interval = setInterval(() => {
      loadResumes();
    }, 3000);

    return () => clearInterval(interval);
  }, [hasPendingResume, loadResumes]);

  const handleSave = async () => {
    try {
      await jobService.updateJob(jobId!, {
        title: editForm.title,
        company: editForm.company,
        salary: editForm.salary,
        location: editForm.location,
        description: editForm.description,
        requirements: editForm.requirements.filter((r) => r.trim() !== ''),
      });
      toast.success(t('jobDetail.saveSuccess'));
      setIsEditing(false);
      loadJob();
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || t('jobDetail.saveError'));
    }
  };

  const handleCancelEdit = () => {
    if (job?.parsedContent) {
      setEditForm({
        title: job.parsedContent.title || '',
        company: job.parsedContent.company || '',
        salary: job.parsedContent.salary || '',
        location: job.parsedContent.location || '',
        description: job.parsedContent.description || '',
        requirements: job.parsedContent.requirements || [],
      });
    }
    setIsEditing(false);
  };

  const handleScore = async () => {
    if (!selectedResumeVersionId) {
      toast.error(t('jobDetail.selectResumeError'));
      return;
    }
    try {
      setIsScoring(true);
      const result = await jobService.scoreJob(jobId!, { resumeVersionId: selectedResumeVersionId });
      setScoreResult(result);
      toast.success(t('jobDetail.scoreSuccess'));
      loadScoreHistory(); // 刷新评分历史
    } catch (error: unknown) {
      const err = error as { response?: { status?: number; data?: { message?: string } } };
      if (err.response?.status === 503) {
        toast.warning(err.response?.data?.message || t('jobDetail.aiServiceUnavailable'));
      } else if (err.response?.status === 422) {
        toast.warning(err.response?.data?.message || t('jobDetail.scoringNotReady'));
      } else {
        toast.error(err.response?.data?.message || t('jobDetail.scoreError'));
      }
    } finally {
      setIsScoring(false);
    }
  };

  const handleTrackAction = async (action: 'APPLY' | 'REJECT') => {
    try {
      await jobService.trackAction(jobId!, action, selectedResumeVersionId || undefined);
      toast.success(action === 'APPLY' ? t('jobDetail.trackAppliedSuccess') : t('jobDetail.trackRejectedSuccess'));
    } catch (error: unknown) {
      toast.error(error instanceof Error ? error.message : t('jobDetail.trackActionError'));
    }
  };

  const updateRequirement = (index: number, value: string) => {
    const next = [...editForm.requirements];
    next[index] = value;
    setEditForm((prev) => ({ ...prev, requirements: next }));
  };

  const addRequirement = () => {
    setEditForm((prev) => ({ ...prev, requirements: [...prev.requirements, ''] }));
  };

  const removeRequirement = (index: number) => {
    setEditForm((prev) => ({
      ...prev,
      requirements: prev.requirements.filter((_, i) => i !== index),
    }));
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-64" />
      </div>
    );
  }

  if (!job) {
    return (
      <div className="text-center py-12">
        <h2 className="text-2xl font-semibold mb-4">{t('jobDetail.notFound')}</h2>
        <Button onClick={() => navigate('/jobs')}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          {t('jobDetail.backToList')}
        </Button>
      </div>
    );
  }

  const parsed = job.parsedContent;
  const selectedResumeParseStatus = resumes
    .flatMap((g) => [g.originalVersion, g.convertedVersion, g.aiOptimizedVersion])
    .find((v) => v?.versionId === selectedResumeVersionId)?.parseStatus;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Button variant="ghost" onClick={() => navigate('/jobs')} className="pl-0">
          <ArrowLeft className="w-4 h-4 mr-2" />
          {t('jobDetail.backToList')}
        </Button>
        {!isEditing && (
          <Button variant="outline" size="sm" onClick={() => setIsEditing(true)}>
            <Pencil className="w-4 h-4 mr-2" />
            {t('jobDetail.edit')}
          </Button>
        )}
      </div>

      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div className="flex-1">
          {isEditing ? (
            <div className="space-y-3">
              <Input
                value={editForm.title}
                onChange={(e) => setEditForm((p) => ({ ...p, title: e.target.value }))}
                placeholder={t('jobDetail.titlePlaceholder')}
                className="text-xl font-bold"
              />
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Input
                  value={editForm.company}
                  onChange={(e) => setEditForm((p) => ({ ...p, company: e.target.value }))}
                  placeholder={t('jobDetail.companyPlaceholder')}
                />
                <Input
                  value={editForm.salary}
                  onChange={(e) => setEditForm((p) => ({ ...p, salary: e.target.value }))}
                  placeholder={t('jobDetail.salaryPlaceholder')}
                />
                <Input
                  value={editForm.location}
                  onChange={(e) => setEditForm((p) => ({ ...p, location: e.target.value }))}
                  placeholder={t('jobDetail.locationPlaceholder')}
                />
              </div>
            </div>
          ) : (
            <>
              <h1 className="text-3xl font-bold text-gray-900">
                {parsed?.title || t('jobDetail.unknownTitle')}
              </h1>
              <div className="flex flex-wrap items-center gap-4 mt-2 text-sm text-gray-500">
                <span className="flex items-center">
                  <Building2 className="w-4 h-4 mr-1" />
                  {parsed?.company || t('jobDetail.unknownCompany')}
                </span>
                {parsed?.location && (
                  <span className="flex items-center">
                    <MapPin className="w-4 h-4 mr-1" />
                    {parsed.location}
                  </span>
                )}
                {parsed?.salary && (
                  <span className="flex items-center">
                    <DollarSign className="w-4 h-4 mr-1" />
                    {parsed.salary}
                  </span>
                )}
                <Badge variant={job.status === 'COMPLETED' ? 'default' : 'secondary'}>
                  {t(`jobDetail.status.${job.status}`)}
                </Badge>
              </div>
            </>
          )}
        </div>

        <div className="flex items-center gap-3">
          {isEditing ? (
            <>
              <Button variant="outline" size="sm" onClick={handleCancelEdit}>
                <X className="w-4 h-4 mr-2" />
                {t('common.cancel')}
              </Button>
              <Button size="sm" onClick={handleSave}>
                <Save className="w-4 h-4 mr-2" />
                {t('jobDetail.save')}
              </Button>
            </>
          ) : (
            <>
              {resumes.length > 0 && (
                <select
                  className="h-10 px-3 rounded-md border border-input bg-background text-sm"
                  value={selectedResumeVersionId}
                  onChange={(e) => setSelectedResumeVersionId(e.target.value)}
                >
                  <option value="">{t('jobDetail.selectResume')}</option>
                  {resumes.map((group) =>
                    [group.convertedVersion, group.aiOptimizedVersion]
                      .filter((v): v is NonNullable<typeof v> => !!v && v.exists)
                      .map((version) => (
                        <option key={version.versionId} value={version.versionId}>
                          {group.title} - {version.versionId.slice(0, 8)}
                        </option>
                      ))
                  )}
                </select>
              )}
              <Button
                onClick={handleScore}
                disabled={isScoring || !selectedResumeVersionId || job.status !== 'COMPLETED' || selectedResumeParseStatus !== 'COMPLETED'}
                title={job.status !== 'COMPLETED' ? t('jobDetail.scoringNotReady') : selectedResumeParseStatus !== 'COMPLETED' ? t('jobDetail.resumeParsing') : undefined}
              >
                {isScoring ? (
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                ) : (
                  <Sparkles className="w-4 h-4 mr-2" />
                )}
                {t('jobDetail.startScore')}
              </Button>
              {job.originalUrl && (
                <Button variant="outline" asChild>
                  <a href={job.originalUrl} target="_blank" rel="noopener noreferrer">
                    <ExternalLink className="w-4 h-4 mr-2" />
                    {t('jobDetail.viewSource')}
                  </a>
                </Button>
              )}
              <div className="flex items-center gap-2 ml-4 pl-4 border-l border-gray-200">
                <Button variant="default" onClick={() => handleTrackAction('APPLY')} className="bg-green-600 hover:bg-green-700">
                  <ExternalLink className="w-4 h-4 mr-2" />
                  {t('jobDetail.markAsApplied')}
                </Button>
                <Button variant="destructive" onClick={() => handleTrackAction('REJECT')}>
                  <X className="w-4 h-4 mr-2" />
                  {t('jobDetail.notInterested')}
                </Button>
              </div>
            </>
          )}
        </div>
      </div>

      {scoreResult && !isEditing && (
        <Card className="border-amber-200 bg-amber-50/50">
          <CardContent className="py-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs text-gray-400">
                {t('jobDetail.scoredWithResume')}: {
                  (() => {
                    for (const group of resumes) {
                      for (const v of [group.convertedVersion, group.aiOptimizedVersion, group.originalVersion]) {
                        if (v?.versionId === selectedResumeVersionId) return group.title;
                      }
                    }
                    return t('common.notSpecified');
                  })()
                }
              </span>
            </div>
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
                {t('jobList.matchScore', { score: Math.round(scoreResult.finalScore * 100) })}
              </Badge>
              <span className="text-sm font-medium text-gray-700">
                {scoreResult.suitable ? t('jobList.suitable') : t('jobList.notSuitable')}
              </span>
            </div>
            <p className="text-sm text-gray-700">{scoreResult.summary}</p>
            <div className="flex flex-wrap gap-3 text-xs text-gray-500">
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
          </CardContent>
        </Card>
      )}

      <div className="grid gap-6">
        <Card>
          <CardHeader className="pb-3">
            <h3 className="text-lg font-semibold flex items-center">
              <FileText className="w-5 h-5 mr-2 text-muted-foreground" />
              {t('jobDetail.description')}
            </h3>
          </CardHeader>
          <CardContent>
            {isEditing ? (
              <Textarea
                value={editForm.description}
                onChange={(e) => setEditForm((p) => ({ ...p, description: e.target.value }))}
                placeholder={t('jobDetail.descriptionPlaceholder')}
                rows={8}
              />
            ) : parsed?.description ? (
              <p className="text-gray-700 whitespace-pre-wrap">{parsed.description}</p>
            ) : (
              <p className="text-gray-400 italic">{t('jobDetail.noDescription')}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-3">
            <h3 className="text-lg font-semibold flex items-center">
              <List className="w-5 h-5 mr-2 text-muted-foreground" />
              {t('jobDetail.requirements')}
            </h3>
          </CardHeader>
          <CardContent>
            {isEditing ? (
              <div className="space-y-2">
                {editForm.requirements.map((req, index) => (
                  <div key={index} className="flex items-center gap-2">
                    <Input
                      value={req}
                      onChange={(e) => updateRequirement(index, e.target.value)}
                      placeholder={t('jobDetail.requirementPlaceholder')}
                    />
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => removeRequirement(index)}
                    >
                      <Trash2 className="w-4 h-4 text-red-500" />
                    </Button>
                  </div>
                ))}
                <Button variant="outline" size="sm" onClick={addRequirement}>
                  <Plus className="w-4 h-4 mr-2" />
                  {t('jobDetail.addRequirement')}
                </Button>
              </div>
            ) : parsed?.requirements && parsed.requirements.length > 0 ? (
              <ul className="list-disc pl-5 space-y-2">
                {parsed.requirements.map((req, index) => (
                  <li key={index} className="text-gray-700">
                    {req}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-gray-400 italic">{t('jobDetail.noRequirements')}</p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
