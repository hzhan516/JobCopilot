import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
  const navigate = useNavigate();
  const { groups, loading, fetchGroups, uploadResume, pollParseStatus, deleteGroup } = useResumeStore();
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  useEffect(() => {
    fetchGroups();
  }, [fetchGroups]);

  const handleUpload = async (file: File) => {
    try {
      const data = await uploadResume(file, file.name.replace(/\.[^/.]+$/, ''));
      toast.success('Resume uploaded successfully. Parsing started...');
      setIsUploadOpen(false);
      
      const status = await pollParseStatus(data.groupId);
      if (status === 'COMPLETED') {
        toast.success('Resume parsed successfully!');
      } else if (status === 'FAILED') {
        toast.error('Failed to parse resume.');
      } else {
        toast.warning('Parsing is taking longer than expected.');
      }
      
      await fetchGroups();
    } catch (error) {
      toast.error('Failed to upload resume.');
      console.error(error);
    }
  };

  const handleView = (groupId: string) => {
    navigate(`/resumes/${groupId}/edit`);
  };

  const handleDelete = async (groupId: string) => {
    if (window.confirm('Are you sure you want to delete this resume?')) {
      try {
        await deleteGroup(groupId);
        toast.success('Resume deleted successfully.');
      } catch (error) {
        toast.error('Failed to delete resume.');
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
          <h1 className="text-3xl font-bold tracking-tight">My Resumes</h1>
          <p className="text-muted-foreground mt-1">
            Manage your resumes and track their parsing status.
          </p>
        </div>
        <Button onClick={() => setIsUploadOpen(true)} className="w-full sm:w-auto">
          <Upload className="w-4 h-4 mr-2" />
          Upload Resume
        </Button>
      </div>

      {groups.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-16 border-2 border-dashed rounded-lg bg-muted/10">
          <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mb-4">
            <FileText className="w-8 h-8 text-primary" />
          </div>
          <h3 className="text-lg font-medium mb-2">No resumes yet</h3>
          <p className="text-muted-foreground mb-6 text-center max-w-sm">
            Upload your first resume to get started. We'll automatically parse it and extract your information.
          </p>
          <Button onClick={() => setIsUploadOpen(true)}>
            <Upload className="w-4 h-4 mr-2" />
            Upload Resume
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
            <DialogTitle>Upload Resume</DialogTitle>
            <DialogDescription>
              Upload your resume in PDF, DOCX, MD, or TXT format. Maximum file size is 10MB.
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
