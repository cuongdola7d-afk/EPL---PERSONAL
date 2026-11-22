import { hasMatchScore } from './seasons.js'

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

export function formatLiveStatusBadge(liveState) {
  if (!liveState || typeof liveState !== 'object') return null
  if (liveState.status === 'LIVE') {
    return Number.isInteger(liveState.minute) && liveState.minute > 0
      ? `LIVE ${liveState.minute}'`
      : 'LIVE'
  }
  if (liveState.status === 'FT') {
    return 'Hết giờ'
  }
  if (liveState.status === 'PRE') {
    return 'Sắp diễn ra'
  }
  return null
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

export function applyLiveSnapshot(currentByFixture = {}, snapshotList = []) {
  if (!Array.isArray(snapshotList)) {
    return { byFixture: currentByFixture, flashedIds: [] }
  }
  const next = {}
  const flashedIds = []
  for (const item of snapshotList) {
    if (!isValidLiveMatchState(item)) continue
    next[item.fixtureId] = item
    const prev = currentByFixture[item.fixtureId]
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
      [liveState.fixtureId]: liveState,
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

export function resolveMatchDisplay(match, liveState, { preferManualStats = false } = {}) {
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
      badgeText: formatLiveStatusBadge(liveState),
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
