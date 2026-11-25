const isFiniteNumber = value => typeof value === 'number' && Number.isFinite(value)

export async function fetchLatestFantasyResult(signal) {
  const response = await fetch('/api/fantasy/2026/latest-result-summary', {
    credentials: 'include',
    headers: { Accept: 'application/json' },
    signal,
  })
  if (response.status === 204) return { gameweek: null }
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  const data = await response.json()
  if (!data || typeof data !== 'object') throw new Error('Invalid latest result summary payload')
  if (data.gameweek === null) return { gameweek: null }
  if (!Number.isInteger(data.gameweek) || data.gameweek < 6 || data.gameweek > 38) {
    throw new Error('Invalid gameweek in latest result summary')
  }
  if (!Number.isInteger(data.participants) || data.participants < 0) {
    throw new Error('Invalid participants count in latest result summary')
  }
  if (data.averagePoints !== null && !isFiniteNumber(data.averagePoints)) {
    throw new Error('Invalid averagePoints in latest result summary')
  }
  return data
}
