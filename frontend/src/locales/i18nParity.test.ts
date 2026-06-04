import { describe, it, expect } from 'vitest'
import en from './en.json'
import zhCN from './zh-CN.json'
import zhTW from './zh-TW.json'

type LocaleValue = string | number | boolean | null
type LocaleRecord = Record<string, LocaleValue>

const VARIABLE_REGEX = /{{([^}]+)}}/g

const flattenKeys = (input: Record<string, unknown>, prefix = ''): LocaleRecord => {
  return Object.entries(input).reduce<LocaleRecord>((acc, [key, value]) => {
    const nextKey = prefix ? `${prefix}.${key}` : key

    if (value !== null && typeof value === 'object' && !Array.isArray(value)) {
      Object.assign(acc, flattenKeys(value as Record<string, unknown>, nextKey))
      return acc
    }

    acc[nextKey] = value as LocaleValue
    return acc
  }, {})
}

const extractVariables = (value: LocaleValue): string[] => {
  const source = String(value)
  const variables = new Set<string>()
  let match: RegExpExecArray | null = VARIABLE_REGEX.exec(source)

  while (match) {
    variables.add(match[1].trim())
    match = VARIABLE_REGEX.exec(source)
  }

  return Array.from(variables).sort()
}

describe('i18n parity', () => {
  it('matches all locale keys and interpolation variables', () => {
    const flattenedEn = flattenKeys(en)
    const flattenedZhCN = flattenKeys(zhCN)
    const flattenedZhTW = flattenKeys(zhTW)

    const enKeys = Object.keys(flattenedEn).sort()
    const zhCNKeys = Object.keys(flattenedZhCN).sort()
    const zhTWKeys = Object.keys(flattenedZhTW).sort()

    expect(enKeys).toEqual(zhCNKeys)
    expect(enKeys).toEqual(zhTWKeys)

    enKeys.forEach((key) => {
      const enVars = extractVariables(flattenedEn[key])
      const zhCNVars = extractVariables(flattenedZhCN[key])
      const zhTWVars = extractVariables(flattenedZhTW[key])

      expect(enVars).toEqual(zhCNVars)
      expect(enVars).toEqual(zhTWVars)
    })
  })

  it('keeps cross-locale brand and terminology aligned', () => {
    expect(en.common.appName).toBe('JobCopilot')
    expect(zhCN.common.appName).toBe('JobCopilot')
    expect(zhTW.common.appName).toBe('JobCopilot')

    expect(extractVariables(en.common.copyright)).toEqual(['appName'])
    expect(extractVariables(zhCN.common.copyright)).toEqual(['appName'])
    expect(extractVariables(zhTW.common.copyright)).toEqual(['appName'])

    expect(JSON.stringify(zhTW)).not.toContain('簡歷')
    expect(JSON.stringify(zhCN)).not.toContain('履歷')
  })
})
