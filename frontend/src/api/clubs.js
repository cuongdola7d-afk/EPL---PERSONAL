import { fetchApiList, fetchApiJson } from './request.js'

function isValidClub(club) {
  return club !== null &&
    typeof club.id === 'number' &&
    typeof club.name === 'string' &&
    typeof club.city === 'string'
}

export async function fetchClub(id, season, signal) {
  const club = await fetchApiJson(`/api/clubs/${id}?season=${season}`, signal, 'Không tìm thấy câu lạc bộ trong mùa đã chọn.')
  if (!isValidClub(club)) throw new Error('Dữ liệu câu lạc bộ không đúng định dạng.')
  return club
}

export async function fetchClubStatistics(id, season, signal) {
  const stats = await fetchApiJson(`/api/clubs/${id}/statistics?season=${season}`, signal, 'Chưa có thống kê câu lạc bộ.')
  const number = value => value === null || typeof value === 'number' && Number.isFinite(value)
  if (!stats || stats.clubId !== id || stats.season !== season || !number(stats.averageRating) ||
      !Number.isInteger(stats.ratedAppearances) || !Number.isInteger(stats.recordedMatches) ||
      !Array.isArray(stats.players) || !stats.players.every(player => Number.isInteger(player.playerId) &&
        Number.isInteger(player.appearances) && Number.isInteger(player.ratedAppearances) &&
        number(player.goals) && number(player.assists) && number(player.averageRating))) {
    throw new Error('Dữ liệu thống kê câu lạc bộ không đúng định dạng.')
  }
  return stats
}

export async function fetchClubs(keyword, signal, season = 2024) {
  const params = new URLSearchParams({ season })
  if (keyword) params.set('keyword', keyword)
  const path = `/api/clubs${keyword ? '/search' : ''}?${params}`

  return fetchApiList(path, signal, isValidClub, 'câu lạc bộ')
}
