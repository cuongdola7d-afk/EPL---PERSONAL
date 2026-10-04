export const GAMEWEEK_LABELS = {
  OPEN: 'Đang mở', LOCKED: 'Đã khóa', AWAITING_RESULTS: 'Chờ kết quả', PUBLISHED: 'Đã công bố kết quả',
}

export function formatDeadline(value) {
  if (!value) return 'Chưa công bố'
  const parts = new Intl.DateTimeFormat('en-GB', { timeZone: 'Asia/Ho_Chi_Minh',
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
  }).formatToParts(new Date(value))
  const part = type => parts.find(p => p.type === type)?.value
  return `${part('day')}/${part('month')}/${part('year')} · ${part('hour')}:${part('minute')}`
}

// performance.now is monotonic; changing the browser wall clock cannot unlock a GW.
export function remainingMilliseconds(deadlineUtc, serverTimeUtc, receivedAt, monotonicNow) {
  return Math.max(0, Date.parse(deadlineUtc) - Date.parse(serverTimeUtc) - Math.max(0, monotonicNow - receivedAt))
}

export function countdownLabel(milliseconds) {
  const seconds = Math.ceil(milliseconds / 1000)
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor(seconds % 86400 / 3600)
  const minutes = Math.floor(seconds % 3600 / 60)
  return `${days} ngày ${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`
}

export function shouldRefreshDeadline(view, milliseconds) {
  return view?.status === 'OPEN' && view.configured && milliseconds === 0
}
