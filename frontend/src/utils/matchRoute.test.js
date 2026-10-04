import test from 'node:test'
import assert from 'node:assert/strict'
import { matchDetailHash, matchListHash, parseMatchRoute } from './matchRoute.js'

test('match routes retain the round and filters when opening a detail', () => {
  const state = { season: 2026, week: 4, filter: 'finished', club: 'Arsenal FC' }
  assert.deepEqual(parseMatchRoute(matchListHash(state)), { matchId: null, ...state })
  assert.deepEqual(parseMatchRoute(matchDetailHash(123, state)), { matchId: 123, ...state })
  assert.deepEqual(parseMatchRoute('#matches'), {
    matchId: null, season: null, week: null, filter: 'all', club: '',
  })
  assert.equal(parseMatchRoute('#matches/0?season=2026'), null)
  assert.equal(parseMatchRoute('#matches/123?season=2025'), null)
})

test('player match links retain the season, gameweek and full profile return route', () => {
  for (const season of [2024, 2026]) {
    const player = `#players/2000020085?season=${season}&from=%23fantasy`
    const detail = matchDetailHash(123, { season, week: 5 }, player)
    assert.deepEqual(parseMatchRoute(detail), {
      matchId: 123, season, week: 5, filter: 'all', club: '', backHash: player,
    })
  }
})

test('match return links reject external URLs and invalid player targets', () => {
  for (const from of ['https://evil.test/', 'javascript:alert(1)', '#players/0?season=2026',
    '#players/123?season=2025', '#players/123', '#players?season=2026']) {
    assert.equal(parseMatchRoute(matchDetailHash(123, { season: 2026 }, from)).backHash, undefined)
    assert.equal(parseMatchRoute(`#matches/123?season=2026&from=${encodeURIComponent(from)}`).backHash, undefined)
  }
})
