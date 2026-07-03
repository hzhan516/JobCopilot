import { describe, it, expect } from 'vitest';
import { formatTokens } from './formatTokens';

describe('formatTokens', () => {
  it('returns raw number for values under 1k', () => {
    expect(formatTokens(0)).toBe('0');
    expect(formatTokens(500)).toBe('500');
    expect(formatTokens(999)).toBe('999');
  });

  it('formats thousands as k', () => {
    expect(formatTokens(1000)).toBe('1.0k');
    expect(formatTokens(1500)).toBe('1.5k');
    expect(formatTokens(199300)).toBe('199k');
    expect(formatTokens(999999)).toBe('1000k');
  });

  it('formats millions as M', () => {
    expect(formatTokens(1_000_000)).toBe('1.0M');
    expect(formatTokens(1_200_000)).toBe('1.2M');
    expect(formatTokens(10_000_000)).toBe('10.0M');
  });

  it('rounds large values', () => {
    expect(formatTokens(100_000)).toBe('100k');
    expect(formatTokens(100_000_000)).toBe('100M');
  });
});
