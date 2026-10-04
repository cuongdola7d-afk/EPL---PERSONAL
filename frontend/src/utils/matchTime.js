const TIME_ZONE = 'Asia/Ho_Chi_Minh'
const UTC_TIMESTAMP = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|\+00:00)$/
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

export function compareMatchSchedule(a, b, season = 2026) {
  const dayOrder = (matchDayKey(a, season) ?? '').localeCompare(matchDayKey(b, season) ?? '')
  if (dayOrder) return dayOrder
  const first = kickoffInstant(a, season)
  const second = kickoffInstant(b, season)
  if (first && second) return first - second || a.id - b.id
  if (first || second) return first ? -1 : 1
  return a.id - b.id
}
