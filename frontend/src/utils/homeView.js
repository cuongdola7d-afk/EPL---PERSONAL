import { compareMatchSchedule, matchDayKey } from './matchTime.js'

const inactive = new Set(['CANCELLED', 'POSTPONED', 'SUSPENDED'])

export function homeFixturePreview(matches, today, season = 2026) {
  const ordered = [...matches].filter(match => !inactive.has(match.status))
    .sort((a, b) => compareMatchSchedule(a, b, season))
  const next = ordered.find(match => match.status !== 'FINISHED' && matchDayKey(match, season) >= today)
  const latest = ordered.filter(match => match.status === 'FINISHED').at(-1)
  const selected = next ?? latest ?? ordered[0]
  if (!selected) return { week: null, mode: 'empty', matches: [] }
  return { week: selected.matchweek, mode: next ? 'upcoming' : latest ? 'recent' : 'scheduled',
    matches: ordered.filter(match => match.matchweek === selected.matchweek).slice(0, 5) }
}

export function vietnamToday(now = new Date()) {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(now)
  return ['year', 'month', 'day'].map(type => parts.find(part => part.type === type).value).join('-')
}

export function fixtureDateLabel(date) {
  const value = new Date(`${date}T00:00:00Z`)
  if (!Number.isFinite(value.getTime())) return 'Chưa có ngày thi đấu'
  return new Intl.DateTimeFormat('vi-VN', {
    weekday: 'long', day: '2-digit', month: '2-digit', timeZone: 'UTC',
  }).format(value)
}
