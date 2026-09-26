import assert from 'node:assert/strict'
import test from 'node:test'
import { canOpenMatchStats, hasMatchScore } from './seasons.js'

test('only the verified 2024 GW1 dataset offers player statistics', () => {
  assert.equal(canOpenMatchStats(2024, { matchweek: 1, status: 'FINISHED' }), true)
  assert.equal(canOpenMatchStats(2026, { matchweek: 1, status: 'FINISHED' }), false)
  assert.equal(canOpenMatchStats(2026, { matchweek: 6, status: 'SCHEDULED' }), false)
  assert.equal(canOpenMatchStats(2024, { matchweek: 2, status: 'FINISHED' }), false)
})

test('future or postponed matches never display a pretend 0-0 score', () => {
  assert.equal(hasMatchScore({ status: 'SCHEDULED', homeGoals: null, awayGoals: null }), false)
  assert.equal(hasMatchScore({ status: 'SCHEDULED', homeGoals: 0, awayGoals: 0 }), false)
  assert.equal(hasMatchScore({ status: 'POSTPONED', homeGoals: 0, awayGoals: 0 }), false)
  assert.equal(canOpenMatchStats(2024, { matchweek: 1, status: 'SCHEDULED' }), false)
})

test('confirmed goalless draws display 0-0, missing or partial scores remain unavailable', () => {
  assert.equal(hasMatchScore({ status: 'FINISHED', homeGoals: 0, awayGoals: 0 }), true)
  assert.equal(hasMatchScore({ status: 'FINISHED', homeGoals: null, awayGoals: null }), false)
  assert.equal(hasMatchScore({ status: 'LIVE', homeGoals: 1, awayGoals: 0 }), true)
  assert.equal(hasMatchScore({ status: 'FINISHED', homeGoals: 1 }), false)
})
