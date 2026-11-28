import { hasMatchScore } from './seasons.js'

const MAX_SMOOTH_INCREMENT_MINUTES = 2

export function isValidLiveMatchState(item) {
  return item !== null &&
    typeof item === 'object' &&
    Number.isInteger(item.fixtureId) &&
    ['PRE', 'LIVE', 'FT'].includes(item.status) &&
    Number.isInteger(item.minute) &&
    (item.homeScore === null || Number.isInteger(item.homeScore)) &&
    (item.awayScore === null || Number.isInteger(item.awayScore)) &&
    Array.isArray(item.goals) &&
    Array.isArray(item.assists) &&
    Array.isArray(item.redCards)
}

/**
 * Computes the smooth displayed minute for a LIVE match:
 * - Returns 0 when not LIVE or server minute <= 0.
 * - Stands still at 45 and 90 (does not auto-increment during stoppage time / halftime).
 * - Otherwise adds floor((nowMs - updatedAt) / 60000), capped at server minute + 2.
 * - When the server sends a new minute, resets immediately to the server minute (even if smaller).
 *   If server minute did not change (minuteChanged === false), prevents jumping backward by > 1 minute.
 */
export function computeLiveDisplayMinute(liveState, nowMs = Date.now(), prevDisplayMinute = null) {
  if (!liveState || typeof liveState !== 'object' || liveState.status !== 'LIVE') {
    return 0
  }
  const serverMinute = Number.isInteger(liveState.minute) && liveState.minute >= 0
    ? liveState.minute
    : 0
  if (serverMinute <= 0) {
    return 0
  }
  if (serverMinute === 45 || serverMinute === 90) {
    return serverMinute
  }

  const baseTimestamp = liveState.minuteUpdatedAt ?? liveState.updatedAt
  const updatedAtMs = typeof baseTimestamp === 'number'
    ? baseTimestamp
    : typeof baseTimestamp === 'string'
      ? Date.parse(baseTimestamp)
      : NaN

  let added = 0
  if (Number.isFinite(updatedAtMs) && Number.isFinite(nowMs) && nowMs > updatedAtMs) {
    const elapsedMinutes = Math.floor((nowMs - updatedAtMs) / 60000)
    added = Math.min(MAX_SMOOTH_INCREMENT_MINUTES, Math.max(0, elapsedMinutes))
  }

  const candidate = serverMinute + added

  if (Number.isInteger(prevDisplayMinute) && prevDisplayMinute > 0 && candidate < prevDisplayMinute) {
    // When the server sends a new minute (minuteChanged !== false), reset to candidate immediately:
    // - If diff <= 1 (e.g. client was at +2 and server advanced +1), it steps back at most 1 minute.
    // - If diff > 1 (large server correction), it resets to the server's minute.
    // Only when the server did NOT change the minute (minuteChanged === false) do we clamp backward jump to <= 1.
    if (liveState.minuteChanged === false) {
      return Math.max(candidate, prevDisplayMinute - 1)
    }
  }

  return candidate
}

export function formatLiveMinuteLabel(liveState, nowMs = Date.now(), prevDisplayMinute = null) {
  if (!liveState || typeof liveState !== 'object' || liveState.status !== 'LIVE') {
    return ''
  }
  if (liveState.minute === 45) {
    return "45'+"
  }
  if (liveState.minute === 90) {
    return "90'+"
  }
  const displayMinute = computeLiveDisplayMinute(liveState, nowMs, prevDisplayMinute)
  return displayMinute > 0 ? `${displayMinute}'` : ''
}

export function formatLiveStatusBadge(liveState, nowMs = Date.now(), prevDisplayMinute = null) {
  if (!liveState || typeof liveState !== 'object') return null
  if (liveState.status === 'LIVE') {
    const minuteLabel = formatLiveMinuteLabel(liveState, nowMs, prevDisplayMinute)
    return minuteLabel ? `Đang đá ${minuteLabel}` : 'Đang đá'
  }
  if (liveState.status === 'FT') {
    return 'Kết thúc'
  }
  if (liveState.status === 'PRE') {
    return 'Sắp đá'
  }
  return null
}

