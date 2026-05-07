import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { authService } from '@/services/api';
import type { LoginRequest, RegisterRequest } from '@/types';

interface AuthContextType {
  user: { userId: string; email: string } | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (data: LoginRequest, rememberMe?: boolean) => Promise<void>;
  loginByGoogle: (idToken: string, rememberMe?: boolean) => Promise<void>;
  register: (data: RegisterRequest, rememberMe?: boolean) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Rehydrate user from storage on mount to preserve auth state across page refreshes
  // 从存储中恢复用户状态，确保页面刷新后认证信息不丢失
  const [user, setUser] = useState<{ userId: string; email: string } | null>(() => {
    return authService.getCurrentUser();
  });
  const [isLoading, setIsLoading] = useState(false);

  const login = useCallback(async (data: LoginRequest, rememberMe = false) => {
    setIsLoading(true);
    try {
      const response = await authService.login(data, rememberMe);
      setUser({ userId: response.userId, email: response.email });
    } finally {
      setIsLoading(false);
    }
  }, []);

  const loginByGoogle = useCallback(async (idToken: string, rememberMe = false) => {
    setIsLoading(true);
    try {
      const response = await authService.loginByGoogle({ idToken }, rememberMe);
      setUser({ userId: response.userId, email: response.email });
    } finally {
      setIsLoading(false);
    }
  }, []);

  const register = useCallback(async (data: RegisterRequest, rememberMe = false) => {
    setIsLoading(true);
    try {
      const response = await authService.register(data, rememberMe);
      setUser({ userId: response.userId, email: response.email });
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        loginByGoogle,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
