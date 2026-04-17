import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';

interface PublicRouteProps {
  children: React.ReactNode;
}

/**
 * 公开路由守卫
 * 已登录用户访问登录/注册页时，自动跳转到简历列表
 */
export default function PublicRoute({ children }: PublicRouteProps) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (isAuthenticated) {
    const from = (location.state as { from?: { pathname?: string } })?.from?.pathname;
    // 避免从受保护页面带过来的 state 造成循环，默认跳转到 /resumes
    return <Navigate to={from && from !== '/login' && from !== '/register' ? from : '/resumes'} replace />;
  }

  return <>{children}</>;
}
