const TIME_ZONE = 'Asia/Ho_Chi_Minh'
const UTC_TIMESTAMP = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|\+00:00)$/
const DAY_KEY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const WEEKDAYS_VI = ['Chủ nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy']
const DAY_MS = 24 * 60 * 60 * 1000
const vietnamDateTime = new Intl.DateTimeFormat('vi-VN', {
  timeZone: TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
})

export function kickoffInstant(match, season = 2026) {
  if (season !== 2026) return null
  const timestamp = match.kickoffUtc ?? match.utcDate
  if (typeof timestamp !== 'string' || !UTC_TIMESTAMP.test(timestamp)) return null
  const instant = new Date(timestamp)
  return Number.isFinite(instant.getTime()) ? instant : null
}

function vietnamParts(instant) {
  return Object.fromEntries(vietnamDateTime.formatToParts(instant).map(part => [part.type, part.value]))
}

export function vietnamDayKeyFromDate(date = new Date()) {
  const instant = date instanceof Date ? date : new Date(date)
  if (!Number.isFinite(instant.getTime())) return null
  const { year, month, day } = vietnamParts(instant)
  return `${year}-${month}-${day}`
}

function parseDayKeyParts(dayKey) {
  const m = DAY_KEY_PATTERN.exec(dayKey ?? '')
  if (!m) return null
  const year = Number(m[1])
  const month = Number(m[2])
  const day = Number(m[3])
  const utc = new Date(Date.UTC(year, month - 1, day))
  if (!Number.isFinite(utc.getTime())) return null
  return {
    year: m[1],
    month: m[2],
    day: m[3],
    weekday: WEEKDAYS_VI[utc.getUTCDay()],
    utcMs: utc.getTime(),
  }
}

// A date-only fallback is a calendar date, never an assumed midnight kickoff.
export function matchDayKey(match, season = 2026) {
  const instant = kickoffInstant(match, season)
  if (!instant) return match.date
  const { year, month, day } = vietnamParts(instant)
  return `${year}-${month}-${day}`
}

export function matchDayLabel(match, season = 2026) {
  const key = matchDayKey(match, season)
  return /^\d{4}-\d{2}-\d{2}$/.test(key ?? '') ? key.split('-').reverse().join('/') : 'Chưa xác định ngày'
}

export function matchDateTimeLabel(match, season = 2026) {
  const instant = kickoffInstant(match, season)
  if (instant) {
    const { year, month, day, hour, minute } = vietnamParts(instant)
    return `${day}/${month}/${year} · ${hour}:${minute}`
  }
  const day = matchDayLabel(match, season)
  return season === 2026 ? `${day} · Chưa xác định giờ` : day
}

/**
 * Formats a match's date in Vietnam time with weekday and dd/mm, e.g. "Thứ Bảy 21/11".
 * Returns "Chưa xác định ngày" when the match has no valid date.
 */
export function matchWeekdayDateLabel(match, season = 2026) {
  const key = matchDayKey(match, season)
  const parts = parseDayKeyParts(key)
  if (!parts) return 'Chưa xác định ngày'
  return `${parts.weekday} ${parts.day}/${parts.month}`
}

/**
 * Returns structured Vietnam-time kickoff parts for schedule cards:
 * { instant, time: '22:00' | null, weekdayDate: 'Thứ Bảy 21/11', dayKey, shortDate: '21/11' | null }
 */
export function matchKickoffParts(match, season = 2026) {
  const instant = kickoffInstant(match, season)
  const dayKey = matchDayKey(match, season)
  const dayParts = parseDayKeyParts(dayKey)
  let time = null
  if (instant) {
    const { hour, minute } = vietnamParts(instant)
    time = `${hour}:${minute}`
  }
  return {
    instant,
    time,
    weekdayDate: dayParts ? `${dayParts.weekday} ${dayParts.day}/${dayParts.month}` : 'Chưa xác định ngày',
    dayKey: dayParts ? dayKey : null,
    shortDate: dayParts ? `${dayParts.day}/${dayParts.month}` : null,
  }
}

/**
 * Formats a day group heading from a YYYY-MM-DD Vietnam day key:
 * { label: 'Thứ Bảy, 21/11/2026', relative: 'Hôm nay' | 'Ngày mai' | 'Hôm qua' | '' }
 */
export function formatDayGroupHeading(dayKey, now = new Date()) {
  const parts = parseDayKeyParts(dayKey)
  if (!parts) return { label: 'Chưa xác định ngày', relative: '' }
  const label = `${parts.weekday}, ${parts.day}/${parts.month}/${parts.year}`
  const todayKey = vietnamDayKeyFromDate(now)
  const todayParts = parseDayKeyParts(todayKey)
  let relative = ''
  if (todayParts) {
    const diffDays = Math.round((parts.utcMs - todayParts.utcMs) / DAY_MS)
    if (diffDays === 0) relative = 'Hôm nay'
    else if (diffDays === 1) relative = 'Ngày mai'
    else if (diffDays === -1) relative = 'Hôm qua'
  }
  return { label, relative }
}

/**
 * Formats a YYYY-MM-DD day key as dd/mm (e.g. '2026-11-21' -> '21/11').
 */
export function formatShortDayMonth(dayKey) {
  const parts = parseDayKeyParts(dayKey)
  return parts ? `${parts.day}/${parts.month}` : ''
}

/**
 * Formats an ISO-8601 UTC timestamp to HH:mm in Vietnam time (Asia/Ho_Chi_Minh).
 */
export function formatVietnamTimeHHmm(isoTimestamp) {
  if (typeof isoTimestamp !== 'string' || !isoTimestamp.trim()) return null
  const instant = new Date(isoTimestamp)
  if (!Number.isFinite(instant.getTime())) return null
  const { hour, minute } = vietnamParts(instant)
  return `${hour}:${minute}`
}

export function compareMatchSchedule(a, b, season = 2026) {
  const dayOrder = (matchDayKey(a, season) ?? '').localeCompare(matchDayKey(b, season) ?? '')
  if (dayOrder) return dayOrder
  const first = kickoffInstant(a, season)
  const second = kickoffInstant(b, season)
  if (first && second) return first - second || a.id - b.id
  if (first || second) return first ? -1 : 1
  return a.id - b.id
}
