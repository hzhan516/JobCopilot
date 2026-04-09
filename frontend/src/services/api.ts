import axios, { type AxiosInstance, type AxiosError } from 'axios';
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest } from '@/types';

// 创建 axios 实例
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 添加认证token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器 - 统一错误处理
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401) {
      // Token过期，清除本地存储并跳转到登录页
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 认证服务
export const authService = {
  // 邮箱注册
  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/register/email', data);
    if (response.data.code === 200) {
      const { accessToken, refreshToken, ...user } = response.data.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 邮箱登录
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/v1/auth/login/email', data);
    if (response.data.code === 200) {
      const { accessToken, refreshToken, ...user } = response.data.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('user', JSON.stringify(user));
      return response.data.data;
    }
    throw new Error(response.data.message);
  },

  // 登出
  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  },

  // 获取当前用户
  getCurrentUser: (): { userId: string; email: string } | null => {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      return JSON.parse(userStr);
    }
    return null;
  },

  // 检查是否已登录
  isAuthenticated: (): boolean => {
    return !!localStorage.getItem('accessToken');
  },
};

export default apiClient;
