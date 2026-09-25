import assert from 'node:assert/strict'
import test from 'node:test'
import { additionError, buildReplayPool, lineupError, normalizeSavedIds, replayTotal } from './replay.js'

const positions = ['G', 'D', 'D', 'D', 'D', 'M', 'M', 'M', 'M', 'F', 'F']
const lineup = positions.map((position, index) => ({
  id: index + 1, name: `Player ${index + 1}`, position,
  clubId: Math.floor(index / 2) + 1, points: index === 10 ? -2 : index + 1,
}))
const ids = lineup.map((player) => player.id)

test('11 unique players in a legal formation can reveal the exact sum, including deductions', () => {
  assert.equal(lineupError(ids, lineup), '')
  assert.equal(replayTotal(ids, lineup), 53)
  assert.equal(additionError(ids.slice(0, 10), lineup[10], lineup), '')
})

test('incomplete, duplicate and illegal formations cannot reveal a total', () => {
  assert.match(lineupError(ids.slice(0, 10), lineup), /đủ 11/)
  assert.match(lineupError([...ids.slice(0, 10), 1], lineup), /trùng/)
  assert.match(lineupError([...ids.slice(0, 10), 999], lineup), /không thuộc GW1/)
  assert.throws(() => replayTotal(ids.slice(0, 10), lineup), /đủ 11/)
  const noGoalkeeper = lineup.map((player) => player.id === 1 ? { ...player, position: 'M' } : player)
  assert.match(lineupError(ids, noGoalkeeper), /Thủ môn/)
  const fourFromClub = lineup.map((player) => player.id <= 4 ? { ...player, clubId: 1 } : player)
  assert.match(lineupError(ids, fourFromClub), /Tối đa 3/)
  assert.match(additionError([1], lineup[0], lineup), /đã có/)
  assert.match(additionError(ids, { id: 12, position: 'M', clubId: 8 }, lineup), /chỉ có 11/)
})

test('saved IDs are limited to unique players in the verified pool', () => {
  assert.deepEqual(normalizeSavedIds([1, 1, 999, '2', 2], lineup), [1, 2])
  assert.deepEqual(normalizeSavedIds({ ids: [1] }, lineup), [])
})

test('pool uses ten verified fixture details with 40 complete scores each', () => {
  const matches = Array.from({ length: 10 }, (_, index) => ({
    id: index + 100, matchweek: 1, status: 'FINISHED',
    homeClubId: index * 2 + 1, homeClub: `Home ${index}`,
    awayClubId: index * 2 + 2, awayClub: `Away ${index}`,
  }))
  const details = matches.map((match) => ({
    match, evidenceStatus: 'VERIFIED',
    homePlayers: Array.from({ length: 20 }, (_, index) => ({
      playerId: match.id * 100 + index, playerName: `Home player ${index}`,
      clubId: match.homeClubId, position: 'D', score: { status: 'COMPLETE', confirmedPoints: 2 },
    })),
    awayPlayers: Array.from({ length: 20 }, (_, index) => ({
      playerId: match.id * 100 + 20 + index, playerName: `Away player ${index}`,
      clubId: match.awayClubId, position: 'M', score: { status: 'COMPLETE', confirmedPoints: -1 },
    })),
  }))
  assert.equal(buildReplayPool(matches, details).length, 400)
  assert.equal(buildReplayPool(matches, details)[0].points, -1)
  assert.throws(() => buildReplayPool(matches.slice(1), details.slice(1)), /đủ 10/)
  const unverified = details.map((detail, index) => index === 0 ? { ...detail, evidenceStatus: 'INVALID' } : detail)
  assert.throws(() => buildReplayPool(matches, unverified), /chưa đủ 40 điểm/)
  const provisional = details.map((detail, index) => index === 0 ? {
    ...detail, homePlayers: detail.homePlayers.map((player, row) => row === 0
      ? { ...player, score: { status: 'PROVISIONAL', confirmedPoints: 2 } } : player),
  } : detail)
  assert.throws(() => buildReplayPool(matches, provisional), /chưa đủ 40 điểm/)
})
