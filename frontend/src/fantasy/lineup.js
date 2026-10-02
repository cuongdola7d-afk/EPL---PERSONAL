export const FANTASY_STORAGE_KEY = 'premierhub:fantasy:2026:lineup'
export const FANTASY_AS_OF = '2026-10-02'
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

export function movePicks(oldFormation, newFormation, picks) {
  const byGroup = new Map(Object.values(GROUP).map((group) => [group, []]))
  formationSlots(oldFormation).forEach((slot) => {
    if (picks[slot.key] != null) byGroup.get(slot.group).push(picks[slot.key])
  })
  return Object.fromEntries(formationSlots(newFormation).flatMap((slot) => {
    const next = byGroup.get(slot.group).shift()
    return next == null ? [] : [[slot.key, next]]
  }))
}

export function pickError(player, slot, picks, players) {
  if (!player || !slot || player.position !== slot.group) return 'Cầu thủ không đúng vị trí.'
  if (ESTIMATED_OVR_PLAYER_IDS.has(player.id)) return 'OVR của cầu thủ là ước tính, chưa thể chọn Fantasy.'
  if (player.fc27Overall == null) return 'Cầu thủ chưa có OVR nên chưa thể chọn Fantasy.'
  const others = Object.entries(picks).filter(([key]) => key !== slot.key).map(([, id]) =>
    players.find((candidate) => candidate.id === id)).filter(Boolean)
  if (others.some((candidate) => candidate.id === player.id)) return 'Cầu thủ đã có trong đội hình.'
  if (others.filter((candidate) => candidate.clubId === player.clubId).length >= 3) {
    return 'Tối đa 3 cầu thủ từ một CLB.'
  }
  return ''
}

export function validateLineup(formation, picks, players) {
  const slots = formationSlots(formation)
  if (Object.keys(picks).length !== 11) return 'Chọn đủ 11 cầu thủ để xem kết quả.'
  for (const slot of slots) {
    const player = players.find((candidate) => candidate.id === picks[slot.key])
    if (!player || pickError(player, slot, Object.fromEntries(Object.entries(picks)
      .filter(([key]) => key !== slot.key)), players)) return 'Đội hình có cầu thủ thiếu OVR, sai vị trí hoặc vượt giới hạn CLB.'
  }
  return ''
}

export function normalizeLineup(saved, players) {
  const formation = FORMATIONS[saved?.formation] ? saved.formation : '4-2-1-3'
  const slots = formationSlots(formation)
  const picks = {}
  for (const slot of slots) {
    const player = players.find((candidate) => candidate.id === saved?.picks?.[slot.key])
    if (player && !pickError(player, slot, picks, players)) picks[slot.key] = player.id
  }
  return { formation, picks }
}
