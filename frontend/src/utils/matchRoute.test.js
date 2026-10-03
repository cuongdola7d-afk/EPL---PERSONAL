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
