import { useState } from 'react';
import { Button } from '../ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import { Download, Loader2 } from 'lucide-react';
import type { DownloadFormat, ResumeVersion } from '../../types/resume';
import { downloadResume } from '../../utils/file';

interface DownloadButtonProps {
  versionId: string;
  versionType: ResumeVersion['versionType'];
  filename?: string;
}

export function DownloadButton({ versionId, versionType, filename = 'resume' }: DownloadButtonProps) {
  const [isDownloading, setIsDownloading] = useState(false);

  const handleDownload = async (format: DownloadFormat) => {
    try {
      setIsDownloading(true);
      await downloadResume(versionId, format, filename);
    } catch (error) {
      console.error('Failed to download resume:', error);
    } finally {
      setIsDownloading(false);
    }
  };

  const formats: { label: string; value: DownloadFormat }[] = [
    { label: 'Original File', value: 'original' },
    { label: 'PDF Document', value: 'pdf' },
    { label: 'Word Document', value: 'docx' },
    { label: 'Markdown', value: 'md' },
    { label: 'HTML', value: 'html' },
    { label: 'Plain Text', value: 'txt' },
  ];

  const availableFormats = versionType === 'ORIGINAL' 
    ? formats.filter(f => f.value === 'original')
    : formats;

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
        Download
      </Button>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" disabled={isDownloading} className="flex items-center gap-2">
          {isDownloading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
          Download
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        {availableFormats.map((format) => (
          <DropdownMenuItem 
            key={format.value} 
            onClick={() => handleDownload(format.value)}
            className="cursor-pointer"
          >
            {format.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
