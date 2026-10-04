import test from 'node:test'
import assert from 'node:assert/strict'
import { validResult, formatPoints } from './results.js'
import { fetchFantasyResult } from '../api/fantasyEntries.js'
import { fetchFantasyReadiness, publishFantasyResults } from '../api/fantasyResults.js'

const waiting = { accountId: 102, season: 2026, gameweek: 6, status: 'AWAITING_RESULTS',
  version: null, publishedAt: null, result: null }
function published() {
  return { ...waiting, status: 'PUBLISHED', version: 1, publishedAt: '2026-10-20T00:00:00Z',
    result: { formation: '4-2-1-3', totalPoints: 66.77, players: Array.from({ length: 11 }, (_, i) => ({
      slotKey: 'slot-' + i, playerId: i + 1, name: 'Player ' + (i + 1), club: 'Club 1', position: 'CM', points: i < 2 ? 0 : 7.11,
      matches: [{ fixtureId: 601, rating: i < 2 ? null : 7.11, points: i < 2 ? 0 : 7.11,
        reason: i === 0 ? 'DID_NOT_PLAY' : i === 1 ? 'SOFASCORE_UNRATED_CONFIRMED' : 'SOFASCORE_RATING' }],
    })) } }
}
test('pending results cannot carry provisional points; published results need all eleven players', () => {
  assert.equal(validResult(waiting, 102, 6), true)
  assert.equal(validResult({ ...waiting, result: { totalPoints: 0 } }, 102, 6), false)
  const data = published()
  assert.equal(validResult(data, 102, 6), true)
  assert.equal(formatPoints(66.77), '66,77')
  data.result.players.pop()
  assert.equal(validResult(data, 102, 6), false)
})
test('a zero requires a confirmed reason; a player-fixture cannot occur twice', () => {
  const data = published()
  data.result.players[0].matches[0].reason = 'RATING_PENDING'
  assert.equal(validResult(data, 102, 6), false)
  data.result.players[0].matches[0].reason = 'DID_NOT_PLAY'
  data.result.players[0].matches.push({ ...data.result.players[0].matches[0] })
  assert.equal(validResult(data, 102, 6), false)
})
test('private result requests use owner session on same origin and reject another account', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  let data = waiting
  global.fetch = async (url, options) => {
    assert.equal(url, '/api/fantasy/2026/me/gameweeks/6/result')
    assert.equal(options.credentials, 'include')
    assert.equal(options.headers['X-PrismaXI-Account-ID'], '102')
    assert.equal(options.method, undefined)
    return new Response(JSON.stringify(data))
  }
  assert.equal((await fetchFantasyResult(102, 6)).status, 'AWAITING_RESULTS')
  data = { ...waiting, accountId: 103 }
  await assert.rejects(fetchFantasyResult(102, 6), error => error.code === 'SESSION_CHANGED')
})
test('admin publication uses CSRF and expected version; readiness errors expose only blockers', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  const calls = []
  global.fetch = async (url, options) => {
    calls.push([url, options])
    const data = url === '/api/auth/csrf' ? { headerName: 'X-CSRF-TOKEN', token: 'synthetic' } :
      { season: 2026, gameweek: 6, ready: true, currentVersion: 0, issues: [] }
    return new Response(JSON.stringify(data))
  }
  await fetchFantasyReadiness(6)
  await publishFantasyResults(6, { expectedVersion: 0, reason: 'Verified local data' }, false)
  assert.equal(calls[2][0], '/api/fantasy/2026/admin/gameweeks/6/publish-results')
  assert.equal(calls[2][1].headers['X-CSRF-TOKEN'], 'synthetic')
  assert.equal(calls[2][1].credentials, 'include')
  assert.deepEqual(JSON.parse(calls[2][1].body), { expectedVersion: 0, reason: 'Verified local data' })
  global.fetch = async url => new Response(JSON.stringify(url === '/api/auth/csrf' ?
    { headerName: 'X-CSRF-TOKEN', token: 'synthetic' } :
    { code: 'RESULTS_NOT_READY', message: 'Đang chờ dữ liệu', readiness: { issues: [{ fixtureId: 601, playerId: 5, code: 'RATING_PENDING' }] } }),
    { status: url === '/api/auth/csrf' ? 200 : 409 })
  await assert.rejects(publishFantasyResults(6, { expectedVersion: 0, reason: 'Verified local data' }, false),
    error => error.status === 409 && error.readiness.issues[0].playerId === 5)
})
