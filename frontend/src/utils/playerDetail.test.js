import assert from 'node:assert/strict'
import test from 'node:test'
import { effectiveStat, matchRating, matchResult, matchState, seasonSummary } from './playerDetail.js'
import { parsePlayerDetailHash, parsePlayerListHash, playerDetailHash, playerListHash } from './playerRoute.js'

const finished = { id: 1, homeClubId: 10, awayClubId: 20, homeGoals: 2, awayGoals: 1, status: 'FINISHED', matchweek: 1, date: '2024-08-16' }

test('player URL retains id and season and rejects malformed routes', () => {
  assert.deepEqual(parsePlayerDetailHash(playerDetailHash(123, 2026)), { playerId: 123, season: 2026 })
  assert.equal(parsePlayerDetailHash('#players/0?season=2024'), null)
  assert.equal(parsePlayerDetailHash('#players/123?season=2025'), null)
})

test('player detail can return to the exact previous match or player list', () => {
  const match = '#matches/123?season=2026&week=4&filter=finished&club=Arsenal+FC'
  assert.deepEqual(parsePlayerDetailHash(playerDetailHash(45, 2026, match)),
    { playerId: 45, season: 2026, backHash: match })
  const list = playerListHash({ season: 2026, query: 'Saka', view: 'list', visibleCount: 24 })
  assert.equal(parsePlayerListHash(list).query, 'Saka')
  assert.equal(parsePlayerDetailHash(playerDetailHash(45, 2026, list)).backHash, list)
  assert.deepEqual(parsePlayerDetailHash('#players/45?season=2026&from=https%3A%2F%2Fevil.test'),
    { playerId: 45, season: 2026 })
})

test('verified inference is used without replacing missing raw values', () => {
  const row = { match: finished, clubId: 20, evidenceStatus: 'VERIFIED', stats: { minutes: null, goals: null, assists: null, rating: null, inferred: { minutes: 0, goals: 0, assists: 0 } } }
  assert.equal(effectiveStat(row, 'minutes'), 0)
  assert.equal(row.stats.minutes, null)
  assert.equal(matchState(row), 'did-not-play')
  assert.equal(matchResult(row), 'L')
  assert.equal(effectiveStat({ ...row, evidenceStatus: 'UNVERIFIED' }, 'minutes'), null)
})

test('unfinished and missing statistics stay distinct; season totals are not fabricated', () => {
  const missing = { match: finished, clubId: 10, stats: null }
  const upcoming = { match: { ...finished, status: 'SCHEDULED', homeGoals: null, awayGoals: null }, clubId: 10, stats: null }
  assert.equal(matchState(missing), 'missing')
  assert.equal(matchResult(missing), 'W')
  assert.equal(matchState(upcoming), 'upcoming')
  assert.equal(matchResult(upcoming), null)
  assert.equal(matchRating({ stats: { rating: '' } }), null)
  const played = { match: finished, clubId: 10, evidenceStatus: 'VERIFIED', stats: { minutes: 90, goals: null, assists: 1, rating: '7.2', inferred: { goals: null } } }
  const summary = seasonSummary([played, missing, upcoming])
  assert.equal(summary.appearances, 1)
  assert.equal(summary.totalMinutes, 90)
  assert.equal(summary.averageMinutes, 90)
  assert.equal(summary.goals, null)
  assert.equal(summary.assists, 1)
  assert.equal(summary.yellowCards, null)
  assert.equal(summary.redCards, null)
  assert.equal(summary.averageRating, 7.2)
  assert.equal(summary.ratedMatches.length, 1)
})

test('manual participation keeps an ungraded appearance separate from a DNP', () => {
  const played = { match: finished, clubId: 10, stats: { participationStatus: 'PLAYED', minutes: null, goals: 0, assists: 0, yellowCards: 0, redCards: 0, rating: null } }
  const dnp = { match: finished, clubId: 10, stats: { participationStatus: 'DID_NOT_PLAY', minutes: 0, goals: 0, assists: 0, yellowCards: 0, redCards: 0, rating: null } }
  assert.equal(matchState(played), 'played')
  assert.equal(matchState(dnp), 'did-not-play')
  assert.equal(seasonSummary([played, dnp]).appearances, 1)
  assert.equal(seasonSummary([played, dnp]).totalMinutes, null)
})
