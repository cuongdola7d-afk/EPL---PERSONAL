import { hasMatchScore } from './seasons.js'

export function effectiveStat(row, field) {
  if (!row.stats) return null
  const raw = row.stats[field]
  if (raw !== null && raw !== undefined) return raw
  if (row.evidenceStatus === 'VERIFIED' && ['minutes', 'goals', 'assists'].includes(field)) {
    return row.stats.inferred?.[field] ?? null
  }
  return null
}

export function matchState(row) {
  if (row.match.status !== 'FINISHED') return 'upcoming'
  if (!row.stats) return 'missing'
  if (row.stats.participationStatus === 'PLAYED') return 'played'
  if (row.stats.participationStatus === 'DID_NOT_PLAY') return 'did-not-play'
  if (effectiveStat(row, 'minutes') === null) return 'missing'
  return effectiveStat(row, 'minutes') === 0 ? 'did-not-play' : 'played'
}

export function matchResult(row) {
  if (row.match.status !== 'FINISHED' || !hasMatchScore(row.match)) return null
  const own = row.clubId === row.match.homeClubId ? row.match.homeGoals : row.match.awayGoals
  const other = row.clubId === row.match.homeClubId ? row.match.awayGoals : row.match.homeGoals
  return own > other ? 'W' : own < other ? 'L' : 'D'
}

export function matchRating(row) {
  const raw = row.stats?.rating
  if (typeof raw !== 'string' || !raw.trim()) return null
  const value = Number(raw)
  return Number.isFinite(value) && value >= 0 ? value : null
}

export function seasonSummary(rows) {
  const played = rows.filter((row) => matchState(row) === 'played')
  const sum = (field) => played.every((row) => effectiveStat(row, field) !== null)
    ? played.reduce((total, row) => total + effectiveStat(row, field), 0) : null
  const ratings = played.map(matchRating).filter((value) => value !== null)
  const minutes = sum('minutes')
  return {
    appearances: played.length,
    totalMinutes: minutes,
    averageMinutes: minutes === null || !played.length ? null : minutes / played.length,
    goals: sum('goals'),
    assists: sum('assists'),
    yellowCards: sum('yellowCards'),
    redCards: sum('redCards'),
    averageRating: ratings.length ? ratings.reduce((total, value) => total + value, 0) / ratings.length : null,
    ratedMatches: played.map((row) => ({ row, rating: matchRating(row) }))
      .filter((entry) => entry.rating !== null),
  }
}
