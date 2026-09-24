import { fetchApiList } from './request.js'

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
