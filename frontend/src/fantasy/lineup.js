export const FANTASY_STORAGE_KEY = 'premierhub:fantasy:2026:lineup'
export const FANTASY_AS_OF = '2026-10-02'
export const MAX_OVR = 910
// OVR 72 của Dowman là mức ước tính do người dùng chọn; không phải rating EA đã xác minh.
export const ESTIMATED_OVR_PLAYER_IDS = new Set([2000001025])

export const FORMATIONS = {
  '4-2-1-3': [['LW', 'ST', 'RW'], ['CAM'], ['LCM', 'RCM'], ['LB', 'LCB', 'RCB', 'RB'], ['GK']],
  '4-3-3': [['LW', 'ST', 'RW'], ['LCM', 'CM', 'RCM'], ['LB', 'LCB', 'RCB', 'RB'], ['GK']],
  '4-4-2': [['ST', 'ST'], ['LM', 'LCM', 'RCM', 'RM'], ['LB', 'LCB', 'RCB', 'RB'], ['GK']],
  '3-5-2': [['ST', 'ST'], ['LM', 'LCM', 'CM', 'RCM', 'RM'], ['LCB', 'CB', 'RCB'], ['GK']],
}

export const GROUP = {
  GK: 'GOALKEEPER', LB: 'DEFENDER', LCB: 'DEFENDER', CB: 'DEFENDER',
  RCB: 'DEFENDER', RB: 'DEFENDER', LM: 'MIDFIELDER', LCM: 'MIDFIELDER',
  CM: 'MIDFIELDER', RCM: 'MIDFIELDER', RM: 'MIDFIELDER', CAM: 'MIDFIELDER',
  LW: 'FORWARD', ST: 'FORWARD', RW: 'FORWARD',
}

export const GROUP_LABEL = {
  GOALKEEPER: 'Thủ môn', DEFENDER: 'Hậu vệ', MIDFIELDER: 'Tiền vệ', FORWARD: 'Tiền đạo',
}

export function formationSlots(formation) {
  return FORMATIONS[formation].flatMap((row, rowIndex) => row.map((position, columnIndex) => ({
    key: `${rowIndex}-${columnIndex}`,
    position,
    group: GROUP[position],
    x: 50 + ((columnIndex + 1) / (row.length + 1) - 0.5) *
      (row.length >= 5 ? 120 : row.length >= 4 ? 112 : row.length === 3 ? 100 : 80),
    y: 12 + rowIndex * (76 / (FORMATIONS[formation].length - 1)),
  })))
}

export function slotPosition(position) {
  return ({ LCB: 'CB', RCB: 'CB', LCM: 'CM', RCM: 'CM' })[position] ?? position
}

export function playerDataError(player) {
  if (!player) return 'Cầu thủ không còn trong roster hiện hành.'
  if (!Number.isInteger(player.fc27Overall) || player.fc27Overall < 1 || player.fc27Overall > 99)
    return 'Cầu thủ chưa có OVR hợp lệ nên chưa thể chọn Fantasy.'
  if (!player.primaryPosition || !Array.isArray(player.eligiblePositions) ||
      !player.eligiblePositions.includes(player.primaryPosition))
    return 'Cầu thủ chưa có dữ liệu vị trí hợp lệ.'
  return ''
}

export function fitsSlot(player, slot) {
  return !playerDataError(player) && Boolean(slot) &&
    player.eligiblePositions.includes(slotPosition(slot.position))
}

// Maximum bipartite matching: move a flexible player to free a slot for another.
// Unmatched IDs stay available for review, including players no longer in the roster.
export function movePicks(oldFormation, newFormation, picks, players, unassigned = []) {
  const ids = [...new Set([...formationSlots(oldFormation).map(slot => picks[slot.key]),
    ...Object.values(picks), ...unassigned].filter(id => id != null))]
  const byId = new Map(players.map(player => [player.id, player]))
  const slots = formationSlots(newFormation)
  const assigned = new Map()
  function place(id, visited) {
    for (const slot of slots) {
      if (visited.has(slot.key) || !fitsSlot(byId.get(id), slot)) continue
      visited.add(slot.key)
      if (!assigned.has(slot.key) || place(assigned.get(slot.key), visited)) {
        assigned.set(slot.key, id)
        return true
      }
    }
    return false
  }
  ids.forEach(id => place(id, new Set()))
  const placed = new Set(assigned.values())
  return { formation: newFormation, picks: Object.fromEntries(assigned),
    unassigned: ids.filter(id => !placed.has(id)) }
}

