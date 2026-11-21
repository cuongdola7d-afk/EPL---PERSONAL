import { matchPlayerState, matchPlayerValue, matchRating } from './matchView.js'

const EXTRA_KEYS = new Set([
  'saves',
  'goalsConceded',
  'bps',
  'bonus',
  'tackles',
  'clearancesBlocksInterceptions',
  'recoveries',
  'defensiveContribution',
  'expectedGoals',
  'expectedAssists',
])

const BASE_KEYS = new Set(['minutes', 'goals', 'assists', 'yellowCards', 'redCards'])

function toNumber(value) {
  if (value == null || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function sumNullable(values) {
  let hasAny = false
  let total = 0
  for (const raw of values) {
    const num = toNumber(raw)
    if (num != null) {
      hasAny = true
      total += num
    }
  }
  return hasAny ? total : null
}

export function hasAnyExtraStats(players) {
  return players.some((player) => {
    const extra = player?.extra
    if (!extra || typeof extra !== 'object') return false
    for (const key of EXTRA_KEYS) {
      if (toNumber(extra[key]) != null) return true
    }
    return false
  })
}

export function playerStatValue(player, key, evidenceStatus) {
  if (!player) return null
  if (key === 'rating') return matchRating(player)
  if (BASE_KEYS.has(key)) return toNumber(matchPlayerValue(player, key, evidenceStatus))
  if (EXTRA_KEYS.has(key)) return toNumber(player.extra?.[key])
  return null
}

export function averageRating(players) {
  const ratings = players
    .filter((player) => matchPlayerState(player) === 'played')
    .map(matchRating)
    .filter((value) => value != null)
  if (!ratings.length) return null
  return ratings.reduce((sum, value) => sum + value, 0) / ratings.length
}

export function teamTotals(players, evidenceStatus) {
  const played = players.filter((player) => matchPlayerState(player) === 'played')
  return {
    averageRating: averageRating(players),
    expectedGoals: sumNullable(played.map((player) => player.extra?.expectedGoals)),
    expectedAssists: sumNullable(played.map((player) => player.extra?.expectedAssists)),
    saves: sumNullable(played.map((player) => player.extra?.saves)),
    tackles: sumNullable(played.map((player) => player.extra?.tackles)),
    clearancesBlocksInterceptions: sumNullable(
      played.map((player) => player.extra?.clearancesBlocksInterceptions),
    ),
    recoveries: sumNullable(played.map((player) => player.extra?.recoveries)),
    yellowCards: sumNullable(
      played.map((player) => matchPlayerValue(player, 'yellowCards', evidenceStatus)),
    ),
  }
}

export function sortPlayers(players, key, direction = 'desc', evidenceStatus) {
  const dir = direction === 'asc' ? 1 : -1
  return [...players].sort((a, b) => {
    const va = playerStatValue(a, key, evidenceStatus)
    const vb = playerStatValue(b, key, evidenceStatus)
    if (va == null && vb == null) {
      return (a.playerName ?? '').localeCompare(b.playerName ?? '') || (a.playerId ?? 0) - (b.playerId ?? 0)
    }
    if (va == null) return 1
    if (vb == null) return -1
    if (va !== vb) return (va - vb) * dir
    const ra = matchRating(a)
    const rb = matchRating(b)
    if (ra != null && rb != null && ra !== rb) return rb - ra
    return (a.playerName ?? '').localeCompare(b.playerName ?? '') || (a.playerId ?? 0) - (b.playerId ?? 0)
  })
}

export function topValues(players, keys, evidenceStatus) {
  const played = players.filter((player) => matchPlayerState(player) === 'played')
  const result = {}
  for (const key of keys) {
    let best = null
    for (const player of played) {
      const value = playerStatValue(player, key, evidenceStatus)
      if (value != null && value > 0 && (best == null || value > best)) {
        best = value
      }
    }
    result[key] = best
  }
  return result
}
