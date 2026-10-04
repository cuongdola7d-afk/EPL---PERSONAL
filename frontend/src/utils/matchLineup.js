// Independent of Fantasy: these slots describe a match layout, not position eligibility.
export function formationLines(formation) {
  if (typeof formation !== 'string' || !/^[1-5](?:-[1-5]){1,4}$/.test(formation)) return null
  const lines = formation.split('-').map(Number)
  return lines.reduce((sum, count) => sum + count, 0) === 10 ? [1, ...lines] : null
}

// Display labels describe formation slots; player positions and assignment affinities stay separate.
const FORMATION_LABELS = {
  '4-2-3-1': [['GK'], ['LB', 'CB', 'CB', 'RB'], ['CM', 'CM'], ['LW', 'CAM', 'RW'], ['ST']],
  '5-4-1': [['GK'], ['LB', 'CB', 'CB', 'CB', 'RB'], ['LM', 'CM', 'CM', 'RM'], ['ST']],
  '3-4-3': [['GK'], ['CB', 'CB', 'CB'], ['LM', 'CM', 'CM', 'RM'], ['LW', 'ST', 'RW']],
}

function formationSlots(lines) {
  const labels = FORMATION_LABELS[lines.slice(1).join('-')]
  const last = lines.length - 1
  return lines.flatMap((count, rowIndex) => Array.from({ length: count }, (_, slotIndex) => {
    const lane = Math.sign(slotIndex - (count - 1) / 2)
    const edge = count > 1 && (slotIndex === 0 || slotIndex === count - 1)
    const prefix = lane < 0 ? 'L' : lane > 0 ? 'R' : ''
    let role, layoutPosition
    if (rowIndex === 0) { role = 'GK'; layoutPosition = 'GK' }
    else if (rowIndex === 1) {
      role = count >= 4 && edge ? 'FB' : 'CB'
      layoutPosition = role === 'FB' ? `${prefix}${count === 5 ? 'WB' : 'B'}` : `${prefix}CB`
    } else if (rowIndex === last) {
      role = count >= 3 && edge ? 'W' : 'ST'
      layoutPosition = role === 'W' ? `${prefix}W` : count === 1 ? 'ST' : `${prefix}ST`
    } else if (count >= 4 && edge) {
      role = lines[1] === 3 ? 'WB' : 'WM'
      layoutPosition = `${prefix}${role === 'WB' ? 'WB' : 'M'}`
    } else if (lines.length >= 5 && rowIndex === last - 1 && count <= 3) {
      role = 'AM'; layoutPosition = lane === 0 ? 'CAM' : `${prefix}AM`
    } else {
      role = rowIndex === 2 && count <= 2 && lines.length >= 5 ? 'DM' : 'CM'
      layoutPosition = `${prefix}${role === 'DM' ? 'DM' : 'CM'}`
    }
    return { rowIndex, slotIndex, lane, role, layoutPosition: labels?.[rowIndex]?.[slotIndex] ?? layoutPosition }
  }))
}

// Affinity describes an illustration, never a player's Fantasy permissions or verified match position.
const AFFINITIES = {
  CB: { CB: 1000, FB: 350, WB: 250, DM: 400, CM: 100 },
  FB: { FB: 1000, WB: 950, WM: 650, CB: 250, DM: 150, CM: 200 },
  WB: { WB: 1000, FB: 900, WM: 800, CM: 300, DM: 150, CB: 200 },
  DM: { DM: 1000, CM: 850, CB: 400, WB: 250, WM: 150, AM: 100 },
  CM: { CM: 1000, DM: 850, AM: 650, WM: 450, WB: 250 },
  AM: { AM: 1000, W: 850, CM: 700, WM: 650, ST: 350, DM: 200 },
  WM: { WM: 1000, WB: 800, AM: 700, W: 650, CM: 450, FB: 250, DM: 150 },
  W: { W: 1000, AM: 850, WM: 700, ST: 400, WB: 200, CM: 100 },
  ST: { ST: 1000, W: 400, AM: 300, WM: 100 },
  D: { CB: 800, FB: 800, WB: 600, DM: 200 },
  M: { CM: 800, DM: 700, AM: 650, WM: 600, WB: 300, W: 200 },
  F: { ST: 800, W: 800, AM: 400, WM: 200 },
}

