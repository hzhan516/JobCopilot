import React from 'react';
import { useTranslation } from 'react-i18next';
import type { ResumeVersion } from '@/types/resume.ts';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { Edit3, FileText, Briefcase, List, Copy, Play } from 'lucide-react';
import { DownloadButton } from './DownloadButton';

interface VersionDetailProps {
  version: ResumeVersion;
  onEdit: () => void;
  /**
   * 基于此版本创建副本 / Create a copy based on this version
   */
  onCreateCopy?: () => void;
  /**
   * 激活此版本 / Activate this version
   */
  onActivate?: () => void;
}

export const VersionDetail: React.FC<VersionDetailProps> = ({ version, onEdit, onCreateCopy, onActivate }) => {
  const { t } = useTranslation();
  const { parsedContent, content, versionType, status, parseStatus } = version;

  const getStatusBadge = () => {
    switch (parseStatus) {
      case 'COMPLETED':
        return <Badge variant="default" className="bg-green-500">{t('resume.versionDetail.parsed')}</Badge>;
      case 'PARSING':
        return <Badge variant="secondary" className="bg-blue-500 text-white">{t('resume.parseStatus.parsing')}</Badge>;
      case 'FAILED':
        return <Badge variant="destructive">{t('resume.parseStatus.failed')}</Badge>;
      case 'NOT_APPLICABLE':
        return null;
      default:
        return <Badge variant="outline">{t('resume.parseStatus.pending')}</Badge>;
    }
  };

  const getTypeBadge = () => {
    switch (versionType) {
      case 'ORIGINAL':
        return <Badge variant="outline" className="border-blue-200 text-blue-700 bg-blue-50">{t('resume.versionDetail.original')}</Badge>;
      case 'CONVERTED':
        return <Badge variant="outline" className="border-purple-200 text-purple-700 bg-purple-50">{t('resume.versionDetail.converted')}</Badge>;
      case 'AI_OPTIMIZED':
        return <Badge variant="outline" className="border-amber-200 text-amber-700 bg-amber-50">{t('resume.versionDetail.aiOptimized')}</Badge>;
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div className="flex items-center space-x-3">
          <h2 className="text-2xl font-bold tracking-tight">{t('resume.versionDetail.title')}</h2>
          {getTypeBadge()}
          {getStatusBadge()}
          {status === 'ARCHIVED' && <Badge variant="secondary">{t('resume.timeline.archived')}</Badge>}
        </div>
        
        <div className="flex items-center space-x-2">
          <DownloadButton
            versionId={version.versionId}
            versionType={versionType}
            filename={`resume-${versionType.toLowerCase()}`}
          />
          {onCreateCopy && (
            <Button onClick={onCreateCopy} variant="ghost" size="sm">
              <Copy className="w-4 h-4 mr-2" />
              {t('resume.versionDetail.createCopy')}
            </Button>
          )}
          {onActivate && versionType !== 'ORIGINAL' && status === 'ARCHIVED' && (
            <Button onClick={onActivate} variant="ghost" size="sm" className="text-green-600 hover:text-green-700 hover:bg-green-50">
              <Play className="w-4 h-4 mr-2" />
              {t('resume.versionDetail.setActive')}
            </Button>
          )}
          {versionType !== 'ORIGINAL' && status === 'ACTIVE' && (
            <Button onClick={onEdit} variant="outline" size="sm">
              <Edit3 className="w-4 h-4 mr-2" />
              {t('resume.versionDetail.editContent')}
            </Button>
          )}
        </div>
      </div>

      {parsedContent && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-lg flex items-center">
                <Briefcase className="w-5 h-5 mr-2 text-muted-foreground" />
                {t('resume.versionDetail.basicInfo')}
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <h4 className="text-sm font-medium text-muted-foreground mb-1">{t('resume.versionDetail.jobTitle')}</h4>
                <p className="text-base font-medium">{parsedContent.title || t('common.notSpecified')}</p>
              </div>
              <div>
                <h4 className="text-sm font-medium text-muted-foreground mb-1">{t('resume.versionDetail.company')}</h4>
                <p className="text-base">{parsedContent.company || t('common.notSpecified')}</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-lg flex items-center">
                <List className="w-5 h-5 mr-2 text-muted-foreground" />
                {t('resume.versionDetail.requirements')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              {parsedContent.requirements && parsedContent.requirements.length > 0 ? (
                <ul className="list-disc pl-5 space-y-1">
                  {parsedContent.requirements.map((req, index) => (
                    <li key={index} className="text-sm">{req}</li>
                  ))}
                </ul>
              ) : (
                <p className="text-sm text-muted-foreground italic">{t('resume.versionDetail.noRequirements')}</p>
              )}
            </CardContent>
          </Card>

          <Card className="md:col-span-2">
            <CardHeader className="pb-3">
              <CardTitle className="text-lg flex items-center">
                <FileText className="w-5 h-5 mr-2 text-muted-foreground" />
                {t('resume.versionDetail.description')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm whitespace-pre-wrap">
                {parsedContent.description || t('resume.versionDetail.noDescription')}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-lg flex items-center">
            <FileText className="w-5 h-5 mr-2 text-muted-foreground" />
            {t('resume.versionDetail.rawContent')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {content ? (
            <div className="bg-muted/50 p-4 rounded-md overflow-auto max-h-[500px]">
              <pre className="text-sm whitespace-pre-wrap font-mono">{content}</pre>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground italic text-center py-8">
              {t('resume.versionDetail.contentNotAvailable')}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
