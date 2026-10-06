export const ZERO_REASONS = {
  DID_NOT_PLAY: 'Không ra sân · đã xác nhận',
  SOFASCORE_UNRATED_CONFIRMED: 'SofaScore không chấm · đã xác nhận',
  SOFASCORE_RATING: 'Rating SofaScore',
}
export const formatPoints = value => new Intl.NumberFormat('vi-VN', {
  minimumFractionDigits: 2, maximumFractionDigits: 2,
}).format(value)
const points = value => typeof value === 'number' && Number.isFinite(value) && value >= 0
export function publishedPlayerPoints(data, accountId, gameweek) {
  if (!data || !validResult(data, accountId, gameweek) || data.status !== 'PUBLISHED') return new Map()
  return new Map(data.result.players.map(player => [player.playerId, player.points]))
}
export function validResult(data, accountId, gameweek) {
  if (data?.accountId !== accountId || data.season !== 2026 || data.gameweek !== gameweek) return false
  if (['NOT_PARTICIPATING', 'AWAITING_RESULTS'].includes(data.status)) {
    return data.result === null && data.version === null && data.publishedAt === null
  }
  const result = data.result
  return data.status === 'PUBLISHED' && Number.isInteger(data.version) && data.version > 0 &&
    typeof data.publishedAt === 'string' && Number.isFinite(Date.parse(data.publishedAt)) &&
    typeof result?.formation === 'string' && points(result.totalPoints) &&
    Array.isArray(result.players) && result.players.length === 11 &&
    new Set(result.players.map(p => p.playerId)).size === 11 &&
    new Set(result.players.map(p => p.slotKey)).size === 11 &&
    result.players.every(p => Number.isInteger(p.playerId) && p.playerId > 0 && typeof p.name === 'string' &&
      typeof p.club === 'string' && typeof p.position === 'string' && points(p.points) &&
      Array.isArray(p.matches) && p.matches.length > 0 &&
      new Set(p.matches.map(m => m.fixtureId)).size === p.matches.length &&
      p.matches.every(m => Number.isInteger(m.fixtureId) && points(m.points) && ZERO_REASONS[m.reason] &&
        (m.reason === 'SOFASCORE_RATING' ? points(m.rating) && m.points === m.rating : m.rating === null && m.points === 0)))
}
