# Frontend Architecture Details

## Route Structure

```
/login          → PublicRoute  → Login page
/register       → PublicRoute  → Register page
/               → ProtectedRoute → Dashboard (home)
/resumes        → ProtectedRoute → ResumeList
/resumes/:groupId → ProtectedRoute → ResumeDetail
/resumes/:groupId/versions/:versionId/edit → ProtectedRoute → ResumeEdit
/jobs           → ProtectedRoute → JobList
/jobs/:jobId    → ProtectedRoute → JobDetail
/chat           → ProtectedRoute → Chat
/applications   → ProtectedRoute → Tracking
/profile        → ProtectedRoute → Profile
*               → Redirect to /
```

## Component Tree

```
App
├── AuthProvider (context)
├── Router
│   ├── PublicRoute (redirects to / if authenticated)
│   │   ├── Login
│   │   └── Register
│   └── ProtectedRoute (redirects to /login if unauthenticated)
│       └── MainLayout (sidebar + header + content area)
│           ├── Dashboard
│           ├── ResumeList / ResumeDetail / ResumeEdit
│           ├── JobList / JobDetail
│           ├── Chat
│           ├── Tracking
│           └── Profile
└── Toaster (sonner notifications)
```

## State Management (Zustand)

| Store | File | Purpose |
|-------|------|---------|
| `useResumeStore` | `store/resume.store.ts` | Resume list, current resume, versions, parse status |
| `useJobStore` | `store/job.store.ts` | Job list, current job, match results |
| `useProfileStore` | `store/profile.store.ts` | User profile data |
| `useLanguageStore` | `store/language.store.ts` | Current language selection |

## Service Layer

Services are plain TypeScript modules (not classes) that wrap Axios calls:

| Service | File | Endpoints |
|---------|------|-----------|
| `authService` | `services/api.ts` | login, register, logout, refresh, CAPTCHA, Google OAuth |
| `resumeService` | `services/resumeService.ts` | CRUD resumes, upload, trigger parse, get versions |
| `jobService` | `services/jobService.ts` | CRUD jobs, get matches |
| `chatService` | `services/chatService.ts` | Send message, get history |
| `trackingService` | `services/trackingService.ts` | CRUD applications, status updates |
| `profileService` | `services/profileService.ts` | Get/update profile |

## API Client Features

- **Base URL**: `VITE_API_BASE_URL` or `/api` (relative, proxied through Nginx)
- **Auto JWT attach**: Interceptor adds `Bearer` token to every request
- **Auto refresh**: On 401, automatically refreshes token and retries (queues concurrent requests)
- **Retry**: GET requests retry up to 2 times on network errors with exponential backoff
- **Language header**: `Accept-Language` sent with every request (i18n support)
- **Abortable**: `createAbortableRequest()` utility for canceling in-flight requests

## Key Custom Hooks

| Hook | File | Purpose |
|------|------|---------|
| `useAuth` | `hooks/useAuth.tsx` | Auth context provider, exposes user state, login/logout |
| `useAbortableRequest` | `hooks/useAbortableRequest.ts` | Wraps API calls with auto-cancel on unmount or new request |
| `useMobile` | `hooks/use-mobile.ts` | Responsive breakpoint detection |
| `useTimeZone` | `hooks/useTimeZone.ts` | User timezone detection |

## i18n Setup

- **Library**: i18next + react-i18next
- **Languages**: `en`, `zh-Hans`, `zh-Hant`
- **Namespace files**: `locales/{lang}/` (JSON)
- **Detection**: `i18next-browser-languagedetector`
- **Init**: `i18n/index.ts`

## UI Components

- **Framework**: shadcn/ui (based on Radix UI primitives)
- **Styling**: Tailwind CSS 3 + `tailwindcss-animate`
- **Icons**: lucide-react
- **Charts**: recharts
- **Markdown**: @uiw/react-md-editor
