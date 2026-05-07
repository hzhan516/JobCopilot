import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    // Preserve the original path so user can be redirected back after login
    // 保存原始路径，登录后自动跳转回原页面
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}
