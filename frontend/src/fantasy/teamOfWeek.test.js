import test from 'node:test'
import assert from 'node:assert/strict'
import { completedTeamOfWeekGameweeks, isValidTeamOfWeek, TEAM_OF_WEEK_SLOTS, teamOfWeekGameweek,
  readTeamOfWeekSelection, saveTeamOfWeekSelection, TEAM_OF_WEEK_STORAGE_KEY } from './teamOfWeek.js'

const permission = slot => ({ LCB: 'CB', RCB: 'CB', LCM: 'CM', RCM: 'CM' })[slot] ?? slot
function fixtures(week, status = 'FINISHED') {
  return Array.from({ length: 10 }, (_, index) => ({ id: week * 100 + index, matchweek: week,
    homeClubId: index * 2 + 1, awayClubId: index * 2 + 2, status }))
}

test('uses complete finished schedules, including skipped contests with no statistics', () => {
  const matches = [...fixtures(8), ...fixtures(10), ...fixtures(11, 'SCHEDULED'), ...fixtures(38, 'SCHEDULED')]
  const weeks = completedTeamOfWeekGameweeks(matches)
  assert.deepEqual(weeks, [8, 10])
  assert.equal(teamOfWeekGameweek(weeks, null), 10)
  assert.equal(teamOfWeekGameweek(weeks, 8), 8)
})

test('rejects missing fixtures, unfinished matches, duplicate fixtures and repeated clubs', () => {
  assert.deepEqual(completedTeamOfWeekGameweeks(fixtures(1).slice(1)), [])
  for (const status of ['SCHEDULED', 'IN_PLAY', 'POSTPONED', 'CANCELLED']) {
    const round = fixtures(1); round[9].status = status
    assert.deepEqual(completedTeamOfWeekGameweeks(round), [])
  }
  const duplicate = fixtures(1); duplicate[9] = duplicate[0]
  assert.deepEqual(completedTeamOfWeekGameweeks(duplicate), [])
  const repeatedClub = fixtures(1); repeatedClub[9].awayClubId = repeatedClub[0].homeClubId
  assert.deepEqual(completedTeamOfWeekGameweeks(repeatedClub), [])
  assert.deepEqual(completedTeamOfWeekGameweeks([...fixtures(1), ...fixtures(1)]), [])
})

test('empty calendars have no default and no eligible old selection', () => {
  assert.deepEqual(completedTeamOfWeekGameweeks([]), [])
  assert.equal(teamOfWeekGameweek([], null), null)
  assert.equal(teamOfWeekGameweek([], 10), null)
})

test('invalid saved rounds fall back to latest finished round and valid choices survive polling', () => {
  for (const selection of [11, 38, 0, '8', { currentGameweek: 11, gameweek: 38 }]) {
    assert.equal(teamOfWeekGameweek([8, 9, 10], selection), 10)
  }
  assert.equal(teamOfWeekGameweek([8, 9, 10], { currentGameweek: 11, gameweek: 8 }), 8)
  assert.equal(teamOfWeekGameweek([8, 9, 10, 11], 8), 8)
  assert.equal(teamOfWeekGameweek([9, 10], 8), 10)
  assert.deepEqual(completedTeamOfWeekGameweeks([...fixtures(0), ...fixtures(39), ...fixtures(10), ...fixtures(8)]), [8, 10])
})
test('reload restores a viewing preference, revalidates it and leaves Fantasy team storage alone', () => {
  const values = new Map([['fantasy-lineup', 'unchanged']])
  const storage = { getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) }
  assert.equal(readTeamOfWeekSelection(storage), null)
  saveTeamOfWeekSelection(8, storage)
  assert.equal(teamOfWeekGameweek([8, 9, 10], readTeamOfWeekSelection(storage)), 8)
  saveTeamOfWeekSelection(38, storage)
  assert.equal(teamOfWeekGameweek([8, 9, 10], readTeamOfWeekSelection(storage)), 10)
  values.set(TEAM_OF_WEEK_STORAGE_KEY, 'invalid JSON')
  assert.equal(readTeamOfWeekSelection(storage), null)
  saveTeamOfWeekSelection(null, storage)
  assert.deepEqual([...values], [['fantasy-lineup', 'unchanged']])
  const blocked = { getItem() { throw new Error('disabled') }, setItem() { throw new Error('disabled') } }
  assert.equal(readTeamOfWeekSelection(blocked), null)
  assert.doesNotThrow(() => saveTeamOfWeekSelection(8, blocked))
})

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
