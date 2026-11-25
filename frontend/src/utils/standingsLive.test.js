import test from 'node:test'
import assert from 'node:assert/strict'
import { applyLiveToStandings, formatLiveChipMinute } from './standingsLive.js'

const T0 = '2026-11-29T14:05:00Z'
const T0_MS = Date.parse(T0)

function row(position, clubId, clubName, played, won, drawn, lost, gf, ga) {
  return {
    position,
    clubId,
    clubName,
    played,
    won,
    drawn,
    lost,
    goalsFor: gf,
    goalsAgainst: ga,
    goalDifference: gf - ga,
    points: won * 3 + drawn,
  }
}

function finishedFixturesFor(clubRows) {
  // Create synthetic FINISHED fixtures so each club has exactly `played` finished matches.
  const list = []
  let id = 1
  for (const r of clubRows) {
    for (let i = 0; i < r.played; i++) {
      list.push({
        id: id++,
        homeClubId: r.clubId,
        awayClubId: 999999,
        homeClub: r.clubName,
        awayClub: 'Opponent',
        status: 'FINISHED',
        homeGoals: 1,
        awayGoals: 0,
        date: `2026-09-${String(i + 1).padStart(2, '0')}`,
        matchweek: i + 1,
      })
    }
  }
  return list
}

function liveFixture(id, homeClubId, homeClub, awayClubId, awayClub, status = 'SCHEDULED') {
  return {
    id,
    homeClubId,
    awayClubId,
    homeClub,
    awayClub,
    status,
    homeGoals: null,
    awayGoals: null,
    date: '2026-11-29',
    matchweek: 12,
  }
}

function liveState(fixtureId, homeScore, awayScore, minute = 58, extra = {}) {
  return {
    fixtureId,
    status: 'LIVE',
    minute,
    homeScore,
    awayScore,
    goals: [],
    assists: [],
    redCards: [],
    updatedAt: T0,
    source: 'FPL',
    ...extra,
  }
}

test('no live matches returns identical standings without arrows, chips, or mutations', () => {
  const official = [
    row(1, 10, 'Arsenal FC', 11, 9, 1, 1, 17, 7),
    row(2, 20, 'Brighton & Hove Albion FC', 11, 7, 1, 3, 26, 9),
  ]
  const fixtures = finishedFixturesFor(official)
  const result = applyLiveToStandings(official, fixtures, {}, { nowMs: T0_MS })

  assert.equal(result.length, 2)
  assert.deepEqual(
    result.map((r) => [r.position, r.clubId, r.played, r.points, r.rankDelta, r.isLive, r.liveChip, r.pointsDelta]),
    [
      [1, 10, 11, 28, 0, false, null, null],
      [2, 20, 11, 22, 0, false, null, null],
    ]
  )
  // Original array not mutated
  assert.equal(official[0].played, 11)
})

test('1 live match handles win/draw/loss, rank up/down arrows, chip metadata, and pointsDelta', () => {
  const official = [
    row(1, 10, 'Manchester City FC', 11, 7, 1, 3, 22, 14), // 22 pts
    row(2, 20, 'Liverpool FC', 11, 6, 3, 2, 18, 10),       // 21 pts
    row(3, 30, 'Chelsea FC', 11, 5, 1, 5, 20, 20),         // 16 pts
  ]
  const fixtures = [
    ...finishedFixturesFor(official),
    liveFixture(501, 20, 'Liverpool FC', 30, 'Chelsea FC'),
  ]
  const live = { 501: liveState(501, 2, 1, 67) }
  const result = applyLiveToStandings(official, fixtures, live, { nowMs: T0_MS })

  // Liverpool 21+3=24 jumps from #2 -> #1 (▲1), Man City drops #1 -> #2 (▼1), Chelsea stays #3 (+0)
  assert.equal(result[0].clubId, 20)
  assert.equal(result[0].position, 1)
  assert.equal(result[0].rankDelta, 1)
  assert.equal(result[0].played, 12)
  assert.equal(result[0].won, 7)
  assert.equal(result[0].goalsFor, 20)
  assert.equal(result[0].goalsAgainst, 11)
  assert.equal(result[0].goalDifference, 9)
  assert.equal(result[0].points, 24)
  assert.equal(result[0].pointsDelta, 3)
  assert.equal(result[0].provisionalFormResult, 'W')
  assert.deepEqual(result[0].liveChip, {
    fixtureId: 501,
    homeCode: 'LIV',
    awayCode: 'CHE',
    homeScore: 2,
    awayScore: 1,
    minuteLabel: "67'",
    result: 'W',
  })

  assert.equal(result[1].clubId, 10)
  assert.equal(result[1].position, 2)
  assert.equal(result[1].rankDelta, -1)
  assert.equal(result[1].isLive, false)

  assert.equal(result[2].clubId, 30)
  assert.equal(result[2].position, 3)
  assert.equal(result[2].rankDelta, 0)
  assert.equal(result[2].lost, 6)
  assert.equal(result[2].points, 16)
  assert.equal(result[2].pointsDelta, 0)
  assert.equal(result[2].provisionalFormResult, 'L')
  assert.equal(result[2].liveChip.result, 'L')
})

