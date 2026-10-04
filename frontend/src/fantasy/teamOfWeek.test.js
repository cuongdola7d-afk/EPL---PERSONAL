import test from 'node:test'
import assert from 'node:assert/strict'
import { isValidTeamOfWeek, TEAM_OF_WEEK_SLOTS } from './teamOfWeek.js'

const permission = slot => ({ LCB: 'CB', RCB: 'CB', LCM: 'CM', RCM: 'CM' })[slot] ?? slot
function team() {
  return { season: 2026, gameweek: 1, formation: '4-3-3', status: 'COMPLETE', totalRating: 88,
    playedCount: 12, ratedCount: 11, candidateCount: 11, excludedMissingPositions: 0, excludedNullRatings: 1,
    completedFixtures: 10, recordedFixtures: 10, missingSlots: [], conflicts: [],
    picks: TEAM_OF_WEEK_SLOTS.map((slot, index) => ({ slot, player: { playerId: index + 1,
      name: `Player ${index + 1}`, clubId: 1, club: 'Match club', fixtureId: 100,
      rating: 8, eligiblePositions: [permission(slot)] } })) }
}

test('accepts exact 433 with stored ratings without OVR or club-count constraints', () => {
  assert.equal(isValidTeamOfWeek(team(), 1), true)
  assert.equal(isValidTeamOfWeek({ ...team(), season: 2024 }, 1), false)
  assert.equal(isValidTeamOfWeek(team(), 2), false)
})

test('rejects duplicate IDs, wrong position, null ratings and inconsistent totals', () => {
  const duplicate = team(); duplicate.picks[2].player.playerId = duplicate.picks[3].player.playerId
  assert.equal(isValidTeamOfWeek(duplicate, 1), false)
  const position = team(); position.picks[2].player.eligiblePositions = ['LB']
  assert.equal(isValidTeamOfWeek(position, 1), false)
  const missing = team(); missing.picks[0].player.rating = null
  assert.equal(isValidTeamOfWeek(missing, 1), false)
  assert.equal(isValidTeamOfWeek({ ...team(), totalRating: 89 }, 1), false)
})

test('accepts honest partial assignments with empty slots and no total for eleven', () => {
  const partial = team(); partial.status = 'INSUFFICIENT_DATA'; partial.totalRating = null
  partial.picks[0].player = null; partial.missingSlots = ['GK']
  assert.equal(isValidTeamOfWeek(partial, 1), true)
  assert.equal(isValidTeamOfWeek({ ...partial, missingSlots: ['RB'] }, 1), false)
})

test('multiple appearances require an explicit conflict with no computed team', () => {
  const conflict = team(); conflict.status = 'MULTIPLE_MATCHES'; conflict.totalRating = null
  conflict.picks = TEAM_OF_WEEK_SLOTS.map(slot => ({ slot, player: null })); conflict.missingSlots = TEAM_OF_WEEK_SLOTS
  conflict.conflicts = [{ playerId: 1, name: 'Player 1', fixtureIds: [100, 101] }]
  assert.equal(isValidTeamOfWeek(conflict, 1), true)
  assert.equal(isValidTeamOfWeek({ ...conflict, conflicts: [] }, 1), false)
})
