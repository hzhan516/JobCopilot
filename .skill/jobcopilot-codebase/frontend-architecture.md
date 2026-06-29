# Frontend Architecture Details

## Route Structure

| Path | Guard | Rendered Page |
|------|-------|---------------|
| `/login` | `PublicRoute` | `Login` |
| `/register` | `PublicRoute` | `Register` |
| `/` | `ProtectedRoute` | `Dashboard` |
| `/resumes` | `ProtectedRoute` | `ResumeList` |
| `/resumes/:groupId` | `ProtectedRoute` | `ResumeDetail` |
| `/resumes/:groupId/versions/:versionId/edit` | `ProtectedRoute` | `ResumeEdit` |
| `/jobs` | `ProtectedRoute` | `JobList` |
| `/jobs/:jobId` | `ProtectedRoute` | `JobDetail` |
| `/chat` | `ProtectedRoute` | `Chat` |
| `/applications` | `ProtectedRoute` | `Tracking` |
| `/profile` | `ProtectedRoute` | `Profile` |
| `*` | — | `Navigate to="/"` |

## Component Tree

```
StrictMode
└── GoogleOAuthProvider
    └── AuthProvider (context)
        └── Router
            └── ErrorBoundary
                ├── PublicRoute
                │   ├── Login
                │   └── Register
                └── ProtectedRoute
                    └── MainLayout (header nav + user menu + content area)
                        ├── Dashboard
                        ├── ResumeList / ResumeDetail / ResumeEdit
                        ├── JobList / JobDetail
                        ├── Chat
                        ├── Tracking
                        └── Profile
        └── Toaster (sonner notifications)
```

Notable shared components:

| Component | File | Purpose |
|-----------|------|---------|
| `MainLayout` | `components/layout/MainLayout.tsx` | Top navigation, mobile sheet menu, user dropdown, language switcher |
| `ErrorBoundary` | `components/layout/ErrorBoundary.tsx` | Global class component error boundary |
| `ProtectedRoute` | `components/ProtectedRoute.tsx` | Redirects unauthenticated users to `/login`, preserving `from` location |
| `PublicRoute` | `components/PublicRoute.tsx` | Redirects authenticated users away from `/login`/`/register` |
| `LanguageSwitcher` | `components/LanguageSwitcher.tsx` | Language dropdown using `useLanguageStore` |
| `SliderCaptcha` | `components/SliderCaptcha.tsx` | Pointer-based slider CAPTCHA |
| `ResumeUpload` | `components/resume/ResumeUpload.tsx` | Drag-and-drop resume upload (PDF/DOCX/MD/TXT, max 10 MB) |
| `ResumeCard` | `components/resume/ResumeCard.tsx` | Summary card for a resume group |
| `VersionTimeline` | `components/resume/VersionTimeline.tsx` | Timeline of versions within a group |
| `VersionDetail` | `components/resume/VersionDetail.tsx` | Version content and metadata display |
| `MarkdownEditor` | `components/resume/MarkdownEditor.tsx` | Resume content editor |
| `AIOptimizeCompare` | `components/resume/AIOptimizeCompare.tsx` | AI optimization diff/compare view |
| `DownloadButton` | `components/resume/DownloadButton.tsx` | Resume download trigger |
| `ParseStatusBadge` | `components/resume/ParseStatusBadge.tsx` | Parse status indicator |
| `JobCard` | `pages/jobs/components/JobCard.tsx` | Job list item card |
| `JobCreateModal` | `pages/jobs/components/JobCreateModal.tsx` | Modal to submit a new job URL/screenshot |
| `JobFilterBar` | `pages/jobs/components/JobFilterBar.tsx` | Search/sort controls for job list |
| `JobScorePanel` | `pages/jobs/components/JobScorePanel.tsx` | Resume selection + job scoring UI |

## State Management (Zustand)

| Store | File | Purpose |
|-------|------|---------|
| `useResumeStore` | `store/resume.store.ts` | Resume groups, current group detail, upload progress, parse polling, version lifecycle |
| `useJobStore` | `store/job.store.ts` | Job list, client-side search/sort, score results, scoring state, selected resume per job |
| `useProfileStore` | `store/profile.store.ts` | User profile data, avatar URL |
| `useLanguageStore` | `store/language.store.ts` | Current language selection (`en`, `zh-CN`, `zh-TW`) |

