import test from 'node:test'
import assert from 'node:assert/strict'
import { fetchFantasyLeaderboard } from '../api/fantasyLeaderboard.js'

test('public leaderboard uses same-origin GET without account cookies and keeps tied ranks', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  global.fetch = async (url, options) => {
    assert.equal(url, '/api/fantasy/2026/leaderboard?gameweek=6'); assert.equal(options.credentials, 'omit')
    return new Response(JSON.stringify({ season: 2026, gameweek: 6, status: 'PUBLISHED', version: 2, publishedGameweeks: 1,
      players: [101, 102].map(accountId => ({ accountId, displayName: 'Player ' + accountId, rank: 1, totalPoints: 67.77, gameweeksPlayed: 1 })) }))
  }
  const data = await fetchFantasyLeaderboard(6); assert.deepEqual(data.players.map(p => p.rank), [1, 1])
})
test('pending leaderboard cannot contain scores; season mode requests published totals', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  let data = { season: 2026, gameweek: null, status: 'AWAITING_RESULTS', version: null, publishedGameweeks: 0, players: [] }
  global.fetch = async url => { assert.equal(url, '/api/fantasy/2026/leaderboard'); return new Response(JSON.stringify(data)) }
  assert.deepEqual((await fetchFantasyLeaderboard(null)).players, [])
  data.players = [{ accountId: 101, displayName: 'Pending', rank: 1, totalPoints: 0, gameweeksPlayed: 1 }]
  await assert.rejects(fetchFantasyLeaderboard(null), /không đúng định dạng/)
})
