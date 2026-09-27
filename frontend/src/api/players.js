import { fetchApiJson, fetchApiList } from './request.js'
import { isValidMatch } from './matches.js'

export function isValidPlayer(player) {
  return player !== null &&
    typeof player.id === 'number' &&
    typeof player.name === 'string' &&
    typeof player.clubId === 'number' &&
    typeof player.club === 'string' &&
    typeof player.position === 'string' &&
    (player.goals === null || typeof player.goals === 'number') &&
    (player.assists === null || typeof player.assists === 'number')
}

export function playerPath(filters, season) {
  const params = new URLSearchParams({ season })
  if (filters.club) params.set('club', filters.club)
  if (filters.position) params.set('position', filters.position)

  return `/api/players?${params}`
}

export function fetchPlayers(filters, signal, season = 2024) {
  return fetchApiList(playerPath(filters, season), signal, isValidPlayer, 'cầu thủ')
}

export async function fetchPlayer(id, season, signal) {
  const player = await fetchApiJson(`/api/players/${id}?season=${season}`, signal, 'Không tìm thấy cầu thủ này trong mùa đã chọn.')
  if (!isValidPlayer(player)) throw new Error('Dữ liệu cầu thủ từ API không đúng định dạng.')
  return player
}

export function fetchPlayerMatches(id, season, signal) {
  return fetchApiList(`/api/players/${id}/matches?season=${season}`, signal,
    (row) => row !== null && isValidMatch(row.match) && Number.isInteger(row.clubId) &&
      (row.stats === null || (row.stats !== null && Number.isInteger(row.stats.playerId) &&
        ['minutes', 'goals', 'assists', 'yellowCards', 'redCards'].every((field) =>
          row.stats[field] === null || Number.isInteger(row.stats[field])) &&
        (row.stats.rating === null || typeof row.stats.rating === 'string'))),
    'lịch sử trận đấu')
}
