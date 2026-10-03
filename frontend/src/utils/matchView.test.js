import test from 'node:test'
import assert from 'node:assert/strict'
import { matchPlayerState, matchPlayerValue, preferredMatchweek } from './matchView.js'

test('prefers the latest complete gameweek and falls back to the first available week', () => {
  const matches = [
    ...Array.from({ length: 10 }, (_, id) => ({ id, matchweek: 5, status: 'FINISHED' })),
    ...Array.from({ length: 10 }, (_, id) => ({ id: id + 10, matchweek: 6, status: 'SCHEDULED' })),
  ]
  assert.equal(preferredMatchweek(matches), 5)
  assert.equal(preferredMatchweek(matches.slice(10)), 6)
  assert.equal(preferredMatchweek([]), 1)
})

test('uses explicit participation before minutes and preserves missing statistics', () => {
  assert.equal(matchPlayerState({ participationStatus: 'PLAYED', minutes: null }), 'played')
  assert.equal(matchPlayerState({ participationStatus: 'DID_NOT_PLAY', minutes: null }), 'did-not-play')
  assert.equal(matchPlayerState({ minutes: null, inferred: null }), 'unknown')
  assert.equal(matchPlayerValue({ goals: null, inferred: { goals: 1 } }, 'goals', 'MISSING'), null)
  assert.equal(matchPlayerValue({ goals: null, inferred: { goals: 1 } }, 'goals', 'VERIFIED'), 1)
})
