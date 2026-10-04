import { compareMatchSchedule, matchDayKey } from './matchTime.js'

export const FOOT_LABEL = { LEFT: 'Chân trái', RIGHT: 'Chân phải', BOTH: 'Hai chân' }
export const valueLabel = value => value == null ? '—' : String(value)
export const ratingClass = value => value == null ? 'na' : value >= 9 ? 'blue' : value >= 7 ? 'green' : value >= 5 ? 'orange' : 'red'

export function standingBand(position, season, size = 20) {
  if (season !== 2026) return ''
  return position >= 1 && position <= 5 ? 'rank-champions' : position <= 7 && position >= 6 ?
    'rank-europa' : position > size - 3 && position <= size ? 'rank-relegation' : ''
}

export function clubMatchResult(match, clubId) {
  if (match.status !== 'FINISHED' || match.homeGoals == null || match.awayGoals == null) return null
  const home = match.homeClubId === clubId
  const own = home ? match.homeGoals : match.awayGoals
  const other = home ? match.awayGoals : match.homeGoals
  return own > other ? 'W' : own < other ? 'L' : 'D'
}

export function clubMatches(matches, clubId, today, season = 2026) {
  const own = matches.filter(match => match.homeClubId === clubId || match.awayClubId === clubId)
  const done = own.filter(match => match.status === 'FINISHED')
    .sort((a,b) => compareMatchSchedule(b, a, season))
  const soon = own.filter(match => ['SCHEDULED', 'LIVE'].includes(match.status) && matchDayKey(match, season) >= today)
    .sort((a,b) => compareMatchSchedule(a, b, season))
  const pending = own.filter(match => match.status !== 'FINISHED' && !['CANCELLED', 'AWARDED'].includes(match.status))
    .sort((a,b) => compareMatchSchedule(a, b, season))
  return { done, soon, pending }
}

export function clubRanking(roster, statistics, metric) {
  const field = metric === 'rating' ? 'averageRating' : metric
  const byId = new Map((statistics?.players ?? []).map(row => [row.playerId, row]))
  return roster.flatMap(player => {
    const stats = byId.get(player.id)
    return stats && stats[field] != null && stats.appearances > 0 ? [{ player, stats, value: stats[field] }] : []
  }).sort((a,b) => b.value-a.value || a.player.name.localeCompare(b.player.name))
}
