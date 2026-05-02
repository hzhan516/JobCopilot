import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatDate } from '@/utils/i18n';
import { useResumeStore } from '../../store/resume.store';
import { VersionTimeline } from '../../components/resume/VersionTimeline';
import { VersionDetail } from '../../components/resume/VersionDetail';
import { AIOptimizeCompare } from '../../components/resume/AIOptimizeCompare';
import { Loader2, ArrowLeft, Sparkles, FileText, Copy } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { toast } from 'sonner';


type TabType = 'detail' | 'compare';

export default function ResumeDetail() {
  const { t } = useTranslation();
  const { groupId } = useParams<{ groupId: string }>();
  const safeGroupId = groupId!;
  const navigate = useNavigate();
  const { currentGroup, loading, fetchGroupDetail, createVersion } = useResumeStore();
  const [activeTab, setActiveTab] = useState<TabType>('detail');
  const [selectedVersionId, setSelectedVersionId] = useState<string | null>(() => {
    if (currentGroup && currentGroup.versions.length > 0) {
      const sortedVersions = [...currentGroup.versions].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      return sortedVersions[0].versionId;
    }
    return null;
  });

  useEffect(() => {
    if (groupId) {
      fetchGroupDetail(groupId);
    }
  }, [groupId, fetchGroupDetail]);

  useEffect(() => {
    if (currentGroup && currentGroup.versions.length > 0 && !selectedVersionId) {
      const sortedVersions = [...currentGroup.versions].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      setSelectedVersionId(sortedVersions[0].versionId);
    }
  }, [currentGroup]);

  if (loading && !currentGroup) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="w-8 h-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!currentGroup) {
    return (
      <div className="text-center py-12">
        <h2 className="text-2xl font-semibold mb-4">{t('resume.detail.notFound')}</h2>
        <Button onClick={() => navigate('/resumes')}>{t('resume.detail.backToList')}</Button>
      </div>
    );
  }

  const selectedVersion = currentGroup.versions.find(v => v.versionId === selectedVersionId);

  // 找到 Original 和 AI Optimized 版本用于对比
  const originalVersion = currentGroup.versions.find(
    (v) => v.versionType === 'ORIGINAL'
  ) || null;
  const aiOptimizedVersion = currentGroup.versions.find(
    (v) => v.versionType === 'AI_OPTIMIZED'
  ) || null;
  const hasAIOptimized = aiOptimizedVersion && aiOptimizedVersion.parseStatus === 'COMPLETED';

  return (
    <div className="container mx-auto py-6 space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-4">
          <Button variant="ghost" size="icon" onClick={() => navigate('/resumes')}>
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">{currentGroup.title}</h1>
            <p className="text-muted-foreground">
              {t('resume.detail.createdOn')} {formatDate(currentGroup.createdAt)}
            </p>
          </div>
        </div>
        <Button
          variant="outline"
          onClick={async () => {
            try {
              const sourceVersion = selectedVersion && selectedVersion.versionType !== 'ORIGINAL'
                ? selectedVersion.versionId
                : undefined;
              const newVersionId = await createVersion(safeGroupId, sourceVersion);
              toast.success(t('resume.detail.createCopySuccess'));
              navigate(`/resumes/${safeGroupId}/versions/${newVersionId}/edit`);
            } catch {
              toast.error(t('resume.detail.createCopyError'));
            }
          }}
        >
          <Copy className="w-4 h-4 mr-2" />
          {t('resume.detail.manualEdit')}
        </Button>
      </div>

      <div className="flex flex-col md:flex-row gap-6">
        <div className="w-full md:w-1/4">
          <div className="sticky top-6 space-y-4">
            <h3 className="text-lg font-semibold">{t('resume.detail.versionHistory')}</h3>
            <VersionTimeline
              versions={currentGroup.versions}
              selectedVersionId={selectedVersionId || ''}
              onSelectVersion={(id) => {
                setSelectedVersionId(id);
                setActiveTab('detail');
              }}
            />
          </div>
        </div>

        <div className="w-full md:w-3/4 space-y-4">
          {/* Tab 切换 */}
          <div className="flex items-center space-x-1 border-b">
            <button
              onClick={() => setActiveTab('detail')}
              className={`flex items-center px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'detail'
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <FileText className="w-4 h-4 mr-2" />
              {t('resume.detail.versionDetail')}
            </button>
            {hasAIOptimized && (
              <button
                onClick={() => setActiveTab('compare')}
                className={`flex items-center px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === 'compare'
                    ? 'border-amber-600 text-amber-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                <Sparkles className="w-4 h-4 mr-2" />
                {t('resume.compare.title')}
              </button>
            )}
          </div>

          {/* Tab 内容 */}
          {activeTab === 'detail' && selectedVersion ? (
            <VersionDetail
              version={selectedVersion}
              onEdit={() =>
                navigate(`/resumes/${groupId}/versions/${selectedVersion.versionId}/edit`)
              }
              onCreateCopy={async () => {
                try {
                  const newVersionId = await createVersion(safeGroupId, selectedVersion.versionId);
                  toast.success(t('resume.detail.createCopySuccess'));
                  navigate(`/resumes/${safeGroupId}/versions/${newVersionId}/edit`);
                } catch {
                  toast.error(t('resume.detail.createCopyError'));
                }
              }}
            />
          ) : activeTab === 'detail' ? (
            <div className="flex items-center justify-center h-64 border rounded-lg bg-muted/20">
              <p className="text-muted-foreground">{t('resume.detail.selectVersion')}</p>
            </div>
          ) : null}

          {activeTab === 'compare' && (
            <AIOptimizeCompare
              originalVersion={originalVersion}
              aiOptimizedVersion={aiOptimizedVersion}
            />
          )}
        </div>
      </div>
    </div>
  );
}
