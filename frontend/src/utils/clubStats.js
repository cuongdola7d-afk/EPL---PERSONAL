import { stripClubSuffix } from './clubView.js'
import { clubVisual } from './matchView.js'

const SURNAME_PARTICLES = new Set([
  'van', 'von', 'de', 'del', 'della', 'di', 'da', 'do', 'dos', 'das',
  'el', 'al', 'le', 'la', 'les', 'den', 'der', 'ten', 'ter', 'st', 'san', 'ben', 'mac', 'mc',
])

export function shortSurname(name) {
  if (typeof name !== 'string') return ''
  const parts = name.trim().split(/\s+/).filter(Boolean)
  if (parts.length <= 1) return parts[0] ?? ''
  let start = parts.length - 1
  while (start > 1 && SURNAME_PARTICLES.has(parts[start - 1].toLowerCase())) {
    start -= 1
  }
  if (start === 1 && SURNAME_PARTICLES.has(parts[0].toLowerCase())) {
    start = 0
  }
  return parts.slice(start).join(' ')
}

export function normalizeMatchesFromFixtures(doneMatches, clubId) {
  if (!Array.isArray(doneMatches)) return []
  return [...doneMatches]
    .reverse()
    .filter(m => m && m.status === 'FINISHED' && m.homeGoals != null && m.awayGoals != null)
    .map(m => {
      const home = m.homeClubId === clubId
      const opponentName = home ? m.awayClub : m.homeClub
      const goalsFor = home ? m.homeGoals : m.awayGoals
      const goalsAgainst = home ? m.awayGoals : m.homeGoals
      const result = goalsFor > goalsAgainst ? 'W' : goalsFor < goalsAgainst ? 'L' : 'D'
      return {
        fixtureId: m.id,
        matchweek: m.matchweek,
        home,
        opponentName,
        opponentClean: stripClubSuffix(opponentName),
        opponentCode: clubVisual(opponentName).code,
        goalsFor,
        goalsAgainst,
        result,
        points: result === 'W' ? 3 : result === 'D' ? 1 : 0,
        teamRating: null,
        xgFor: null,
        xgAgainst: null,
      }
    })
}

export function normalizeMatchesFromTrends(trendMatches) {
  if (!Array.isArray(trendMatches)) return []
  return trendMatches.map(m => {
    const result = m.goalsFor > m.goalsAgainst ? 'W' : m.goalsFor < m.goalsAgainst ? 'L' : 'D'
    return {
      fixtureId: m.fixtureId,
      matchweek: m.matchweek,
      home: m.home,
      opponentName: m.opponentName,
      opponentClean: stripClubSuffix(m.opponentName),
      opponentCode: clubVisual(m.opponentName).code,
      goalsFor: m.goalsFor,
      goalsAgainst: m.goalsAgainst,
      result,
      points: result === 'W' ? 3 : result === 'D' ? 1 : 0,
      teamRating: m.teamRating ?? null,
      xgFor: m.xgFor ?? null,
      xgAgainst: m.xgAgainst ?? null,
    }
  })
}

export function summarizeResults(matches) {
  let won = 0
  let drawn = 0
  let lost = 0
  let goalsFor = 0
  let goalsAgainst = 0
  let cleanSheets = 0
  for (const m of matches ?? []) {
    goalsFor += m.goalsFor
    goalsAgainst += m.goalsAgainst
    if (m.goalsAgainst === 0) cleanSheets += 1
    if (m.goalsFor > m.goalsAgainst) won += 1
    else if (m.goalsFor < m.goalsAgainst) lost += 1
    else drawn += 1
  }
  const played = won + drawn + lost
  const points = won * 3 + drawn
  return { played, won, drawn, lost, points, goalsFor, goalsAgainst, cleanSheets }
}

export function countCleanSheets(matches) {
  return (matches ?? []).reduce((acc, m) => acc + (m.goalsAgainst === 0 ? 1 : 0), 0)
}

export function splitHomeAway(matches) {
  const homeMatches = []
  const awayMatches = []
  for (const m of matches ?? []) {
    if (m.home) homeMatches.push(m)
    else awayMatches.push(m)
  }
  return {
    home: summarizeResults(homeMatches),
    away: summarizeResults(awayMatches),
  }
}