export function pickError(player, slot, picks, players, unassigned = []) {
  const dataError = playerDataError(player)
  if (dataError) return dataError
  if (!fitsSlot(player, slot)) return `Cầu thủ không được chơi ô ${slotPosition(slot?.position)}.`
  const ids = [...Object.entries(picks).filter(([key]) => key !== slot.key).map(([, id]) => id), ...unassigned]
  if (ids.includes(player.id)) return 'Cầu thủ đã có trong đội hình.'
  if (ids.length >= 11) return 'Đã giữ 11 cầu thủ; hãy bỏ người cần thay trước.'
  const others = ids.map(id => players.find(candidate => candidate.id === id)).filter(Boolean)
  if (others.filter(candidate => candidate.clubId === player.clubId).length >= 3)
    return 'Tối đa 3 cầu thủ từ một CLB.'
  const total = others.reduce((sum, candidate) => sum + (candidate.fc27Overall ?? 0), player.fc27Overall)
  if (total > MAX_OVR) return `Tổng OVR ${total} vượt giới hạn ${MAX_OVR}.`
  return ''
}

export function lineupIssues(formation, picks, players, unassigned = []) {
  if (!FORMATIONS[formation]) return ['Sơ đồ không được hỗ trợ.']
  const issues = []
  const slots = formationSlots(formation)
  const seen = new Set(), clubs = new Map()
  let total = 0
  for (const [key, id] of Object.entries(picks)) {
    const slot = slots.find(candidate => candidate.key === key)
    const player = players.find(candidate => candidate.id === id)
    const label = `${slot?.position ?? key}: ${player?.name ?? `#${id}`}`
    const dataError = playerDataError(player)
    if (dataError) issues.push(`${label} — ${dataError}`)
    else if (!fitsSlot(player, slot)) issues.push(`${label} — không được chơi ô này.`)
    if (seen.has(id)) issues.push(`${label} — chọn trùng cầu thủ.`)
    seen.add(id)
    if (player) {
      total += player.fc27Overall ?? 0
      clubs.set(player.clubId, (clubs.get(player.clubId) ?? 0) + 1)
      if (clubs.get(player.clubId) === 4) issues.push(`${player.club ?? player.clubId} — vượt tối đa 3 cầu thủ/CLB.`)
    }
  }
  for (const id of unassigned) {
    const player = players.find(candidate => candidate.id === id)
    if (!player || seen.has(id)) continue
    seen.add(id)
    total += player.fc27Overall ?? 0
    clubs.set(player.clubId, (clubs.get(player.clubId) ?? 0) + 1)
    if (clubs.get(player.clubId) === 4) issues.push(`${player.club ?? player.clubId} — vượt tối đa 3 cầu thủ/CLB.`)
  }
  if (total > MAX_OVR) issues.push(`Tổng OVR ${total} vượt giới hạn ${MAX_OVR}.`)
  if (unassigned.length) issues.push('Có cầu thủ cần thay hoặc chưa xếp được vào sơ đồ.')
  if (slots.some(slot => picks[slot.key] == null) || Object.keys(picks).length !== 11)
    issues.push('Chọn đủ 11 cầu thủ ở đúng các ô để kiểm tra đội hình.')
  return issues
}

export function validateLineup(formation, picks, players, unassigned = []) {
  return lineupIssues(formation, picks, players, unassigned).join(' ')
}

export function normalizeLineup(saved, players) {
  const formation = FORMATIONS[saved?.formation] ? saved.formation : '4-2-1-3'
  const picks = {}, unassigned = [], seen = new Set()
  const notices = Array.isArray(saved?.notices) ? saved.notices.filter(value => typeof value === 'string') : []
  if (saved?.formation && !FORMATIONS[saved.formation]) notices.push('Sơ đồ đã lưu không được hỗ trợ; hãy kiểm tra lại lựa chọn.')
  for (const [key, id] of Object.entries(saved?.picks && typeof saved.picks === 'object' ? saved.picks : {})) {
    if (seen.has(id)) { notices.push(`Lựa chọn đã lưu bị trùng cầu thủ #${id}; hãy kiểm tra lại.`); continue }
    seen.add(id)
    const player = players.find(candidate => candidate.id === id)
    if (fitsSlot(player, formationSlots(formation).find(slot => slot.key === key))) picks[key] = id
    else unassigned.push(id)
  }
  for (const id of Array.isArray(saved?.unassigned) ? saved.unassigned : []) {
    if (!seen.has(id)) { seen.add(id); unassigned.push(id) }
  }
  return { formation, picks, unassigned, notices: [...new Set(notices)] }
}
