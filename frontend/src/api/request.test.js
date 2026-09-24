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

test('production rejects a missing or non-origin backend URL', () => {
  assert.throws(() => buildApiUrl('/api/clubs', '', false), /VITE_API_BASE_URL/)
  assert.throws(() => buildApiUrl('/api/clubs', 'https://railway.example/api', false), /origin backend/)
  assert.throws(() => buildApiUrl('/api/clubs', 'https://railway.example?token=secret', false), /origin backend/)
})