function positionAffinity(position, slot) {
  if (position === 'GK' || position === 'G') return slot.role === 'GK' ? 1000 : -Infinity
  if (slot.role === 'GK') return -Infinity
  const left = ['LB', 'LWB', 'LM', 'LW', 'LCB', 'LCM', 'LAM'].includes(position)
  const right = ['RB', 'RWB', 'RM', 'RW', 'RCB', 'RCM', 'RAM'].includes(position)
  if (left && slot.lane > 0 || right && slot.lane < 0) return -Infinity
  const family = ['LB', 'RB'].includes(position) ? 'FB' : ['LWB', 'RWB'].includes(position) ? 'WB' :
    ['LM', 'RM'].includes(position) ? 'WM' : ['LW', 'RW'].includes(position) ? 'W' :
      ['LCB', 'RCB'].includes(position) ? 'CB' : ['LCM', 'RCM'].includes(position) ? 'CM' :
        ['CAM', 'LAM', 'RAM'].includes(position) ? 'AM' : ['CDM', 'LDM', 'RDM'].includes(position) ? 'DM' :
          ['CF', 'LST', 'RST'].includes(position) ? 'ST' : position
  const affinities = AFFINITIES[family]
  if (!affinities) return 0
  const score = affinities[slot.role]
  if (score === undefined) return -Infinity
  return score + (left || right ? slot.lane !== 0 ? 40 : 0 : ['CB', 'CM', 'DM', 'AM', 'ST'].includes(family) && slot.lane === 0 ? 40 : 0)
}

function illustrationScore(player, slot) {
  const primary = player.matchPosition ?? player.seasonPosition ?? player.position
  const eligible = !player.matchPosition && Array.isArray(player.seasonEligiblePositions) ? player.seasonEligiblePositions : []
  let score = positionAffinity(primary, slot)
  for (const position of eligible) {
    if (position !== primary) score = Math.max(score, positionAffinity(position, slot) - 120)
  }
  if (!Number.isFinite(score)) return score
  const left = eligible.some(position => ['LB', 'LWB', 'LM', 'LW'].includes(position))
  const right = eligible.some(position => ['RB', 'RWB', 'RM', 'RW'].includes(position))
  return score + (left !== right && slot.lane === (left ? -1 : 1) ? 25 : 0)
}

function smallerIds(first, second) {
  for (let index = 0; index < first.length; index++) {
    const a = first[index]?.playerId ?? Infinity, b = second[index]?.playerId ?? Infinity
    if (a !== b) return a < b
  }
  return false
}

// Assign the confirmed XI to illustrative slots, without asserting actual match positions.
function illustrate(players, slots) {
  const full = (1 << slots.length) - 1
  const states = new Map([[0, { score: 0, players: [] }]])
  for (const player of [...players].sort((a, b) => a.playerId - b.playerId)) {
    const next = new Map()
    for (const [mask, state] of states) for (let index = 0; index < slots.length; index++) {
      if (mask & (1 << index)) continue
      const key = mask | (1 << index)
      const affinity = illustrationScore(player, slots[index])
      if (!Number.isFinite(affinity)) continue
      const score = state.score + affinity
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
  const slots = formationSlots(lines)
  const occupied = new Map()
  const anchored = new Set()
  if (evidence.formationSource === 'FIXTURE') for (const player of starters) {
    if (!player.matchPosition || !Number.isInteger(player.rowIndex) || !Number.isInteger(player.slotIndex)) continue
    const index = slots.findIndex(slot => slot.rowIndex === player.rowIndex && slot.slotIndex === player.slotIndex)
    if (index < 0 || occupied.has(index)) return { ...result, positionSource: 'INVALID' }
    occupied.set(index, player); anchored.add(player.playerId)
  }
  const free = slots.map((slot, index) => ({ ...slot, index })).filter(slot => !occupied.has(slot.index))
  const assigned = illustrate(starters.filter(player => !anchored.has(player.playerId)), free)
  if (assigned.length !== free.length) return { ...result, positionSource: 'INSUFFICIENT_POSITIONS' }
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
