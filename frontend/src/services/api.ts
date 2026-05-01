import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, LoginByGoogleRequest } from '@/types';
import tokenStorage from './tokenStorage';

// 创建 axios 实例
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 添加认证 token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = tokenStorage.getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 刷新 token 相关状态
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

// 尝试刷新 token
async function doRefreshToken(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) return null;

  try {
    // 使用独立 axios 实例避免拦截器循环
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
    // 刷新失败，静默处理，由调用方决定如何跳转
  }
  return null;
}

// 响应拦截器 - 统一错误处理与 Token 自动续期
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      // 登录/注册失败时的 401 由调用方自行处理，不执行全局跳转
      if (
        originalRequest.url?.includes('/v1/auth/login/email') ||
        originalRequest.url?.includes('/v1/auth/register/email')
      ) {
        return Promise.reject(error);
      }

      // 避免在刷新接口本身出错时进入死循环
      if (originalRequest.url?.includes('/v1/auth/refresh')) {
        clearAuthAndRedirect();
        return Promise.reject(error);
      }

      originalRequest._retry = true;

      if (isRefreshing) {
        // 等待刷新完成后重试
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

      // 刷新失败，强制登出
      clearAuthAndRedirect();
    }

    return Promise.reject(error);
  }
);

// 认证服务
export const authService = {
  // 邮箱注册
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

  // 邮箱登录
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

  // Google 登录
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

  // 登出
  logout: () => {
    tokenStorage.clear();
  },

  // 获取当前用户
  getCurrentUser: (): { userId: string; email: string } | null => {
    return tokenStorage.getUser();
  },

  // 检查是否已登录
  isAuthenticated: (): boolean => {
    return !!tokenStorage.getAccessToken();
  },

  // Token 是否即将过期
  isTokenExpired: (): boolean => {
    return tokenStorage.isTokenExpired();
  },
};

export default apiClient;
