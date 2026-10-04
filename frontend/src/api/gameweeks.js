import { fetchApiJson } from './request.js'
import { GAMEWEEK_LABELS } from '../fantasy/gameweek.js'

const timestamp = value => typeof value === 'string' && value.includes('T') && Number.isFinite(Date.parse(value))
export async function fetchGameweeks(signal) {
  const data = await fetchApiJson('/api/fantasy/2026/gameweeks', signal)
  if (!timestamp(data?.serverTimeUtc) || !Array.isArray(data.gameweeks) || data.gameweeks.length !== 38 ||
      !(data.recommendedGameweek === null || Number.isInteger(data.recommendedGameweek) && data.recommendedGameweek >= 6 && data.recommendedGameweek <= 38) ||
      data.gameweeks.some((v, index) => v.season !== 2026 || v.gameweek !== index + 1 ||
        typeof v.configured !== 'boolean' || typeof v.canEdit !== 'boolean' || !Array.isArray(v.deadlineChanges) ||
        (v.configured ? !timestamp(v.deadlineUtc) || !timestamp(v.deadlinePublishedAt) || !GAMEWEEK_LABELS[v.status] || v.canEdit !== (v.status === 'OPEN')
          : v.status !== null || v.deadlineUtc !== null || v.canEdit))) {
    throw new Error('Thông tin Gameweek không đúng định dạng.')
  }
  return data
}
