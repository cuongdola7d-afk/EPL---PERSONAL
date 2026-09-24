import { fetchApiList } from './request.js'

function isValidClub(club) {
  return club !== null &&
    typeof club.id === 'number' &&
    typeof club.name === 'string' &&
    typeof club.city === 'string'
}

export async function fetchClubs(keyword, signal) {
  const path = keyword
    ? `/api/clubs/search?${new URLSearchParams({ keyword })}`
    : '/api/clubs'

  return fetchApiList(path, signal, isValidClub, 'câu lạc bộ')
}
