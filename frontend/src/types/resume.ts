export interface ResumeGroup {
  groupId: string;
  title: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
  versions: ResumeVersion[];
}

export interface ResumeVersion {
  versionId: string;
  groupId: string;
  versionType: 'ORIGINAL' | 'CONVERTED' | 'AI_OPTIMIZED';
  status: 'ACTIVE' | 'ARCHIVED';
  storagePath: string;
  content?: string;
  parsedContent?: {
    title: string;
    company: string;
    description: string;
    requirements: string[];
  };
  parseStatus: 'PENDING' | 'PARSING' | 'COMPLETED' | 'FAILED' | 'NOT_APPLICABLE';
  parseErrorMessage?: string;
  createdAt: string;
}

export interface UploadResponse {
  code: number;
  data: {
    groupId: string;
    originalVersionId: string;
    title: string;
    createdAt: string;
  };
}

export type DownloadFormat = 'original' | 'pdf' | 'docx' | 'md' | 'html' | 'txt';
