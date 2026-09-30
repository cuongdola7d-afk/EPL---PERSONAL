export const SEASONS = { 2024: '2024/25', 2026: '2026/27' }

export function canOpenMatchStats(season, match) {
  if (match.status !== 'FINISHED') return false
  return (season === 2024 && match.matchweek === 1) ||
    (season === 2026 && match.hasManualStats === true)
}

export function hasMatchScore(match) {
  return !['SCHEDULED', 'POSTPONED', 'CANCELLED'].includes(match.status) &&
    Number.isInteger(match.homeGoals) && Number.isInteger(match.awayGoals)
}
