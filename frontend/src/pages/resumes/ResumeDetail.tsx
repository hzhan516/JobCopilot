import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatDate } from '@/utils/i18n';
import { useResumeStore } from '../../store/resume.store';
import { VersionTimeline } from '../../components/resume/VersionTimeline';
import { VersionDetail } from '../../components/resume/VersionDetail';
import { Loader2, ArrowLeft } from 'lucide-react';
import { Button } from '../../components/ui/button';

export default function ResumeDetail() {
  const { t } = useTranslation();
  const { groupId } = useParams<{ groupId: string }>();
  const navigate = useNavigate();
  const { currentGroup, loading, fetchGroupDetail } = useResumeStore();
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

  return (
    <div className="container mx-auto py-6 space-y-6">
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

      <div className="flex flex-col md:flex-row gap-6">
        <div className="w-full md:w-1/4">
          <div className="sticky top-6">
            <h3 className="text-lg font-semibold mb-4">{t('resume.detail.versionHistory')}</h3>
            <VersionTimeline
              versions={currentGroup.versions}
              selectedVersionId={selectedVersionId || ''}
              onSelectVersion={setSelectedVersionId}
            />
          </div>
        </div>
        
        <div className="w-full md:w-3/4">
          {selectedVersion ? (
            <VersionDetail
              version={selectedVersion}
              onEdit={() => navigate(`/resumes/${groupId}/versions/${selectedVersion.versionId}/edit`)}
            />
          ) : (
            <div className="flex items-center justify-center h-64 border rounded-lg bg-muted/20">
              <p className="text-muted-foreground">{t('resume.detail.selectVersion')}</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
