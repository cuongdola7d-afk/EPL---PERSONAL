export const SEASONS = { 2024: '2024/25', 2026: '2026/27' }

// Only the imported 2024 GW1 snapshot currently includes player-match statistics.
export function canOpenMatchStats(season, match) {
  return season === 2024 && match.matchweek === 1 && match.status === 'FINISHED'
}

export function hasMatchScore(match) {
  return !['SCHEDULED', 'POSTPONED', 'CANCELLED'].includes(match.status) &&
    Number.isInteger(match.homeGoals) && Number.isInteger(match.awayGoals)
}
