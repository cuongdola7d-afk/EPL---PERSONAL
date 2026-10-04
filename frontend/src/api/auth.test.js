import test from 'node:test'
import assert from 'node:assert/strict'
import { currentAccount, loginAccount, logoutAccount } from './auth.js'

test('unauthenticated account is null; server failures remain visible', async () => {
  const original = globalThis.fetch
  try {
    globalThis.fetch = async () => new Response(JSON.stringify({ message: 'Bạn cần đăng nhập.' }), { status: 401 })
    assert.equal(await currentAccount(), null)
    globalThis.fetch = async () => new Response(JSON.stringify({ message: 'Server unavailable' }), { status: 500 })
    await assert.rejects(currentAccount(), /Server unavailable/)
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
