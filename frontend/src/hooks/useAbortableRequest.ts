import { useCallback, useEffect, useRef } from 'react';

/**
 * useAbortableRequest Hook
 *
 * 封装 AbortController 生命周期，防止 React 组件卸载后请求回调引发内存泄漏。
 * Wraps AbortController lifecycle to prevent memory leaks when React components unmount.
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

  /**
   * 组件卸载时自动取消正在进行的请求
   * Auto-abort pending request on component unmount
   */
  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort('Component unmounted');
      abortControllerRef.current = null;
    };
  }, []);

  /**
   * 执行一个可取消的请求
   * Execute an abortable request
   *
   * @param fn - 接收 AbortSignal 的异步工厂函数
   * @returns 原始 Promise 结果
   */
  const execute = useCallback(
    <T>(fn: (signal: AbortSignal) => Promise<T>): Promise<T> => {
      // 取消之前的请求，确保同一时刻只有一个活跃请求
      abortControllerRef.current?.abort('Superseded by new request');

      const controller = new AbortController();
      abortControllerRef.current = controller;

      return fn(controller.signal).finally(() => {
        // 仅清理未被再次覆盖的 controller
        if (abortControllerRef.current === controller) {
          abortControllerRef.current = null;
        }
      });
    },
    []
  );

  /**
   * 手动取消当前请求
   * Manually abort the current request
   */
  const abort = useCallback((reason?: string): void => {
    abortControllerRef.current?.abort(reason);
    abortControllerRef.current = null;
  }, []);

  /**
   * 获取当前 AbortSignal（用于需要直接读取 signal 的场景）
   * Get the current AbortSignal
   */
  const getSignal = useCallback((): AbortSignal | undefined => {
    return abortControllerRef.current?.signal;
  }, []);

  return { execute, abort, getSignal } as const;
}

export default useAbortableRequest;