export function formatDetailLiveStatusBadge(liveState, nowMs = Date.now(), prevDisplayMinute = null) {
  if (!liveState || typeof liveState !== 'object') return null
  if (liveState.status === 'LIVE') {
    const minuteLabel = formatLiveMinuteLabel(liveState, nowMs, prevDisplayMinute)
    return minuteLabel ? `Trực tiếp ${minuteLabel}` : 'Trực tiếp'
  }
  if (liveState.status === 'FT') {
    return 'Kết thúc'
  }
  if (liveState.status === 'PRE') {
    return 'Sắp đá'
  }
  return null
}

export function buildMatchGoalGroups(goalEvents, match, formatName = (name) => name) {
  if (!Array.isArray(goalEvents) || goalEvents.length === 0 || !match) return null
  const homeClubId = match.homeClubId
  const awayClubId = match.awayClubId
  let homeCount = 0
  let awayCount = 0
  for (const event of goalEvents) {
    if (!event || typeof event !== 'object') return null
    if (event.clubId === homeClubId) homeCount += 1
    else if (event.clubId === awayClubId) awayCount += 1
    else return null
  }
  if (Number.isInteger(match.homeGoals) && Number.isInteger(match.awayGoals)) {
    if (homeCount !== match.homeGoals || awayCount !== match.awayGoals) {
      return null
    }
  }
  const groupSide = (clubId) => {
    const map = new Map()
    for (const event of goalEvents) {
      if (event.clubId !== clubId) continue
      const isOwnGoal = event.goalType === 'OWN_GOAL'
      const rawName = event.playerName && String(event.playerName).trim()
        ? String(event.playerName).trim()
        : 'Không rõ'
      const key = `${event.playerId ?? rawName}:${isOwnGoal ? 'OG' : 'G'}`
      let entry = map.get(key)
      if (!entry) {
        entry = {
          key,
          playerId: event.playerId,
          playerName: rawName,
          displayName: formatName(rawName),
          isOwnGoal,
          minutes: [],
          count: 0,
        }
        map.set(key, entry)
      }
      entry.count += 1
      if (Number.isInteger(event.minute) && event.minute >= 0) {
        entry.minutes.push(`${event.minute}'`)
      }
    }
    return [...map.values()].map((entry) => {
      const og = entry.isOwnGoal ? ' (phản lưới)' : ''
      let text
      if (entry.minutes.length > 0) {
        text = `${entry.displayName}${og} ${entry.minutes.join(', ')}`
      } else if (entry.count > 1) {
        text = `${entry.displayName}${og} ×${entry.count}`
      } else {
        text = `${entry.displayName}${og}`
      }
      return { ...entry, text }
    })
  }
  return {
    home: groupSide(homeClubId),
    away: groupSide(awayClubId),
  }
}

const LIVE_MATCH_STATUSES = new Set(['LIVE', 'IN_PLAY', 'PAUSED', 'EXTRA_TIME', 'PENALTY_SHOOTOUT'])

export function shouldAutoRefreshMatchDetail(match, detail, liveState) {
  if (!match) return false
  const isConfirmed = detail?.dataStatus === 'CONFIRMED' || Boolean(match.hasManualStats)
  if (match.status === 'FINISHED' && isConfirmed) {
    return false
  }
  if (liveState?.status === 'LIVE') return true
  if (LIVE_MATCH_STATUSES.has(match.status)) return true
  if (detail?.dataStatus === 'PROVISIONAL_FPL' && match.status !== 'FINISHED') return true
  return false
}

export function startVisibleInterval({
  intervalMs = 60_000,
  isVisible = () => typeof document === 'undefined' || document.visibilityState === 'visible',
  onTick,
  setIntervalFn = setInterval,
  clearIntervalFn = clearInterval,
}) {
  const id = setIntervalFn(() => {
    if (isVisible()) {
      onTick()
    }
  }, intervalMs)
  return () => clearIntervalFn(id)
}

