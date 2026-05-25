export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PaginatedResponse<T> {
  list: T[];
  page: number;
  size: number;
  total: number;
  totalPages: number;
}

export interface User {
  userId: string;
  email: string;
}

export interface AuthResponse {
  userId: string;
  email: string;
  accessToken: string;
  expiresIn: number;
}

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

export interface UpdateProfileRequest {
  fullName: string;
  phone: string;
  targetPosition: string;
  preferredLocation: string;
}

export interface UpdateAvatarRequest {
  avatarUrl: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  captchaToken?: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  verificationCode?: string;
  captchaToken?: string;
}

export interface SendVerificationCodeRequest {
  email: string;
  captchaToken?: string;
}

export interface LoginByGoogleRequest {
  idToken: string;
  captchaToken?: string;
}

export interface VersionSummary {
  versionId: string;
  status: 'ACTIVE' | 'ARCHIVED';
  parseStatus: 'PENDING' | 'PARSING' | 'COMPLETED' | 'FAILED' | 'NOT_APPLICABLE';
  createdAt: string;
  exists: boolean;
}

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

export interface ResumeVersion {
  versionId: string;
  groupId: string;
  versionType: 'ORIGINAL' | 'CONVERTED' | 'AI_OPTIMIZED';
  status: 'ACTIVE' | 'ARCHIVED';
  parseStatus: 'PENDING' | 'PARSING' | 'COMPLETED' | 'FAILED' | 'NOT_APPLICABLE';
  originalFileName: string;
  fileType: string;
  fileSize: number;
  content: string | null;
  editable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ResumeUploadResponse {
  groupId: string;
  originalVersionId: string;
  title: string;
  createdAt: string;
}

export interface ResumeEditRequest {
  content: string;
}

export interface ParsedJobContent {
  title: string;
  company: string;
  salary: string;
  location: string;
  description: string;
  requirements: string[];
}

export interface Job {
  id: string;
  userId: string;
  originalUrl: string;
  status: 'PENDING' | 'SCRAPING' | 'PARSING' | 'COMPLETED' | 'FAILED';
  parsedContent: ParsedJobContent | null;
  imageCheckEnabled: boolean;
  errorMessage: string | null;
  createdAt?: string;
}

export interface MatchFactors {
  skillMatch: number;
  experienceMatch: number;
  locationMatch: number;
}

export interface MatchItem {
  jobId: string;
  title: string;
  company: string;
  matchScore: number;
  matchFactors: MatchFactors;
  description: string;
  matchReason?: string;
}

export interface JobMatchRequest {
  resumeVersionId: string;
  query?: string;
  topK?: number;
  filters?: Record<string, string>;
}

export interface JobMatchResponse {
  matchId: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  matches: MatchItem[];
  total: number;
  recallTime: number;
  rankTime: number;
}

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

export interface Conversation {
  conversationId: string;
  userId?: string;
  title: string;
  status?: string;
  resumeVersionId?: string | null;
  jobId?: string | null;
  messages?: Message[];
  createdAt: string;
  updatedAt: string;
}

export interface Message {
  messageId: string;
  conversationId?: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  sequence?: number;
  fileUrl?: string | null;
  createdAt: string;
}

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
  createdAt: string;
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
  companyName?: string;
  jobTitle?: string;
  status?: Tracking['status'];
  appliedAt?: string;
  notes?: string;
}

export interface TrackingStatsResponse {
  total: number;
  pending: number;
  applied: number;
  screening: number;
  interview: number;
  offer: number;
  rejected: number;
  withdrawn: number;
  successRate: number;
}

export interface CaptchaChallengeResponse {
  captchaId: string;
  targetX: number;
}

export interface CaptchaVerifyRequest {
  captchaId: string;
  offsetX: number;
}

export interface JobScoreRequest {
  resumeVersionId: string;
}

export interface JobScoreResponse {
  suitable: boolean;
  summary: string;
  finalScore: number;
  breakdown: {
    skillScore: number;
    experienceScore: number;
    overallScore: number;
  };
}

export interface JobScoreHistoryResponse {
  id: string;
  jobId: string;
  resumeVersionId: string;
  suitable: boolean;
  finalScore: number;
  skillScore: number;
  experienceScore: number;
  overallScore: number;
  summary: string;
  createdAt: string;
}

export interface UpdateJobRequest {
  title: string;
  company: string;
  salary: string;
  location: string;
  description: string;
  requirements: string[];
}

// Kept for backward compatibility during gradual migration
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
