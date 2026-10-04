import { parseClubRoute } from './clubRoute.js'

const VALID_FILTERS = new Set(['all', 'finished', 'upcoming'])

function isValidBackHash(hash) {
  if (parseClubRoute(hash ?? '')) return true
  // Validate the player target; the profile validates its optional return route.
  const player = /^#players\/[1-9]\d*\?(.*)$/.exec(hash ?? '')
  return player !== null && ['2024', '2026'].includes(new URLSearchParams(player[1]).get('season'))
}

export function matchListHash({ season, week, filter = 'all', club = '' }) {
  const params = new URLSearchParams({ season: String(season) })
  if (week != null) params.set('week', String(week))
  if (filter !== 'all') params.set('filter', filter)
  if (club) params.set('club', club)
  return `#matches?${params}`
}

export function matchDetailHash(id, state, fromHash) {
  const hash = matchListHash(state).replace('#matches?', `#matches/${encodeURIComponent(id)}?`)
  return isValidBackHash(fromHash) ? `${hash}&from=${encodeURIComponent(fromHash)}` : hash
}

export function parseMatchRoute(hash) {
  const match = /^#matches(?:\/([1-9]\d*))?(?:\?(.*))?$/.exec(hash)
  if (!match) return null
  const params = new URLSearchParams(match[2] ?? '')
  const seasonText = params.get('season')
  if (seasonText && !['2024', '2026'].includes(seasonText)) return null
  const weekText = params.get('week')
  const week = weekText == null ? null : Number(weekText)
  if (weekText != null && (!Number.isInteger(week) || week < 1 || week > 38)) return null
  const filter = params.get('filter') ?? 'all'
  if (!VALID_FILTERS.has(filter)) return null
  return {
    matchId: match[1] == null ? null : Number(match[1]),
    season: seasonText == null ? null : Number(seasonText),
    week, filter, club: params.get('club') ?? '',
    ...(isValidBackHash(params.get('from')) ? { backHash: params.get('from') } : {}),
  }
}
