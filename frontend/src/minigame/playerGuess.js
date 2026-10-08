export const HINT_KEYS = ['height', 'foot', 'age', 'ovr', 'nationality', 'position', 'club', 'shirtNumber']
export const isFinished = game => !!game && game.status !== 'IN_PROGRESS'
export function parseMinigameRoute(hash) {
  if (hash === '#minigame') return { mode: null }
  const match = /^#minigame\/guess\/(daily|practice)$/.exec(hash)
  return match ? { mode: match[1].toUpperCase() } : null
}
export const gameHash = mode => `#minigame/guess/${mode.toLowerCase()}`
// Advance from the server timestamp with a monotonic clock, not the computer's date.
export function remainingSeconds(serverTime, deadline, elapsedMs = 0) {
  const duration = Date.parse(deadline) - Date.parse(serverTime) - elapsedMs
  return Number.isFinite(duration) ? Math.max(0, Math.ceil(duration / 1000)) : null
}
export function formatCountdown(seconds) {
  if (seconds === null) return '—:—:—'
  return [Math.floor(seconds / 3600), Math.floor(seconds / 60) % 60, seconds % 60]
    .map(value => String(value).padStart(2, '0')).join(':')
}
export function createAction(kind, game, playerId, actionId = crypto.randomUUID()) {
  const body = { actionId, expectedVersion: game?.version ?? 0 }
  if (kind === 'start') body.expectedGameId = game?.gameId ?? null
  if (kind === 'guess') body.player_id = playerId
  return { kind, gameId: game?.gameId ?? null, body }
}
