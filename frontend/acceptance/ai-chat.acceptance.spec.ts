import { expect, test, type APIRequestContext } from '@playwright/test'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'

type JsonObject = Record<string, unknown>

const token = process.env.REAL_PROVIDER_CHAT_TOKEN?.trim()
const resumeVersionId = process.env.REAL_PROVIDER_CHAT_RESUME_VERSION_ID?.trim()
const jobId = process.env.REAL_PROVIDER_CHAT_JOB_ID?.trim()

function bearer() {
  return { Authorization: `Bearer ${token}` }
}

async function apiData(response: Awaited<ReturnType<APIRequestContext['get']>>): Promise<JsonObject> {
  expect(response.ok(), `HTTP ${response.status()} ${response.url()}`).toBeTruthy()
  const body = await response.json() as JsonObject
  expect(body.code).toBe(200)
  return body.data as JsonObject
}

async function waitForReply(
  request: APIRequestContext,
  conversationId: string,
  requestId: string,
): Promise<JsonObject> {
  const deadline = Date.now() + 180_000
  while (Date.now() < deadline) {
    const conversation = await apiData(await request.get(
      `/api/v1/conversations/${conversationId}?page=0&size=50`,
      { headers: bearer() },
    ))
    const reply = conversation.aiReply as JsonObject
    expect(reply.requestId).toBe(requestId)
    if (reply.status === 'FAILED' || reply.status === 'TIMED_OUT') {
      throw new Error(`AI reply ${requestId} entered ${String(reply.status)}`)
    }
    if (reply.status === 'COMPLETED') return conversation
    await new Promise((resolve) => setTimeout(resolve, 2_000))
  }
  throw new Error(`AI reply ${requestId} did not complete within 180 seconds`)
}

test('real-provider AI Chat exact-request smoke', async ({ request }) => {
  test.skip(!token || !resumeVersionId || !jobId, 'Real-provider Chat seed variables are required')

  const created = await apiData(await request.post('/api/v1/conversations', {
    headers: bearer(),
    data: { title: `AI Chat Smoke ${Date.now()}`, resumeVersionId, jobId },
  }))
  const conversationId = created.conversationId as string
  const initRequestId = (created.aiReply as JsonObject).requestId as string
  expect(initRequestId).toBeTruthy()
  const initialized = await waitForReply(request, conversationId, initRequestId)
  const initialMessages = initialized.messages as JsonObject[]
  const assistantCount = initialMessages.filter((message) => message.role === 'ASSISTANT').length

  const nonce = `CHAT-SMOKE-${Date.now()}`
  const pending = await apiData(await request.post(
    `/api/v1/conversations/${conversationId}/messages`,
    { headers: bearer(), data: { content: `Reply with this exact token and no other text: ${nonce}`, fileUrls: [] } },
  ))
  const requestId = (pending.aiReply as JsonObject).requestId as string
  expect(requestId).not.toBe(initRequestId)
  expect((pending.aiReply as JsonObject).status).toBe('PENDING')

  const completed = await waitForReply(request, conversationId, requestId)
  const reply = completed.aiReply as JsonObject
  const messages = completed.messages as JsonObject[]
  const correlatedAssistant = messages.find((message) =>
    message.role === 'ASSISTANT' && message.sequence === reply.assistantMessageSequence)
  expect(correlatedAssistant).toBeTruthy()
  expect(String(correlatedAssistant?.content ?? '')).toContain(nonce)
  expect(messages.filter((message) => message.role === 'ASSISTANT')).toHaveLength(assistantCount + 1)

  const compacting = await apiData(await request.post(
    `/api/v1/conversations/${conversationId}/compact`,
    { headers: bearer(), data: {} },
  ))
  const compactionRequestId = compacting.compactionRequestId as string
  expect(compactionRequestId).toBeTruthy()
  const compactDeadline = Date.now() + 180_000
  while (Date.now() < compactDeadline) {
    const current = await apiData(await request.get(
      `/api/v1/conversations/${conversationId}?page=0&size=50`,
      { headers: bearer() },
    ))
    expect(current.compactionRequestId).toBe(compactionRequestId)
    if (current.status === 'ACTIVE' && Number(current.contextTokens) > 0) {
      const reportPath = resolve('test-results/ai-chat-provider-smoke.json')
      await mkdir(dirname(reportPath), { recursive: true })
      await writeFile(reportPath, JSON.stringify({
        passed: true,
        requestId,
        compactionRequestId,
        completedAt: new Date().toISOString(),
      }, null, 2), 'utf8')
      return
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000))
  }
  throw new Error(`Compaction ${compactionRequestId} did not complete within 180 seconds`)
})
