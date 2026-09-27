export function playerDetailHash(id, season) {
  return `#players/${encodeURIComponent(id)}?season=${encodeURIComponent(season)}`
}

export function parsePlayerDetailHash(hash) {
  const match = /^#players\/([1-9]\d*)\?season=(2024|2026)$/.exec(hash)
  return match ? { playerId: Number(match[1]), season: Number(match[2]) } : null
}
