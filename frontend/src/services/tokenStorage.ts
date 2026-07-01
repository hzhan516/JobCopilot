/**
 * Token storage manager with "Remember Me" support.
 * - Remember Me: persists in localStorage across sessions
 * - Not Remember Me: uses sessionStorage, cleared on tab close
 *
 * Token 存储管理，支持"记住我"模式
 * - 记住我：使用 localStorage，跨会话持久化
 * - 不记住：使用 sessionStorage，标签页关闭即清除
 */

const ACCESS_TOKEN_KEY = 'accessToken';
const USER_KEY = 'user';
const EXPIRES_AT_KEY = 'expiresAt';
const REMEMBER_ME_KEY = 'rememberMe';

export interface StoredUser {
  userId: string;
  email: string;
  role: 'ADMIN' | 'JOB_SEEKER';
}

function getStorage(persistent: boolean): Storage {
  return persistent ? localStorage : sessionStorage;
}

function isPersistent(): boolean {
  // Read rememberMe from localStorage first to survive page refreshes
  // 优先从 localStorage 读取 rememberMe，确保页面刷新后配置不丢失
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

  setTokens(accessToken: string, expiresIn: number, rememberMe: boolean): void {
    this.setRememberMe(rememberMe);
    const storage = getStorage(rememberMe);
    const expiresAt = Date.now() + expiresIn * 1000;
    storage.setItem(ACCESS_TOKEN_KEY, accessToken);
    storage.setItem(EXPIRES_AT_KEY, String(expiresAt));
  },

  getAccessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY) || localStorage.getItem(ACCESS_TOKEN_KEY);
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
    // Consider token expired 5 minutes early to allow proactive refresh before actual expiry
    // 提前 5 分钟标记过期，为异步刷新预留缓冲时间
    return Date.now() >= expiresAt - 5 * 60 * 1000;
  },

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(EXPIRES_AT_KEY);
    localStorage.removeItem(REMEMBER_ME_KEY);
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(EXPIRES_AT_KEY);
  },
};

export default tokenStorage;
