import assert from 'node:assert/strict'
import test from 'node:test'
import { buildApiUrl } from './request.js'

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
