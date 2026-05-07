import { useTranslation } from 'react-i18next';
import { formatDate, formatTime } from '@/utils/i18n';
import { Badge } from '../ui/badge';
import type { ResumeVersion } from '../../types/resume';
import { ParseStatusBadge } from './ParseStatusBadge';
import { FileText, Sparkles, FileCode2, Play } from 'lucide-react';

interface VersionTimelineProps {
  versions: ResumeVersion[];
  selectedVersionId: string;
  onSelectVersion: (versionId: string) => void;
  onActivateVersion?: (versionId: string) => void;
}

export function VersionTimeline({ versions, selectedVersionId, onSelectVersion, onActivateVersion }: VersionTimelineProps) {
  const { t } = useTranslation();
  const sortedVersions = [...versions].sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  );

  const getVersionIcon = (type: ResumeVersion['versionType']) => {
    switch (type) {
      case 'ORIGINAL':
        return <FileText className="w-4 h-4" />;
      case 'CONVERTED':
        return <FileCode2 className="w-4 h-4" />;
      case 'AI_OPTIMIZED':
        return <Sparkles className="w-4 h-4 text-amber-500" />;
    }
  };

  const getVersionLabel = (type: ResumeVersion['versionType']) => {
    switch (type) {
      case 'ORIGINAL':
        return t('resume.timeline.original');
      case 'CONVERTED':
        return t('resume.timeline.converted');
      case 'AI_OPTIMIZED':
        return t('resume.timeline.aiOptimized');
    }
  };

  return (
    <div className="relative border-l-2 border-muted ml-3 space-y-6 py-2">
      {sortedVersions.map((version) => {
        const isSelected = version.versionId === selectedVersionId;
        const date = new Date(version.createdAt);
        
        return (
          <div 
            key={version.versionId} 
            className="relative pl-6 cursor-pointer group"
            onClick={() => onSelectVersion(version.versionId)}
          >
            <div className={`absolute -left-[9px] top-1 w-4 h-4 rounded-full border-2 transition-colors ${
              isSelected 
                ? 'bg-primary border-primary' 
                : 'bg-background border-muted-foreground group-hover:border-primary'
            }`} />
            
            <div className={`p-3 rounded-lg border transition-all ${
              isSelected 
                ? 'border-primary bg-primary/5 shadow-sm' 
                : 'border-border hover:border-primary/50 hover:bg-muted/50'
            }`}>
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <div className={`p-1.5 rounded-md ${
                    version.versionType === 'AI_OPTIMIZED' ? 'bg-amber-100 dark:bg-amber-900/30' : 'bg-muted'
                  }`}>
                    {getVersionIcon(version.versionType)}
                  </div>
                  <span className="font-medium text-sm">
                    {getVersionLabel(version.versionType)} - {version.versionId.slice(0, 8)}
                  </span>
                </div>
                {version.status === 'ACTIVE' ? (
                  <Badge variant="outline" className="text-[10px] px-1.5 py-0 h-5">{t('resume.timeline.active')}</Badge>
                ) : (
                  <Badge variant="secondary" className="text-[10px] px-1.5 py-0 h-5">{t('resume.timeline.archived')}</Badge>
                )}
              </div>
              
              <div className="flex items-center justify-between mt-3">
                <span className="text-xs text-muted-foreground">
                  {formatDate(date)} {formatTime(date)}
                </span>
                <div className="flex items-center gap-2">
                  {onActivateVersion && version.status === 'ARCHIVED' && version.versionType !== 'ORIGINAL' && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        onActivateVersion(version.versionId);
                      }}
                      className="flex items-center text-[10px] px-2 py-0.5 rounded bg-green-100 text-green-700 hover:bg-green-200 transition-colors"
                      title={t('resume.timeline.setActive')}
                    >
                      <Play className="w-3 h-3 mr-1" />
                      {t('resume.timeline.setActive')}
                    </button>
                  )}
                  <ParseStatusBadge status={version.parseStatus} />
                </div>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
