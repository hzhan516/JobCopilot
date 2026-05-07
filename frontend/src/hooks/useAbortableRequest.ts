import { useCallback, useEffect, useRef } from 'react';

/**
 * Manages AbortController lifecycle to prevent state updates on unmounted components
 * and to supersede stale requests during rapid user interactions.
 *
 * 封装 AbortController 生命周期，防止组件卸载后回调导致内存泄漏，
 * 并在用户高频操作时自动取消前一请求。
 *
 * @example
 * function JobList() {
 *   const { execute } = useAbortableRequest();
 *
 *   useEffect(() => {
 *     execute(async (signal) => {
 *       const jobs = await jobService.getJobs(signal);
 *       setJobs(jobs);
 *     });
 *   }, [execute]);
 *
 *   return <div>...</div>;
 * }
 */
export function useAbortableRequest() {
  const abortControllerRef = useRef<AbortController | null>(null);

  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort('Component unmounted');
      abortControllerRef.current = null;
    };
  }, []);

  const execute = useCallback(
    <T>(fn: (signal: AbortSignal) => Promise<T>): Promise<T> => {
      // Supersede previous request to avoid race conditions in rapid interactions
      // 取消前一请求，避免高频操作下的竞态条件
      abortControllerRef.current?.abort('Superseded by new request');

      const controller = new AbortController();
      abortControllerRef.current = controller;

      return fn(controller.signal).finally(() => {
        // Only clear if this controller wasn't already replaced by a newer request
        // 仅清理未被新请求覆盖的 controller，防止误删当前活跃实例
        if (abortControllerRef.current === controller) {
          abortControllerRef.current = null;
        }
      });
    },
    []
  );

  const abort = useCallback((reason?: string): void => {
    abortControllerRef.current?.abort(reason);
    abortControllerRef.current = null;
  }, []);

  const getSignal = useCallback((): AbortSignal | undefined => {
    return abortControllerRef.current?.signal;
  }, []);

  return { execute, abort, getSignal } as const;
}

export default useAbortableRequest;
