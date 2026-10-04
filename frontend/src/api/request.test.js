import assert from 'node:assert/strict'
import test from 'node:test'
import { buildApiUrl, fetchApiList } from './request.js'
import { currentAccount } from './auth.js'

test('local development always uses the Vite proxy', () => {
  assert.equal(buildApiUrl('/api/clubs', 'https://railway.example/', true), '/api/clubs')
})

test('production joins the backend origin and API path with one slash', () => {
  assert.equal(buildApiUrl('/api/clubs', ' https://railway.example/// ', false),
    'https://railway.example/api/clubs')
  assert.equal(buildApiUrl('api/matches?status=FINISHED', 'https://railway.example', false),
    'https://railway.example/api/matches?status=FINISHED')
})

test('production defaults to the same-origin proxy and rejects malformed explicit backend origins', () => {
  assert.equal(buildApiUrl('/api/clubs', '', false), '/api/clubs')
  assert.equal(buildApiUrl('/api/clubs', undefined, false), '/api/clubs')
  assert.throws(() => buildApiUrl('/api/clubs', 'https://railway.example/api', false), /origin backend/)
  assert.throws(() => buildApiUrl('/api/clubs', 'https://railway.example?token=secret', false), /origin backend/)
})

test('public statistics omit session credentials while the account endpoint keeps them', async () => {
  const originalFetch = globalThis.fetch
  const controller = new AbortController()
  const rows = [{ clubId: 1, position: 1 }]
  const account = { id: 7, displayName: 'Test account' }
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options })
    return { ok: true, status: 200, json: async () => url === '/api/auth/me' ? account : rows }
  }
  try {
    assert.deepEqual(await fetchApiList('/api/standings?season=2026', controller.signal,
      item => Number.isInteger(item.clubId), 'bảng xếp hạng'), rows)
    assert.deepEqual(await currentAccount(controller.signal), account)
    assert.equal(calls.length, 2)
    assert.equal(calls[0].options.credentials, 'omit')
    assert.equal(calls[1].options.credentials, 'include')
    assert.ok(calls.every(({ options }) => options.signal === controller.signal))
  } finally { globalThis.fetch = originalFetch }
})

test('public lookups still validate data and propagate aborted requests', async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => ({ ok: true, status: 200, json: async () => [{ invalid: true }] })
  try {
    await assert.rejects(fetchApiList('/api/standings?season=2026', undefined,
      item => Number.isInteger(item.clubId), 'bảng xếp hạng'), /không đúng định dạng/)
    const controller = new AbortController()
    controller.abort()
    globalThis.fetch = async (_url, options) => { throw options.signal.reason }
    await assert.rejects(fetchApiList('/api/standings?season=2026', controller.signal,
      () => true, 'bảng xếp hạng'), { name: 'AbortError' })
  } finally { globalThis.fetch = originalFetch }
})
