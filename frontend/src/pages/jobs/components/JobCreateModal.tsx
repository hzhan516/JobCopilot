import { useState, useCallback } from 'react';
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
import { Plus, Link as LinkIcon, Image, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useTranslation } from 'react-i18next';

const MAX_SCREENSHOT_SIZE = 5 * 1024 * 1024; // 5MB

interface JobCreateModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (url: string, screenshot: File) => Promise<void>;
}

/**
 * 创建职位弹窗
 * Job creation modal — URL + screenshot upload form
 */
export default function JobCreateModal({ open, onOpenChange, onSubmit }: JobCreateModalProps) {
  const { t } = useTranslation();
  const [jobUrl, setJobUrl] = useState('');
  const [screenshotFile, setScreenshotFile] = useState<File | null>(null);
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
            <Input type="file" accept="image/*" onChange={handleFileChange} />
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
