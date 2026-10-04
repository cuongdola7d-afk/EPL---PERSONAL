// Independent of Fantasy: these slots describe a match layout, not position eligibility.
export function formationLines(formation) {
  if (typeof formation !== 'string' || !/^[1-5](?:-[1-5]){1,4}$/.test(formation)) return null
  const lines = formation.split('-').map(Number)
  return lines.reduce((sum, count) => sum + count, 0) === 10 ? [1, ...lines] : null
}

function illustrationScore(player, slot, lines) {
  const position = player.matchPosition ?? player.seasonPosition ?? player.position
  const last = lines.length - 1
  if (position === 'GK' || position === 'G') return slot.rowIndex === 0 ? 1000 : -1000
  if (slot.rowIndex === 0) return -1000
  const target = ['CB', 'LB', 'RB', 'D'].includes(position) ? 1 :
    ['LWB', 'RWB'].includes(position) ? lines[1] === 3 ? 2 : 1 :
      ['ST', 'CF', 'F'].includes(position) ? last :
        ['LW', 'RW', 'LM', 'RM'].includes(position) ? lines[last] >= 3 ? last : last - 1 :
          ['CAM', 'AM'].includes(position) ? last - 1 : ['CM', 'CDM', 'DM', 'M'].includes(position) ? 2 : null
  return target === null ? 0 : 100 - Math.abs(target - slot.rowIndex) * 30
}

function smallerIds(first, second) {
  for (let index = 0; index < first.length; index++) {
    const a = first[index]?.playerId ?? Infinity, b = second[index]?.playerId ?? Infinity
    if (a !== b) return a < b
  }
  return false
}

// Assign the confirmed XI to illustrative slots, without asserting actual match positions.
function illustrate(players, slots, lines) {
  const full = (1 << slots.length) - 1
  const states = new Map([[0, { score: 0, players: [] }]])
  for (const player of [...players].sort((a, b) => a.playerId - b.playerId)) {
    const next = new Map()
    for (const [mask, state] of states) for (let index = 0; index < slots.length; index++) {
      if (mask & (1 << index)) continue
      const key = mask | (1 << index)
      const score = state.score + illustrationScore(player, slots[index], lines)
      const assignment = state.players.slice(); assignment[index] = player
      const previous = next.get(key)
      if (!previous || score > previous.score || score === previous.score && smallerIds(assignment, previous.players)) {
        next.set(key, { score, players: assignment })
      }
    }
    states.clear(); next.forEach((state, mask) => states.set(mask, state))
  }
  return states.get(full)?.players ?? []
}

export function matchLineup(players, evidence, side) {
  const missing = { formation: null, formationSource: 'MISSING', startersStatus: 'MISSING',
    starters: [], substitutes: [], bench: [], unknown: players, nodes: [], positionSource: null }
  const formation = formationLines(evidence?.formation) ? evidence.formation : null
  const metadata = evidence?.players ?? []
  const byId = new Map(metadata.map(player => [player.playerId, player]))
  const ids = new Set(players.map(player => player.playerId))
  const valid = evidence?.startersStatus === 'VERIFIED' && byId.size === metadata.length && ids.size === players.length &&
    byId.size === ids.size && metadata.every(player => ids.has(player.playerId) && ['STARTER', 'SUB_USED', 'SUB_UNUSED'].includes(player.role)) &&
    metadata.filter(player => player.role === 'STARTER').length === 11 &&
    players.every(player => player.participationStatus === (byId.get(player.playerId)?.role === 'SUB_UNUSED' ? 'DID_NOT_PLAY' : 'PLAYED'))
  if (!valid) return { ...missing, formation, formationSource: formation ? evidence.formationSource : 'MISSING',
    startersStatus: evidence?.startersStatus === 'VERIFIED' ? 'INCOMPLETE' : evidence?.startersStatus ?? 'MISSING' }
  const joined = players.map(player => ({ ...player, ...byId.get(player.playerId) }))
  const starters = joined.filter(player => player.role === 'STARTER')
  const result = { formation, formationSource: formation ? evidence.formationSource : 'MISSING', startersStatus: 'VERIFIED',
    starters, substitutes: joined.filter(player => player.role === 'SUB_USED'), bench: joined.filter(player => player.role === 'SUB_UNUSED'),
    unknown: [], nodes: [], positionSource: null }
  if (!formation) return result
  const lines = formationLines(formation)
  const slots = lines.flatMap((count, rowIndex) => Array.from({ length: count }, (_, slotIndex) => ({ rowIndex, slotIndex })))
  const occupied = new Map()
  const anchored = new Set()
  if (evidence.formationSource === 'FIXTURE') for (const player of starters) {
    if (!player.matchPosition || !Number.isInteger(player.rowIndex) || !Number.isInteger(player.slotIndex)) continue
    const index = slots.findIndex(slot => slot.rowIndex === player.rowIndex && slot.slotIndex === player.slotIndex)
    if (index < 0 || occupied.has(index)) return { ...result, positionSource: 'INVALID' }
    occupied.set(index, player); anchored.add(player.playerId)
  }
  const free = slots.map((slot, index) => ({ ...slot, index })).filter(slot => !occupied.has(slot.index))
  const assigned = illustrate(starters.filter(player => !anchored.has(player.playerId)), free, lines)
  free.forEach((slot, index) => occupied.set(slot.index, assigned[index]))
  result.positionSource = anchored.size === 11 ? 'MATCH' : 'ILLUSTRATION'
  result.nodes = slots.map((slot, index) => {
    const player = occupied.get(index)
    const along = 7 + slot.rowIndex * 36 / (lines.length - 1)
    const across = (slot.slotIndex + 1) * 100 / (lines[slot.rowIndex] + 1)
    return { player, ...slot, x: side === 'home' ? along : 100 - along,
      y: side === 'home' ? across : 100 - across, positionSource: anchored.has(player.playerId) ? 'MATCH' : 'ILLUSTRATION' }
  })
  return result
}
