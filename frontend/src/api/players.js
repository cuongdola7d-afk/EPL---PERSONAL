import { fetchApiList } from './request.js'

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
