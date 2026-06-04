import React, { type ReactNode, type ErrorInfo } from 'react';
import { Button } from '@/components/ui/button';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';
import i18n from '@/i18n';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onReset?: () => void;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

/**
 * Global error boundary compatible with React 19 and TypeScript.
 * Placed at the route root to prevent a single subtree crash from bringing down the entire app.
 *
 * React 19 + TypeScript 兼容的全局错误边界。
 * 置于路由根层级，防止单个子树渲染错误导致整个应用崩溃。
 */
class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  override componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    // Hook for external error tracking (Sentry / LogRocket) in production
    // 生产环境可接入 Sentry / LogRocket 等监控服务
    console.error('[ErrorBoundary] Caught an error:', error, errorInfo);
  }

  private handleReset = (): void => {
    this.props.onReset?.();
    this.setState({ hasError: false, error: null });
  };

  private handleGoHome = (): void => {
    window.location.href = '/';
  };

  override render(): ReactNode {
    const { hasError, error } = this.state;
    const { children, fallback } = this.props;

    if (!hasError) {
      return children;
    }

    if (fallback) {
      return fallback;
    }

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="max-w-md w-full bg-white rounded-xl shadow-lg border border-gray-100 p-8 text-center space-y-6">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto">
            <AlertTriangle className="w-8 h-8 text-red-500" />
          </div>

          <div className="space-y-2">
            <h2 className="text-xl font-semibold text-gray-900">
              {i18n.t('errorBoundary.title')}
            </h2>
            <p className="text-sm text-gray-500">
              {i18n.t('errorBoundary.message')}
            </p>
          </div>

          {error && (
            <div className="rounded-lg bg-gray-50 border border-gray-100 p-4 text-left overflow-auto">
              <p className="text-xs font-mono text-red-600 break-all">
                {error.message}
              </p>
            </div>
          )}

          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Button
              variant="default"
              onClick={this.handleReset}
              className="gap-2"
            >
              <RefreshCw className="w-4 h-4" />
              {i18n.t('errorBoundary.tryAgain')}
            </Button>
            <Button
              variant="outline"
              onClick={this.handleGoHome}
              className="gap-2"
            >
              <Home className="w-4 h-4" />
              {i18n.t('errorBoundary.backHome')}
            </Button>
          </div>
        </div>
      </div>
    );
  }
}

export default ErrorBoundary;
