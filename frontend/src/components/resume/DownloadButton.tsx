import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import { Download, Loader2 } from 'lucide-react';
import type { DownloadFormat } from '@/types/resume.ts';
import { downloadResume } from '@/utils/file.ts';

interface DownloadButtonProps {
  versionId: string;
  filename?: string;
}

export function DownloadButton({ versionId, filename = 'resume' }: DownloadButtonProps) {
  const { t } = useTranslation();
  const [isDownloading, setIsDownloading] = useState(false);

  const handleDownload = async (format: DownloadFormat) => {
    try {
      setIsDownloading(true);
      await downloadResume(versionId, format, filename);
    } catch (error) {
      console.error(t('resume.download.error'), error);
    } finally {
      setIsDownloading(false);
    }
  };

  const formats: { labelKey: string; value: DownloadFormat }[] = [
    { labelKey: 'resume.download.pdf', value: 'pdf' },
    { labelKey: 'resume.download.docx', value: 'docx' },
    { labelKey: 'resume.download.md', value: 'md' },
    { labelKey: 'resume.download.html', value: 'html' },
    { labelKey: 'resume.download.txt', value: 'txt' },
  ];

  const availableFormats = formats;

  if (availableFormats.length === 1) {
    return (
      <Button 
        variant="outline" 
        size="sm" 
        onClick={() => handleDownload(availableFormats[0].value)}
        disabled={isDownloading}
        className="flex items-center gap-2"
      >
        {isDownloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
        {t('resume.download.button')}
      </Button>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" disabled={isDownloading} className="flex items-center gap-2">
          {isDownloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
          {t('resume.download.button')}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {availableFormats.map((format) => (
          <DropdownMenuItem 
            key={format.value} 
            onClick={() => handleDownload(format.value)}
            className="cursor-pointer"
          >
            {t(format.labelKey)}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
