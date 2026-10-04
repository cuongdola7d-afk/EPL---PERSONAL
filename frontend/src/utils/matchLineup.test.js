import test from 'node:test'
import assert from 'node:assert/strict'
import { formationLines, matchLineup } from './matchLineup.js'

function sample(formation, formationSource = 'FIXTURE', startId = 1) {
  const lines = formationLines(formation)
  const slots = lines.flatMap((count, rowIndex) => Array.from({ length: count }, (_, slotIndex) => ({ rowIndex, slotIndex })))
  const players = Array.from({ length: 13 }, (_, index) => ({ playerId: startId + index, playerName: `Player ${startId + index}`,
    participationStatus: index === 12 ? 'DID_NOT_PLAY' : 'PLAYED', position: index === 0 ? 'G' : 'M',
    minutes: index === 11 ? 90 : null, rating: index === 11 ? '10.0' : null, yellowCards: index === 2 ? 1 : null }))
  const metadata = players.map((player, index) => ({ playerId: player.playerId, role: index < 11 ? 'STARTER' : index === 11 ? 'SUB_USED' : 'SUB_UNUSED',
    matchPosition: formationSource === 'FIXTURE' && index < 11 ? index === 0 ? 'GK' : 'CM' : null,
    rowIndex: formationSource === 'FIXTURE' && index < 11 ? slots[index].rowIndex : null,
    slotIndex: formationSource === 'FIXTURE' && index < 11 ? slots[index].slotIndex : null,
    seasonPosition: index === 0 ? 'GK' : 'CM', substitutionInMinute: index === 11 ? 65 : null,
    substitutionOutMinute: index === 2 ? 65 : null }))
  return { players, evidence: { formation, formationSource, startersStatus: 'VERIFIED', players: metadata } }
}

test('two teams use independent formations and halves, with fixture positions before season positions', () => {
  const home = sample('4-2-3-1'), away = sample('3-4-2-1', 'CLUB_DEFAULT', 101)
  const left = matchLineup(home.players, home.evidence, 'home'), right = matchLineup(away.players, away.evidence, 'away')
  assert.equal(left.positionSource, 'MATCH'); assert.equal(right.positionSource, 'ILLUSTRATION')
  assert.deepEqual(formationLines(left.formation), [1, 4, 2, 3, 1]); assert.deepEqual(formationLines(right.formation), [1, 3, 4, 2, 1])
  for (const team of [left, right]) {
    assert.equal(team.nodes.length, 11); assert.equal(new Set(team.nodes.map(node => node.player.playerId)).size, 11)
    assert.equal(team.substitutes.length, 1); assert.equal(team.bench.length, 1)
    assert.ok(team.nodes.every(node => node.player.role === 'STARTER' && node.y > 0 && node.y < 100))
    const counts = formationLines(team.formation)
    assert.deepEqual(counts.map((_, row) => team.nodes.filter(node => node.rowIndex === row).length), counts)
  }
  assert.ok(left.nodes.every(node => node.x > 0 && node.x < 50)); assert.ok(right.nodes.every(node => node.x > 50 && node.x < 100))
  assert.equal(left.nodes[2].player.yellowCards, 1); assert.equal(left.nodes[2].player.substitutionOutMinute, 65)
  assert.equal(left.substitutes[0].playerId, 12); assert.equal(left.substitutes[0].rating, '10.0')
  assert.ok(!left.nodes.some(node => node.player.playerId === 12))
})

test('missing formation or confirmed starters never generates a fake XI from minutes, rating or permissions', () => {
  const data = sample('4-3-3')
  const noRoles = matchLineup(data.players, undefined, 'home')
  assert.equal(noRoles.nodes.length, 0); assert.equal(noRoles.starters.length, 0); assert.equal(noRoles.unknown.length, 13)
  const noFormation = matchLineup(data.players, { ...data.evidence, formation: null, formationSource: 'MISSING' }, 'home')
  assert.equal(noFormation.nodes.length, 0); assert.equal(noFormation.starters.length, 11)
  const wrong = structuredClone(data.evidence); wrong.players[1].playerId = wrong.players[0].playerId
  assert.equal(matchLineup(data.players, wrong, 'home').nodes.length, 0)
})

test('partial fixture coordinates are retained while remaining nodes are explicitly illustrative', () => {
  const data = sample('4-1-4-1')
  for (const player of data.evidence.players.slice(1)) { player.rowIndex = null; player.slotIndex = null; player.matchPosition = null }
  const result = matchLineup(data.players, data.evidence, 'home')
  assert.equal(result.positionSource, 'ILLUSTRATION'); assert.equal(result.nodes.length, 11)
  assert.equal(result.nodes[0].player.playerId, 1); assert.equal(result.nodes[0].positionSource, 'MATCH')
  assert.equal(result.nodes.filter(node => node.positionSource === 'MATCH').length, 1)
})

test('supports formation lines beyond Fantasy and refuses invalid counts or duplicated actual slots', () => {
  for (const formation of ['4-5-1', '5-4-1', '3-1-4-2', '3-1-3-2-1']) assert.ok(formationLines(formation))
  for (const formation of [null, '4-3-4', '4-0-6', '433']) assert.equal(formationLines(formation), null)
  const data = sample('4-3-3'); data.evidence.players[1].rowIndex = 0; data.evidence.players[1].slotIndex = 0
  const invalid = matchLineup(data.players, data.evidence, 'home')
  assert.equal(invalid.positionSource, 'INVALID'); assert.equal(invalid.nodes.length, 0)
})
