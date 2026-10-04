import test from 'node:test'
import assert from 'node:assert/strict'
import { currentAccount, loginAccount, logoutAccount, startGoogle, confirmGoogleLink } from './auth.js'

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
