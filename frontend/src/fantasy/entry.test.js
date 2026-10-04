import test from 'node:test'
import assert from 'node:assert/strict'
import { emptyLineup, entryLineup, sameLineup } from './entry.js'
import { fetchFantasyEntry, saveFantasyDraft, submitFantasyEntry } from '../api/fantasyEntries.js'

test('new GW starts empty, never inherits browser choices; order does not change draft comparison', () => {
  assert.deepEqual(entryLineup({ draft: null, submitted: null }), emptyLineup())
  const a = { formation: '4-4-2', picks: { '0-0': 1, '0-1': 2 } }
  const b = { formation: '4-4-2', picks: { '0-1': 2, '0-0': 1 } }
  assert.equal(sameLineup(a, b), true)
  assert.equal(sameLineup(a, { ...b, picks: { '0-0': 3 } }), false)
  const loaded = entryLineup({ draft: a }); loaded.picks['0-0'] = 3
  assert.equal(a.picks['0-0'], 1)
})
test('private requests use same origin/session, CSRF and expected revision; draft and submit are explicit', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  const calls = []
  global.fetch = async (url, options) => {
    calls.push([url, options]); assert.equal(options.credentials, 'include')
    return new Response(JSON.stringify(url === '/api/auth/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'synthetic-test-token' } :
      { accountId: 101, season: 2026, gameweek: 6, version: 1, draft: null, submitted: null }))
  }
  await fetchFantasyEntry(101, 6)
  const body = { formation: '4-4-2', picks: {}, expectedVersion: 0 }
  await saveFantasyDraft(101, 6, body)
  await submitFantasyEntry(101, 6, body)
  assert.equal(calls[0][0], '/api/fantasy/2026/me/gameweeks/6')
  assert.equal(calls[2][1].headers['X-CSRF-TOKEN'], 'synthetic-test-token')
  assert.equal(calls[2][1].headers['X-PrismaXI-Account-ID'], '101')
  assert.deepEqual(JSON.parse(calls[2][1].body), body)
  assert.equal(calls[4][0], '/api/fantasy/2026/me/gameweeks/6/submit')
})
test('stale revision reports conflict without retrying or replacing server data', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original }); let calls = 0
  global.fetch = async url => { calls++; return new Response(JSON.stringify(url === '/api/auth/csrf' ?
    { headerName: 'X-CSRF-TOKEN', token: 'synthetic' } : { code: 'FANTASY_ENTRY_REQUEST', message: 'Dữ liệu đã thay đổi' }), { status: url === '/api/auth/csrf' ? 200 : 409 }) }
  await assert.rejects(saveFantasyDraft(101, 6, { expectedVersion: 0 }), error => error.status === 409)
  assert.equal(calls, 2)
})
test('wrong session response is rejected before rendering another account team', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  global.fetch = async () => new Response(JSON.stringify({ accountId: 102, season: 2026, gameweek: 6, version: 0, draft: null, submitted: null }))
  await assert.rejects(fetchFantasyEntry(101, 6), error => error.code === 'SESSION_CHANGED')
})
