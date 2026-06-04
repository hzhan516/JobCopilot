import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/hooks/useAuth';
import { getPostAuthRedirectPath } from '@/utils/authRedirect';

interface PublicRouteProps {
  children: React.ReactNode;
}

/**
 * Redirects authenticated users away from login/register pages.
 * Uses the stored 'from' location if available, defaulting to home to avoid loop.
 *
 * 已登录用户访问登录/注册页时自动跳转。
 * 优先使用登录前保存的原始路径，避免循环跳转默认回 home
 */
export default function PublicRoute({ children }: PublicRouteProps) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (isAuthenticated) {
    return <Navigate to={getPostAuthRedirectPath(location.state)} replace />;
  }

  return <>{children}</>;
}
