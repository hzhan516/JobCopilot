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
import ResumeList from '@/pages/resumes/ResumeList';
import ResumeDetail from '@/pages/resumes/ResumeDetail';
import ResumeEdit from '@/pages/resumes/ResumeEdit';
import JobList from '@/pages/jobs/JobList';
import JobDetail from '@/pages/jobs/JobDetail';
import Chat from '@/pages/chat/Chat';
import Tracking from '@/pages/tracking/Tracking';
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

            <Route
              path="/"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <Dashboard />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/resumes"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <ResumeList />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/resumes/:groupId"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <ResumeDetail />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/resumes/:groupId/versions/:versionId/edit"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <ResumeEdit />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/jobs"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <JobList />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/jobs/:jobId"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <JobDetail />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/chat"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <Chat />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/applications"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <Tracking />
                  </MainLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <MainLayout>
                    <Profile />
                  </MainLayout>
                </ProtectedRoute>
              }
            />

            {/* Admin routes — lazy loaded, ADMIN role only */}
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
      <Toaster position="top-center" richColors duration={6000} offset="88px" mobileOffset="72px" />
    </AuthProvider>
  );
}

export default App;
