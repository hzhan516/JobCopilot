import { describe, it, expect } from 'vitest'
import { cn } from './utils'

describe('cn', () => {
  it('merges class names correctly', () => {
    expect(cn('foo', 'bar')).toBe('foo bar')
  })

  it('handles conditional classes', () => {
    const isActive = true
    expect(cn('base', isActive && 'active')).toBe('base active')
    expect(cn('base', !isActive && 'active')).toBe('base')
  })

  it('merges Tailwind conflicting classes using tailwind-merge', () => {
    expect(cn('px-2', 'px-4')).toBe('px-4')
    expect(cn('text-red-500', 'text-blue-500')).toBe('text-blue-500')
  })

  it('ignores falsy values', () => {
    expect(cn('foo', undefined, null, false, '', 'bar')).toBe('foo bar')
  })

  it('handles object syntax from clsx', () => {
    expect(cn({ foo: true, bar: false, baz: true })).toBe('foo baz')
  })
})
