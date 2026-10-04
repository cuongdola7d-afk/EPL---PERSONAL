import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

test('Vercel routes API before SPA fallback and injects only an environment-backed request secret', async () => {
  const config = JSON.parse(await fs.readFile(new URL('../../vercel.json', import.meta.url), 'utf8'))
  const api = config.routes[0]
  const path = '/api/auth/google/callback'
  const match = new RegExp(`^${api.src}$`).exec(path)
  assert.equal(api.dest.replace('$1', match[1]), 'https://epl-personal-production.up.railway.app' + path)
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
