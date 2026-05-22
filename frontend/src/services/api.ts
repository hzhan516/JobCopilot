import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, SendVerificationCodeRequest, LoginByGoogleRequest, CaptchaChallengeResponse, CaptchaVerifyRequest } from '@/types';
import tokenStorage from './tokenStorage';
import i18n from '@/i18n';

// Maximum retry attempts for transient network failures
// 网络抖动时的最大重试次数
const MAX_RETRIES = 2;

const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token to every outgoing request to ensure authenticated API access
// 为每个请求附加 JWT，确保后端接口的认证访问
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // 传递用户当前选择的语言，使后端 i18n 生效
    config.headers['Accept-Language'] = i18n.language || 'en';
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let refreshSubscribers: Array<(token: string) => void> = [];

function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

function addRefreshSubscriber(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function clearAuthAndRedirect() {
  tokenStorage.clear();
  window.location.href = '/login';
}

async function doRefreshToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) return null;

  try {
    // Use a standalone axios instance to avoid interceptor recursion
    // 使用独立 axios 实例，防止拦截器循环触发
    const response = await axios.post<ApiResponse<AuthResponse>>(
      `${import.meta.env.VITE_API_BASE_URL || '/api'}/v1/auth/refresh`,
      { refreshToken }
    );
    if (response.data.code === 200) {
      const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data.data;
      const rememberMe = tokenStorage.getRememberMe();
      tokenStorage.setTokens(accessToken, newRefreshToken, expiresIn, rememberMe);
      return accessToken;
    }
  } catch {
    // Silently fail; let the caller decide whether to redirect
    // 刷新失败静默处理，由调用方决定跳转策略
  }
  return null;
}

/**
 * Retryable network errors are those without a response (network layer failure),
 * excluding intentional timeouts to avoid masking slow server issues.
 *
 * 可重试错误仅限无响应的网络层失败，排除主动超时以避免掩盖服务端性能问题
 */
function isRetryableNetworkError(error: AxiosError<unknown>): boolean {
  return (
    !error.response &&
    error.code !== 'ECONNABORTED'
  );
}

// Response interceptor: handles token refresh, request retry, and unified error propagation
// 响应拦截器：统一处理 Token 续期、请求重试与错误传播
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean; _retryCount?: number };

    // ===== Retry GET requests on transient network failures using exponential backoff =====
    // ===== 对 GET 请求使用指数退避重试，提高弱网环境下的请求成功率 =====
    if (
      originalRequest &&
      originalRequest.method?.toLowerCase() === 'get' &&
      isRetryableNetworkError(error)
    ) {
      const retryCount = originalRequest._retryCount ?? 0;
      if (retryCount < MAX_RETRIES) {
        originalRequest._retryCount = retryCount + 1;
        // Exponential backoff: 1s, 2s
        // 指数退避：1s, 2s
        const delayMs = 1000 * Math.pow(2, retryCount);
        await new Promise((resolve) => setTimeout(resolve, delayMs));
        return apiClient(originalRequest);
      }
    }

    // ===== Automatic token refresh on 401 =====
    // ===== Token 自动续期 =====
    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      // Login/register 401 should be handled by callers to show field-level errors
      // 登录/注册失败的 401 由调用方处理，以展示表单级错误提示
      if (
        originalRequest.url?.includes('/v1/auth/login/email') ||
        originalRequest.url?.includes('/v1/auth/register/email')
      ) {
        return Promise.reject(error);
      }

      // Prevent infinite loop when the refresh endpoint itself returns 401
      // 避免刷新接口返回 401 时进入死循环
      if (originalRequest.url?.includes('/v1/auth/refresh')) {
        clearAuthAndRedirect();
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      if (isRefreshing) {
        // Queue subsequent requests until refresh completes
        // 刷新期间将后续请求入队，避免并发触发多次刷新
        return new Promise((resolve) => {
          addRefreshSubscriber((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(apiClient(originalRequest));
          });
        });
      }

      isRefreshing = true;
      const newToken = await doRefreshToken();
      isRefreshing = false;

      if (newToken) {
        onRefreshed(newToken);
        originalRequest.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(originalRequest);
      }

      clearAuthAndRedirect();
    }

    return Promise.reject(error);
  }
);

