import { fetchApiList } from './request.js'
import { isValidPlayer } from './players.js'
import { FANTASY_AS_OF } from '../fantasy/lineup.js'

export function fetchFantasyPlayers(signal) {
  return fetchApiList(`/api/players?season=2026&asOf=${FANTASY_AS_OF}`, signal,
    isValidPlayer, 'cầu thủ Fantasy')
}
