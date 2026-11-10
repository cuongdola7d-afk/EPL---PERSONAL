export const TEAM_OF_WEEK_SLOTS = ['GK', 'LB', 'LCB', 'RCB', 'RB', 'LCM', 'CM', 'RCM', 'LW', 'ST', 'RW']
export const TEAM_OF_WEEK_STORAGE_KEY = 'prismaxi-team-of-week-2026-gameweek'

export function readTeamOfWeekSelection(storage) {
  try { return JSON.parse((storage ?? globalThis.localStorage).getItem(TEAM_OF_WEEK_STORAGE_KEY) ?? 'null') }
  catch { return null }
}

export function saveTeamOfWeekSelection(gameweek, storage) {
  // This public viewing preference is independent of the saved Fantasy lineup.
  try {
    const target = storage ?? globalThis.localStorage
    if (gameweek === null) target.removeItem(TEAM_OF_WEEK_STORAGE_KEY)
    else target.setItem(TEAM_OF_WEEK_STORAGE_KEY, JSON.stringify(gameweek))
  } catch { /* Viewing still works when browser storage is unavailable. */ }
}

// Premier League: a complete round schedules 20 clubs in ten distinct fixtures.
// Only schedule status matters; contest configuration and rating coverage do not.
export function completedTeamOfWeekGameweeks(matches) {
  const rounds = new Map()
  for (const match of matches) {
    if (!Number.isInteger(match.matchweek) || match.matchweek < 1 || match.matchweek > 38) continue
    const round = rounds.get(match.matchweek) ?? []
    round.push(match)
    rounds.set(match.matchweek, round)
  }
  return [...rounds].filter(([, fixtures]) => fixtures.length === 10 &&
    new Set(fixtures.map(match => match.id)).size === 10 &&
    new Set(fixtures.flatMap(match => [match.homeClubId, match.awayClubId])).size === 20 &&
    fixtures.every(match => match.status === 'FINISHED')).map(([week]) => week).sort((a, b) => a - b)
}

export function teamOfWeekGameweek(completedGameweeks, selection) {
  // Accept old persisted selection shapes too, but revalidate against the schedule.
  const selected = typeof selection === 'object' && selection !== null ? selection.gameweek : selection
  return completedGameweeks.includes(selected) ? selected : Math.max(...completedGameweeks, 0) || null
}
const permission = slot => ({ LCB: 'CB', RCB: 'CB', LCM: 'CM', RCM: 'CM' })[slot] ?? slot

export function isValidTeamOfWeek(team, gameweek) {
  if (!team || team.season !== 2026 || team.gameweek !== gameweek || team.formation !== '4-3-3' ||
      !['COMPLETE', 'INSUFFICIENT_DATA', 'MULTIPLE_MATCHES'].includes(team.status) ||
      !Array.isArray(team.picks) || team.picks.length !== 11 || !Array.isArray(team.missingSlots) ||
      !Array.isArray(team.conflicts) || !['playedCount', 'ratedCount', 'candidateCount', 'excludedMissingPositions',
        'excludedNullRatings', 'completedFixtures', 'recordedFixtures'].every(key => Number.isInteger(team[key]) && team[key] >= 0)) return false
  const seen = new Set()
  const missing = []
  for (let index = 0; index < 11; index++) {
    const pick = team.picks[index]
    if (!pick || pick.slot !== TEAM_OF_WEEK_SLOTS[index]) return false
    const player = pick.player
    if (player === null) { missing.push(pick.slot); continue }
    if (!player || !Number.isInteger(player.playerId) || player.playerId <= 0 || seen.has(player.playerId) ||
        typeof player.name !== 'string' || typeof player.club !== 'string' || !Number.isInteger(player.clubId) ||
        !Number.isInteger(player.fixtureId) || !Number.isFinite(player.rating) || !Array.isArray(player.eligiblePositions) ||
        !player.eligiblePositions.includes(permission(pick.slot))) return false
    seen.add(player.playerId)
  }
  if (missing.join(',') !== team.missingSlots.join(',')) return false
  if (team.status === 'COMPLETE') {
    return missing.length === 0 && team.conflicts.length === 0 && Number.isFinite(team.totalRating) &&
      Math.round(team.totalRating * 100) === team.picks.reduce((sum, pick) => sum + Math.round(pick.player.rating * 100), 0)
  }
  return missing.length > 0 && team.totalRating === null && (team.status !== 'MULTIPLE_MATCHES' ||
    seen.size === 0 && team.conflicts.length > 0 && team.conflicts.every(conflict =>
      Number.isInteger(conflict.playerId) && typeof conflict.name === 'string' &&
      Array.isArray(conflict.fixtureIds) && conflict.fixtureIds.length > 1 && conflict.fixtureIds.every(Number.isInteger)))
}
