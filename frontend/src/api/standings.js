import { fetchApiList } from './request.js'

const NUMBER_FIELDS = [
  'position', 'clubId', 'played', 'won', 'drawn', 'lost',
  'goalsFor', 'goalsAgainst', 'goalDifference', 'points',
]

function isValidStanding(row) {
  return row !== null &&
    typeof row.clubName === 'string' &&
    NUMBER_FIELDS.every((field) => Number.isInteger(row[field]))
}

export function fetchStandings(signal, season = 2024) {
  return fetchApiList(`/api/standings?${new URLSearchParams({ season })}`, signal, isValidStanding, 'bảng xếp hạng')
}
