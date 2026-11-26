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

const CLUB_SHORT_NAMES = {
  'arsenal': 'Arsenal',
  'afc bournemouth': 'Bournemouth',
  'bournemouth': 'Bournemouth',
  'aston villa': 'Aston Villa',
  'brentford': 'Brentford',
  'brighton & hove albion': 'Brighton',
  'brighton and hove albion': 'Brighton',
  'chelsea': 'Chelsea',
  'coventry city': 'Coventry',
  'crystal palace': 'C. Palace',
  'everton': 'Everton',
  'fulham': 'Fulham',
  'hull city': 'Hull',
  'ipswich town': 'Ipswich',
  'leeds united': 'Leeds',
  'leicester city': 'Leicester',
  'liverpool': 'Liverpool',
  'manchester city': 'Man City',
  'manchester united': 'Man Utd',
  'newcastle united': 'Newcastle',
  'nottingham forest': 'Forest',
  'southampton': 'Southampton',
  'sunderland': 'Sunderland',
  'tottenham hotspur': 'Spurs',
  'west ham united': 'West Ham',
  'wolverhampton wanderers': 'Wolves',
  'wolves': 'Wolves',
}

export function stripClubSuffix(name) {
  if (typeof name !== 'string') return ''
  return name.trim().replace(/\s+(?:A?FC)$/i, '').trim()
}

export function clubShortName(name) {
  const cleaned = stripClubSuffix(name)
  if (!cleaned) return ''
  return CLUB_SHORT_NAMES[cleaned.toLowerCase()] ?? cleaned
}

export function clubPerspectiveScore(match, clubId) {
  if (!match || match.homeGoals == null || match.awayGoals == null) return null
  const isHome = match.homeClubId === clubId
  const own = isHome ? match.homeGoals : match.awayGoals
  const other = isHome ? match.awayGoals : match.homeGoals
  return `${own}–${other}`
}

export function formatShortKickoffDate(match, season = 2026) {
  if (!match) return { date: '', time: null }
  if (season !== 2026) {
    return { date: matchDayLabel(match, season), time: null }
  }
  const parts = matchKickoffParts(match, season)
  const spaceIdx = parts.weekdayDate.lastIndexOf(' ')
  const weekdayFull = spaceIdx > 0 ? parts.weekdayDate.slice(0, spaceIdx) : ''
  const shortWd = SHORT_WEEKDAY[weekdayFull] ?? weekdayFull
  const date = parts.shortDate && shortWd ? `${shortWd} ${parts.shortDate}` : parts.weekdayDate
  return { date, time: parts.time }
}

export function groupMatchesByVietnamMonth(matches, season = 2026) {
  if (!Array.isArray(matches) || matches.length === 0) return []
  const groups = []
  let current = null
  for (const match of matches) {
    const dayKey = matchDayKey(match, season) ?? ''
    const m = /^(\d{4})-(\d{2})-\d{2}$/.exec(dayKey)
    const key = m ? `${m[1]}-${m[2]}` : 'unknown'
    const label = m ? `Tháng ${Number(m[2])} · ${m[1]}` : 'Chưa xác định'
    if (!current || current.key !== key) {
      current = { key, label, matches: [] }
      groups.push(current)
    }
    current.matches.push(match)
  }
  return groups
}

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

export const SQUAD_POSITION_GROUPS = [
  { code: 'GOALKEEPER', label: 'Thủ môn', fallbackTag: 'GK' },
  { code: 'DEFENDER', label: 'Hậu vệ', fallbackTag: 'DEF' },
  { code: 'MIDFIELDER', label: 'Tiền vệ', fallbackTag: 'MID' },
  { code: 'FORWARD', label: 'Tiền đạo', fallbackTag: 'FWD' },
]

