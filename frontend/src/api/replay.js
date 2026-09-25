import { fetchMatchDetail, fetchMatches } from './matches.js'
import { buildReplayPool } from '../fantasy/replay.js'

export async function fetchReplayPlayers(signal) {
  const matches = await fetchMatches({ season: 2024, matchweek: 1 }, signal)
  const details = []
  for (let start = 0; start < matches.length; start += 3) {
    const batch = matches.slice(start, start + 3)
    details.push(...await Promise.all(batch.map((match) => fetchMatchDetail(match.id, signal, 2024))))
  }
  return buildReplayPool(matches, details)
}
