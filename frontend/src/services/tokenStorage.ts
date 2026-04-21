/**
 * Token 存储管理
 * 支持 "记住我" 模式：
 * - 记住我：使用 localStorage（持久化）
 * - 不记住：使用 sessionStorage（会话级）
 */

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const USER_KEY = 'user';
const EXPIRES_AT_KEY = 'expiresAt';
const REMEMBER_ME_KEY = 'rememberMe';

export interface StoredUser {
  userId: string;
  email: string;
}

function getStorage(persistent: boolean): Storage {
  return persistent ? localStorage : sessionStorage;
}

function isPersistent(): boolean {
  // 优先读取 localStorage 中的 rememberMe 标记
  const rememberMe = localStorage.getItem(REMEMBER_ME_KEY);
  return rememberMe !== 'false';
}

export const tokenStorage = {
  setRememberMe(rememberMe: boolean): void {
    localStorage.setItem(REMEMBER_ME_KEY, String(rememberMe));
  },

  getRememberMe(): boolean {
    return localStorage.getItem(REMEMBER_ME_KEY) !== 'false';
  },

  setTokens(accessToken: string, refreshToken: string, expiresIn: number, rememberMe: boolean): void {
    this.setRememberMe(rememberMe);
    const storage = getStorage(rememberMe);
    const expiresAt = Date.now() + expiresIn * 1000;
    storage.setItem(ACCESS_TOKEN_KEY, accessToken);
    storage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    storage.setItem(EXPIRES_AT_KEY, String(expiresAt));
  },

  getAccessToken(): string | null {
    // sessionStorage 优先级高于 localStorage（如果用户选择了不记住我，在同一标签页里应读取 sessionStorage）
    return sessionStorage.getItem(ACCESS_TOKEN_KEY) || localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY) || localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  getExpiresAt(): number | null {
    const value = sessionStorage.getItem(EXPIRES_AT_KEY) || localStorage.getItem(EXPIRES_AT_KEY);
    return value ? Number(value) : null;
  },

  setUser(user: StoredUser, rememberMe?: boolean): void {
    const storage = getStorage(rememberMe ?? isPersistent());
    storage.setItem(USER_KEY, JSON.stringify(user));
  },

  getUser(): StoredUser | null {
    const userStr = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY);
    if (userStr) {
      try {
        return JSON.parse(userStr) as StoredUser;
      } catch {
        return null;
      }
    }
    return null;
  },

  isTokenExpired(): boolean {
    const expiresAt = this.getExpiresAt();
    if (!expiresAt) return true;
    // 提前 5 分钟认为过期，预留刷新时间
    return Date.now() >= expiresAt - 5 * 60 * 1000;
  },

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(EXPIRES_AT_KEY);
    localStorage.removeItem(REMEMBER_ME_KEY);
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(EXPIRES_AT_KEY);
  },
};

export default tokenStorage;
