import { parseMatchRoute } from './matchRoute.js'

const POSITIONS = new Set(['', 'GOALKEEPER', 'DEFENDER', 'MIDFIELDER', 'FORWARD'])
const SORTS = new Set(['overall', 'name', 'club', 'goals', 'assists'])

export function playerListHash({ season, query = '', club = '', position = '', view = 'grid',
                                 sortBy = 'overall', direction = 'desc', visibleCount = 12 }) {
  const params = new URLSearchParams({ season: String(season) })
  if (query) params.set('q', query)
  if (club) params.set('club', club)
  if (position) params.set('position', position)
  if (view !== 'grid') params.set('view', view)
  if (sortBy !== 'overall') params.set('sort', sortBy)
  if (direction !== 'desc') params.set('direction', direction)
  if (visibleCount !== 12) params.set('count', String(visibleCount))
  return `#players?${params}`
}

export function parsePlayerListHash(hash) {
  const match = /^#players(?:\?(.*))?$/.exec(hash)
  if (!match) return null
  const params = new URLSearchParams(match[1] ?? '')
  const seasonText = params.get('season')
  if (seasonText && !['2024', '2026'].includes(seasonText)) return null
  const position = params.get('position') ?? ''
  const view = params.get('view') ?? 'grid'
  const sortBy = params.get('sort') ?? 'overall'
  const direction = params.get('direction') ?? 'desc'
  const countText = params.get('count')
  const visibleCount = countText == null ? 12 : Number(countText)
  if (!POSITIONS.has(position) || !['grid', 'list'].includes(view) || !SORTS.has(sortBy) ||
      !['asc', 'desc'].includes(direction) || !Number.isInteger(visibleCount) ||
      visibleCount < 12 || visibleCount > 10000) return null
  return { season: seasonText == null ? null : Number(seasonText), query: params.get('q') ?? '',
    club: params.get('club') ?? '', position, view, sortBy, direction, visibleCount }
}

function safeBackHash(hash) {
  if (typeof hash !== 'string') return null
  const valid = parseMatchRoute(hash) || parsePlayerListHash(hash) ||
    ['#clubs', '#standings', '#fantasy'].includes(hash)
  return valid ? hash : null
}

export function playerDetailHash(id, season, fromHash) {
  const params = new URLSearchParams({ season: String(season) })
  const safeFrom = safeBackHash(fromHash)
  if (safeFrom) params.set('from', safeFrom)
  return `#players/${encodeURIComponent(id)}?${params}`
}

export function parsePlayerDetailHash(hash) {
  const match = /^#players\/([1-9]\d*)\?(.*)$/.exec(hash)
  if (!match) return null
  const params = new URLSearchParams(match[2])
  const season = Number(params.get('season'))
  if (![2024, 2026].includes(season)) return null
  const result = { playerId: Number(match[1]), season }
  const backHash = safeBackHash(params.get('from'))
  return backHash ? { ...result, backHash } : result
}
