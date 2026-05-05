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
 * JobList 容器组件
 * JobList container — orchestrates data fetching, state management,
 * and assembles presentational child components.
 */
export default function JobList() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  // === Job Store（全局业务状态）===
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

  // === 局部 UI 状态（无需全局共享）===
  const [addDialogOpen, setAddDialogOpen] = useState(false);

  // === 简历数据（Resume 领域，由本地管理）===
  const [resumes, setResumes] = useState<ResumeGroup[]>([]);

  // === 初始化数据加载 ===
  useEffect(() => {
    fetchJobs();
    loadScoreHistory();

    // 初始化加载简历列表（内联以避免 ESLint set-state-in-effect 误报）
    resumeService
      .getResumeGroups()
      .then((data) => {
        setResumes(data);
      })
      .catch(() => {
        // 简历加载非关键路径，静默处理
      });
  }, [fetchJobs, loadScoreHistory]);

  // === 智能轮询：简历解析状态 ===
  useEffect(() => {
    const hasPendingResume = resumes.some((group) =>
      [group.originalVersion, group.convertedVersion, group.aiOptimizedVersion]
        .filter((v): v is NonNullable<typeof v> => !!v)
        .some((v) => v.parseStatus === 'PENDING' || v.parseStatus === 'PARSING')
    );
    if (!hasPendingResume) return;

    const interval = setInterval(() => {
      resumeService
        .getResumeGroups()
        .then((data) => {
          setResumes(data);
        })
        .catch(() => {
          // 静默处理
        });
    }, 3000);

    return () => clearInterval(interval);
  }, [resumes]);

  // === 派生数据：可用简历版本列表 ===
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

  // === 事件处理器 ===
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

  // === 渲染加载态 ===
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
        onSearchChange={setSearchQuery}
        onSortChange={(value) => setSortBy(value as 'date' | 'status')}
      />

      <div className="grid gap-4">
        {jobs.length === 0 ? (
          <JobEmptyState
            title={t('jobList.emptyTitle')}
            description={t('jobList.emptyDesc')}
          />
        ) : (
          jobs.map((job) => {
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
              />
            );
          })
        )}
      </div>

      <JobCreateModal
        open={addDialogOpen}
        onOpenChange={setAddDialogOpen}
        onSubmit={handleSubmitJob}
      />
    </div>
  );
}
