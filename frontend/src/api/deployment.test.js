import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

test('Vercel routes API before SPA fallback and injects only an environment-backed request secret', async () => {
  const config = JSON.parse(await fs.readFile(new URL('../../vercel.json', import.meta.url), 'utf8'))
  const api = config.routes[0]
  const path = '/api/auth/google/callback'
  const match = new RegExp(`^${api.src}$`).exec(path)
  assert.equal(api.dest.replace('$1', match[1]), 'https://epl-personal.onrender.com' + path)
  assert.deepEqual(api.transforms, [{
    type: 'request.headers', op: 'set', target: { key: 'x-prismaxi-proxy-secret' },
    args: '$PREMIERHUB_AUTH_PROXY_SECRET', env: ['PREMIERHUB_AUTH_PROXY_SECRET'],
  }])
  assert.equal(api.headers['Vercel-CDN-Cache-Control'], 'no-store')
  assert.equal(api.headers['x-vercel-enable-rewrite-caching'], '0')
  assert.ok(!Object.keys(api.headers).some(key => key.toLowerCase().includes('secret')))
  assert.equal(config.routes[1].handle, 'filesystem')
  assert.equal(config.routes.at(-1).dest, '/index.html')
})

test('Prepared Render proxy keeps the current same-origin routing, secret and cache protections', async () => {
  const current = JSON.parse(await fs.readFile(new URL('../../vercel.json', import.meta.url), 'utf8'))
  const prepared = JSON.parse(await fs.readFile(new URL('../../../docs/deployment/vercel.render.example.json', import.meta.url), 'utf8'))
  const api = prepared.routes[0]
  assert.equal(new URL(api.dest).hostname, 'your-render-service.onrender.com')
  for (const path of ['/api/auth/google/callback', '/api/fantasy/2026/me/gameweeks/6', '/api/minigame/2026/player-guess/practice/start']) {
    const match = new RegExp(`^${api.src}$`).exec(path)
    assert.equal(new URL(api.dest.replace('$1', match[1])).pathname, path)
  }
  prepared.routes[0].dest = current.routes[0].dest
  assert.deepEqual(prepared, current, 'Only the upstream destination may change at cutover')
})