const SQUAD_COLUMNS = {
  GOALKEEPER: [
    { key: 'appearances', label: 'Trận', format: 'int', highlight: false },
    { key: 'minutes', label: 'Phút', format: 'int', highlight: false },
    { key: 'cleanSheets', label: 'Sạch lưới', format: 'int', highlight: true },
    { key: 'saves', label: 'Cứu thua', format: 'int', highlight: true },
    { key: 'goalsConceded', label: 'Thủng lưới', format: 'int', highlight: false },
  ],
  DEFENDER: [
    { key: 'appearances', label: 'Trận', format: 'int', highlight: false },
    { key: 'minutes', label: 'Phút', format: 'int', highlight: false },
    { key: 'cleanSheets', label: 'Sạch lưới', format: 'int', highlight: true },
    { key: 'tackles', label: 'Tắc bóng', format: 'int', highlight: true },
    { key: 'clearancesBlocksInterceptions', label: 'Cắt chặn phá', format: 'int', highlight: true },
  ],
  MIDFIELDER: [
    { key: 'appearances', label: 'Trận', format: 'int', highlight: false },
    { key: 'minutes', label: 'Phút', format: 'int', highlight: false },
    { key: 'goals', label: 'Bàn', format: 'int', highlight: true },
    { key: 'assists', label: 'Kiến tạo', format: 'int', highlight: true },
    { key: 'recoveries', label: 'Thu hồi', format: 'int', highlight: true },
  ],
  FORWARD: [
    { key: 'appearances', label: 'Trận', format: 'int', highlight: false },
    { key: 'minutes', label: 'Phút', format: 'int', highlight: false },
    { key: 'goals', label: 'Bàn', format: 'int', highlight: true },
    { key: 'assists', label: 'Kiến tạo', format: 'int', highlight: true },
    { key: 'expectedGoals', label: 'xG', format: 'dec1', highlight: true },
  ],
}

export function squadColumnsForGroup(groupCode) {
  return SQUAD_COLUMNS[groupCode] ?? SQUAD_COLUMNS.MIDFIELDER
}

export function calculateAge(birthDate, todayIso) {
  if (typeof birthDate !== 'string' || typeof todayIso !== 'string') return null
  const b = /^(\d{4})-(\d{2})-(\d{2})$/.exec(birthDate.trim())
  const t = /^(\d{4})-(\d{2})-(\d{2})$/.exec(todayIso.trim())
  if (!b || !t) return null
  const [by, bm, bd] = [Number(b[1]), Number(b[2]), Number(b[3])]
  const [ty, tm, td] = [Number(t[1]), Number(t[2]), Number(t[3])]
  let age = ty - by
  if (tm < bm || (tm === bm && td < bd)) age -= 1
  return age >= 0 ? age : null
}

export function playerPositionChips(player, limit = 2) {
  if (!player) return []
  if (Array.isArray(player.eligiblePositions) && player.eligiblePositions.length > 0) {
    return player.eligiblePositions.filter(code => typeof code === 'string' && code.length > 0).slice(0, limit)
  }
  return typeof player.primaryPosition === 'string' && player.primaryPosition.length > 0
    ? [player.primaryPosition]
    : []
}

export function ovrTier(overall) {
  if (overall == null) return 'na'
  if (overall >= 85) return 'gold'
  if (overall >= 78) return 'green'
  if (overall >= 70) return 'blue'
  return 'silver'
}

export function squadRatingBand(rating) {
  if (rating == null) return null
  if (rating >= 7.2) return 'high'
  if (rating >= 6.9) return 'good'
  if (rating >= 6.6) return 'mid'
  return 'low'
}

export function formatSquadStat(value, format = 'int') {
  if (value == null) return '–'
  if (format === 'dec1') return Number(value).toFixed(1)
  return String(value)
}

export function sortSquadRows(rows, hasStats = true) {
  return [...rows].sort((a, b) => {
    if (hasStats) {
      if (a.played !== b.played) return a.played ? -1 : 1
      const minA = a.played ? (a.stats?.minutes ?? -1) : -1
      const minB = b.played ? (b.stats?.minutes ?? -1) : -1
      if (minB !== minA) return minB - minA
      const appA = a.played ? (a.stats?.appearances ?? 0) : 0
      const appB = b.played ? (b.stats?.appearances ?? 0) : 0
      if (appB !== appA) return appB - appA
    }
    const ovrA = a.player.fc27Overall ?? -1
    const ovrB = b.player.fc27Overall ?? -1
    if (ovrB !== ovrA) return ovrB - ovrA
    return a.player.name.localeCompare(b.player.name)
  })
}

export function squadColumnLeaders(rows, columns, minMinutes = 270) {
  const leaders = {}
  for (const col of columns) {
    if (!col.highlight) {
      leaders[col.key] = null
      continue
    }
    let best = null
    for (const row of rows) {
      if (!row.played || (row.stats?.minutes ?? 0) < minMinutes) continue
      const val = row.stats?.[col.key]
      if (typeof val === 'number' && Number.isFinite(val) && val > 0) {
        if (best === null || val > best) best = val
      }
    }
    leaders[col.key] = best
  }
  return leaders
}

export function isSquadColumnLeader(row, col, leaders, minMinutes = 270) {
  if (!col.highlight || !row.played || (row.stats?.minutes ?? 0) < minMinutes) return false
  const best = leaders?.[col.key]
  if (best == null) return false
  const val = row.stats?.[col.key]
  return typeof val === 'number' && val === best
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
