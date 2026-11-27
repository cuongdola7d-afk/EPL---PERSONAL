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
  const number = value => value === null || (typeof value === 'number' && Number.isFinite(value))
  const optionalNumber = value => value === undefined || number(value)
  if (!stats || stats.clubId !== id || stats.season !== season || !number(stats.averageRating) ||
      !Number.isInteger(stats.ratedAppearances) || !Number.isInteger(stats.recordedMatches) ||
      !Array.isArray(stats.players) || !stats.players.every(player => Number.isInteger(player.playerId) &&
        Number.isInteger(player.appearances) && Number.isInteger(player.ratedAppearances) &&
        number(player.goals) && number(player.assists) && number(player.averageRating) &&
        optionalNumber(player.minutes) && optionalNumber(player.cleanSheets) &&
        optionalNumber(player.saves) && optionalNumber(player.goalsConceded) &&
        optionalNumber(player.tackles) && optionalNumber(player.clearancesBlocksInterceptions) &&
        optionalNumber(player.recoveries) && optionalNumber(player.expectedGoals))) {
    throw new Error('Dữ liệu thống kê câu lạc bộ không đúng định dạng.')
  }
  return stats
}

export async function fetchClubOverview(id, season, signal) {
  const data = await fetchApiJson(`/api/clubs/${id}/overview?season=${season}`, signal, 'Chưa có tổng quan câu lạc bộ.')
  const validLeader = row => row && Number.isInteger(row.rank) && Number.isInteger(row.playerId) &&
    typeof row.name === 'string' && Number.isInteger(row.appearances) &&
    typeof row.value === 'number' && Number.isFinite(row.value)
  const validXi = player => player && Number.isInteger(player.playerId) &&
    typeof player.name === 'string' && typeof player.shortName === 'string' &&
    typeof player.position === 'string' && Number.isInteger(player.rowIndex) &&
    Number.isInteger(player.slotIndex) && typeof player.x === 'number' &&
    typeof player.y === 'number' && Number.isInteger(player.startsAtPosition)
  if (!data || data.clubId !== id || data.season !== season ||
      (data.formation !== null && typeof data.formation !== 'string') ||
      !Number.isInteger(data.lineupMatches) ||
      !Array.isArray(data.formationCounts) ||
      !Array.isArray(data.xi) || !data.xi.every(validXi) ||
      !Array.isArray(data.topScorers) || !data.topScorers.every(validLeader) ||
      !Array.isArray(data.topAssists) || !data.topAssists.every(validLeader) ||
      !Array.isArray(data.topRatings) || !data.topRatings.every(validLeader)) {
    throw new Error('Dữ liệu tổng quan câu lạc bộ không đúng định dạng.')
  }
  return data
}

export function isValidClubSeasonTrends(data, expectedId = null, expectedSeason = null) {
  const nullableNumber = value => value === null || (typeof value === 'number' && Number.isFinite(value))
  const validMatch = row => row !== null && typeof row === 'object' &&
    Number.isInteger(row.fixtureId) &&
    Number.isInteger(row.matchweek) && row.matchweek >= 1 &&
    (row.kickoffUtc === null || typeof row.kickoffUtc === 'string') &&
    typeof row.home === 'boolean' &&
    Number.isInteger(row.opponentClubId) &&
    typeof row.opponentName === 'string' &&
    Number.isInteger(row.goalsFor) && row.goalsFor >= 0 &&
    Number.isInteger(row.goalsAgainst) && row.goalsAgainst >= 0 &&
    nullableNumber(row.teamRating) &&
    nullableNumber(row.xgFor) &&
    nullableNumber(row.xgAgainst)
  const validRank = row => row !== null && typeof row === 'object' &&
    Number.isInteger(row.matchweek) && row.matchweek >= 1 &&
    Number.isInteger(row.position) && row.position >= 1
  const validScorer = row => row !== null && typeof row === 'object' &&
    Number.isInteger(row.playerId) &&
    typeof row.name === 'string' &&
    Number.isInteger(row.goals) && row.goals >= 0
  const validAssister = row => row !== null && typeof row === 'object' &&
    Number.isInteger(row.playerId) &&
    typeof row.name === 'string' &&
    Number.isInteger(row.assists) && row.assists >= 0

  return data !== null && typeof data === 'object' &&
    Number.isInteger(data.clubId) && (expectedId === null || data.clubId === expectedId) &&
    Number.isInteger(data.season) && (expectedSeason === null || data.season === expectedSeason) &&
    Array.isArray(data.matches) && data.matches.every(validMatch) &&
    Array.isArray(data.rankHistory) && data.rankHistory.every(validRank) &&
    Array.isArray(data.scorers) && data.scorers.every(validScorer) &&
    Array.isArray(data.assisters) && data.assisters.every(validAssister) &&
    Number.isInteger(data.ownGoalsFor) && data.ownGoalsFor >= 0
}

export async function fetchClubSeasonTrends(id, season, signal) {
  const data = await fetchApiJson(`/api/clubs/${id}/season-trends?season=${season}`, signal, 'Chưa có thống kê xu hướng mùa giải.')
  if (!isValidClubSeasonTrends(data, id, season)) {
    throw new Error('Dữ liệu thống kê xu hướng mùa giải không đúng định dạng.')
  }
  return data
}

export async function fetchClubs(keyword, signal, season = 2024) {
  const params = new URLSearchParams({ season })
  if (keyword) params.set('keyword', keyword)
  const path = `/api/clubs${keyword ? '/search' : ''}?${params}`

  return fetchApiList(path, signal, isValidClub, 'câu lạc bộ')
}
