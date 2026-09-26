import { fetchApiList } from './request.js'

function isValidClub(club) {
  return club !== null &&
    typeof club.id === 'number' &&
    typeof club.name === 'string' &&
    typeof club.city === 'string'
}

export async function fetchClubs(keyword, signal, season = 2024) {
  const params = new URLSearchParams({ season })
  if (keyword) params.set('keyword', keyword)
  const path = `/api/clubs${keyword ? '/search' : ''}?${params}`

  return fetchApiList(path, signal, isValidClub, 'câu lạc bộ')
}
