import test from 'node:test'
import assert from 'node:assert/strict'
import { getVisitorId, resolvePageKey } from './analytics.js'

test('resolvePageKey returns fixed page keys without entity IDs or URLs', () => {
  assert.equal(resolvePageKey({ page: 'clubs' }), 'home')
  assert.equal(resolvePageKey({ page: 'matches' }), 'schedule')
  assert.equal(resolvePageKey({ page: 'matches', matchRoute: { matchId: 1000560649 } }), 'match-detail')
  assert.equal(resolvePageKey({ page: 'players', playerDetail: { playerId: 2000030002 } }), 'player')
  assert.equal(resolvePageKey({ page: 'clubs', clubRoute: { clubId: 1000000402 } }), 'club')
  assert.equal(resolvePageKey({ page: 'standings' }), 'standings')
  assert.equal(resolvePageKey({ page: 'fantasy' }), 'fantasy')
  assert.equal(resolvePageKey({ page: 'minigame' }), 'minigame')
  assert.equal(resolvePageKey({ page: 'admin' }), 'admin')
  assert.equal(resolvePageKey({ page: 'clubs', liveTest: true }), null)
})

test('getVisitorId persists UUID in storage and falls back when storage throws', () => {
  const store = new Map()
  const storage = {
    getItem: key => store.get(key) ?? null,
    setItem: (key, val) => store.set(key, val),
  }
  const first = getVisitorId(storage)
  const second = getVisitorId(storage)
  assert.match(first, /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)
  assert.equal(second, first)

  const brokenStorage = {
    getItem() { throw new Error('blocked') },
    setItem() { throw new Error('quota') },
  }
  const fallback = getVisitorId(brokenStorage)
  assert.match(fallback, /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i)
})
