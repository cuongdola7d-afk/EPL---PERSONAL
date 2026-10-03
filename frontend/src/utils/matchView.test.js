import test from 'node:test'
import assert from 'node:assert/strict'
import { estimatedPitchPlayers, matchPlayerState, matchPlayerValue, pitchPositions, preferredMatchweek } from './matchView.js'

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

test('pitch estimate selects eleven unique played players and keeps substitutes and DNP on bench', () => {
  const players = [
    { playerId: 1, position: 'G', participationStatus: 'PLAYED', minutes: 90 },
    ...Array.from({ length: 11 }, (_, index) => ({
      playerId: index + 2, position: index < 4 ? 'D' : index < 8 ? 'M' : 'F',
      participationStatus: 'PLAYED', minutes: 89 - index,
    })),
    { playerId: 13, position: 'D', participationStatus: 'DID_NOT_PLAY', minutes: null },
  ]
  const { selected, bench } = estimatedPitchPlayers(players, 'MANUAL_VERIFIED')
  assert.equal(selected.length, 11)
  assert.equal(selected[0].playerId, 1)
  assert.deepEqual(bench.map((player) => player.playerId), [12, 13])
  const nodes = pitchPositions(selected, 'home')
  assert.equal(nodes.length, 11)
  assert.ok(nodes.every(({ x, y }) => x > 0 && x < 50 && y > 0 && y < 100))
})
