import test from 'node:test'
import assert from 'node:assert/strict'
import { recentClubForm, playerLeaders, mergePlayerRatings } from './standingsView.js'

const fixture = (id, date, homeGoals, awayGoals, extra = {}) => ({
  id, date, homeClubId: 1, awayClubId: 2, status: 'FINISHED', homeGoals, awayGoals, ...extra,
})

test('form keeps the latest five scored finished matches in oldest-to-newest order for either side', () => {
  const matches = [fixture(6, '2026-09-06', 0, 2), fixture(1, '2026-09-01', 5, 0),
    fixture(5, '2026-09-05', 1, 1), fixture(2, '2026-09-02', 0, 0),
    fixture(4, '2026-09-04', 2, 1), fixture(3, '2026-09-03', 0, 1),
    fixture(7, '2026-09-07', null, null), fixture(8, '2026-09-08', 2, 0, { status: 'LIVE' }),
    fixture(9, '2026-09-09', 1, 0, { homeClubId: 3, awayClubId: 4 })]
  assert.deepEqual(recentClubForm(matches, 1).map(row => row.match.id), [2, 3, 4, 5, 6])
  assert.deepEqual(recentClubForm(matches, 1).map(row => row.result), ['D', 'L', 'W', 'D', 'L'])
  assert.deepEqual(recentClubForm(matches, 2).map(row => row.result), ['D', 'W', 'L', 'D', 'W'])
  assert.equal(matches[0].id, 6)
})

test('leaderboards exclude missing values, retain confirmed zeroes and use stable tie ordering', () => {
  const players = [{ id: 1, name: 'C', goals: null }, { id: 2, name: 'B', goals: 3 },
    { id: 3, name: 'A', goals: 3 }, { id: 4, name: 'D', goals: 0 },
    { id: 5, name: 'E', goals: undefined }]
  assert.deepEqual(playerLeaders(players, 'goals').map(row => row.id), [3, 2, 4])
  assert.deepEqual(playerLeaders(players, 'goals', 1).map(row => row.id), [3])
  assert.equal(players[0].id, 1)
})

test('ratings weight appearances across clubs after transfers and exclude unrated appearances', () => {
  const players = [{ id: 1, name: 'A' }, { id: 2, name: 'B' }, { id: 3, name: 'C' }]
  const clubStatistics = [{ players: [{ playerId: 1, averageRating: 8, ratedAppearances: 1 },
    { playerId: 2, averageRating: null, ratedAppearances: 0 }] },
    { players: [{ playerId: 1, averageRating: 6, ratedAppearances: 3 },
      { playerId: 3, averageRating: 9, ratedAppearances: 0 }] }]
  const merged = mergePlayerRatings(players, clubStatistics)
  assert.equal(merged[0].averageRating, 6.5)
  assert.equal(merged[0].ratedAppearances, 4)
  assert.equal(merged[1].averageRating, null)
  assert.equal(merged[2].averageRating, null)
  assert.deepEqual(playerLeaders(merged, 'averageRating').map(row => row.id), [1])
  assert.equal(players[0].averageRating, undefined)
})
