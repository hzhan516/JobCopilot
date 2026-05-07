import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ResumeVersion } from '@/types/resume.ts';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { ArrowRight, FileText, Sparkles } from 'lucide-react';
import MDEditor from '@uiw/react-md-editor';

interface AIOptimizeCompareProps {
  originalVersion: ResumeVersion | null;
  aiOptimizedVersion: ResumeVersion | null;
}

export const AIOptimizeCompare: React.FC<AIOptimizeCompareProps> = ({
  originalVersion,
  aiOptimizedVersion,
}) => {
  const { t } = useTranslation();
  const [viewMode, setViewMode] = useState<'split' | 'original' | 'ai'>('split');

  if (!originalVersion && !aiOptimizedVersion) {
    return (
      <Card className="border-dashed">
        <CardContent className="flex flex-col items-center justify-center py-12">
          <Sparkles className="w-12 h-12 text-gray-300 mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">
            {t('resume.compare.noVersions')}
          </h3>
          <p className="text-gray-500">{t('resume.compare.uploadFirst')}</p>
        </CardContent>
      </Card>
    );
  }

  const renderContent = (version: ResumeVersion | null, label: string, type: 'original' | 'ai') => {
    if (!version) {
      return (
        <div className="flex flex-col items-center justify-center h-64 border rounded-lg bg-muted/20">
          <p className="text-muted-foreground">{t('resume.compare.versionNotAvailable', { version: label })}</p>
        </div>
      );
    }

    const isAI = type === 'ai';

    return (
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            {isAI ? (
              <Sparkles className="w-4 h-4 text-amber-600" />
            ) : (
              <FileText className="w-4 h-4 text-blue-600" />
            )}
            <span className={`font-medium ${isAI ? 'text-amber-700' : 'text-blue-700'}`}>
              {label}
            </span>
            <Badge
              variant="outline"
              className={
                isAI
                  ? 'border-amber-200 text-amber-700 bg-amber-50'
                  : 'border-blue-200 text-blue-700 bg-blue-50'
              }
            >
              {version.parseStatus === 'COMPLETED'
                ? t('resume.parseStatus.completed')
                : version.parseStatus === 'PARSING'
                ? t('resume.parseStatus.parsing')
                : version.parseStatus === 'FAILED'
                ? t('resume.parseStatus.failed')
                : version.parseStatus === 'NOT_APPLICABLE'
                ? t('resume.parseStatus.notApplicable')
                : t('resume.parseStatus.pending')}
            </Badge>
          </div>
        </div>

        <div
          className={`border rounded-lg overflow-hidden ${
            isAI ? 'bg-amber-50/30' : 'bg-blue-50/30'
          }`}
        >
          {version.content ? (
            <div className="p-4 max-h-[600px] overflow-auto" data-color-mode="light">
              <MDEditor.Markdown source={version.content} />
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center h-64">
              <p className="text-muted-foreground italic">
                {t('resume.versionDetail.contentNotAvailable')}
              </p>
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold">{t('resume.compare.title')}</h3>
        <div className="flex items-center space-x-2">
          <Button
            variant={viewMode === 'split' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setViewMode('split')}
          >
            <ArrowRight className="w-3 h-3 mr-1" />
            {t('resume.compare.splitView')}
          </Button>
          <Button
            variant={viewMode === 'original' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setViewMode('original')}
          >
            <FileText className="w-3 h-3 mr-1" />
            {t('resume.compare.originalOnly')}
          </Button>
          <Button
            variant={viewMode === 'ai' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setViewMode('ai')}
          >
            <Sparkles className="w-3 h-3 mr-1" />
            {t('resume.compare.aiOnly')}
          </Button>
        </div>
      </div>

      {viewMode === 'split' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {renderContent(originalVersion, t('resume.versionDetail.original'), 'original')}
          <div className="hidden lg:flex items-center justify-center">
            <div className="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center">
              <ArrowRight className="w-4 h-4 text-gray-500" />
            </div>
          </div>
          {renderContent(aiOptimizedVersion, t('resume.versionDetail.aiOptimized'), 'ai')}
        </div>
      )}

      {viewMode === 'original' && (
        renderContent(originalVersion, t('resume.versionDetail.original'), 'original')
      )}

      {viewMode === 'ai' && (
        renderContent(aiOptimizedVersion, t('resume.versionDetail.aiOptimized'), 'ai')
      )}
    </div>
  );
};
