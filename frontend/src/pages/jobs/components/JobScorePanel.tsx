import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Sparkles, Loader2, Star } from 'lucide-react';
import type { JobScoreResponse, ResumeGroup } from '@/types';
import { useTranslation } from 'react-i18next';

interface ResumeVersionOption {
  versionId: string;
  label: string;
  parseStatus: string;
}

interface JobScorePanelProps {
  jobStatus: string;
  availableResumeVersions: ResumeVersionOption[];
  selectedResumeId: string;
  resumes: ResumeGroup[];
  scoreResult?: JobScoreResponse;
  isScoring: boolean;
  onSelectResume: (versionId: string) => void;
  onScore: () => void;
}

export default function JobScorePanel({
  jobStatus,
  availableResumeVersions,
  selectedResumeId,
  resumes,
  scoreResult,
  isScoring,
  onSelectResume,
  onScore,
}: JobScorePanelProps) {
  const { t } = useTranslation();

  const currentResume = availableResumeVersions.find((v) => v.versionId === selectedResumeId);

  /**
   * Looks up the resume group title from a versionId by iterating all groups.
   * Used because the flat version list doesn't carry parent group metadata.
   *
   * 遍历所有简历组，根据版本 ID 查找对应的组标题。
   * 扁平化的版本列表不携带所属组元数据，因此需要反向查找。
   */
  const getResumeTitle = (versionId: string): string => {
    for (const group of resumes) {
      for (const v of [group.convertedVersion, group.aiOptimizedVersion, group.originalVersion]) {
        if (v?.versionId === versionId) {
          return group.title;
        }
      }
    }
    return t('common.notSpecified');
  };

  const canScore =
    selectedResumeId.length > 0 &&
    !isScoring &&
    jobStatus === 'COMPLETED' &&
    currentResume?.parseStatus === 'COMPLETED';

  return (
    <div className="border rounded-lg p-4 bg-gray-50/50 space-y-3">
      <div className="flex flex-col sm:flex-row sm:items-center gap-3">
        <Select value={selectedResumeId} onValueChange={onSelectResume}>
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
          onClick={onScore}
          disabled={!canScore}
          title={
            jobStatus !== 'COMPLETED'
              ? (t('jobList.scoringNotReady') ?? '')
              : currentResume?.parseStatus !== 'COMPLETED'
              ? (t('jobList.resumeParsing') ?? '')
              : undefined
          }
        >
          {isScoring ? (
            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
          ) : (
            <Sparkles className="w-4 h-4 mr-2" />
          )}
          {t('jobList.startScore')}
        </Button>
      </div>

      {scoreResult && (
        <div className="rounded-md border bg-white p-3 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-400">
              {t('jobList.scoredWithResume')}: {getResumeTitle(selectedResumeId)}
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
              {t('jobList.matchScore', {
                score: Math.round(scoreResult.finalScore * 100),
              })}
            </Badge>
            <span className="text-sm text-gray-500">
              {scoreResult.suitable ? t('jobList.suitable') : t('jobList.notSuitable')}
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
  );
}
