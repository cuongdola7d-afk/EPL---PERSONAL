import test from 'node:test'
import assert from 'node:assert/strict'
import { isValidFantasyPlayer } from './fantasy.js'

const player = { id: 1, name: 'Amad', clubId: 1, club: 'MU', position: 'FORWARD',
  goals: null, assists: null, fc27Overall: 79, primaryPosition: 'RM',
  eligiblePositions: ['RM', 'RB', 'RW'], positionStatus: 'VERIFIED' }

test('Fantasy read accepts verified permissions and explicit missing data without inventing OVR', () => {
  assert.ok(isValidFantasyPlayer(player))
  assert.ok(isValidFantasyPlayer({ ...player, fc27Overall: null, primaryPosition: null,
    eligiblePositions: [], positionStatus: 'MISSING' }))
})

test('malformed Fantasy position arrays and IDs are rejected before rendering', () => {
  for (const eligiblePositions of [undefined, null, 'RW|RM', [1]])
    assert.equal(isValidFantasyPlayer({ ...player, eligiblePositions }), false)
  assert.equal(isValidFantasyPlayer({ ...player, id: 1.5 }), false)
  assert.equal(isValidFantasyPlayer({ ...player, primaryPosition: 1 }), false)
})