test('4 simultaneous live matches update all 8 teams and compute draws, wins, losses, and rank deltas', () => {
  const official = [
    row(1, 1, 'Arsenal FC', 11, 9, 1, 1, 17, 7),
    row(2, 2, 'Brighton & Hove Albion FC', 11, 7, 1, 3, 26, 9),
    row(3, 3, 'Manchester City FC', 11, 7, 1, 3, 22, 14),
    row(4, 4, 'Liverpool FC', 11, 6, 3, 2, 18, 10),
    row(5, 5, 'AFC Bournemouth', 11, 5, 3, 3, 18, 14),
    row(6, 6, 'Tottenham Hotspur FC', 11, 4, 3, 4, 12, 11),
    row(7, 7, 'Hull City AFC', 11, 3, 5, 3, 13, 11),
    row(8, 8, 'Newcastle United FC', 11, 3, 4, 4, 15, 18),
    row(9, 9, 'Fulham FC', 11, 2, 5, 4, 11, 13),
    row(10, 10, 'Sunderland AFC', 11, 2, 3, 6, 8, 17),
    row(11, 11, 'Crystal Palace FC', 11, 2, 1, 8, 12, 27),
  ]
  const fixtures = [
    ...finishedFixturesFor(official),
    liveFixture(601, 9, 'Fulham FC', 5, 'AFC Bournemouth'),
    liveFixture(602, 2, 'Brighton & Hove Albion FC', 8, 'Newcastle United FC'),
    liveFixture(603, 10, 'Sunderland AFC', 6, 'Tottenham Hotspur FC'),
    liveFixture(604, 11, 'Crystal Palace FC', 7, 'Hull City AFC'),
  ]
  const live = {
    601: liveState(601, 0, 1, 67),
    602: liveState(602, 2, 2, 58),
    603: liveState(603, 1, 0, 71),
    604: liveState(604, 0, 2, 63),
  }
  const result = applyLiveToStandings(official, fixtures, live, { nowMs: T0_MS })
  const byId = Object.fromEntries(result.map((r) => [r.clubId, r]))

  assert.equal(result.filter((r) => r.isLive).length, 8)
  // Brighton 22+1=23 stays #2, Newcastle 13+1=14 (D)
  assert.equal(byId[2].points, 23)
  assert.equal(byId[2].pointsDelta, 1)
  assert.equal(byId[2].provisionalFormResult, 'D')
  assert.equal(byId[8].points, 14)
  assert.equal(byId[8].pointsDelta, 1)
  assert.equal(byId[8].liveChip.result, 'D')
  // Bournemouth 18+3=21, GD 4+1=5 vs Liverpool 21, GD 8 -> stays #5
  assert.equal(byId[5].points, 21)
  assert.equal(byId[5].position, 5)
  // Hull City 14+3=17 jumps above Tottenham (15) -> #6 (▲1), Spurs drops to #7 (▼1)
  assert.equal(byId[7].points, 17)
  assert.equal(byId[7].position, 6)
  assert.equal(byId[7].rankDelta, 1)
  assert.equal(byId[6].position, 7)
  assert.equal(byId[6].rankDelta, -1)
  // Sunderland 9+3=12 jumps above Fulham (11)
  assert.equal(byId[10].points, 12)
  assert.ok(byId[10].position < byId[9].position)
  assert.ok(byId[10].rankDelta > 0)
  assert.ok(byId[9].rankDelta < 0)
})