export function formatLiveGoalText(goal) {
  if (!goal || typeof goal !== 'object') return ''
  const name = goal.playerName && String(goal.playerName).trim()
    ? String(goal.playerName).trim()
    : 'Không rõ'
  const hasMinute = Number.isInteger(goal.approxMinute) && goal.approxMinute >= 0
  const minutePart = hasMinute ? ` ~${goal.approxMinute}'` : ''
  const og = goal.ownGoal ? ' (phản lưới)' : ''
  return `${name}${minutePart}${og}`
}

function enrichLiveStateTiming(prev, item) {
  if (!prev || prev.status !== 'LIVE' || item.status !== 'LIVE') {
    return {
      ...item,
      minuteUpdatedAt: item.updatedAt,
      minuteChanged: true,
    }
  }
  if (prev.minute === item.minute) {
    return {
      ...item,
      minuteUpdatedAt: prev.minuteUpdatedAt ?? prev.updatedAt ?? item.updatedAt,
      minuteChanged: false,
    }
  }
  return {
    ...item,
    minuteUpdatedAt: item.updatedAt,
    minuteChanged: true,
  }
}

export function applyLiveSnapshot(currentByFixture = {}, snapshotList = []) {
  if (!Array.isArray(snapshotList)) {
    return { byFixture: currentByFixture, flashedIds: [] }
  }
  const next = {}
  const flashedIds = []
  for (const item of snapshotList) {
    if (!isValidLiveMatchState(item)) continue
    const prev = currentByFixture[item.fixtureId]
    next[item.fixtureId] = enrichLiveStateTiming(prev, item)
    if (prev && hasScoreChanged(prev, item)) {
      flashedIds.push(item.fixtureId)
    }
  }
  return { byFixture: next, flashedIds }
}

export function applyLiveScoreUpdate(currentByFixture = {}, liveState) {
  if (!isValidLiveMatchState(liveState)) {
    return { byFixture: currentByFixture, scoreChanged: false }
  }
  const prev = currentByFixture[liveState.fixtureId]
  const scoreChanged = hasScoreChanged(prev, liveState)
  return {
    byFixture: {
      ...currentByFixture,
      [liveState.fixtureId]: enrichLiveStateTiming(prev, liveState),
    },
    scoreChanged,
  }
}

function hasScoreChanged(prev, next) {
  if (!next || next.homeScore === null || next.awayScore === null) {
    return false
  }
  if (!prev) {
    return false
  }
  return prev.homeScore !== next.homeScore || prev.awayScore !== next.awayScore
}

export function resolveMatchDisplay(match, liveState, { preferManualStats = false, nowMs = Date.now() } = {}) {
  if (!match) {
    return {
      scored: false,
      homeGoals: null,
      awayGoals: null,
      isLive: false,
      isLiveFt: false,
      badgeText: null,
      useLiveGoals: false,
    }
  }

  const manualOverride = preferManualStats && Boolean(match.hasManualStats)
  const hasActiveLive = !manualOverride &&
    liveState != null &&
    (liveState.status === 'LIVE' || liveState.status === 'FT')

  if (hasActiveLive) {
    const homeGoals = liveState.homeScore ?? match.homeGoals ?? 0
    const awayGoals = liveState.awayScore ?? match.awayGoals ?? 0
    const scored = liveState.homeScore != null && liveState.awayScore != null
    return {
      scored,
      homeGoals,
      awayGoals,
      isLive: liveState.status === 'LIVE',
      isLiveFt: liveState.status === 'FT',
      badgeText: formatLiveStatusBadge(liveState, nowMs),
      useLiveGoals: !match.hasManualStats && Array.isArray(liveState.goals) && liveState.goals.length > 0,
    }
  }

  const scored = hasMatchScore(match)
  return {
    scored,
    homeGoals: match.homeGoals,
    awayGoals: match.awayGoals,
    isLive: false,
    isLiveFt: false,
    badgeText: null,
    useLiveGoals: false,
  }
}
