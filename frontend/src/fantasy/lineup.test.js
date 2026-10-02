import test from 'node:test'
import assert from 'node:assert/strict'
import { formationSlots, movePicks, normalizeLineup, pickError, validateLineup } from './lineup.js'

function fixture() {
  const slots = formationSlots('4-2-1-3')
  const players = slots.map((slot, index) => ({ id: index + 1, position: slot.group,
    clubId: index % 4, fc27Overall: 80 }))
  const picks = Object.fromEntries(slots.map((slot, index) => [slot.key, players[index].id]))
  return { slots, players, picks }
}

test('valid 11-player formation requires matching positions and at most three per club', () => {
  const { slots, players, picks } = fixture()
  assert.equal(validateLineup('4-2-1-3', picks, players), '')
  assert.match(validateLineup('4-2-1-3', { ...picks, [slots[0].key]: picks[slots[1].key] }, players), /sai vị trí|giới hạn|thiếu OVR/i)
  assert.match(validateLineup('4-2-1-3', { ...picks, [slots[0].key]: undefined }, players), /thiếu OVR|sai vị trí|giới hạn/i)
})

test('missing or estimated OVR stays in roster but is not eligible for Fantasy', () => {
  const { slots, players } = fixture()
  assert.match(pickError({ ...players[0], fc27Overall: null }, slots[0], {}, players), /chưa có OVR/)
  assert.match(pickError({ ...players[0], id: 2000001025, fc27Overall: 72 }, slots[0], {}, players), /ước tính/)
  assert.equal(normalizeLineup({ formation: '4-2-1-3', picks: { [slots[0].key]: 2000001025 } },
    [...players, { ...players[0], id: 2000001025, fc27Overall: 72 }]).picks[slots[0].key], undefined)
})

test('formation changes preserve selected players by position group', () => {
  const { slots, players, picks } = fixture()
  const moved = movePicks('4-2-1-3', '4-3-3', picks)
  assert.equal(Object.keys(moved).length, 11)
  assert.equal(formationSlots('4-3-3').filter((slot) => moved[slot.key] != null).length, 11)
  assert.equal(normalizeLineup({ formation: '4-3-3', picks: moved }, players).formation, '4-3-3')
})
