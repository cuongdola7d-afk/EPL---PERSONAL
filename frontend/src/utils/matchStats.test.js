import test from 'node:test'
import assert from 'node:assert/strict'
import { averageRating, hasAnyExtraStats, sortPlayers, teamTotals, topValues } from './matchStats.js'

const samplePlayers = [
  {
    playerId: 1, playerName: 'Bruno Fernandes', participationStatus: 'PLAYED',
    rating: '8.20', minutes: 90, goals: 1, assists: 1, yellowCards: 0, redCards: 0,
    extra: {
      saves: 0, goalsConceded: 4, bps: 38, bonus: 2, tackles: 0,
      clearancesBlocksInterceptions: 0, recoveries: 1, defensiveContribution: 1,
      expectedGoals: 0.99, expectedAssists: 0.56,
    },
  },
  {
    playerId: 2, playerName: 'Ayden Heaven', participationStatus: 'PLAYED',
    rating: '6.60', minutes: 45, goals: 0, assists: 0, yellowCards: 1, redCards: 0,
    extra: {
      saves: 0, goalsConceded: 2, bps: -3, bonus: 0, tackles: 3,
      clearancesBlocksInterceptions: 2, recoveries: 5, defensiveContribution: 5,
      expectedGoals: 0, expectedAssists: null,
    },
  },
  {
    playerId: 3, playerName: 'Unmapped Played', participationStatus: 'PLAYED',
    rating: null, minutes: 20, goals: 0, assists: 0, yellowCards: null, redCards: 0,
    extra: null,
  },
  {
    playerId: 4, playerName: 'Unused Sub', participationStatus: 'DID_NOT_PLAY',
    rating: null, minutes: 0, goals: 0, assists: 0, yellowCards: 1, redCards: 0,
    extra: {
      saves: 9, goalsConceded: 0, bps: 50, bonus: 3, tackles: 9,
      clearancesBlocksInterceptions: 9, recoveries: 9, defensiveContribution: 9,
      expectedGoals: 5.0, expectedAssists: 5.0,
    },
  },
]

test('teamTotals sums only played players, skips null extra, and returns null when whole team lacks a stat', () => {
  const totals = teamTotals(samplePlayers, 'MANUAL_VERIFIED')
  assert.ok(Math.abs(totals.averageRating - 7.4) < 1e-9)
  assert.ok(Math.abs(totals.expectedGoals - 0.99) < 1e-9)
  assert.ok(Math.abs(totals.expectedAssists - 0.56) < 1e-9)
  assert.equal(totals.saves, 0)
  assert.equal(totals.tackles, 3)
  assert.equal(totals.clearancesBlocksInterceptions, 2)
  assert.equal(totals.recoveries, 6)
  assert.equal(totals.yellowCards, 1) // excludes DNP yellowCards

  const noExtraTeam = [
    { playerId: 10, playerName: 'A', participationStatus: 'PLAYED', rating: '7.0', minutes: 90, yellowCards: null, extra: null },
    { playerId: 11, playerName: 'B', participationStatus: 'PLAYED', rating: null, minutes: 90, yellowCards: null, extra: null },
  ]
  const emptyTotals = teamTotals(noExtraTeam, 'MANUAL_VERIFIED')
  assert.equal(emptyTotals.averageRating, 7.0)
  assert.equal(emptyTotals.expectedGoals, null)
  assert.equal(emptyTotals.expectedAssists, null)
  assert.equal(emptyTotals.saves, null)
  assert.equal(emptyTotals.tackles, null)
  assert.equal(emptyTotals.clearancesBlocksInterceptions, null)
  assert.equal(emptyTotals.recoveries, null)
  assert.equal(emptyTotals.yellowCards, null)
  assert.equal(hasAnyExtraStats(noExtraTeam), false)
  assert.equal(hasAnyExtraStats(samplePlayers), true)
})

test('averageRating ignores unrated and DNP players and returns null if none rated', () => {
  assert.ok(Math.abs(averageRating(samplePlayers) - 7.4) < 1e-9)
  assert.equal(averageRating([samplePlayers[2], samplePlayers[3]]), null)
  assert.equal(averageRating([]), null)
})

test('sortPlayers keeps null values at the end for both desc and asc directions and handles ties', () => {
  const playedOnly = samplePlayers.slice(0, 3)

  const byRatingDesc = sortPlayers(playedOnly, 'rating', 'desc', 'MANUAL_VERIFIED')
  assert.deepEqual(byRatingDesc.map((p) => p.playerId), [1, 2, 3])

  const byRatingAsc = sortPlayers(playedOnly, 'rating', 'asc', 'MANUAL_VERIFIED')
  assert.deepEqual(byRatingAsc.map((p) => p.playerId), [2, 1, 3])

  const byXaDesc = sortPlayers(playedOnly, 'expectedAssists', 'desc', 'MANUAL_VERIFIED')
  assert.deepEqual(byXaDesc.map((p) => p.playerId), [1, 2, 3])

  const byXaAsc = sortPlayers(playedOnly, 'expectedAssists', 'asc', 'MANUAL_VERIFIED')
  assert.deepEqual(byXaAsc.map((p) => p.playerId), [1, 2, 3])

  const tied = [
    { playerId: 20, playerName: 'Zed', participationStatus: 'PLAYED', rating: '7.0', extra: { tackles: 3 } },
    { playerId: 21, playerName: 'Amy', participationStatus: 'PLAYED', rating: '7.5', extra: { tackles: 3 } },
    { playerId: 22, playerName: 'Bob', participationStatus: 'PLAYED', rating: '7.0', extra: { tackles: null } },
  ]
  assert.deepEqual(sortPlayers(tied, 'tackles', 'desc', 'MANUAL_VERIFIED').map((p) => p.playerId), [21, 20, 22])
  assert.deepEqual(sortPlayers(tied, 'tackles', 'asc', 'MANUAL_VERIFIED').map((p) => p.playerId), [21, 20, 22])
})

test('topValues returns highest positive value per key and ignores null, zero, negative and DNP', () => {
  const tops = topValues(
    samplePlayers,
    ['goals', 'saves', 'tackles', 'bps', 'expectedGoals'],
    'MANUAL_VERIFIED',
  )
  assert.equal(tops.goals, 1)
  assert.equal(tops.saves, null) // all played players have 0 saves
  assert.equal(tops.tackles, 3)
  assert.equal(tops.bps, 38)
  assert.equal(tops.expectedGoals, 0.99)

  const negativeOnly = [
    { playerId: 30, playerName: 'Neg', participationStatus: 'PLAYED', extra: { bps: -4 } },
  ]
  assert.equal(topValues(negativeOnly, ['bps'], 'MANUAL_VERIFIED').bps, null)
})
