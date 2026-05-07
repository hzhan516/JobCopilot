import { afterEach, vi } from 'vitest';

class MemoryStorage implements Storage {
  private store = new Map<string, string>();

  get length(): number {
    return this.store.size;
  }

  clear(): void {
    this.store.clear();
  }

  getItem(key: string): string | null {
    return this.store.get(key) ?? null;
  }

  key(index: number): string | null {
    return Array.from(this.store.keys())[index] ?? null;
  }

  removeItem(key: string): void {
    this.store.delete(key);
  }

  setItem(key: string, value: string): void {
    this.store.set(key, String(value));
  }
}

function ensureStorage(name: 'localStorage' | 'sessionStorage') {
  const current = globalThis[name] as Storage | undefined;
  if (current && typeof current.getItem === 'function' && typeof current.clear === 'function') {
    return;
  }

  Object.defineProperty(globalThis, name, {
    value: new MemoryStorage(),
    configurable: true,
  });
}

ensureStorage('localStorage');
ensureStorage('sessionStorage');

afterEach(() => {
  localStorage.clear();
  sessionStorage.clear();
  vi.clearAllMocks();
  vi.useRealTimers();
});
