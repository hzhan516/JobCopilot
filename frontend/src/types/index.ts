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

// 用户资料类型
// User profile type
export interface Profile {
  userId: string;
  fullName: string | null;
  avatarUrl: string | null;
  phone: string | null;
  targetPosition: string | null;
  preferredLocation: string | null;
  createdAt: string;
  updatedAt: string;
}

// 更新资料请求
// Update profile request
export interface UpdateProfileRequest {
  fullName: string;
  phone: string;
  targetPosition: string;
  preferredLocation: string;
}

// 更新头像请求
// Update avatar request
export interface UpdateAvatarRequest {
  avatarUrl: string;
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

export interface LoginByGoogleRequest {
  idToken: string;
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

// ========== 职位类型 ==========

// 职位内容（解析后）
export interface ParsedJobContent {
  title: string;
  company: string;
  description: string;
  requirements: string[];
}

// 职位基础类型
export interface Job {
  id: string;
  userId: string;
  originalUrl: string;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  parsedContent: ParsedJobContent | null;
  imageCheckEnabled: boolean;
  errorMessage: string | null;
  createdAt?: string;
}

// 匹配因子
export interface MatchFactors {
  skillMatch: number;
  experienceMatch: number;
  locationMatch: number;
}

// 匹配项
export interface MatchItem {
  jobId: string;
  title: string;
  company: string;
  matchScore: number;
  matchFactors: MatchFactors;
  description: string;
}

// 发起匹配请求
export interface JobMatchRequest {
  resumeVersionId: string;
  query?: string;
  topK?: number;
  filters?: Record<string, string>;
}

// 匹配响应
export interface JobMatchResponse {
  matchId: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  matches: MatchItem[];
  total: number;
  recallTime: number;
  rankTime: number;
}

// 匹配历史
export interface JobMatchHistoryResponse {
  matchId: string;
  userId: string;
  resumeVersionId: string;
  query: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  matches: MatchItem[];
  total: number;
  recallTime: number;
  rankTime: number;
  modelVersion: string;
  createdAt: string;
  completedAt: string;
}

// ========== 对话类型 ==========

export interface Conversation {
  conversationId: string;
  userId?: string;
  title: string;
  status?: string;
  resumeId?: string;
  resumeVersionId?: string | null;
  jobId?: string | null;
  messages?: Message[];
  createdAt: string;
  updatedAt: string;
}

// 消息类型
export interface Message {
  messageId: string;
  conversationId?: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  sequence?: number;
  fileUrl?: string | null;
  createdAt: string;
}

// ========== 求职跟踪类型 ==========

export interface TrackingEvent {
  eventId: string;
  trackingId: string;
  eventType: string;
  description: string;
  createdAt: string;
}

export interface Tracking {
  trackingId: string;
  userId: string;
  job: Job | null;
  companyName: string;
  jobTitle: string;
  status: 'PENDING' | 'APPLIED' | 'SCREENING' | 'INTERVIEWING' | 'OFFER' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';
  appliedAt: string;
  updatedAt: string;
  notes: string | null;
  events: TrackingEvent[];
}

export interface CreateTrackingRequest {
  jobId?: string;
  companyName: string;
  jobTitle: string;
  status?: Tracking['status'];
  appliedAt?: string;
  notes?: string;
}

export interface UpdateTrackingRequest {
  status: Tracking['status'];
  notes?: string;
}

export interface TrackingStatsResponse {
  total: number;
  applied: number;
  screening: number;
  interview: number;
  offer: number;
  rejected: number;
  withdrawn: number;
}

// 保留旧名称以兼容现有代码（将被逐步替换）
export interface JobApplication {
  applicationId: string;
  jobId: string;
  jobTitle: string;
  company: string;
  status: 'APPLIED' | 'SCREENING' | 'INTERVIEW' | 'OFFER' | 'REJECTED' | 'WITHDRAWN';
  appliedAt: string;
  notes?: string;
}
