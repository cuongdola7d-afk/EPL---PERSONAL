import { fetchApiJson, fetchApiList } from './request.js'

function isValidMatch(match) {
  return match !== null &&
    Number.isInteger(match.id) &&
    Number.isInteger(match.homeClubId) &&
    typeof match.homeClub === 'string' &&
    Number.isInteger(match.awayClubId) &&
    typeof match.awayClub === 'string' &&
    Number.isInteger(match.matchweek) &&
    typeof match.date === 'string' &&
    typeof match.status === 'string' &&
    (match.homeGoals === null || Number.isInteger(match.homeGoals)) &&
    (match.awayGoals === null || Number.isInteger(match.awayGoals))
}

export function fetchMatches(filters, signal) {
  const params = new URLSearchParams()
  if (filters.club) params.set('club', filters.club)
  if (filters.matchweek) params.set('matchweek', filters.matchweek)
  if (filters.status) params.set('status', filters.status)

  const query = params.toString()
  return fetchApiList(`/api/matches${query ? `?${query}` : ''}`, signal, isValidMatch, 'trận đấu')
}

export async function fetchMatchDetail(id, signal) {
  const detail = await fetchApiJson(`/api/matches/${id}/details`, signal, 'Không tìm thấy trận đấu này.')
  const isValidPlayer = (player) => player !== null && Number.isInteger(player.playerId) &&
    typeof player.playerName === 'string' && Number.isInteger(player.clubId)
  if (!detail || !isValidMatch(detail.match) ||
      !Array.isArray(detail.homePlayers) || !detail.homePlayers.every(isValidPlayer) ||
      !Array.isArray(detail.awayPlayers) || !detail.awayPlayers.every(isValidPlayer)) {
    throw new Error('Dữ liệu chi tiết trận đấu từ API không đúng định dạng.')
  }
  return detail
}
