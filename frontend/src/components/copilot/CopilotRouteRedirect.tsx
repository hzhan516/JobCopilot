import { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { useCopilotStore } from '@/store/copilot.store';

/** 兼容旧的 /chat 深链：打开当前断点对应的 Copilot 载体并回到主工作区。 */
export default function CopilotRouteRedirect() {
  const open = useCopilotStore((state) => state.open);

  useEffect(() => {
    open({ type: 'general' });
  }, [open]);

  return <Navigate to="/" replace />;
}