test('teams tied on points, goal difference, and goals for keep official table order (stable sort)', () => {
  // Club B is officially #1 ahead of Club A (#2), both identical stats
  const official = [
    row(1, 200, 'West Club', 10, 5, 2, 3, 15, 10),  // 17 pts, GD +5, GF 15
    row(2, 100, 'Alpha Club', 10, 5, 2, 3, 15, 10), // 17 pts, GD +5, GF 15
    row(3, 300, 'Beta Club', 10, 4, 2, 4, 14, 10),  // 14 pts -> wins 1-0 -> 17 pts, GD +5, GF 15!
    row(4, 400, 'Delta Club', 10, 1, 1, 8, 5, 20),
  ]
  const fixtures = [
    ...finishedFixturesFor(official),
    liveFixture(701, 300, 'Beta Club', 400, 'Delta Club'),
  ]
  const live = { 701: liveState(701, 1, 0, 80) }
  const result = applyLiveToStandings(official, fixtures, live, { nowMs: T0_MS })

  // All 3 top clubs have 17 pts, GD +5, GF 15 -> official order (200, 100, 300) MUST be preserved!
  assert.deepEqual(
    result.slice(0, 3).map((r) => r.clubId),
    [200, 100, 300]
  )
  assert.deepEqual(
    result.slice(0, 3).map((r) => r.rankDelta),
    [0, 0, 0]
  )
})

test('FT match not yet recorded in DB/standings IS applied provisionally', () => {
  const official = [
    row(1, 10, 'Arsenal FC', 11, 9, 1, 1, 17, 7),
    row(2, 20, 'Fulham FC', 11, 2, 5, 4, 11, 13),
  ]
  const fixtures = [
    ...finishedFixturesFor(official),
    liveFixture(801, 10, 'Arsenal FC', 20, 'Fulham FC', 'LIVE'), // DB not FINISHED yet
  ]
  const live = { 801: liveState(801, 3, 0, 90, { status: 'FT' }) }
  const result = applyLiveToStandings(official, fixtures, live, { nowMs: T0_MS })

  assert.equal(result[0].played, 12)
  assert.equal(result[0].points, 31)
  assert.equal(result[0].pointsDelta, 3)
  assert.equal(result[0].liveChip.minuteLabel, 'FT')
})

test('FT match already included in official standings (by DB FINISHED or by played count) is NOT double-counted', () => {
  // Case A: DB fixture is already FINISHED
  const officialA = [
    row(1, 10, 'Arsenal FC', 12, 10, 1, 1, 20, 7),
    row(2, 20, 'Fulham FC', 12, 2, 5, 5, 11, 16),
  ]
  const fixturesA = [
    ...finishedFixturesFor(officialA),
    { ...liveFixture(801, 10, 'Arsenal FC', 20, 'Fulham FC', 'FINISHED'), homeGoals: 3, awayGoals: 0 },
  ]
  const liveA = { 801: liveState(801, 3, 0, 90, { status: 'FT' }) }
  const resA = applyLiveToStandings(officialA, fixturesA, liveA, { nowMs: T0_MS })
  assert.equal(resA[0].played, 12)
  assert.equal(resA[0].points, 31)
  assert.equal(resA[0].isLive, false)

  // Case B: DB fixture still LIVE, but standings.played ALREADY equals finishedCount + 1
  const officialB = [
    row(1, 10, 'Arsenal FC', 12, 10, 1, 1, 20, 7),
    row(2, 20, 'Fulham FC', 12, 2, 5, 5, 11, 16),
  ]
  const fixturesB = [
    ...finishedFixturesFor([
      row(1, 10, 'Arsenal FC', 11, 9, 1, 1, 17, 7),
      row(2, 20, 'Fulham FC', 11, 2, 5, 4, 11, 13),
    ]),
    liveFixture(802, 10, 'Arsenal FC', 20, 'Fulham FC', 'LIVE'),
  ]
  const liveB = { 802: liveState(802, 3, 0, 90, { status: 'FT' }) }
  const warnings = []
  const resB = applyLiveToStandings(officialB, fixturesB, liveB, { nowMs: T0_MS, warn: (m) => warnings.push(m) })
  assert.equal(resB[0].played, 12)
  assert.equal(resB[0].points, 31)
  assert.equal(resB[0].isLive, false)
  assert.equal(warnings.length, 0)
})

