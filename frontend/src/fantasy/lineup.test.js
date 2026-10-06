import test from 'node:test'
import assert from 'node:assert/strict'
import { FORMATIONS, fitsSlot, formationSlots, movePicks, normalizeLineup, pickError,
  slotPosition, validateLineup } from './lineup.js'

function fixture(formation = '4-2-1-3', overall = 78) {
  const slots = formationSlots(formation)
  const players = slots.map((slot, index) => ({ id: index + 1, name: `Player ${index + 1}`,
    position: slot.group, clubId: index % 4, fc27Overall: overall,
    primaryPosition: slotPosition(slot.position), eligiblePositions: [slotPosition(slot.position)] }))
  const picks = Object.fromEntries(slots.map((slot, index) => [slot.key, players[index].id]))
  return { slots, players, picks }
}

test('all four formations normalize CB/CM and accept eleven players under 910', () => {
  for (const formation of Object.keys(FORMATIONS)) {
    const { slots, players, picks } = fixture(formation)
    assert.equal(slots.length, 11)
    assert.equal(validateLineup(formation, picks, players), '')
  }
  assert.equal(slotPosition('RCB'), 'CB')
  assert.equal(slotPosition('LCM'), 'CM')
})

test('Amad RM/RB/RW permissions ignore broad FORWARD; Shaw LB and RW alone do not imply other roles', () => {
  const amad = { id: 1, position: 'FORWARD', fc27Overall: 79, primaryPosition: 'RM', eligiblePositions: ['RM', 'RB', 'RW'] }
  for (const position of ['RM', 'RB', 'RW']) assert.ok(fitsSlot(amad, { position }))
  for (const position of ['ST', 'LM', 'LB']) assert.equal(fitsSlot(amad, { position }), false)
  assert.equal(fitsSlot({ ...amad, primaryPosition: 'RW', eligiblePositions: ['RW'] }, { position: 'RM' }), false)
  assert.equal(fitsSlot({ ...amad, primaryPosition: 'LB', eligiblePositions: ['LB'] }, { position: 'LCB' }), false)
})

test('missing OVR or positions block selection; actual stored Dowman 72 is used', () => {
  const { slots, players } = fixture()
  assert.match(pickError({ ...players[0], fc27Overall: null }, slots[0], {}, players), /OVR/)
  assert.match(pickError({ ...players[0], primaryPosition: null, eligiblePositions: [] }, slots[0], {}, players), /vị trí/)
  assert.equal(pickError({ ...players[0], id: 2000001025, fc27Overall: 72 }, slots[0], {}, players), '')
})

test('duplicates, wrong slot, club cap and OVR cap include replacement budget', () => {
  const { slots, players, picks } = fixture('4-2-1-3', 82)
  players[0].eligiblePositions.push('ST')
  assert.match(pickError(players[0], slots[1], picks, players), /đã có/)
  assert.match(pickError(players[1], slots[0], {}, players), /không được/)
  const fourth = { ...players[0], id: 99 }
  assert.match(pickError(fourth, slots[1], picks, players), /3.*CLB/)
  assert.match(pickError({ ...fourth, clubId: 9, fc27Overall: 91 }, slots[1], picks, players), /911.*910/)
  assert.equal(pickError({ ...fourth, clubId: 9, fc27Overall: 90 }, slots[1], picks, players), '')
  assert.equal(validateLineup('4-2-1-3', picks, players.map((p, i) => ({ ...p, fc27Overall: i === 0 ? 90 : 82 }))), '')
  assert.match(validateLineup('4-2-1-3', picks, players.map(p => ({ ...p, fc27Overall: 83 }))), /913.*910/)
  assert.match(validateLineup('4-2-1-3', { ...picks, [slots[1].key]: players[0].id }, players), /trùng/)
})

test('matching finds alternate placements that a greedy assignment would miss', () => {
  const { players, picks } = fixture()
  players[3].eligiblePositions.push('CM')
  const moved = movePicks('4-2-1-3', '4-3-3', picks, players)
  assert.equal(moved.unassigned.length, 0)
  assert.equal(validateLineup(moved.formation, moved.picks, players), '')
  const flexible = [{ id: 101, fc27Overall: 70, primaryPosition: 'ST', eligiblePositions: ['ST', 'LM'] },
    ...[102, 103].map(id => ({ id, fc27Overall: 70, primaryPosition: 'ST', eligiblePositions: ['ST'] }))]
  const result = movePicks('4-4-2', '4-4-2', { '0-0': 101, '0-1': 102 }, flexible, [103])
  assert.equal(result.unassigned.length, 0)
  assert.equal(result.picks['1-0'], 101)
})

test('impossible formation preserves unmatched IDs and switching back restores all picks', () => {
  const { players, picks } = fixture()
  const moved = movePicks('4-2-1-3', '4-4-2', picks, players)
  assert.ok(moved.unassigned.length > 0)
  assert.deepEqual(new Set([...Object.values(moved.picks), ...moved.unassigned]), new Set(Object.values(picks)))
  for (const slot of formationSlots('4-4-2')) {
    if (moved.picks[slot.key]) assert.ok(fitsSlot(players.find(p => p.id === moved.picks[slot.key]), slot))
  }
  const restored = movePicks('4-4-2', '4-2-1-3', moved.picks, players, moved.unassigned)
  assert.equal(restored.unassigned.length, 0)
  assert.equal(validateLineup(restored.formation, restored.picks, players), '')
})

test('reload preserves invalid picks, warns on duplicates and rechecks legacy cap', () => {
  const { players, picks } = fixture()
  const normalized = normalizeLineup({ formation: '4-2-1-3', picks: { ...picks, '0-0': 999, '0-1': 3 } }, players)
  assert.ok(normalized.unassigned.includes(999))
  assert.ok(normalized.unassigned.includes(3))
  assert.ok(normalized.notices.some(notice => notice.includes('trùng')))
  assert.deepEqual(normalizeLineup(normalized, players), normalized)
  const high = players.map(p => ({ ...p, fc27Overall: 85 }))
  const legacy = normalizeLineup({ formation: '4-2-1-3', picks }, high)
  assert.equal(Object.keys(legacy.picks).length, 11)
  assert.match(validateLineup(legacy.formation, legacy.picks, high), /935.*910/)
  assert.match(validateLineup(normalized.formation, normalized.picks, players, normalized.unassigned), /chưa xếp/)
})

test('unassigned picks count toward duplicate and club limits', () => {
  const { players, slots } = fixture()
  assert.match(pickError(players[0], slots[0], {}, players, [players[0].id]), /đã có/)
  assert.match(pickError({ ...players[0], id: 99 }, slots[0], {}, players, [1, 5, 9]), /CLB/)
})
