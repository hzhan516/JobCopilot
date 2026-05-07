import { saveAs } from 'file-saver';
import { resumeApi } from '../services/resume.api';
import type { DownloadFormat } from '../types/resume';

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

export const downloadResume = async (versionId: string, format: DownloadFormat, filename: string) => {
  try {
    const blob = await resumeApi.downloadVersion(versionId, format);
    const extension = `.${format}`;
    saveAs(blob, `${filename}${extension}`);
  } catch (error) {
    console.error('Download failed:', error);
    throw error;
  }
};
