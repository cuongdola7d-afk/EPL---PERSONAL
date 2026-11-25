export const LATEST_RESULT_DISMISS_PREFIX = 'prismaxi:fantasy:latest-result-dismissed:2026:gw'

const VN_WEEKDAYS = {
  Mon: 'Thứ Hai',
  Tue: 'Thứ Ba',
  Wed: 'Thứ Tư',
  Thu: 'Thứ Năm',
  Fri: 'Thứ Sáu',
  Sat: 'Thứ Bảy',
  Sun: 'Chủ Nhật',
}

export function dismissStorageKey(gameweek) {
  return `${LATEST_RESULT_DISMISS_PREFIX}${gameweek}`
}

export function isLatestResultDismissed(gameweek, storage = globalThis.window?.localStorage) {
  if (!Number.isInteger(gameweek) || !storage) return false
  try {
    return storage.getItem(dismissStorageKey(gameweek)) === '1'
  } catch {
    return false
  }
}

export function dismissLatestResult(gameweek, storage = globalThis.window?.localStorage) {
  if (!Number.isInteger(gameweek) || !storage) return false
  try {
    storage.setItem(dismissStorageKey(gameweek), '1')
    return true
  } catch {
    return false
  }
}

export function formatCardPoints(value) {
  if (value === null || value === undefined || !Number.isFinite(Number(value))) return '–'
  const num = Number(value)
  const two = num.toFixed(2)
  return two.endsWith('0') ? num.toFixed(1) : two
}

export function formatPublishedDayMonth(value) {
  if (!value || !Number.isFinite(Date.parse(value))) return ''
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit',
    month: '2-digit',
  }).formatToParts(new Date(value))
  const get = type => parts.find(p => p.type === type)?.value ?? ''
  return `${get('day')}/${get('month')}`
}

export function formatOpenDeadlineFriendly(value) {
  if (!value || !Number.isFinite(Date.parse(value))) return ''
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Asia/Ho_Chi_Minh',
    weekday: 'short',
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date(value))
  const get = type => parts.find(p => p.type === type)?.value ?? ''
  const weekday = VN_WEEKDAYS[get('weekday')] ?? ''
  return `${get('hour')}:${get('minute')}${weekday ? ` ${weekday}` : ''} ${get('day')}/${get('month')}`
}

export function formatSeasonRankChange(change) {
  if (change === null || change === undefined || !Number.isInteger(change)) return null
  if (change > 0) return `↑${change}`
  if (change < 0) return `↓${Math.abs(change)}`
  return '='
}

export function shouldShowLatestResultCard({ summary, viewingGameweek, isGuest, dismissed }) {
  if (!summary || !Number.isInteger(summary.gameweek)) return false
  if (!(summary.participants > 0)) return false
  if (summary.gameweek === viewingGameweek) return false
  if (!isGuest && dismissed) return false
  return true
}
