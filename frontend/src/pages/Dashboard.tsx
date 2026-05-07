import { useEffect, useState, useMemo, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { resumeService } from '@/services/resumeService';
import { jobService } from '@/services/jobService';
import { trackingService } from '@/services/trackingService';
import { chatService } from '@/services/chatService';
import { formatDate } from '@/utils/i18n';
import type { Tracking } from '@/types';
import {
  FileText,
  Briefcase,
  MessageSquare,
  ClipboardList,
  ArrowRight,
  Sparkles,
  TrendingUp,
  Building2,
  Calendar,
} from 'lucide-react';

interface StatItem {
  labelKey: string;
  value: number;
  icon: React.ElementType;
  isLoading: boolean;
}

export default function Dashboard() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [stats, setStats] = useState<StatItem[]>([
    { labelKey: 'dashboard.stats.resumes', value: 0, icon: FileText, isLoading: true },
    { labelKey: 'dashboard.stats.matches', value: 0, icon: Briefcase, isLoading: true },
    { labelKey: 'dashboard.stats.applications', value: 0, icon: ClipboardList, isLoading: true },
    { labelKey: 'dashboard.stats.chats', value: 0, icon: MessageSquare, isLoading: true },
  ]);

  const [trackings, setTrackings] = useState<Tracking[]>([]);
  const [trackingsLoading, setTrackingsLoading] = useState(true);

  const [recommendedJobs, setRecommendedJobs] = useState<{
    id: string;
    jobId: string;
    title: string;
    company: string;
    resumeName: string;
    skillScore: number;
    experienceScore: number;
    overallScore: number;
  }[]>([]);
  const [recommendedJobsLoading, setRecommendedJobsLoading] = useState(true);

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

  const quickActions = [
    {
      titleKey: 'dashboard.quickActions.resumes',
      descKey: 'dashboard.quickActions.resumesDesc',
      icon: FileText,
      path: '/resumes',
      color: 'bg-blue-100 text-blue-600',
    },
    {
      titleKey: 'dashboard.quickActions.jobs',
      descKey: 'dashboard.quickActions.jobsDesc',
      icon: Briefcase,
      path: '/jobs',
      color: 'bg-green-100 text-green-600',
    },
    {
      titleKey: 'dashboard.quickActions.chat',
      descKey: 'dashboard.quickActions.chatDesc',
      icon: MessageSquare,
      path: '/chat',
      color: 'bg-purple-100 text-purple-600',
    },
    {
      titleKey: 'dashboard.quickActions.tracking',
      descKey: 'dashboard.quickActions.trackingDesc',
      icon: ClipboardList,
      path: '/tracking',
      color: 'bg-orange-100 text-orange-600',
    },
  ];

  const loadStats = useCallback(async () => {
    const updateStat = (index: number, value: number) => {
      setStats((prev) =>
        prev.map((s, i) => (i === index ? { ...s, value, isLoading: false } : s))
      );
    };

    // 并行加载所有数�?
    const [resumesResult, jobsResult, scoreHistoryResult, trackingsResult, chatsResult] = await Promise.allSettled([
      resumeService.getResumeGroups(),
      jobService.getJobs(),
      jobService.getScoreHistory(),
      trackingService.getTrackings(),
      chatService.getConversations(),
    ]);

    // 处理简历统�?
    if (resumesResult.status === 'fulfilled') {
      updateStat(0, resumesResult.value.length);
    } else {
      updateStat(0, 0);
    }

    // 处理职位统计
    if (jobsResult.status === 'fulfilled') {
      updateStat(1, jobsResult.value.length);
    } else {
      updateStat(1, 0);
    }

    // 处理跟踪统计
    if (trackingsResult.status === 'fulfilled') {
      updateStat(2, trackingsResult.value.length);
      setTrackings(trackingsResult.value);
    } else {
      updateStat(2, 0);
    }
    setTrackingsLoading(false);

    // 处理对话统计
    if (chatsResult.status === 'fulfilled') {
      updateStat(3, chatsResult.value.length);
    } else {
      updateStat(3, 0);
    }

    // 组装推荐职位列表（从评分历史�?
    if (scoreHistoryResult.status === 'fulfilled' && jobsResult.status === 'fulfilled' && resumesResult.status === 'fulfilled') {
      const jobs = jobsResult.value;
      const resumes = resumesResult.value;
      const history = scoreHistoryResult.value;

      const jobMap = new Map(jobs.map((j) => [j.id, j]));
      const resumeNameMap = new Map<string, string>();
      for (const group of resumes) {
        for (const version of [group.convertedVersion, group.aiOptimizedVersion, group.originalVersion]) {
          if (version?.versionId) {
            resumeNameMap.set(version.versionId, group.title);
          }
        }
      }

      // �?jobId 去重，只保留每个职位最新的评分记录
      const seenJobIds = new Set<string>();
      const deduped = history.filter((record) => {
        if (seenJobIds.has(record.jobId)) return false;
        seenJobIds.add(record.jobId);
        return true;
      });

      const items = deduped.map((record) => {
          const job = jobMap.get(record.jobId);
          return {
            id: record.id,
            jobId: record.jobId,
            title: job?.parsedContent?.title || t('jobDetail.unknownTitle'),
            company: job?.parsedContent?.company || t('jobDetail.unknownCompany'),
            resumeName: resumeNameMap.get(record.resumeVersionId) || t('common.notSpecified'),
            skillScore: record.skillScore,
            experienceScore: record.experienceScore,
            overallScore: record.overallScore,
          };
        });

      setRecommendedJobs(items);
    } else {
      setRecommendedJobs([]);
    }
    setRecommendedJobsLoading(false);
  }, [t]);

  useEffect(() => {
    const timer = setTimeout(() => {
      loadStats();
    }, 0);
    return () => clearTimeout(timer);
  }, [loadStats]);

  return (
    <div className="space-y-8">
      {/* 欢迎区域 */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-600 rounded-2xl p-8 text-white">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div>
            <h1 className="text-3xl font-bold mb-2">{t('dashboard.welcome')}</h1>
            <p className="text-blue-100 text-lg">{t('dashboard.subtitle')}</p>
          </div>
          <div className="flex space-x-4">
            <Button
              variant="secondary"
              onClick={() => navigate('/resumes')}
              className="bg-white text-blue-600 hover:bg-blue-50"
            >
              <FileText className="w-4 h-4 mr-2" />
              {t('dashboard.uploadResume')}
            </Button>
            <Button
              variant="outline"
              onClick={() => navigate('/jobs')}
              className="border-white text-white bg-transparent hover:bg-white/10"
            >
              <Briefcase className="w-4 h-4 mr-2" />
              {t('dashboard.viewJobs')}
            </Button>
          </div>
        </div>
      </div>

      {/* 统计卡片 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <Card key={index} className="hover:shadow-md transition-shadow">
              <CardContent className="p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-sm text-gray-500">{t(stat.labelKey)}</p>
                    {stat.isLoading ? (
                      <Skeleton className="h-9 w-12 mt-1" />
                    ) : (
                      <p className="text-3xl font-bold text-gray-900 mt-1">{stat.value}</p>
                    )}
                  </div>
                  <div className="w-12 h-12 bg-blue-50 rounded-xl flex items-center justify-center">
                    <Icon className="w-6 h-6 text-blue-600" />
                  </div>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* 快速操�?*/}
      <div>
        <h2 className="text-xl font-bold text-gray-900 mb-4">{t('dashboard.quickActions.title')}</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {quickActions.map((action, index) => {
            const Icon = action.icon;
            return (
              <Card
                key={index}
                className="cursor-pointer hover:shadow-lg transition-all group"
                onClick={() => navigate(action.path)}
              >
                <CardContent className="p-6">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center mb-4 ${action.color}`}>
                    <Icon className="w-6 h-6" />
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-1">{t(action.titleKey)}</h3>
                  <p className="text-sm text-gray-500 mb-4">{t(action.descKey)}</p>
                  <div className="flex items-center text-sm text-blue-600 group-hover:translate-x-1 transition-transform">
                    {t('common.enter')}
                    <ArrowRight className="w-4 h-4 ml-1" />
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>

      {/* 推荐内容 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 最新职位推�?*/}
        <Card>
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg flex items-center">
                <Sparkles className="w-5 h-5 mr-2 text-yellow-500" />
                {t('dashboard.recommendedJobs.title')}
              </CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/jobs')}>
                {t('common.viewMore')}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {recommendedJobsLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-16" />
                <Skeleton className="h-16" />
                <Skeleton className="h-16" />
              </div>
            ) : recommendedJobs.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <Briefcase className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                <p>{t('dashboard.noScoreRecords')}</p>
                <Button variant="link" onClick={() => navigate('/jobs')}>
                  {t('dashboard.viewJobs')}
                </Button>
              </div>
            ) : (
              <div className="space-y-3 max-h-[400px] overflow-y-auto pr-1">
                {recommendedJobs.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
                    onClick={() => navigate(`/jobs/${item.jobId}`)}
                  >
                    <div className="flex-1 min-w-0">
                      <h4 className="font-semibold text-gray-900 truncate">
                        {item.title}-{item.company}-{item.resumeName}
                      </h4>
                      <div className="flex items-center space-x-3 text-xs text-gray-500 mt-1">
                        <span>
                          {t('jobList.skillScore')}: {Math.round(item.skillScore * 100)}%
                        </span>
                        <span>
                          {t('jobList.experienceScore')}: {Math.round(item.experienceScore * 100)}%
                        </span>
                        <span>
                          {t('jobList.overallScore')}: {Math.round(item.overallScore * 100)}%
                        </span>
                      </div>
                    </div>
                    <ArrowRight className="w-4 h-4 text-gray-400 ml-2 flex-shrink-0" />
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 求职进展 */}
        <Card>
          <CardHeader className="pb-4">
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg flex items-center">
                <TrendingUp className="w-5 h-5 mr-2 text-green-500" />
                {t('dashboard.applicationProgress.title')}
              </CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/tracking')}>
                {t('common.viewAll')}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {trackingsLoading ? (
              <div className="space-y-3">
                <Skeleton className="h-16" />
                <Skeleton className="h-16" />
                <Skeleton className="h-16" />
              </div>
            ) : trackings.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <ClipboardList className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                <p>{t('tracking.emptyTitle')}</p>
                <Button variant="link" onClick={() => navigate('/tracking')}>
                  {t('tracking.addRecord')}
                </Button>
              </div>
            ) : (
              <div className="space-y-3 max-h-[400px] overflow-y-auto pr-1">
                {trackings
                  .slice()
                  .sort((a, b) => new Date(b.appliedAt).getTime() - new Date(a.appliedAt).getTime())
                  .map((tracking) => {
                    const config = statusConfig[tracking.status] || {
                      labelKey: 'tracking.status.UNKNOWN',
                      color: 'bg-gray-100 text-gray-500',
                    };
                    return (
                      <div
                        key={tracking.trackingId}
                        className="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50 transition-colors cursor-pointer"
                        onClick={() => navigate('/tracking')}
                      >
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center space-x-2 mb-1">
                            <h4 className="font-semibold text-gray-900 truncate">
                              {tracking.jobTitle}
                            </h4>
                            <Badge className={config.color}>{t(config.labelKey)}</Badge>
                          </div>
                          <div className="flex items-center space-x-3 text-sm text-gray-500">
                            <span className="flex items-center">
                              <Building2 className="w-3.5 h-3.5 mr-1" />
                              {tracking.companyName}
                            </span>
                            <span className="flex items-center">
                              <Calendar className="w-3.5 h-3.5 mr-1" />
                              {tracking.appliedAt ? formatDate(tracking.appliedAt) : '-'}
                            </span>
                          </div>
                        </div>
                        <ArrowRight className="w-4 h-4 text-gray-400 ml-2 flex-shrink-0" />
                      </div>
                    );
                  })}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
