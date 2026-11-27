import test from 'node:test'
import assert from 'node:assert/strict'
import { isValidClubSeasonTrends } from './clubs.js'

const VALID_PAYLOAD = {
  clubId: 1000000064,
  season: 2026,
  matches: [
    {
      fixtureId: 1000560550,
      matchweek: 1,
      kickoffUtc: '2026-08-23T15:30:00Z',
      home: false,
      opponentClubId: 1000000067,
      opponentName: 'Newcastle United FC',
      goalsFor: 2,
      goalsAgainst: 2,
      teamRating: 6.75,
      xgFor: 3.01,
      xgAgainst: 1.58,
    },
  ],
  rankHistory: [{ matchweek: 1, position: 10 }],
  scorers: [{ playerId: 2000003009, name: 'Alexander Isak', goals: 7 }],
  assisters: [{ playerId: 2000003017, name: 'Bradley Barcola', assists: 4 }],
  ownGoalsFor: 1,
}

test('isValidClubSeasonTrends accepts valid payloads including nullable rating and xG fields', () => {
  assert.equal(isValidClubSeasonTrends(VALID_PAYLOAD, 1000000064, 2026), true)
  const withNulls = {
    ...VALID_PAYLOAD,
    matches: [{
      ...VALID_PAYLOAD.matches[0],
      kickoffUtc: null,
      teamRating: null,
      xgFor: null,
      xgAgainst: null,
    }],
    ownGoalsFor: 0,
  }
  assert.equal(isValidClubSeasonTrends(withNulls, 1000000064, 2026), true)
})

test('isValidClubSeasonTrends rejects mismatched club/season and malformed fields', () => {
  assert.equal(isValidClubSeasonTrends(VALID_PAYLOAD, 999, 2026), false)
  assert.equal(isValidClubSeasonTrends(VALID_PAYLOAD, 1000000064, 2024), false)
  assert.equal(isValidClubSeasonTrends(null, 1000000064, 2026), false)
  assert.equal(isValidClubSeasonTrends({ ...VALID_PAYLOAD, ownGoalsFor: -1 }, 1000000064, 2026), false)
  assert.equal(isValidClubSeasonTrends({
    ...VALID_PAYLOAD,
    matches: [{ ...VALID_PAYLOAD.matches[0], teamRating: '6.75' }],
  }, 1000000064, 2026), false)
  assert.equal(isValidClubSeasonTrends({
    ...VALID_PAYLOAD,
    matches: [{ ...VALID_PAYLOAD.matches[0], xgFor: Number.NaN }],
  }, 1000000064, 2026), false)
  assert.equal(isValidClubSeasonTrends({
    ...VALID_PAYLOAD,
    matches: [{ ...VALID_PAYLOAD.matches[0], home: 'false' }],
  }, 1000000064, 2026), false)
  assert.equal(isValidClubSeasonTrends({
    ...VALID_PAYLOAD,
    rankHistory: [{ matchweek: 1, position: 0 }],
  }, 1000000064, 2026), false)
  assert.equal(isValidClubSeasonTrends({
    ...VALID_PAYLOAD,
    scorers: [{ playerId: 1, name: 'Isak', goals: '7' }],
  }, 1000000064, 2026), false)
  assert.equal(isValidClubSeasonTrends({
    ...VALID_PAYLOAD,
    assisters: [{ playerId: 1, name: 'Wirtz', assists: -1 }],
  }, 1000000064, 2026), false)
})
