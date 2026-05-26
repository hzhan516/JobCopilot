import React, { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { UploadCloud, FileText, AlertCircle } from 'lucide-react';
import { Card, CardContent } from '../ui/card';
import { Progress } from '../ui/progress';
import { useResumeStore } from '../../store/resume.store';

interface ResumeUploadProps {
  onUpload: (file: File) => Promise<void>;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACCEPTED_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/markdown',
  'text/plain'
];

export const ResumeUpload: React.FC<ResumeUploadProps> = ({ onUpload }) => {
  const { t } = useTranslation();
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { uploadProgress, loading } = useResumeStore();

  const handleDragOver = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const validateAndUpload = useCallback(async (file: File) => {
    setError(null);
    
    if (!ACCEPTED_TYPES.includes(file.type) && !file.name.endsWith('.md')) {
      setError(t('resume.upload.invalidType'));
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setError(t('resume.upload.sizeExceeded'));
      return;
    }

    try {
      await onUpload(file);
    } catch {
      setError(t('resume.upload.uploadError'));
    }
  }, [onUpload, t]);

  const handleDrop = useCallback((e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const file = e.dataTransfer.files[0];
      validateAndUpload(file);
    }
  }, [validateAndUpload]);

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0];
      validateAndUpload(file);
    }
  };

  return (
    <Card className="w-full max-w-2xl mx-auto">
      <CardContent className="p-6">
        <div
          data-testid="resume-upload-dropzone"
          className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
            isDragging ? 'border-primary bg-primary/5' : 'border-muted-foreground/25 hover:border-primary/50'
          } ${loading ? 'opacity-50 pointer-events-none' : 'cursor-pointer'}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => document.getElementById('resume-upload-input')?.click()}
        >
          <input
            id="resume-upload-input"
            data-testid="resume-upload-input"
            type="file"
            className="hidden"
            accept=".pdf,.docx,.md,.txt"
            onChange={handleFileInput}
            disabled={loading}
          />
          
          <div className="flex flex-col items-center justify-center space-y-4">
            <div className="p-4 bg-primary/10 rounded-full">
              <UploadCloud className="w-8 h-8 text-primary" />
            </div>
            
            <div className="space-y-1">
              <h3 className="text-lg font-semibold">
                {t('resume.upload.dragText')}
              </h3>
              <p className="text-sm text-muted-foreground">
                {t('resume.upload.singleUpload')}
              </p>
              <p className="text-xs text-muted-foreground">
                {t('resume.upload.formats')}
              </p>
            </div>
          </div>
        </div>

        {error && (
          <div className="mt-4 flex items-center text-destructive text-sm">
            <AlertCircle className="w-4 h-4 mr-2" />
            {error}
          </div>
        )}

        {loading && uploadProgress > 0 && (
          <div className="mt-6 space-y-2">
            <div className="flex justify-between text-sm">
              <span className="flex items-center text-muted-foreground">
                <FileText className="w-4 h-4 mr-2" />
                {t('resume.upload.uploading')}
              </span>
              <span className="font-medium">{uploadProgress}%</span>
            </div>
            <Progress value={uploadProgress} className="h-2" />
          </div>
        )}
      </CardContent>
    </Card>
  );
};
