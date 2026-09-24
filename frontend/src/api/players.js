import { fetchApiList } from './request.js'

function isValidPlayer(player) {
  return player !== null &&
    typeof player.id === 'number' &&
    typeof player.name === 'string' &&
    typeof player.clubId === 'number' &&
    typeof player.club === 'string' &&
    typeof player.position === 'string' &&
    typeof player.goals === 'number' &&
    typeof player.assists === 'number'
}

export function fetchPlayers(filters, signal) {
  const params = new URLSearchParams()
  if (filters.club) params.set('club', filters.club)
  if (filters.position) params.set('position', filters.position)

  const query = params.toString()
  const path = `/api/players${query ? `?${query}` : ''}`
  return fetchApiList(path, signal, isValidPlayer, 'cầu thủ')
}
