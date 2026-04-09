// 通用响应类型
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 分页响应类型
export interface PaginatedResponse<T> {
  list: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

// 用户类型
export interface User {
  userId: string;
  email: string;
}

// 认证响应
export interface AuthResponse {
  userId: string;
  email: string;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

// 登录/注册请求
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

// 版本摘要
export interface VersionSummary {
  versionId: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  exists: boolean;
}

// 简历组
export interface ResumeGroup {
  groupId: string;
  title: string;
  isDefault: boolean;
  createdAt: string;
  updatedAt: string;
  originalVersion: VersionSummary | null;
  convertedVersion: VersionSummary | null;
  aiOptimizedVersion: VersionSummary | null;
}

// 简历版本
export interface ResumeVersion {
  versionId: string;
  groupId: string;
  versionType: 'ORIGINAL' | 'CONVERTED' | 'AI_OPTIMIZED';
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  originalFileName: string;
  fileType: string;
  fileSize: number;
  content: string | null;
  editable: boolean;
  createdAt: string;
  updatedAt: string;
}

// 上传响应
export interface ResumeUploadResponse {
  groupId: string;
  originalVersionId: string;
  title: string;
  createdAt: string;
}

// 编辑请求
export interface ResumeEditRequest {
  content: string;
}

// 职位类型
export interface Job {
  jobId: string;
  title: string;
  company: string;
  location: string;
  description: string;
  requirements: string[];
  salaryMin?: number;
  salaryMax?: number;
  postedAt: string;
  matchScore?: number;
}

// 对话类型
export interface Conversation {
  conversationId: string;
  title: string;
  resumeId?: string;
  createdAt: string;
  updatedAt: string;
}

// 消息类型
export interface Message {
  messageId: string;
  conversationId: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
}

// 求职投递记录
export interface JobApplication {
  applicationId: string;
  jobId: string;
  jobTitle: string;
  company: string;
  status: 'APPLIED' | 'SCREENING' | 'INTERVIEW' | 'OFFER' | 'REJECTED' | 'WITHDRAWN';
  appliedAt: string;
  notes?: string;
}
