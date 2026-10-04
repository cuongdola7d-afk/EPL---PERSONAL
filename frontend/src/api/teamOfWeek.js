import { fetchApiJson } from './request.js'
import { isValidTeamOfWeek } from '../fantasy/teamOfWeek.js'

export async function fetchTeamOfWeek(gameweek, signal) {
  const result = await fetchApiJson(`/api/fantasy/2026/team-of-week?gameweek=${gameweek}`, signal,
    'Chưa có mục Đội hình tiêu biểu. Vui lòng thử lại sau.')
  if (!isValidTeamOfWeek(result, gameweek)) throw new Error('Dữ liệu đội hình tiêu biểu không đúng định dạng.')
  return result
}
