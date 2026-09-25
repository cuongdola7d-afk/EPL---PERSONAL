export const REPLAY_STORAGE_KEY = 'premierhub:replay:2024:gw1'

export const POSITION_LABELS = { G: 'Thủ môn', D: 'Hậu vệ', M: 'Tiền vệ', F: 'Tiền đạo' }
export const POSITION_LIMITS = { G: [1, 1], D: [3, 5], M: [2, 5], F: [1, 3] }

export function buildReplayPool(matches, details) {
  if (matches.length !== 10 || details.length !== 10 ||
      new Set(matches.map((match) => match.id)).size !== 10 ||
      matches.some((match) => match.matchweek !== 1 || match.status !== 'FINISHED')) {
    throw new Error('Cần đủ 10 trận đã kết thúc của Gameweek 1 để chơi Replay.')
  }

  const matchIds = new Set(matches.map((match) => match.id))
  const players = []
  const seen = new Set()
  for (const detail of details) {
    const match = detail.match
    const rows = [...detail.homePlayers, ...detail.awayPlayers]
    if (!matchIds.has(match.id) || detail.evidenceStatus !== 'VERIFIED' || rows.length !== 40 ||
        rows.some((player) => player.score.status !== 'COMPLETE' ||
          !Number.isInteger(player.score.confirmedPoints))) {
      throw new Error(`Trận #${match.id} chưa đủ 40 điểm đã xác minh.`)
    }
    matchIds.delete(match.id)
    for (const player of rows) {
      if (seen.has(player.playerId) || !POSITION_LABELS[player.position]) {
        throw new Error(`Cầu thủ #${player.playerId} bị trùng hoặc thiếu vị trí trong GW1.`)
      }
      const clubName = player.clubId === match.homeClubId ? match.homeClub
        : player.clubId === match.awayClubId ? match.awayClub : null
      if (!clubName) throw new Error(`CLB của cầu thủ #${player.playerId} không khớp trận.`)
      seen.add(player.playerId)
      players.push({ id: player.playerId, name: player.playerName, clubId: player.clubId,
        clubName, position: player.position, points: player.score.confirmedPoints })
    }
  }
  if (matchIds.size !== 0) throw new Error('Thiếu chi tiết của một trận Gameweek 1.')
  return players.sort((a, b) => a.name.localeCompare(b.name, 'vi'))
}

export function normalizeSavedIds(ids, players) {
  if (!Array.isArray(ids)) return []
  const available = new Set(players.map((player) => player.id))
  return [...new Set(ids.filter((id) => Number.isInteger(id) && available.has(id)))].slice(0, 11)
}

export function additionError(ids, player, players) {
  if (!player) return 'Cầu thủ không thuộc dữ liệu Replay đã xác minh.'
  if (ids.includes(player.id)) return 'Cầu thủ này đã có trong đội hình.'
  if (ids.length >= 11) return 'Đội hình chỉ có 11 cầu thủ.'
  const selected = ids.map((id) => players.find((item) => item.id === id))
  if (selected.filter((item) => item?.position === player.position).length >= POSITION_LIMITS[player.position][1]) {
    return `Đã đạt giới hạn ${POSITION_LABELS[player.position].toLocaleLowerCase('vi')}.`
  }
  if (selected.filter((item) => item?.clubId === player.clubId).length >= 3) {
    return 'Tối đa 3 cầu thủ từ một CLB.'
  }
  return ''
}

export function lineupError(ids, players) {
  if (ids.length !== 11) return `Cần chọn đủ 11 cầu thủ (hiện có ${ids.length}).`
  if (new Set(ids).size !== 11) return 'Đội hình có cầu thủ bị chọn trùng.'
  const byId = new Map(players.map((player) => [player.id, player]))
  const selected = ids.map((id) => byId.get(id))
  if (selected.some((player) => !player)) return 'Đội hình có cầu thủ không thuộc GW1 đã xác minh.'
  for (const [position, [min, max]] of Object.entries(POSITION_LIMITS)) {
    const count = selected.filter((player) => player.position === position).length
    if (count < min || count > max) return `${POSITION_LABELS[position]} cần từ ${min} đến ${max} người (hiện có ${count}).`
  }
  const clubs = new Map()
  for (const player of selected) {
    clubs.set(player.clubId, (clubs.get(player.clubId) ?? 0) + 1)
    if (clubs.get(player.clubId) > 3) return 'Tối đa 3 cầu thủ từ một CLB.'
  }
  return ''
}

export function replayTotal(ids, players) {
  const error = lineupError(ids, players)
  if (error) throw new Error(error)
  const byId = new Map(players.map((player) => [player.id, player]))
  return ids.reduce((sum, id) => sum + byId.get(id).points, 0)
}
