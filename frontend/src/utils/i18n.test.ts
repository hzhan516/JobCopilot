import { describe, it, expect, beforeEach } from 'vitest'
import {
  getUserTimeZone,
  setUserTimeZone,
  clearUserTimeZone,
  getLocale,
  formatDate,
  formatTime,
  formatDateTime,
} from './i18n'

const TIMEZONE_STORAGE_KEY = 'user-timezone'

describe('getUserTimeZone', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns stored timezone when available', () => {
    localStorage.setItem(TIMEZONE_STORAGE_KEY, 'America/New_York')
    expect(getUserTimeZone()).toBe('America/New_York')
  })

  it('falls back to browser timezone when no stored value', () => {
    const browserZone = Intl.DateTimeFormat().resolvedOptions().timeZone
    expect(getUserTimeZone()).toBe(browserZone)
  })
})

describe('setUserTimeZone / clearUserTimeZone', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('stores timezone in localStorage', () => {
    setUserTimeZone('Asia/Shanghai')
    expect(localStorage.getItem(TIMEZONE_STORAGE_KEY)).toBe('Asia/Shanghai')
    expect(getUserTimeZone()).toBe('Asia/Shanghai')
  })

  it('clears stored timezone', () => {
    setUserTimeZone('Europe/London')
    clearUserTimeZone()
    expect(localStorage.getItem(TIMEZONE_STORAGE_KEY)).toBeNull()
  })
})

describe('getLocale', () => {
  it('returns en-US by default when i18n.language is not zh variants', () => {
    expect(getLocale()).toBe('en-US')
  })
})

describe('formatDate', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('formats a date string', () => {
    const date = new Date('2024-06-15T12:00:00Z')
    const result = formatDate(date)
    expect(typeof result).toBe('string')
    expect(result.length).toBeGreaterThan(0)
  })

  it('formats a timestamp number', () => {
    const ts = new Date('2024-01-01').getTime()
    const result = formatDate(ts)
    expect(typeof result).toBe('string')
  })

  it('accepts custom options', () => {
    const date = new Date('2024-06-15T12:00:00Z')
    const result = formatDate(date, { weekday: 'long' })
    expect(typeof result).toBe('string')
  })
})

describe('formatTime', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('formats a time string', () => {
    const date = new Date('2024-06-15T14:30:00Z')
    const result = formatTime(date)
    expect(typeof result).toBe('string')
    expect(result).toContain(':')
  })
})

describe('formatDateTime', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('formats date and time together', () => {
    const date = new Date('2024-06-15T14:30:00Z')
    const result = formatDateTime(date)
    expect(typeof result).toBe('string')
    expect(result.length).toBeGreaterThan(0)
  })
})
