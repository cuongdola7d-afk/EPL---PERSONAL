const COLORS = [
  ['arsenal', '#e0413a'], ['liverpool', '#c8102e'], ['manchester city', '#5ba3d9'],
  ['manchester united', '#da291c'], ['chelsea', '#1f4fa3'], ['newcastle', '#4a545b'],
  ['aston villa', '#7b1d4a'], ['everton', '#1c5bb0'], ['bournemouth', '#ad252f'],
  ['brentford', '#c92834'], ['brighton', '#2362b0'], ['crystal palace', '#2d5eaa'],
  ['fulham', '#48535e'], ['leeds', '#e0b52c'], ['sunderland', '#c72d37'],
  ['nottingham forest', '#d42329'], ['tottenham', '#29436b'], ['ipswich', '#315ba5'],
  ['hull', '#d89720'], ['coventry', '#58a5d0'],
]

const CODES = [
  ['bournemouth', 'BOU'], ['arsenal', 'ARS'], ['aston villa', 'AVL'],
  ['brentford', 'BRE'], ['brighton', 'BHA'], ['chelsea', 'CHE'],
  ['crystal palace', 'CRY'], ['everton', 'EVE'], ['fulham', 'FUL'],
  ['hull', 'HUL'], ['ipswich', 'IPS'], ['leeds', 'LEE'],
  ['liverpool', 'LIV'], ['manchester city', 'MCI'], ['manchester united', 'MUN'],
  ['newcastle', 'NEW'], ['nottingham forest', 'NFO'], ['sunderland', 'SUN'],
  ['tottenham', 'TOT'], ['coventry', 'COV'],
]

export function clubVisual(name) {
  const lower = name.toLowerCase()
  return {
    color: COLORS.find(([part]) => lower.includes(part))?.[1] ?? '#487783',
    code: CODES.find(([part]) => lower.includes(part))?.[1] ??
      name.split(/\s+/).filter(Boolean).slice(0, 3).map((part) => part[0]).join('').toUpperCase(),
  }
}

export function preferredMatchweek(matches) {
  const weeks = [...new Set(matches.map((match) => match.matchweek))].sort((a, b) => a - b)
  const complete = weeks.filter((week) => {
    const fixtures = matches.filter((match) => match.matchweek === week)
    return fixtures.length === 10 && fixtures.every((match) => match.status === 'FINISHED')
  })
  return complete.at(-1) ?? weeks[0] ?? 1
}

export function matchPlayerState(player) {
  if (player.participationStatus === 'PLAYED') return 'played'
  if (player.participationStatus === 'DID_NOT_PLAY') return 'did-not-play'
  const minutes = player.minutes ?? player.inferred?.minutes
  if (minutes === 0) return 'did-not-play'
  if (minutes != null) return 'played'
  return 'unknown'
}

export function matchPlayerValue(player, field, evidenceStatus) {
  return player[field] ?? (evidenceStatus === 'VERIFIED' ? player.inferred?.[field] : null) ?? null
}

export function matchRating(player) {
  if (player.rating == null) return null
  const value = Number(player.rating)
  return Number.isFinite(value) ? value : null
}
