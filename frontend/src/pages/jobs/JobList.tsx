import { useEffect, useState, useCallback, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import type { ResumeGroup } from '@/types';
import { useJobStore } from '@/store/job.store';
import { resumeService } from '@/services/resumeService';
import { toast } from 'sonner';
import JobListHeader from './components/JobListHeader';
import JobFilterBar from './components/JobFilterBar';
import JobCard from './components/JobCard';
import JobListSkeleton from './components/JobListSkeleton';
import JobEmptyState from './components/JobEmptyState';
import JobCreateModal from './components/JobCreateModal';

/**
 * Orchestrates data fetching, state management, and composes presentational child components.
 *
 * 职位列表容器组件，负责数据获取、状态管理与子组件组装
 */
export default function JobList() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const jobs = useJobStore((state) => state.filteredJobs);
  const loading = useJobStore((state) => state.loading);
  const filters = useJobStore((state) => state.filters);
  const scoreResults = useJobStore((state) => state.scoreResults);
  const scoringState = useJobStore((state) => state.scoringState);
  const selectedResumes = useJobStore((state) => state.selectedResumes);
  const fetchJobs = useJobStore((state) => state.fetchJobs);
  const loadScoreHistory = useJobStore((state) => state.loadScoreHistory);
  const setSearchQuery = useJobStore((state) => state.setSearchQuery);
  const setSortBy = useJobStore((state) => state.setSortBy);
  const setSelectedResume = useJobStore((state) => state.setSelectedResume);
  const scoreJob = useJobStore((state) => state.scoreJob);
  const submitJobToStore = useJobStore((state) => state.submitJob);
  const deleteJob = useJobStore((state) => state.deleteJob);

  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [matchFilter, setMatchFilter] = useState<string>('ALL');

  const [resumes, setResumes] = useState<ResumeGroup[]>([]);

  useEffect(() => {
    fetchJobs();
    loadScoreHistory();

    // Inline async to avoid ESLint set-state-in-effect false positives
    // 内联异步以避免 ESLint set-state-in-effect 误报
    resumeService
      .getResumeGroups()
      .then((data) => {
        setResumes(data);
      })
      .catch(() => {
        // Non-critical path — silently degrade
        // 简历加载非关键路径，静默降级
      });
  }, [fetchJobs, loadScoreHistory]);

  // Poll resume parse status every 3s when any resume is pending
  // 有简历正在解析时，每 3 秒轮询一次状态
  const hasPendingResume = resumes.some((group) =>
    [group.originalVersion, group.convertedVersion, group.aiOptimizedVersion]
      .filter((v): v is NonNullable<typeof v> => !!v)
      .some((v) => v.parseStatus === 'PENDING' || v.parseStatus === 'PARSING')
  );

  useEffect(() => {
    if (!hasPendingResume) return;

    const interval = setInterval(() => {
      resumeService
        .getResumeGroups()
        .then((data) => {
        setResumes(data);
      })
      .catch(() => {
        // Silent catch
        // 静默忽略本次轮询失败，下一轮会继续刷新状态
      });
    }, 3000);

    return () => clearInterval(interval);
  }, [hasPendingResume]);

  const availableResumeVersions = useMemo(
    () =>
      resumes.flatMap((group) =>
        [group.convertedVersion, group.aiOptimizedVersion]
          .filter((v): v is NonNullable<typeof v> => !!v && v.exists)
          .map((version) => ({
            versionId: version.versionId,
            label: `${group.title} - ${version.versionId.slice(0, 8)}`,
            parseStatus: version.parseStatus,
          }))
      ),
    [resumes]
  );

  // Client-side match-score filtering using cached score data
  // 基于已有评分数据进行客户端匹配度过滤
  const displayJobs = useMemo(() => {
    if (matchFilter === 'ALL') return jobs;
    return jobs.filter((job) => {
      const currentResumeId = selectedResumes[job.id] ?? '';
      const scoreKey = `${job.id}_${currentResumeId}`;
      const scoreResult = scoreResults[scoreKey];
      if (!scoreResult) return false;
      const score = scoreResult.finalScore;
      switch (matchFilter) {
        case 'HIGH':
          return score >= 0.7;
        case 'MEDIUM':
          return score >= 0.4 && score < 0.7;
        case 'LOW':
          return score < 0.4;
        default:
          return true;
      }
    });
  }, [jobs, matchFilter, selectedResumes, scoreResults]);

  const handleScoreJob = useCallback(
    async (jobId: string) => {
      const resumeVersionId = selectedResumes[jobId];
      if (!resumeVersionId) {
        toast.error(t('jobList.selectResumeForScore'));
        return;
      }
      await scoreJob(jobId, resumeVersionId);
    },
    [selectedResumes, scoreJob, t]
  );

  const handleSubmitJob = useCallback(
    async (url: string, screenshot: File) => {
      await submitJobToStore(url, screenshot);
      setAddDialogOpen(false);
    },
    [submitJobToStore]
  );

  const handleDeleteJob = useCallback(
    async (jobId: string) => {
      if (!window.confirm(t('jobList.deleteConfirm'))) return;
      await deleteJob(jobId);
    },
    [deleteJob, t]
  );

  if (loading && jobs.length === 0) {
    return (
      <JobListSkeleton
        title={t('jobList.title')}
        subtitle={t('jobList.subtitle')}
      />
    );
  }

  return (
    <div className="space-y-6">
      <JobListHeader
        title={t('jobList.title')}
        subtitle={t('jobList.subtitle')}
        addButtonLabel={t('jobList.addJob')}
        onAddClick={() => setAddDialogOpen(true)}
      />

      <JobFilterBar
        searchQuery={filters.searchQuery}
        sortBy={filters.sortBy}
        searchPlaceholder={t('jobList.searchPlaceholder')}
        sortLabel={t('jobList.sortBy')}
        sortOptions={[
          { value: 'date', label: t('jobList.sortDate') },
          { value: 'status', label: t('jobList.sortStatus') },
        ]}
        matchFilter={matchFilter}
        matchFilterLabel={t('jobList.matchFilter')}
        matchFilterOptions={[
          { value: 'ALL', label: t('jobList.matchAll') },
          { value: 'HIGH', label: t('jobList.matchHigh') },
          { value: 'MEDIUM', label: t('jobList.matchMedium') },
          { value: 'LOW', label: t('jobList.matchLow') },
        ]}
        onSearchChange={setSearchQuery}
        onSortChange={(value) => setSortBy(value as 'date' | 'status')}
        onMatchFilterChange={setMatchFilter}
      />

      <div className="max-h-[calc(100vh-280px)] overflow-y-auto pr-1">
        <div className="grid gap-4">
          {displayJobs.length === 0 ? (
            <JobEmptyState
              title={t('jobList.emptyTitle')}
              description={t('jobList.emptyDesc')}
            />
          ) : (
            displayJobs.map((job) => {
              const currentResumeId = selectedResumes[job.id] ?? '';
              const scoreKey = `${job.id}_${currentResumeId}`;
              const isScoring = scoringState[scoreKey] ?? false;
              const scoreResult = scoreResults[scoreKey];

              return (
                <JobCard
                  key={job.id}
                  job={job}
                  availableResumeVersions={availableResumeVersions}
                  selectedResumeId={currentResumeId}
                  scoreResult={scoreResult}
                  isScoring={isScoring}
                  resumes={resumes}
                  onSelectResume={(versionId) => setSelectedResume(job.id, versionId)}
                  onScore={() => handleScoreJob(job.id)}
                  onViewDetail={() => navigate(`/jobs/${job.id}`)}
                  onDelete={() => handleDeleteJob(job.id)}
                />
              );
            })
          )}
        </div>
      </div>

      <JobCreateModal
        open={addDialogOpen}
        onOpenChange={setAddDialogOpen}
        onSubmit={handleSubmitJob}
      />
    </div>
  );
}