export function cumulativePointsSeries(matches) {
  let total = 0
  return (matches ?? []).map(m => {
    const pts = m.goalsFor > m.goalsAgainst ? 3 : m.goalsFor === m.goalsAgainst ? 1 : 0
    total += pts
    return { ...m, matchPoints: pts, cumulativePoints: total }
  })
}

export function summarizeXgTotals(matches, xgField, goalsField) {
  let goals = 0
  let xg = 0
  let count = 0
  for (const m of matches ?? []) {
    if (typeof m[xgField] === 'number' && Number.isFinite(m[xgField])) {
      goals += m[goalsField]
      xg += m[xgField]
      count += 1
    }
  }
  const roundedXg = Math.round(xg * 100) / 100
  const delta = Math.round((goals - roundedXg) * 10) / 10
  return { count, goals, xg: roundedXg, delta }
}

export function buildContributionPieSlices(entries, { ownGoals = 0, minOwnSlice = 2, maxNamedSlices = 4 } = {}) {
  const positive = (entries ?? []).filter(entry => entry && typeof entry.value === 'number' && entry.value > 0)
  const safeOwnGoals = Number.isInteger(ownGoals) && ownGoals > 0 ? ownGoals : 0
  const total = positive.reduce((sum, entry) => sum + entry.value, 0) + safeOwnGoals
  if (total === 0) return { total: 0, slices: [] }

  const qualifyingIndices = []
  for (let i = 0; i < positive.length; i += 1) {
    if (positive[i].value >= minOwnSlice) qualifyingIndices.push(i)
  }

  const chosenIndices = qualifyingIndices.length > 0
    ? qualifyingIndices.slice(0, maxNamedSlices)
    : positive.slice(0, maxNamedSlices).map((_, i) => i)
  const chosenSet = new Set(chosenIndices)

  const slices = chosenIndices.map(idx => {
    const entry = positive[idx]
    const label = shortSurname(entry.name)
    return {
      label,
      fullName: entry.name,
      value: entry.value,
      percent: Math.round((entry.value / total) * 100),
      isOther: false,
      tooltipMembers: [],
    }
  })

  let otherValue = 0
  const otherMembers = []
  for (let i = 0; i < positive.length; i += 1) {
    if (!chosenSet.has(i)) {
      otherValue += positive[i].value
      otherMembers.push(shortSurname(positive[i].name))
    }
  }
  if (safeOwnGoals > 0) {
    otherValue += safeOwnGoals
    otherMembers.push('phản lưới nhà')
  }

  if (otherValue > 0) {
    slices.push({
      label: 'Khác',
      fullName: 'Khác',
      value: otherValue,
      percent: Math.round((otherValue / total) * 100),
      isOther: true,
      tooltipMembers: otherMembers,
    })
  }

  return { total, slices }
}

export function buildXAxisTicks(items, plotWidth, { minSpacingPx = 26, fullModeMinSlotPx = 38 } = {}) {
  const count = Array.isArray(items) ? items.length : 0
  if (count === 0 || !Number.isFinite(plotWidth) || plotWidth <= 0) {
    return { compact: false, showOpponent: false, step: 1, ticks: [] }
  }
  const slotWidth = plotWidth / count
  const compact = slotWidth < fullModeMinSlotPx
  const showOpponent = !compact
  const step = compact ? Math.max(1, Math.ceil(minSpacingPx / slotWidth)) : 1

  const ticks = []
  for (let i = 0; i < count; i += step) {
    const item = items[i]
    const mw = item?.matchweek ?? i + 1
    ticks.push({
      index: i,
      label: compact ? String(mw) : `GW${mw}`,
      sublabel: showOpponent ? (item?.opponentCode ?? null) : null,
      centerOffset: slotWidth * (i + 0.5),
    })
  }
  return { compact, showOpponent, step, ticks }
}

export function formatSignedDelta(value) {
  const rounded = Math.round(value * 10) / 10
  if (Object.is(rounded, -0) || rounded === 0) return '0.0'
  return rounded < 0 ? `−${Math.abs(rounded).toFixed(1)}` : `+${rounded.toFixed(1)}`
}
