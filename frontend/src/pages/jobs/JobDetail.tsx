import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { jobService } from '@/services/jobService';
import { resumeService } from '@/services/resumeService';
import type { Job, ResumeGroup } from '@/types';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  ArrowLeft,
  Building2,
  ExternalLink,
  List,
  FileText,
  Sparkles,
  Loader2,
} from 'lucide-react';
import { toast } from 'sonner';

export default function JobDetail() {
  const { t } = useTranslation();
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  const [job, setJob] = useState<Job | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [resumes, setResumes] = useState<ResumeGroup[]>([]);
  const [selectedResumeVersionId, setSelectedResumeVersionId] = useState<string>('');
  const [isMatching, setIsMatching] = useState(false);

  const loadJob = useCallback(async () => {
    try {
      setIsLoading(true);
      const data = await jobService.getJob(jobId!);
      setJob(data);
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
      // 默认选择第一个简历的原始版本
      if (data.length > 0 && data[0].originalVersion) {
        setSelectedResumeVersionId(data[0].originalVersion.versionId);
      }
    } catch {
      // 静默处理，简历加载失败不影响职位详情展示
    }
  }, []);

  useEffect(() => {
    if (jobId) {
      loadJob();
      loadResumes();
    }
  }, [jobId, loadJob, loadResumes]);

  const handleMatch = async () => {
    if (!selectedResumeVersionId) {
      toast.error(t('jobDetail.selectResumeError'));
      return;
    }
    try {
      setIsMatching(true);
      await jobService.startMatch({
        resumeVersionId: selectedResumeVersionId,
        query: job?.parsedContent?.title || '',
        topK: 10,
      });
      toast.success(t('jobDetail.matchStarted'));
      navigate('/jobs');
    } catch {
      toast.error(t('jobDetail.matchError'));
    } finally {
      setIsMatching(false);
    }
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

  return (
    <div className="space-y-6">
      {/* 返回按钮 */}
      <Button variant="ghost" onClick={() => navigate('/jobs')} className="pl-0">
        <ArrowLeft className="w-4 h-4 mr-2" />
        {t('jobDetail.backToList')}
      </Button>

      {/* 职位头部 */}
      <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            {parsed?.title || t('jobDetail.unknownTitle')}
          </h1>
          <div className="flex flex-wrap items-center gap-4 mt-2 text-sm text-gray-500">
            <span className="flex items-center">
              <Building2 className="w-4 h-4 mr-1" />
              {parsed?.company || t('jobDetail.unknownCompany')}
            </span>
            <Badge variant={job.status === 'COMPLETED' ? 'default' : 'secondary'}>
              {t(`jobDetail.status.${job.status}`)}
            </Badge>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* 简历选择 */}
          {resumes.length > 0 && (
            <select
              className="h-10 px-3 rounded-md border border-input bg-background text-sm"
              value={selectedResumeVersionId}
              onChange={(e) => setSelectedResumeVersionId(e.target.value)}
            >
              <option value="">{t('jobDetail.selectResume')}</option>
              {resumes.map((group) =>
                [group.originalVersion, group.convertedVersion, group.aiOptimizedVersion]
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
            onClick={handleMatch}
            disabled={isMatching || !selectedResumeVersionId}
          >
            {isMatching ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Sparkles className="w-4 h-4 mr-2" />
            )}
            {t('jobDetail.matchThisJob')}
          </Button>
          {job.originalUrl && (
            <Button variant="outline" asChild>
              <a href={job.originalUrl} target="_blank" rel="noopener noreferrer">
                <ExternalLink className="w-4 h-4 mr-2" />
                {t('jobDetail.viewSource')}
              </a>
            </Button>
          )}
        </div>
      </div>

      {/* 职位详情卡片 */}
      <div className="grid gap-6">
        <Card>
          <CardHeader className="pb-3">
            <h3 className="text-lg font-semibold flex items-center">
              <FileText className="w-5 h-5 mr-2 text-muted-foreground" />
              {t('jobDetail.description')}
            </h3>
          </CardHeader>
          <CardContent>
            {parsed?.description ? (
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
            {parsed?.requirements && parsed.requirements.length > 0 ? (
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