test('inconsistent standings.played skips club and logs console.warn without corrupting totals', () => {
  // Official says played=13, finished fixtures say 11, pending live=1 -> inconsistent!
  const official = [
    row(1, 10, 'Arsenal FC', 13, 10, 2, 1, 22, 8),
    row(2, 20, 'Fulham FC', 11, 2, 5, 4, 11, 13),
  ]
  const fixtures = [
    ...finishedFixturesFor([
      row(1, 10, 'Arsenal FC', 11, 9, 1, 1, 17, 7),
      row(2, 20, 'Fulham FC', 11, 2, 5, 4, 11, 13),
    ]),
    liveFixture(901, 10, 'Arsenal FC', 20, 'Fulham FC', 'LIVE'),
  ]
  const live = { 901: liveState(901, 1, 2, 70) }
  const warnings = []
  const result = applyLiveToStandings(official, fixtures, live, { nowMs: T0_MS, warn: (m) => warnings.push(m) })
  const byId = Object.fromEntries(result.map((r) => [r.clubId, r]))

  assert.equal(warnings.length, 1)
  assert.match(warnings[0], /Arsenal FC/)
  // Arsenal skipped (still played=13, isLive=false)
  assert.equal(byId[10].played, 13)
  assert.equal(byId[10].isLive, false)
  // Fulham is consistent (11 === 11) so Fulham still gets its live update
  assert.equal(byId[20].played, 12)
  assert.equal(byId[20].isLive, true)
  assert.equal(byId[20].pointsDelta, 3)
})

test('formats 45+, 90+, and halftime ("Nghỉ giữa hiệp") minute labels accurately', () => {
  assert.equal(formatLiveChipMinute(liveState(1, 1, 0, 45), null, T0_MS), "45'+")
  assert.equal(formatLiveChipMinute(liveState(1, 1, 0, 90), null, T0_MS), "90'+")
  assert.equal(formatLiveChipMinute(liveState(1, 1, 0, 45, { status: 'PAUSED' }), null, T0_MS), 'Nghỉ giữa hiệp')
  assert.equal(formatLiveChipMinute(liveState(1, 1, 0, 45, { halftime: true }), null, T0_MS), 'Nghỉ giữa hiệp')
  assert.equal(
    formatLiveChipMinute(liveState(1, 1, 0, 45), liveFixture(1, 10, 'Arsenal FC', 20, 'Fulham FC', 'PAUSED'), T0_MS),
    'Nghỉ giữa hiệp'
  )
  assert.equal(
    formatLiveChipMinute(liveState(1, 1, 0, 45, { minuteLabel: 'Nghỉ giữa hiệp' }), null, T0_MS),
    'Nghỉ giữa hiệp'
  )

  const official = [
    row(1, 10, 'Arsenal FC', 11, 9, 1, 1, 17, 7),
    row(2, 20, 'Fulham FC', 11, 2, 5, 4, 11, 13),
  ]
  const fixtures = [
    ...finishedFixturesFor(official),
    liveFixture(950, 10, 'Arsenal FC', 20, 'Fulham FC', 'PAUSED'),
  ]
  const resHt = applyLiveToStandings(official, fixtures, { 950: liveState(950, 0, 0, 45) }, { nowMs: T0_MS })
  assert.equal(resHt[0].liveChip.minuteLabel, 'Nghỉ giữa hiệp')

  const res45Plus = applyLiveToStandings(
    official,
    [...finishedFixturesFor(official), liveFixture(951, 10, 'Arsenal FC', 20, 'Fulham FC', 'LIVE')],
    { 951: liveState(951, 1, 0, 45) },
    { nowMs: T0_MS }
  )
  assert.equal(res45Plus[0].liveChip.minuteLabel, "45'+")
})