## Service Layer

Services are plain TypeScript objects (not classes) that wrap Axios calls via `apiClient`:

| Service | File | Key Operations |
|---------|------|----------------|
| `authService` | `services/api.ts` | `login`, `register`, `loginByGoogle`, `logout`, `sendVerificationCode`, `isEmailVerificationEnabled`, `getCaptchaChallenge`, `verifyCaptcha`, `getCurrentUser`, `isAuthenticated`, `isTokenExpired` |
| `resumeService` | `services/resumeService.ts` | `uploadResume`, `downloadResume`, `getResumeGroups`, `getResumeGroup`, `deleteResumeGroup`, `getVersionsByGroup`, `getVersion`, `deleteVersion`, `editVersion`, `createVersion`, `activateVersion` |
| `jobService` | `services/jobService.ts` | `getJobs`, `getJob`, `submitJob`, `updateJob`, `deleteJob`, `scoreJob`, `trackAction`, `getScoreHistory` |
| `chatService` | `services/chatService.ts` | `createConversation`, `getConversations`, `getConversation`, `deleteConversation`, `sendMessage` |
| `trackingService` | `services/trackingService.ts` | `getTrackings`, `getTracking`, `createTracking`, `updateTracking`, `deleteTracking`, `getTrackingStats` |
| `profileService` | `services/profileService.ts` | `getProfile`, `updateProfile`, `updateAvatar` |
| `tokenStorage` | `services/tokenStorage.ts` | Access/refresh token + user persistence with "Remember Me" (`localStorage`/`sessionStorage`) |

## API Client Features

- **File**: `services/api.ts`
- **Base URL**: `VITE_API_BASE_URL` or `/api` (relative, proxied through Nginx)
- **Timeout**: 30 seconds
- **Auto JWT attach**: Request interceptor adds `Authorization: Bearer <accessToken>` from `tokenStorage`
- **Auto refresh**: On `401`, calls `/v1/auth/refresh` (HttpOnly cookie), retries the original request, and queues concurrent requests via `refreshSubscribers`
- **GET retry**: Retries up to 2 times on transient network errors (no response, excluding timeouts) with exponential backoff (1s, 2s)
- **Language header**: `Accept-Language` sent on every request using `i18n.language`
- **Logout**: `authService.logout()` calls `POST /v1/auth/logout` and clears storage

## Key Custom Hooks

| Hook | File | Purpose |
|------|------|---------|
| `useAuth` | `hooks/useAuth.tsx` | Auth context provider; exposes `user`, `isAuthenticated`, `isLoading`, `login`, `loginByGoogle`, `register`, `logout` |
| `useIsMobile` | `hooks/use-mobile.ts` | Detects viewport width below 768 px using `matchMedia` |
| `useTimeZone` | `hooks/useTimeZone.ts` | Reads/writes `user-timezone` from `localStorage`, syncs across tabs |

## i18n Setup

- **Library**: i18next + react-i18next
- **Languages**: `en`, `zh-CN`, `zh-TW`
- **Namespace files**:
  - `locales/en.json`
  - `locales/zh-CN.json`
  - `locales/zh-TW.json`
- **Detection**: `i18next-browser-languagedetector`
  - Order: `localStorage`, `navigator`
  - Cache: `localStorage` under key `i18nextLng`
  - Fallback: `en`
- **Init**: `i18n/index.ts`
- **Utilities**: `utils/i18n.ts` provides `getLocale`, `formatDate`, `formatTime`, `formatDateTime` using the stored time zone

## UI Components

- **Framework**: shadcn/ui New York style (based on Radix UI primitives)
- **Config**: `components.json` (`style: new-york`, `tsx: true`, `baseColor: slate`, CSS variables enabled)
- **Styling**: Tailwind CSS 3.4 + `tailwindcss-animate`
- **Utilities**: `lib/utils.ts` exports `cn()` using `clsx` + `tailwind-merge`
- **Icons**: `lucide-react`
- **Charts**: `recharts`
- **Markdown editor**: `@uiw/react-md-editor`
- **Notifications**: `sonner`
- **shadcn components in `components/ui/`**: alert, badge, button, card, checkbox, dialog, dropdown-menu, form, input, label, progress, select, separator, sheet, skeleton, sonner, spinner, textarea, toggle, tooltip
