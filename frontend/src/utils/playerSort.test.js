import assert from 'node:assert/strict'
import test from 'node:test'
import { matchesPlayerSearch, sortPlayers } from './playerSort.js'

const players = [
  { id: 1, name: 'Álvaro', club: 'Arsenal', fc27Overall: null, goals: 0 },
  { id: 2, name: 'Bruno', club: 'Manchester United', fc27Overall: 85, goals: 3 },
  { id: 3, name: 'Carlos', club: 'Brighton', fc27Overall: 72, goals: 1 },
]

test('FC 27 overall defaults to descending and unknown values stay last in both directions', () => {
  assert.deepEqual(sortPlayers(players).map((player) => player.id), [2, 3, 1])
  assert.deepEqual(sortPlayers(players, 'overall', 'asc').map((player) => player.id), [3, 2, 1])
  assert.deepEqual(players.map((player) => player.id), [1, 2, 3])
})

test('other sort fields and Vietnamese name search work', () => {
  assert.deepEqual(sortPlayers(players, 'name', 'asc').map((player) => player.id), [1, 2, 3])
  assert.deepEqual(sortPlayers(players, 'goals', 'desc').map((player) => player.id), [2, 3, 1])
  assert.equal(matchesPlayerSearch('Đặng Văn Lâm', 'dang van'), true)
  assert.equal(matchesPlayerSearch('Álvaro', 'alvaro'), true)
})
