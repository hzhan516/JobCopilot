import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useResumeStore } from '@/store/resume.store';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Spinner } from '@/components/ui/spinner';
import { FileText, Upload } from 'lucide-react';
import { toast } from 'sonner';
import { ResumeCard } from '@/components/resume/ResumeCard';
import { ResumeUpload } from '@/components/resume/ResumeUpload';

export default function ResumeList() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { groups, loading, fetchGroups, uploadResume, pollParseStatus, deleteGroup } = useResumeStore();
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  useEffect(() => {
    fetchGroups();
  }, [fetchGroups]);

  const handleUpload = async (file: File) => {
    try {
      const data = await uploadResume(file, file.name.replace(/\.[^/.]+$/, ''));
      toast.success(t('resume.list.uploadSuccess'));
      setIsUploadOpen(false);
      
      const status = await pollParseStatus(data.groupId);
      if (status === 'COMPLETED') {
        toast.success(t('resume.list.parseSuccess'));
      } else if (status === 'FAILED') {
        toast.error(t('resume.list.parseFailed'));
      } else {
        toast.warning(t('resume.list.parseTimeout'));
      }
      
      await fetchGroups();
    } catch (error) {
      toast.error(t('resume.list.uploadFailed'));
      console.error(error);
    }
  };

  const handleView = (groupId: string) => {
    navigate(`/resumes/${groupId}`);
  };

  const handleDelete = async (groupId: string) => {
    if (window.confirm(t('resume.list.deleteConfirm'))) {
      try {
        await deleteGroup(groupId);
        toast.success(t('resume.list.deleteSuccess'));
      } catch (error) {
        toast.error(t('resume.list.deleteFailed'));
        console.error(error);
      }
    }
  };

  if (loading && groups.length === 0) {
    return (
      <div className="flex items-center justify-center h-[50vh]">
        <Spinner className="w-8 h-8 text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t('resume.list.title')}</h1>
          <p className="text-muted-foreground mt-1">
            {t('resume.list.subtitle')}
          </p>
        </div>
        <Button onClick={() => setIsUploadOpen(true)} className="w-full sm:w-auto">
          <Upload className="w-4 h-4 mr-2" />
          {t('resume.list.upload')}
        </Button>
      </div>

      {groups.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 border-2 border-dashed rounded-lg bg-muted/10">
          <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            <FileText className="w-8 h-8 text-primary" />
          </div>
          <h3 className="text-lg font-medium mb-2">{t('resume.list.noResumes')}</h3>
          <p className="text-muted-foreground mb-6 text-center max-w-sm">
            {t('resume.list.noResumesDesc')}
          </p>
          <Button onClick={() => setIsUploadOpen(true)}>
            <Upload className="w-4 h-4 mr-2" />
            {t('resume.list.upload')}
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {groups.map((group) => (
            <ResumeCard
              key={group.groupId}
              group={group}
              onView={handleView}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}

      <Dialog open={isUploadOpen} onOpenChange={setIsUploadOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t('resume.list.uploadDialogTitle')}</DialogTitle>
            <DialogDescription>
              {t('resume.list.uploadDialogDesc')}
            </DialogDescription>
          </DialogHeader>
          <div className="mt-4">
            <ResumeUpload onUpload={handleUpload} />
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
