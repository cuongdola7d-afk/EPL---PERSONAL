const VISITOR_KEY = 'prismaxi_visitor_id'
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

let sessionVisitorId = null

function generateUuid() {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID()
    }
  } catch {
    // Fall through to RFC4122 v4 fallback.
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, ch => {
    const r = Math.floor(Math.random() * 16)
    const v = ch === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export function getVisitorId(storage = typeof window !== 'undefined' ? window.localStorage : null) {
  try {
    const existing = storage?.getItem(VISITOR_KEY)
    if (existing && UUID_RE.test(existing)) {
      sessionVisitorId = existing
      return existing
    }
  } catch {
    // localStorage may be blocked (private mode / quota).
  }

  if (sessionVisitorId && UUID_RE.test(sessionVisitorId)) {
    return sessionVisitorId
  }

  const created = generateUuid()
  sessionVisitorId = created
  try {
    storage?.setItem(VISITOR_KEY, created)
  } catch {
    // Keep in-memory session ID when storage write fails.
  }
  return created
}

export function resolvePageKey({ page, playerDetail, clubRoute, matchRoute, liveTest }) {
  if (liveTest) return null
  if (playerDetail) return 'player'
  if (clubRoute) return 'club'
  if (matchRoute?.matchId) return 'match-detail'
  switch (page) {
    case 'clubs': return 'home'
    case 'players': return 'players'
    case 'matches': return 'schedule'
    case 'standings': return 'standings'
    case 'fantasy': return 'fantasy'
    case 'minigame': return 'minigame'
    case 'admin': return 'admin'
    default: return 'home'
  }
}

export function sendPageview(pageKey) {
  if (!pageKey) return
  try {
    const payload = JSON.stringify({ visitorId: getVisitorId(), page: pageKey })
    if (typeof navigator !== 'undefined' && typeof navigator.sendBeacon === 'function') {
      const blob = new Blob([payload], { type: 'application/json' })
      if (navigator.sendBeacon('/api/analytics/pageview', blob)) {
        return
      }
    }
    if (typeof fetch === 'function') {
      fetch('/api/analytics/pageview', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: payload,
        keepalive: true,
        credentials: 'include',
      }).catch(() => {})
    }
  } catch {
    // Never interrupt page navigation.
  }
}

export const PAGE_LABELS = {
  home: 'Trang chủ',
  clubs: 'Câu lạc bộ',
  club: 'Chi tiết câu lạc bộ',
  players: 'Cầu thủ',
  player: 'Hồ sơ cầu thủ',
  schedule: 'Lịch đấu',
  'match-detail': 'Chi tiết trận',
  standings: 'Bảng xếp hạng',
  fantasy: 'Fantasy',
  minigame: 'Minigame',
  admin: 'Quản trị',
}
