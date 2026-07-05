import { lazy } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/hooks/useAuth';
import { Toaster } from '@/components/ui/sonner';

import MainLayout from '@/components/layout/MainLayout';
import ErrorBoundary from '@/components/layout/ErrorBoundary';
import ProtectedRoute from '@/components/ProtectedRoute';
import PublicRoute from '@/components/PublicRoute';
import AdminRoute from '@/components/AdminRoute';
import AdminLayout from '@/components/admin/AdminLayout';

import Login from '@/pages/auth/Login';
import Register from '@/pages/auth/Register';
import Dashboard from '@/pages/Dashboard';
import ResumesPage from '@/pages/resumes/ResumesPage';
import ResumeEdit from '@/pages/resumes/ResumeEdit';
import JobsPage from '@/pages/jobs/JobsPage';
import Chat from '@/pages/chat/Chat';
import TrackingPage from '@/pages/tracking/TrackingPage';
import Profile from '@/pages/profile/Profile';

// Lazy-loaded admin pages
const AdminDashboard = lazy(() => import('@/pages/admin/AdminDashboard'));
const AdminUsers = lazy(() => import('@/pages/admin/AdminUsers'));
const AdminAuditLogs = lazy(() => import('@/pages/admin/AdminAuditLogs'));
const AdminMonitoring = lazy(() => import('@/pages/admin/AdminMonitoring'));
const AdminConfig = lazy(() => import('@/pages/admin/AdminConfig'));
const AdminAIService = lazy(() => import('@/pages/admin/AdminAIService'));

function App() {
  return (
    <AuthProvider>
      <Router>
        <ErrorBoundary>
          <Routes>
            {/* Public routes — 不受 MainLayout 包裹 */}
            <Route
              path="/login"
              element={
                <PublicRoute>
                  <Login />
                </PublicRoute>
              }
            />
            <Route
              path="/register"
              element={
                <PublicRoute>
                  <Register />
                </PublicRoute>
              }
            />

            {/* Protected routes — 统一嵌套在 MainLayout 下（Outlet 模式） */}
            <Route element={<ProtectedRoute><MainLayout /></ProtectedRoute>}>
              <Route index element={<Dashboard />} />
              <Route path="/resumes" element={<ResumesPage />} />
              <Route path="/resumes/:groupId" element={<ResumesPage />} />
              <Route path="/resumes/:groupId/versions/:versionId/edit" element={<ResumeEdit />} />
              <Route path="/jobs" element={<JobsPage />} />
              <Route path="/jobs/:jobId" element={<JobsPage />} />
              <Route path="/chat" element={<Chat />} />
              <Route path="/applications" element={<TrackingPage />} />
              <Route path="/applications/:trackingId" element={<TrackingPage />} />
              <Route path="/profile" element={<Profile />} />
            </Route>

            {/* Admin routes — 独立 AdminLayout，不受 MainLayout 影响 */}
            <Route path="/admin" element={<AdminRoute><AdminLayout /></AdminRoute>}>
              <Route index element={<AdminDashboard />} />
              <Route path="users" element={<AdminUsers />} />
              <Route path="audit-logs" element={<AdminAuditLogs />} />
              <Route path="monitoring" element={<AdminMonitoring />} />
              <Route path="config" element={<AdminConfig />} />
              <Route path="ai" element={<AdminAIService />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </ErrorBoundary>
      </Router>
      {/* header 高度从 4rem→3.5rem，offset 相应从 88px→72px */}
      <Toaster position="top-center" richColors duration={6000} offset="72px" mobileOffset="72px" />
    </AuthProvider>
  );
}

export default App;
