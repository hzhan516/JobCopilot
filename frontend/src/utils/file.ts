import { saveAs } from 'file-saver';
import { resumeService } from '../services/resumeService';
import type { DownloadFormat } from '../types/resume';

// ponytail: hand-rolled formatter; Intl.NumberFormat + k factor works if i18n needed
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

export const downloadResume = async (versionId: string, format: DownloadFormat, filename: string) => {
  try {
    const blob = await resumeService.downloadResume(versionId, format);
    const extension = `.${format}`;
    saveAs(blob, `${filename}${extension}`);
  } catch (error) {
    console.error('Download failed:', error);
    throw error;
  }
};
