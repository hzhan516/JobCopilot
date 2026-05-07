import { describe, it, expect } from 'vitest'
import { formatFileSize } from './file'

describe('formatFileSize', () => {
  it('returns 0 B for zero bytes', () => {
    expect(formatFileSize(0)).toBe('0 B')
  })

  it('formats bytes correctly', () => {
    expect(formatFileSize(512)).toBe('512 B')
  })

  it('formats kilobytes correctly', () => {
    expect(formatFileSize(1024)).toBe('1 KB')
    expect(formatFileSize(1536)).toBe('1.5 KB')
  })

  it('formats megabytes correctly', () => {
    expect(formatFileSize(1024 * 1024)).toBe('1 MB')
    expect(formatFileSize(2.5 * 1024 * 1024)).toBe('2.5 MB')
  })

  it('formats gigabytes correctly', () => {
    expect(formatFileSize(1024 * 1024 * 1024)).toBe('1 GB')
  })

  it('formats terabytes correctly', () => {
    expect(formatFileSize(1024 * 1024 * 1024 * 1024)).toBe('1 TB')
  })

  it('rounds to two decimal places', () => {
    expect(formatFileSize(1234567)).toBe('1.18 MB')
  })
})
