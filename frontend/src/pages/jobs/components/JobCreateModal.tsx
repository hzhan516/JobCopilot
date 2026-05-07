import { useState, useCallback, useRef } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Plus, Link as LinkIcon, Image, Loader2, FileText, UploadCloud } from 'lucide-react';
import { toast } from 'sonner';
import { useTranslation } from 'react-i18next';

const MAX_SCREENSHOT_SIZE = 5 * 1024 * 1024; // 5MB

interface JobCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (url: string, screenshot: File) => Promise<void>;
}

export default function JobCreateModal({ open, onOpenChange, onSubmit }: JobCreateModalProps) {
  const { t } = useTranslation();
  const [jobUrl, setJobUrl] = useState('');
  const [screenshotFile, setScreenshotFile] = useState<File | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const resetForm = useCallback(() => {
    setJobUrl('');
    setScreenshotFile(null);
  }, []);

  const handleOpenChange = useCallback(
    (value: boolean) => {
      onOpenChange(value);
      if (!value) {
        resetForm();
      }
    },
    [onOpenChange, resetForm]
  );

  const handleFileChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0] ?? null;
      if (file && file.size > MAX_SCREENSHOT_SIZE) {
        toast.error(t('jobList.screenshotTooLarge'));
        setScreenshotFile(null);
        e.target.value = '';
        return;
      }
      setScreenshotFile(file);
    },
    [t]
  );

  const handleSubmit = useCallback(async () => {
    if (!jobUrl.trim()) {
      toast.error(t('jobList.urlRequired'));
      return;
    }
    if (!screenshotFile) {
      toast.error(t('jobList.screenshotRequired'));
      return;
    }
    if (screenshotFile.size > MAX_SCREENSHOT_SIZE) {
      toast.error(t('jobList.screenshotTooLarge'));
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(jobUrl.trim(), screenshotFile);
      resetForm();
    } catch {
      // Errors are handled by the caller (toast)
      // 错误已由调用方处理（toast）
    } finally {
      setIsSubmitting(false);
    }
  }, [jobUrl, screenshotFile, onSubmit, resetForm, t]);

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{t('jobList.addJobDialogTitle')}</DialogTitle>
          <DialogDescription>{t('jobList.addJobDialogDesc')}</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <label className="text-sm font-medium mb-2 block flex items-center">
              <LinkIcon className="w-4 h-4 mr-1" />
              {t('jobList.jobUrl')}
            </label>
            <Input
              placeholder={t('jobList.jobUrlPlaceholder')}
              value={jobUrl}
              onChange={(e) => setJobUrl(e.target.value)}
            />
          </div>
          <div>
            <label className="text-sm font-medium mb-2 block flex items-center">
              <Image className="w-4 h-4 mr-1" />
              {t('jobList.jobScreenshot')}
            </label>
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              accept="image/*"
              onChange={handleFileChange}
            />
            <div
              className="border-2 border-dashed border-muted-foreground/25 rounded-lg p-4 text-center cursor-pointer hover:border-primary/50 transition-colors"
              onClick={() => fileInputRef.current?.click()}
            >
              <div className="flex flex-col items-center justify-center space-y-2">
                <UploadCloud className="w-6 h-6 text-muted-foreground" />
                <span className="text-sm text-muted-foreground">
                  {t('jobList.selectScreenshot')}
                </span>
              </div>
            </div>
            {screenshotFile && (
              <div className="mt-2 flex items-center text-sm text-muted-foreground">
                <FileText className="w-4 h-4 mr-2 flex-shrink-0" />
                <span className="truncate">{screenshotFile.name}</span>
              </div>
            )}
            <p className="text-xs text-gray-500 mt-1">{t('jobList.screenshotHint')}</p>
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => handleOpenChange(false)}>
            {t('common.cancel')}
          </Button>
          <Button onClick={handleSubmit} disabled={isSubmitting}>
            {isSubmitting ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Plus className="w-4 h-4 mr-2" />
            )}
            {t('jobList.submitJob')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
