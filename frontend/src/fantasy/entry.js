export const emptyLineup = () => ({ formation: '4-2-1-3', picks: {}, unassigned: [] })
export function entryLineup(data) {
  const lineup = data?.draft ?? data?.submitted
  return lineup ? { formation: lineup.formation, picks: { ...lineup.picks }, unassigned: [] } : emptyLineup()
}
export function sameLineup(left, right) {
  if (!left || !right || left.formation !== right.formation) return false
  const a = Object.entries(left.picks ?? {}).sort(([a], [b]) => a.localeCompare(b))
  const b = Object.entries(right.picks ?? {}).sort(([a], [b]) => a.localeCompare(b))
  return JSON.stringify(a) === JSON.stringify(b) && !(left.unassigned?.length || right.unassigned?.length)
}