/**
 * Creates an abortable request wrapper that superseds previous pending requests.
 * Useful when rapid user interactions (e.g., typing, switching tabs) may trigger
 * overlapping API calls.
 *
 * 创建可取消请求包装器，新请求自动取消前一个_pending_请求。
 * 适用于用户高频操作（如输入、切换标签）可能产生重叠请求的场景。
 *
 * @example
 * const { execute, abort } = createAbortableRequest();
 *
 * execute(async (signal) => {
 *   const jobs = await jobService.getJobs(signal);
 *   return jobs;
 * });
 *
 * abort();
 */
export function createAbortableRequest() {
  let controller: AbortController | null = null;

  const execute = <T>(fn: (signal: AbortSignal) => Promise<T>): Promise<T> => {
    controller?.abort();
    controller = new AbortController();

    return fn(controller.signal).finally(() => {
      if (controller?.signal.aborted === false) {
        controller = null;
      }
    });
  };

  const abort = (reason?: string): void => {
    controller?.abort(reason);
    controller = null;
  };

  return { execute, abort };
}

export const authService = {
  getCaptchaChallenge: async (): Promise<CaptchaChallengeResponse> => {
    const response = await apiClient.get<ApiResponse<CaptchaChallengeResponse>>('/v1/auth/captcha');
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  verifyCaptcha: async (data: CaptchaVerifyRequest): Promise<{ captchaToken: string }> => {
    const response = await apiClient.post<ApiResponse<{ captchaToken: string }>>('/v1/auth/captcha/verify', data);
    if (response.data.code === 200) {
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  register: async (data: RegisterRequest, rememberMe = false): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/register/email', data);
    if (response.data.code === 200) {
      const { accessToken, refreshToken, expiresIn, ...user } = response.data.data;
      tokenStorage.setTokens(accessToken, refreshToken, expiresIn, rememberMe);
      tokenStorage.setUser({ userId: user.userId, email: user.email }, rememberMe);
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  sendVerificationCode: async (data: SendVerificationCodeRequest): Promise<void> => {
    const response = await apiClient.post<ApiResponse<null>>('/v1/auth/send-verification-code', data);
    if (response.data.code !== 200) {
      throw new Error(response.data.message);
    }
  },

  isEmailVerificationEnabled: async (): Promise<boolean> => {
    const response = await apiClient.get<ApiResponse<boolean>>('/v1/auth/email-verification-enabled');
    return response.data.code === 200 ? response.data.data : false;
  },

  login: async (data: LoginRequest, rememberMe = false): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/login/email', data);
    if (response.data.code === 200) {
      const { accessToken, refreshToken, expiresIn, ...user } = response.data.data;
      tokenStorage.setTokens(accessToken, refreshToken, expiresIn, rememberMe);
      tokenStorage.setUser({ userId: user.userId, email: user.email }, rememberMe);
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  loginByGoogle: async (data: LoginByGoogleRequest, rememberMe = false): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/login/google', data);
    if (response.data.code === 200) {
      const { accessToken, refreshToken, expiresIn, ...user } = response.data.data;
      tokenStorage.setTokens(accessToken, refreshToken, expiresIn, rememberMe);
      tokenStorage.setUser({ userId: user.userId, email: user.email }, rememberMe);
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  logout: () => {
    tokenStorage.clear();
  },

  getCurrentUser: (): { userId: string; email: string } | null => {
    return tokenStorage.getUser();
  },

  isAuthenticated: (): boolean => {
    return !!tokenStorage.getAccessToken();
  },

  isTokenExpired: (): boolean => {
    return tokenStorage.isTokenExpired();
  },
};

export default apiClient;
