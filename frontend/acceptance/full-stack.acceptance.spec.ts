import { expect, test, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test'
import { mkdirSync, writeFileSync } from 'node:fs'

type JsonObject = Record<string, unknown>

const REQUIRED_ENV = [
  'GEMINI_API_KEY',
  'GOOGLE_CLIENT_ID',
  'SMOKE_GOOGLE_ID_TOKEN',
  'SMOKE_JOB_URL',
  'ADMIN_EMAIL',
  'ADMIN_PASSWORD',
] as const

const missingEnv = REQUIRED_ENV.filter((name) => !process.env[name]?.trim())

function bearer(token: string) {
  return { Authorization: `Bearer ${token}` }
}

async function apiData<T = JsonObject>(response: Awaited<ReturnType<APIRequestContext['get']>>): Promise<T> {
  expect(response.ok(), `HTTP ${response.status()} ${response.url()}`).toBeTruthy()
  const body = await response.json() as JsonObject
  expect(body.code, `API request failed with code ${body.code}`).toBe(200)
  return body.data as T
}

async function solveCaptcha(request: APIRequestContext): Promise<string> {
  const challenge = await apiData(await request.get('/api/v1/auth/captcha'))
  const result = await apiData(await request.post('/api/v1/auth/captcha/verify', {
    data: { captchaId: challenge.captchaId, offsetX: challenge.targetX },
  }))
  return result.captchaToken
}

async function poll<T>(description: string, read: () => Promise<T>, done: (value: T) => boolean): Promise<T> {
  const deadline = Date.now() + 180_000
  let lastValue!: T
  while (Date.now() < deadline) {
    lastValue = await read()
    const status = (lastValue as JsonObject)?.status ?? (lastValue as JsonObject)?.parseStatus
    if (status === 'FAILED') throw new Error(`${description} entered FAILED state`)
    if (done(lastValue)) return lastValue
    await new Promise((resolve) => setTimeout(resolve, 2_000))
  }
  throw new Error(`${description} did not complete within 180 seconds`)
}

function syntheticResumePdf(): Buffer {
  const text = 'JobCopilot Acceptance Candidate - Software Engineer - Python Java TypeScript SQL'
  const stream = `BT /F1 12 Tf 72 720 Td (${text}) Tj ET`
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>',
    `<< /Length ${Buffer.byteLength(stream)} >>\nstream\n${stream}\nendstream`,
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
  ]
  let pdf = '%PDF-1.4\n'
  const offsets = [0]
  objects.forEach((object, index) => {
    offsets.push(Buffer.byteLength(pdf))
    pdf += `${index + 1} 0 obj\n${object}\nendobj\n`
  })
  const xref = Buffer.byteLength(pdf)
  pdf += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`
  for (const offset of offsets.slice(1)) pdf += `${String(offset).padStart(10, '0')} 00000 n \n`
  pdf += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`
  return Buffer.from(pdf)
}

async function installSession(context: BrowserContext, token: string, user: JsonObject) {
  await context.addInitScript(({ accessToken, storedUser }) => {
    localStorage.setItem('rememberMe', 'true')
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('expiresAt', String(Date.now() + 3_600_000))
    localStorage.setItem('user', JSON.stringify(storedUser))
  }, { accessToken: token, storedUser: user })
}

async function expectWideMasterDetailLayout(page: Page, groupId: string) {
  const list = page.getByTestId(`${groupId}-list`)
  const separator = page.getByTestId(`${groupId}-separator`)
  const detail = page.getByTestId(`${groupId}-detail`)

  await expect(list).toBeVisible()
  await expect(separator).toBeVisible()
  await expect(detail).toBeVisible()

  const [listBox, separatorBox, detailBox] = await Promise.all([
    list.boundingBox(),
    separator.boundingBox(),
    detail.boundingBox(),
  ])
  expect(listBox).not.toBeNull()
  expect(separatorBox).not.toBeNull()
  expect(detailBox).not.toBeNull()
  expect(listBox!.width).toBeGreaterThanOrEqual(339)
  expect(detailBox!.width).toBeGreaterThanOrEqual(459)
  expect(listBox!.x + listBox!.width).toBeLessThanOrEqual(separatorBox!.x + 1)
  expect(separatorBox!.x + separatorBox!.width).toBeLessThanOrEqual(detailBox!.x + 1)

  const hasPageOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > document.documentElement.clientWidth + 1,
  )
  expect(hasPageOverflow).toBe(false)
}

