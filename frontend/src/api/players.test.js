import test from 'node:test'
import assert from 'node:assert/strict'
import { isValidPlayer, playerPath } from './players.js'

test('player request uses the selected season and filters', () => {
  assert.equal(playerPath({ club: 'Arsenal FC', position: 'DEFENDER' }, 2026),
    '/api/players?season=2026&club=Arsenal+FC&position=DEFENDER')
  assert.equal(playerPath({ club: '', position: '' }, 2024), '/api/players?season=2024')
})

test('player response accepts missing 2026 stats without accepting invalid values', () => {
  const player = { id: 1, name: 'Player', clubId: 2, club: 'Club', position: 'DEFENDER', goals: null, assists: null }
  assert.equal(isValidPlayer(player), true)
  assert.equal(isValidPlayer({ ...player, goals: 2, assists: 1 }), true)
  assert.equal(isValidPlayer({ ...player, goals: '0' }), false)
  assert.equal(isValidPlayer({ ...player, fc27Overall: null }), true)
  assert.equal(isValidPlayer({ ...player, fc27Overall: 72 }), true)
  assert.equal(isValidPlayer({ ...player, fc27Overall: '72' }), false)
  assert.equal(isValidPlayer({ ...player, nationality: 'England', birthDate: '2001-09-05', heightCm: 178, preferredFoot: 'LEFT', shirtNumber: 7 }), true)
  assert.equal(isValidPlayer({ ...player, heightCm: '178' }), false)
  assert.equal(isValidPlayer({ ...player, preferredFoot: 'UNKNOWN' }), false)
})
