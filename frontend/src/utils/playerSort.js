const textCompare = (a, b) => a.localeCompare(b, 'vi', { sensitivity: 'base' })

export function sortPlayers(players, sortBy = 'overall', direction = 'desc') {
  const factor = direction === 'asc' ? 1 : -1
  return [...players].sort((a, b) => {
    const first = sortBy === 'overall' ? a.fc27Overall : sortBy === 'goals' ? a.goals : sortBy === 'assists' ? a.assists : sortBy === 'club' ? a.club : a.name
    const second = sortBy === 'overall' ? b.fc27Overall : sortBy === 'goals' ? b.goals : sortBy === 'assists' ? b.assists : sortBy === 'club' ? b.club : b.name
    if (first == null && second != null) return 1
    if (first != null && second == null) return -1
    const comparison = typeof first === 'number' && typeof second === 'number'
      ? first - second : textCompare(String(first ?? ''), String(second ?? ''))
    return factor * comparison || textCompare(a.name, b.name) || a.id - b.id
  })
}

export function matchesPlayerSearch(name, query) {
  const normalize = (value) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('vi').replace(/đ/g, 'd')
  return normalize(name).includes(normalize(query.trim()))
}
