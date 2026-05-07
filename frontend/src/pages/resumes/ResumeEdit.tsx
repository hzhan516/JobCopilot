import { useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useResumeStore } from '@/store/resume.store.ts';
import { MarkdownEditor } from '../../components/resume/MarkdownEditor';
import { Loader2, ArrowLeft } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { toast } from 'sonner';

export default function ResumeEdit() {
  const { t } = useTranslation();
  const { groupId, versionId } = useParams<{ groupId: string; versionId: string }>();
  const navigate = useNavigate();
  const { currentGroup, loading, fetchGroupDetail, saveVersion } = useResumeStore();

  useEffect(() => {
    if (groupId && (!currentGroup || currentGroup.groupId !== groupId)) {
      fetchGroupDetail(groupId);
    }
  }, [groupId, currentGroup, fetchGroupDetail]);

  const version = currentGroup?.versions.find((v) => v.versionId === versionId);

  const handleSave = async (content: string) => {
    if (versionId) {
      await saveVersion(versionId, content);
      toast.success(t('resume.markdownEditor.autoSaveSuccess'));
      navigate(`/resumes/${groupId}`);
    }
  };

  const handleAutoSave = useCallback(async (content: string) => {
    if (versionId) {
      await saveVersion(versionId, content);
    }
  }, [versionId, saveVersion]);

  const handleCancel = () => {
    navigate(`/resumes/${groupId}`);
  };

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
        <Button onClick={() => navigate('/resumes')}>{t('resume.edit.back')}</Button>
      </div>
    );
  }

  if (!version) {
    return (
      <div className="text-center py-12">
        <h2 className="text-2xl font-semibold mb-4">{t('resume.detail.versionNotFound')}</h2>
        <Button onClick={() => navigate(`/resumes/${groupId}`)}>{t('resume.edit.backToDetail')}</Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6 h-[calc(100vh-4rem)] flex flex-col">
      <div className="flex items-center space-x-4 shrink-0">
        <Button variant="ghost" size="icon" onClick={handleCancel}>
          <ArrowLeft className="w-5 h-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{t('resume.edit.title')}</h1>
          <p className="text-muted-foreground text-sm">
            {currentGroup.title} - {version.versionType}
          </p>
        </div>
      </div>

      <div className="flex-grow min-h-0">
        <MarkdownEditor
          initialContent={version.content || ''}
          versionId={version.versionId}
          onSave={handleSave}
          onCancel={handleCancel}
          onAutoSave={version.status === 'ACTIVE' ? handleAutoSave : undefined}
          readOnly={version.status !== 'ACTIVE'}
        />
      </div>
    </div>
  );
}
