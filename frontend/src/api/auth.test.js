import test from 'node:test'
import assert from 'node:assert/strict'
import { currentAccount, loginAccount, logoutAccount, registerAccount, startGoogle, confirmGoogleLink } from './auth.js'

test('unauthenticated account is null; server failures remain visible', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async () => new Response(JSON.stringify({ message: 'Bạn cần đăng nhập.' }), { status: 401 })
    assert.equal(await currentAccount(), null)
    globalThis.fetch = async () => new Response(JSON.stringify({ message: 'Server unavailable' }), { status: 500 })
    await assert.rejects(currentAccount(), /Server unavailable/)
  } finally { globalThis.fetch = original }
})

test('Google start and link confirmation use cookie/CSRF and reject arbitrary authorization URLs', async () => {
  const original = globalThis.fetch
  const requests = []
  let authorizationPath = '/api/auth/google/authorize/google'
  try {
    globalThis.fetch = async (url, options) => {
      requests.push({ url, options })
      if (url.endsWith('/csrf')) return new Response(JSON.stringify({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }))
      return new Response(JSON.stringify({ authorizationPath }))
    }
    assert.equal(await startGoogle('LOGIN', '/#fantasy'), '/api/auth/google/authorize/google')
    assert.deepEqual(JSON.parse(requests[1].options.body), { mode: 'LOGIN', returnPath: '/#fantasy' })
    await confirmGoogleLink()
    assert.deepEqual(JSON.parse(requests[3].options.body), { confirmed: true })
    assert.ok(requests.every(({ options }) => options.credentials === 'include'))
    assert.equal(requests[3].options.headers['X-CSRF-TOKEN'], 'csrf')
    authorizationPath = 'https://evil.example/'
    await assert.rejects(startGoogle('LOGIN', '/'), /không hợp lệ/)
  } finally { globalThis.fetch = original }
})

test('login/logout include cookies, use a fresh CSRF header each time and never store credentials', async () => {
  const original = globalThis.fetch
  const requests = []
  try {
    globalThis.fetch = async (url, options) => {
      requests.push({ url, options })
      if (url.endsWith('/csrf')) return new Response(JSON.stringify({ token: 'csrf-' + requests.length, headerName: 'X-CSRF-TOKEN' }))
      if (url.endsWith('/logout')) return new Response(null, { status: 204 })
      return new Response(JSON.stringify({ id: 1, displayName: 'Player', role: 'USER' }))
    }
    assert.equal((await loginAccount({ email: 'player@example.com', password: 'example-password' })).id, 1)
    assert.equal(await logoutAccount(), null)
    assert.ok(requests.every(({ options }) => options.credentials === 'include'))
    assert.equal(requests[1].options.headers['X-CSRF-TOKEN'], 'csrf-1')
    assert.equal(requests[3].options.headers['X-CSRF-TOKEN'], 'csrf-3')
    assert.equal(requests[1].options.method, 'POST'); assert.equal(requests[3].options.method, 'POST')
  } finally { globalThis.fetch = original }
})

test('429 displays Vietnamese wait time for email login, registration and both Google modes', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async url => {
      if (url.endsWith('/csrf')) return new Response(JSON.stringify({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }))
      return new Response(JSON.stringify({ message: 'Ignored backend detail', retryAfterSeconds: 999 }),
        { status: 429, headers: { 'Retry-After': '42' } })
    }
    for (const attempt of [
      () => loginAccount({ email: 'player@example.com', password: 'example-password' }),
      () => registerAccount({ displayName: 'Player', email: 'player@example.com', password: 'example-password' }),
      () => startGoogle('LOGIN', '/'), () => startGoogle('LINK', '/'),
    ]) {
      await assert.rejects(attempt(), error => error.status === 429 && error.retryAfterSeconds === 42
        && error.message === 'Bạn đã thử quá nhiều lần. Vui lòng thử lại sau 42 giây.')
    }
  } finally { globalThis.fetch = original }
})

test('429 uses Retry-After even for a non-JSON proxy response and falls back safely', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async () => new Response('Too many requests', { status: 429, headers: { 'Retry-After': '8' } })
    await assert.rejects(currentAccount(), error => error.status === 429 && error.retryAfterSeconds === 8)
    globalThis.fetch = async () => new Response(JSON.stringify({ retryAfterSeconds: 12 }), { status: 429 })
    await assert.rejects(currentAccount(), /sau 12 giây/)
    globalThis.fetch = async () => new Response('{}', { status: 429, headers: { 'Retry-After': 'invalid' } })
    await assert.rejects(currentAccount(), error => error.status === 429 && error.retryAfterSeconds === null
      && error.message.includes('chờ một lúc'))
  } finally { globalThis.fetch = original }
})

test('429 accepts an HTTP date in Retry-After', async () => {
  const original = globalThis.fetch
  const originalNow = Date.now
  try {
    Date.now = () => Date.UTC(2026, 9, 4, 12, 0, 0)
    globalThis.fetch = async () => new Response('{}', { status: 429,
      headers: { 'Retry-After': new Date(Date.now() + 30000).toUTCString() } })
    await assert.rejects(currentAccount(), error => error.status === 429 && error.retryAfterSeconds === 30)
  } finally { globalThis.fetch = original; Date.now = originalNow }
})
