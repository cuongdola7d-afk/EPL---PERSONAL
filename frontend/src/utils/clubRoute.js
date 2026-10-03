const TABS = new Set(['standings', 'matches', 'players', 'stats'])

export function clubDetailHash(id, { season = 2026, tab = 'standings', matches = 'done',
                                   players = 'squad', metric = 'rating', count = 5 } = {}) {
  const params = new URLSearchParams({ season: String(season) })
  if (tab !== 'standings') params.set('tab', tab)
  if (matches !== 'done') params.set('matches', matches)
  if (players !== 'squad') params.set('players', players)
  if (metric !== 'rating') params.set('metric', metric)
  if (count !== 5) params.set('count', String(count))
  return `#clubs/${id}?${params}`
}

export function parseClubRoute(hash) {
  const match = /^#clubs\/([1-9]\d*)(?:\?(.*))?$/.exec(hash)
  if (!match || !Number.isSafeInteger(Number(match[1]))) return null
  const params = new URLSearchParams(match[2] ?? '')
  const route = { clubId: Number(match[1]), season: Number(params.get('season') ?? 2026),
    tab: params.get('tab') ?? 'standings', matches: params.get('matches') ?? 'done',
    players: params.get('players') ?? 'squad', metric: params.get('metric') ?? 'rating',
    count: Number(params.get('count') ?? 5) }
  if (![2024, 2026].includes(route.season) || !TABS.has(route.tab) ||
      !['done', 'soon'].includes(route.matches) || !['squad', 'top'].includes(route.players) ||
      !['rating', 'goals', 'assists'].includes(route.metric) ||
      !Number.isInteger(route.count) || route.count < 5 || route.count > 10000) return null
  return route
}
