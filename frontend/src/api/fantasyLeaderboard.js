import { fetchApiJson } from './request.js'

export async function fetchFantasyLeaderboard(gameweek, signal) {
  const path = '/api/fantasy/2026/leaderboard' + (gameweek === null ? '' : '?gameweek=' + gameweek)
  const data = await fetchApiJson(path, signal)
  if (data?.season !== 2026 || data.gameweek !== gameweek || !['AWAITING_RESULTS', 'PUBLISHED'].includes(data.status) ||
      !Number.isInteger(data.publishedGameweeks) || !Array.isArray(data.players) ||
      (data.status === 'AWAITING_RESULTS' && data.players.length !== 0) ||
      data.players.some(player => !Number.isInteger(player.rank) || player.rank < 1 || !Number.isInteger(player.accountId) ||
        typeof player.displayName !== 'string' || typeof player.totalPoints !== 'number' || !Number.isFinite(player.totalPoints) ||
        player.totalPoints < 0 || !Number.isInteger(player.gameweeksPlayed) || player.gameweeksPlayed < 1)) {
    throw new Error('Bảng xếp hạng người chơi không đúng định dạng.')
  }
  return data
}
