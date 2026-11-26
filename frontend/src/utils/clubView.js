import { compareMatchSchedule, matchDayKey, matchDayLabel, matchKickoffParts } from './matchTime.js'

const SHORT_WEEKDAY = {
  'Chủ nhật': 'CN',
  'Thứ Hai': 'T2',
  'Thứ Ba': 'T3',
  'Thứ Tư': 'T4',
  'Thứ Năm': 'T5',
  'Thứ Sáu': 'T6',
  'Thứ Bảy': 'T7',
}

const RESULT_TEXT = { W: 'Thắng', D: 'Hòa', L: 'Thua' }

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

export function formatFormTooltip(match, clubId, season = 2026) {
  if (!match) return null
  const isHome = match.homeClubId === clubId
  const venue = isHome ? 'Nhà' : 'Khách'
  const header = `GW${match.matchweek} · ${venue}`
  const hasScore = match.homeGoals != null && match.awayGoals != null
  const scoreline = hasScore
    ? `${match.homeClub} ${match.homeGoals}–${match.awayGoals} ${match.awayClub}`
    : `${match.homeClub} vs ${match.awayClub}`

  let timeLabel
  if (season === 2026) {
    const parts = matchKickoffParts(match, season)
    const spaceIdx = parts.weekdayDate.lastIndexOf(' ')
    const weekdayFull = spaceIdx > 0 ? parts.weekdayDate.slice(0, spaceIdx) : ''
    const shortWd = SHORT_WEEKDAY[weekdayFull] ?? weekdayFull
    const datePart = parts.shortDate && shortWd ? `${shortWd} ${parts.shortDate}` : parts.weekdayDate
    timeLabel = parts.time ? `${datePart} · ${parts.time} giờ VN` : datePart
  } else {
    timeLabel = matchDayLabel(match, season)
  }

  const result = clubMatchResult(match, clubId)
  const resultPrefix = RESULT_TEXT[result] ?? 'Chưa có tỉ số'
  return {
    result,
    header,
    scoreline,
    timeLabel,
    ariaLabel: `${resultPrefix} · ${header} · ${scoreline} · ${timeLabel}`,
  }
}

export function clampTooltipPosition({
  anchorCenterX,
  containerLeft,
  tooltipWidth,
  viewportWidth,
  margin = 12,
}) {
  const half = tooltipWidth / 2
  const idealViewportLeft = containerLeft + anchorCenterX - half
  const minViewportLeft = margin
  const maxViewportLeft = Math.max(margin, viewportWidth - margin - tooltipWidth)
  const clampedViewportLeft = Math.min(Math.max(idealViewportLeft, minViewportLeft), maxViewportLeft)
  const left = clampedViewportLeft - containerLeft
  const rawArrow = containerLeft + anchorCenterX - clampedViewportLeft
  const arrowLeft = Math.min(Math.max(rawArrow, 14), Math.max(14, tooltipWidth - 14))
  return { left, arrowLeft }
}
