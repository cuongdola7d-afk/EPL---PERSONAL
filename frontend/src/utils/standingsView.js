import { clubMatchResult } from './clubView.js'

export function recentClubForm(matches, clubId) {
  return matches.filter(match => (match.homeClubId === clubId || match.awayClubId === clubId) &&
    clubMatchResult(match, clubId) !== null)
    .sort((a, b) => b.date.localeCompare(a.date) || b.id - a.id)
    .slice(0, 5).reverse().map(match => ({ match, result: clubMatchResult(match, clubId) }))
}

export function playerLeaders(players, metric, limit = 7) {
  return players.filter(player => Number.isFinite(player[metric]) &&
    (metric !== 'averageRating' || player.ratedAppearances > 0))
    .sort((a, b) => b[metric] - a[metric] || a.name.localeCompare(b.name, 'vi') || a.id - b.id)
    .slice(0, limit)
}

export function mergePlayerRatings(players, clubStatistics) {
  const ratings = new Map()
  for (const club of clubStatistics) {
    for (const row of club.players) {
      if (row.averageRating == null || row.ratedAppearances === 0) continue
      const total = ratings.get(row.playerId) ?? { sum: 0, count: 0 }
      total.sum += row.averageRating * row.ratedAppearances
      total.count += row.ratedAppearances
      ratings.set(row.playerId, total)
    }
  }
  return players.map(player => {
    const total = ratings.get(player.id)
    return { ...player, averageRating: total ? total.sum / total.count : null,
      ratedAppearances: total?.count ?? 0 }
  })
}
