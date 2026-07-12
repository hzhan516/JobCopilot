import { defineConfig } from '@playwright/test'

const baseURL = process.env.SMOKE_BASE_URL || 'http://localhost'

export default defineConfig({
  testDir: './acceptance',
  outputDir: './test-results',
  timeout: 15 * 60 * 1000,
  expect: { timeout: 15_000 },
  workers: 1,
  fullyParallel: false,
  retries: 0,
  reporter: [['list'], ['json', { outputFile: 'test-results/report.json' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
})
