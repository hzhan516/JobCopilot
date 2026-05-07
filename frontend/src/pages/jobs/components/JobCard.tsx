import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Building2, ExternalLink, Trash2 } from 'lucide-react';
import type { Job, JobScoreResponse, ResumeGroup } from '@/types';
import { useTranslation } from 'react-i18next';
import JobScorePanel from './JobScorePanel';

interface ResumeVersionOption {
  versionId: string;
  label: string;
  parseStatus: string;
}

interface JobCardProps {
  job: Job;
  availableResumeVersions: ResumeVersionOption[];
  selectedResumeId: string;
  scoreResult?: JobScoreResponse;
  isScoring: boolean;
  resumes: ResumeGroup[];
  onSelectResume: (versionId: string) => void;
  onScore: () => void;
  onViewDetail: () => void;
  onDelete: () => void;
}

export default function JobCard({
  job,
  availableResumeVersions,
  selectedResumeId,
  scoreResult,
  isScoring,
  resumes,
  onSelectResume,
  onScore,
  onViewDetail,
  onDelete,
}: JobCardProps) {
  const { t } = useTranslation();

  return (
    <Card className="hover:shadow-md transition-shadow">
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
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={onViewDetail}>
              <ExternalLink className="w-4 h-4 mr-2" />
              {t('jobList.viewDetails')}
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="text-destructive hover:text-destructive hover:bg-destructive/10"
              onClick={onDelete}
              title={t('jobList.deleteTitle')}
              aria-label={t('jobList.deleteTitle')}
            >
              <Trash2 className="w-4 h-4" />
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-gray-600 line-clamp-2">
          {job.parsedContent?.description || t('jobDetail.noDescription')}
        </p>

        {job.status === 'COMPLETED' && availableResumeVersions.length > 0 && (
          <JobScorePanel
            jobStatus={job.status}
            availableResumeVersions={availableResumeVersions}
            selectedResumeId={selectedResumeId}
            resumes={resumes}
            scoreResult={scoreResult}
            isScoring={isScoring}
            onSelectResume={onSelectResume}
            onScore={onScore}
          />
        )}
      </CardContent>
    </Card>
  );
}
