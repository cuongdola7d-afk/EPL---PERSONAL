import { buildApiUrl, fetchApiJson, fetchApiList } from './request.js'
import { isValidPlayer } from './players.js'
import { FANTASY_AS_OF } from '../fantasy/lineup.js'

export function isValidFantasyPlayer(player) {
  return isValidPlayer(player) && Number.isInteger(player.id) && player.id > 0 &&
    Number.isInteger(player.clubId) && player.clubId > 0 &&
    (player.primaryPosition === null || typeof player.primaryPosition === 'string') &&
    Array.isArray(player.eligiblePositions) && player.eligiblePositions.every(code => typeof code === 'string') &&
    ['VERIFIED', 'MISSING'].includes(player.positionStatus)
}

export async function fetchFantasyPlayers(signal, gameweek = null, rosterAsOf = null) {
  if (gameweek !== null) {
    const data = await fetchApiJson(`/api/fantasy/2026/gameweeks/${gameweek}/players`, signal)
    if (data?.season !== 2026 || data.gameweek !== gameweek || !rosterAsOf || data.rosterAsOf !== rosterAsOf ||
        !Array.isArray(data.players) || data.players.some(player => !isValidFantasyPlayer(player)))
      throw new Error('Mốc roster của GW không khớp. Tải lại trạng thái vòng trước khi chọn đội.')
    return data.players
  }
  // Fixed reference belongs only to the guest practice builder.
  return fetchApiList(`/api/players?season=2026&asOf=${FANTASY_AS_OF}`, signal,
    isValidFantasyPlayer, 'cầu thủ Fantasy')
}

export async function checkFantasyLineup(formation, picks, gameweek = null) {
  const path = `/api/fantasy/2026/validate${gameweek === null ? '' : `?gameweek=${gameweek}`}`
  const response = await fetch(buildApiUrl(path,
    import.meta.env?.VITE_API_BASE_URL, import.meta.env?.DEV ?? true), {
    method: 'POST', headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify({ formation, picks }),
  })
  if (!response.ok) throw new Error(`Không kiểm tra được đội hình: HTTP ${response.status}.`)
  const result = await response.json()
  if (typeof result.valid !== 'boolean' || !Number.isInteger(result.totalOvr) ||
      !Array.isArray(result.issues) || result.issues.some(issue => typeof issue.message !== 'string'))
    throw new Error('Kết quả kiểm tra đội hình không đúng định dạng.')
  return result
}
