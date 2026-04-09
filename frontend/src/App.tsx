import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/hooks/useAuth';
import { Toaster } from '@/components/ui/sonner';

// 布局
import MainLayout from '@/components/layout/MainLayout';
import ProtectedRoute from '@/components/ProtectedRoute';

// 页面
import Login from '@/pages/auth/Login';
import Register from '@/pages/auth/Register';
import Dashboard from '@/pages/Dashboard';
import ResumeList from '@/pages/resumes/ResumeList';
import ResumeUpload from '@/pages/resumes/ResumeUpload';
import ResumeEdit from '@/pages/resumes/ResumeEdit';
import JobList from '@/pages/jobs/JobList';
import Chat from '@/pages/chat/Chat';
import Tracking from '@/pages/tracking/Tracking';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* 公开路由 */}
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          {/* 受保护路由 */}
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
            path="/resumes/upload"
            element={
              <ProtectedRoute>
                <MainLayout>
                  <ResumeUpload />
                </MainLayout>
              </ProtectedRoute>
            }
          />
          <Route
            path="/resumes/:groupId/edit"
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
            path="/tracking"
            element={
              <ProtectedRoute>
                <MainLayout>
                  <Tracking />
                </MainLayout>
              </ProtectedRoute>
            }
          />

          {/* 404 重定向 */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
      <Toaster position="top-center" richColors />
    </AuthProvider>
  );
}

export default App;
