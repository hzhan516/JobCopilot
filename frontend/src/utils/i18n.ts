import i18n from '@/i18n';

const TIMEZONE_STORAGE_KEY = 'user-timezone';

/**
 * 获取用户偏好时区，若未设置则返回浏览器自动检测的本地时区
 * Get user's preferred time zone; fallback to browser-detected local time zone
 */
export function getUserTimeZone(): string {
  try {
    const stored = localStorage.getItem(TIMEZONE_STORAGE_KEY);
    if (stored) return stored;
  } catch {
    // localStorage 不可用时不阻塞 / Ignore if localStorage is unavailable
  }
  return Intl.DateTimeFormat().resolvedOptions().timeZone;
}

/**
 * 设置用户偏好时区
 * Set user's preferred time zone
 */
export function setUserTimeZone(timeZone: string): void {
  try {
    localStorage.setItem(TIMEZONE_STORAGE_KEY, timeZone);
  } catch {
    // ignore
  }
}

/**
 * 清除用户偏好时区，恢复为浏览器自动检测
 * Clear user's preferred time zone to revert to browser auto-detection
 */
export function clearUserTimeZone(): void {
  try {
    localStorage.removeItem(TIMEZONE_STORAGE_KEY);
  } catch {
    // ignore
  }
}

/**
 * 获取当前界面 locale
 * Get current UI locale
 */
export function getLocale(): string {
  if (i18n.language === 'zh-TW') return 'zh-TW';
  if (i18n.language === 'zh-CN') return 'zh-CN';
  return 'en-US';
}

/**
 * 格式化日期，按用户所在时区显示
 * Format date according to user's time zone
 */
export function formatDate(
  date: Date | string | number,
  options?: Intl.DateTimeFormatOptions
): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return d.toLocaleDateString(getLocale(), {
    timeZone: getUserTimeZone(),
    ...options,
  });
}

/**
 * 格式化时间，按用户所在时区显示
 * Format time according to user's time zone
 */
export function formatTime(
  date: Date | string | number,
  options?: Intl.DateTimeFormatOptions
): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return d.toLocaleTimeString(getLocale(), {
    timeZone: getUserTimeZone(),
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  });
}

/**
 * 格式化日期时间，按用户所在时区显示
 * Format date and time according to user's time zone
 */
export function formatDateTime(
  date: Date | string | number,
  options?: Intl.DateTimeFormatOptions
): string {
  const d = typeof date === 'string' || typeof date === 'number' ? new Date(date) : date;
  return d.toLocaleString(getLocale(), {
    timeZone: getUserTimeZone(),
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  });
}
