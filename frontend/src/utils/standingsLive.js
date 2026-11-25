import { formatLiveMinuteLabel } from './liveScores.js'
import { clubVisual } from './matchView.js'

const ACTIVE_LIVE_STATUSES = new Set(['LIVE', 'PAUSED', 'HT'])

function isValidScore(value) {
  return Number.isInteger(value) && value >= 0
}

export function formatLiveChipMinute(liveState, fixture = null, nowMs = Date.now()) {
  if (!liveState || typeof liveState !== 'object') return ''
  if (typeof liveState.minuteLabel === 'string' && liveState.minuteLabel.trim()) {
    return liveState.minuteLabel.trim()
  }
  if (
    liveState.status === 'PAUSED' ||
    liveState.status === 'HT' ||
    liveState.phase === 'HT' ||
    liveState.halftime === true ||
    (liveState.status === 'LIVE' && fixture?.status === 'PAUSED')
  ) {
    return 'Nghỉ giữa hiệp'
  }
  if (liveState.status === 'FT') {
    return 'FT'
  }
  if (liveState.status === 'LIVE') {
    return formatLiveMinuteLabel(liveState, nowMs) || 'Live'
  }
  return ''
}

function matchOutcome(goalsFor, goalsAgainst) {
  if (goalsFor > goalsAgainst) return 'W'
  if (goalsFor < goalsAgainst) return 'L'
  return 'D'
}

function outcomePoints(result) {
  if (result === 'W') return 3
  if (result === 'D') return 1
  return 0
}

function baseRow(row, officialIndex) {
  return {
    ...row,
    officialPosition: row.position,
    officialIndex,
    livePosition: row.position,
    rankDelta: 0,
    isLive: false,
    liveChip: null,
    pointsDelta: null,
    provisionalFormResult: null,
  }
}

/**
 * Pure function that overlays eligible live/unrecorded-FT matches onto official standings.
 * Never double-counts a match already reflected in officialRows.played, and warns without
 * mutating any club whose played count is inconsistent with finished fixtures.
 */
export function applyLiveToStandings(officialRows, fixtures, liveByFixture, options = {}) {
  if (!Array.isArray(officialRows) || officialRows.length === 0) {
    return []
  }
  const { nowMs = Date.now(), warn = console.warn } = options
  const rows = officialRows.map((row, index) => baseRow(row, index))

  if (!liveByFixture || typeof liveByFixture !== 'object') {
    return rows
  }
  if (!Array.isArray(fixtures) || fixtures.length === 0) {
    const anyPlayed = rows.some((row) => row.played > 0)
    if (anyPlayed) return rows
  }

  const fixtureById = new Map()
  const finishedCountByClub = new Map()
  for (const fixture of fixtures ?? []) {
    if (!fixture || !Number.isInteger(fixture.id)) continue
    fixtureById.set(fixture.id, fixture)
    if (fixture.status === 'FINISHED') {
      if (Number.isInteger(fixture.homeClubId)) {
        finishedCountByClub.set(fixture.homeClubId, (finishedCountByClub.get(fixture.homeClubId) ?? 0) + 1)
      }
      if (Number.isInteger(fixture.awayClubId)) {
        finishedCountByClub.set(fixture.awayClubId, (finishedCountByClub.get(fixture.awayClubId) ?? 0) + 1)
      }
    }
  }

  const pendingByClub = new Map()
  for (const [key, liveState] of Object.entries(liveByFixture)) {
    if (!liveState || typeof liveState !== 'object') continue
    const fixtureId = Number.isInteger(liveState.fixtureId) ? liveState.fixtureId : Number(key)
    const fixture = fixtureById.get(fixtureId)
    if (!fixture) continue
    // If DB fixture is already FINISHED, its result belongs to the official table; never overlay.
    if (fixture.status === 'FINISHED') continue

    const isActiveLive = ACTIVE_LIVE_STATUSES.has(liveState.status)
    const isUnrecordedFt = liveState.status === 'FT'
    if (!isActiveLive && !isUnrecordedFt) continue
    if (!isValidScore(liveState.homeScore) || !isValidScore(liveState.awayScore)) continue

    const entry = { fixture, liveState }
    if (Number.isInteger(fixture.homeClubId)) {
      if (!pendingByClub.has(fixture.homeClubId)) pendingByClub.set(fixture.homeClubId, [])
      pendingByClub.get(fixture.homeClubId).push({ ...entry, isHome: true })
    }
    if (Number.isInteger(fixture.awayClubId)) {
      if (!pendingByClub.has(fixture.awayClubId)) pendingByClub.set(fixture.awayClubId, [])
      pendingByClub.get(fixture.awayClubId).push({ ...entry, isHome: false })
    }
  }

  if (pendingByClub.size === 0) {
    return rows
  }

  let anyApplied = false
  for (const row of rows) {
    const pending = pendingByClub.get(row.clubId)
    if (!pending || pending.length === 0) continue

    const finishedCount = finishedCountByClub.get(row.clubId) ?? 0
    if (row.played === finishedCount + pending.length) {
      // Official standings already reflect this match; skip to prevent double counting.
      continue
    }
    if (row.played !== finishedCount) {
      warn?.(
        `[LiveStandings] Skipped club ${row.clubName} (${row.clubId}): standings.played=${row.played}, finishedFixtures=${finishedCount}, pendingLive=${pending.length}`
      )
      continue
    }

    let addWon = 0
    let addDrawn = 0
    let addLost = 0
    let addGf = 0
    let addGa = 0
    let addPts = 0
    let latestChip = null
    let latestResult = null

    for (const item of pending) {
      const { fixture, liveState, isHome } = item
      const gf = isHome ? liveState.homeScore : liveState.awayScore
      const ga = isHome ? liveState.awayScore : liveState.homeScore
      const result = matchOutcome(gf, ga)
      if (result === 'W') addWon += 1
      else if (result === 'D') addDrawn += 1
      else addLost += 1
      addGf += gf
      addGa += ga
      addPts += outcomePoints(result)
      latestResult = result

      const homeCode = clubVisual(fixture.homeClub ?? '').code
      const awayCode = clubVisual(fixture.awayClub ?? '').code
      const minuteLabel = formatLiveChipMinute(liveState, fixture, nowMs)
      latestChip = {
        fixtureId: fixture.id,
        homeCode,
        awayCode,
        homeScore: liveState.homeScore,
        awayScore: liveState.awayScore,
        minuteLabel,
        result,
      }
    }

    row.played += pending.length
    row.won += addWon
    row.drawn += addDrawn
    row.lost += addLost
    row.goalsFor += addGf
    row.goalsAgainst += addGa
    row.goalDifference = row.goalsFor - row.goalsAgainst
    row.points += addPts
    row.isLive = true
    row.liveChip = latestChip
    row.pointsDelta = addPts
    row.provisionalFormResult = latestResult
    anyApplied = true
  }

  if (!anyApplied) {
    return rows
  }

  rows.sort(
    (a, b) =>
      b.points - a.points ||
      b.goalDifference - a.goalDifference ||
      b.goalsFor - a.goalsFor ||
      a.officialIndex - b.officialIndex
  )

  rows.forEach((row, index) => {
    const livePos = index + 1
    row.livePosition = livePos
    row.position = livePos
    row.rankDelta = row.officialPosition - livePos
  })

  return rows
}