test('real-provider full-stack acceptance', async ({ request, browser }) => {
  test.skip(missingEnv.length > 0, `NOT EXECUTED: missing ${missingEnv.join(', ')}`)

  const unique = Date.now()
  const email = `acceptance-${unique}@example.com`
  const password = 'Acceptance-Only-123!'

  const registerCaptcha = await solveCaptcha(request)
  const registered = await apiData(await request.post('/api/v1/auth/register/email', {
    data: { email, password, captchaToken: registerCaptcha },
  }))
  const token = registered.accessToken as string
  expect(token).toBeTruthy()

  const loginCaptcha = await solveCaptcha(request)
  const loggedIn = await apiData(await request.post('/api/v1/auth/login/email', {
    data: { email, password, captchaToken: loginCaptcha },
  }))
  expect(loggedIn.accessToken).toBeTruthy()

  const refreshed = await apiData(await request.post('/api/v1/auth/refresh', { data: {} }))
  expect(refreshed.accessToken).toBeTruthy()

  const googleCaptcha = await solveCaptcha(request)
  const googleUser = await apiData(await request.post('/api/v1/auth/login/google', {
    data: { idToken: process.env.SMOKE_GOOGLE_ID_TOKEN, captchaToken: googleCaptcha },
  }))
  expect(googleUser.accessToken).toBeTruthy()

  const uploaded = await apiData(await request.post('/api/v1/resumes', {
    headers: bearer(token),
    multipart: {
      title: 'Acceptance Resume',
      file: { name: 'acceptance-resume.pdf', mimeType: 'application/pdf', buffer: syntheticResumePdf() },
    },
  }))
  const resumeVersionId = uploaded.originalVersionId as string
  const resumeGroupId = uploaded.groupId as string
  await poll('resume parse', async () => apiData(await request.get(
    `/api/v1/resumes/versions/${resumeVersionId}`,
    { headers: bearer(token) },
  )), (value) => value.parseStatus === 'COMPLETED')

  const job = await apiData(await request.post('/api/v1/jobs', {
    headers: bearer(token),
    multipart: { url: process.env.SMOKE_JOB_URL! },
  }))
  const jobId = job.id as string
  await poll('job parse', async () => apiData(await request.get(`/api/v1/jobs/${jobId}`, {
    headers: bearer(token),
  })), (value) => value.status === 'COMPLETED')

  const score = await apiData(await request.post(`/api/v1/jobs/${jobId}/score`, {
    headers: bearer(token),
    data: { resumeVersionId },
  }))
  expect(score.finalScore).toBeGreaterThanOrEqual(0)
  expect(score.finalScore).toBeLessThanOrEqual(1)

  const conversation = await apiData(await request.post('/api/v1/conversations', {
    headers: bearer(token),
    data: { title: 'Acceptance Conversation', resumeVersionId, jobId },
  }))
  const conversationId = conversation.conversationId as string
  await apiData(await request.post(`/api/v1/conversations/${conversationId}/messages`, {
    headers: bearer(token),
    data: { content: 'Give one concise improvement for this resume and job.', fileUrls: [] },
  }))
  await poll('conversation reply', async () => apiData(await request.get(
    `/api/v1/conversations/${conversationId}`,
    { headers: bearer(token) },
  )), (value) => Array.isArray(value.messages)
    && value.messages.some((message) => (message as JsonObject).role === 'ASSISTANT'))

  await apiData(await request.post(`/api/v1/conversations/${conversationId}/compact`, {
    headers: bearer(token),
    data: {},
  }))
  await poll('conversation compaction', async () => apiData(await request.get(
    `/api/v1/conversations/${conversationId}`,
    { headers: bearer(token) },
  )), (value) => Number(value.contextTokens) > 0 && value.compactAdvised === false)

  const adminCaptcha = await solveCaptcha(request)
  const admin = await apiData(await request.post('/api/v1/auth/login/email', {
    data: {
      email: process.env.ADMIN_EMAIL,
      password: process.env.ADMIN_PASSWORD,
      captchaToken: adminCaptcha,
    },
  }))

  const failedAssets: string[] = []
  const fatalConsole: string[] = []
  const userContext = await browser.newContext()
  await installSession(userContext, token, { userId: registered.userId, email, role: 'JOB_SEEKER' })
  const userPage = await userContext.newPage()
  userPage.on('requestfailed', (request) => {
    if (request.resourceType() === 'script' || request.resourceType() === 'stylesheet') failedAssets.push(request.url())
  })
  userPage.on('console', (message) => {
    if (message.type() === 'error') fatalConsole.push(message.text())
  })
  for (const route of [`/jobs/${jobId}`, `/resumes/${resumeGroupId}`, '/chat']) {
    await userPage.goto(route)
    await expect(userPage.locator('#root')).not.toBeEmpty()
  }

  await userPage.setViewportSize({ width: 1440, height: 900 })
  for (const [route, groupId] of [
    ['/resumes', 'resumes-master-detail'],
    ['/jobs', 'jobs-master-detail'],
    ['/applications', 'tracking-master-detail'],
  ] as const) {
    await userPage.goto(route)
    await expectWideMasterDetailLayout(userPage, groupId)
  }

  await userPage.setViewportSize({ width: 1024, height: 768 })
  for (const [route, groupId] of [
    ['/resumes', 'resumes-master-detail'],
    ['/jobs', 'jobs-master-detail'],
    ['/applications', 'tracking-master-detail'],
  ] as const) {
    await userPage.goto(route)
    await expect(userPage.getByTestId(`${groupId}-container`)).toBeVisible()
    await expect(userPage.getByTestId(groupId)).toHaveCount(0)
  }

  await userPage.setViewportSize({ width: 390, height: 844 })
  await userPage.goto('/chat')
  const drawer = userPage.getByTestId('copilot-drawer-content')
  await expect(drawer).toBeVisible()
  const drawerBox = await drawer.boundingBox()
  expect(drawerBox).not.toBeNull()
  expect(drawerBox!.width).toBeLessThanOrEqual(390)
  expect(await drawer.evaluate((element) => element.scrollWidth <= element.clientWidth + 1)).toBe(true)

  await userContext.close()

  const adminContext = await browser.newContext()
  await installSession(adminContext, admin.accessToken, {
    userId: admin.userId,
    email: process.env.ADMIN_EMAIL,
    role: 'ADMIN',
  })
  const adminPage = await adminContext.newPage()
  await adminPage.goto('/admin/ai')
  await expect(adminPage.locator('#root')).not.toBeEmpty()
  await adminContext.close()

  expect(failedAssets).toEqual([])
  expect(fatalConsole).toEqual([])

  mkdirSync('test-results', { recursive: true })
  writeFileSync('test-results/acceptance-state.json', JSON.stringify({ conversationId }))
})
