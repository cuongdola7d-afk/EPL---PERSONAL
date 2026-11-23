import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildScheduleHeroChips,
  estimatedPitchPlayers,
  matchPlayerState,
  matchPlayerValue,
  matchweekStartDate,
  pitchPositions,
  preferredMatchweek,
  shortPlayerName,
} from './matchView.js'
import { parseMatchRoute } from './matchRoute.js'

test('prefers the latest complete gameweek and falls back to the first available week', () => {
  const matches = [
    ...Array.from({ length: 10 }, (_, id) => ({ id, matchweek: 5, status: 'FINISHED' })),
    ...Array.from({ length: 10 }, (_, id) => ({ id: id + 10, matchweek: 6, status: 'SCHEDULED' })),
  ]
  assert.equal(preferredMatchweek(matches), 5)
  assert.equal(preferredMatchweek(matches.slice(10)), 6)
  assert.equal(preferredMatchweek([]), 1)
})

test('preferredMatchweek opens an in-progress gameweek such as GW11 (9/10 finished)', () => {
  const now = new Date('2026-11-23T12:00:00Z')
  const matches = [
    ...Array.from({ length: 10 }, (_, i) => ({
      id: 100 + i, matchweek: 10, status: 'FINISHED', kickoffUtc: '2026-11-08T15:00:00Z',
    })),
    ...Array.from({ length: 9 }, (_, i) => ({
      id: 200 + i, matchweek: 11, status: 'FINISHED', kickoffUtc: '2026-11-22T15:00:00Z',
    })),
    { id: 209, matchweek: 11, status: 'SCHEDULED', kickoffUtc: '2026-11-23T20:00:00Z' },
    ...Array.from({ length: 10 }, (_, i) => ({
      id: 300 + i, matchweek: 12, status: 'SCHEDULED', kickoffUtc: '2026-11-28T15:00:00Z',
    })),
  ]
  assert.equal(preferredMatchweek(matches, now), 11)

  // If multiple weeks are in-progress, picks the smallest week number.
  const multiInProgress = [
    ...matches,
    { id: 400, matchweek: 12, status: 'FINISHED', kickoffUtc: '2026-11-28T12:30:00Z' },
  ]
  assert.equal(preferredMatchweek(multiInProgress, now), 11)
})

test('preferredMatchweek switches to next week when kickoff is within 2 days, otherwise stays on latest completed', () => {
  const matches = [
    ...Array.from({ length: 10 }, (_, i) => ({
      id: 200 + i, matchweek: 11, status: 'FINISHED', kickoffUtc: '2026-11-23T20:00:00Z',
    })),
    ...Array.from({ length: 10 }, (_, i) => ({
      id: 300 + i, matchweek: 12, status: 'SCHEDULED', kickoffUtc: '2026-11-28T12:30:00Z',
    })),
  ]
  // GW12 starts in 1 day (<= 2 days) -> open GW12
  assert.equal(preferredMatchweek(matches, new Date('2026-11-27T12:30:00Z')), 12)
  // GW12 starts in 5 days (> 2 days) -> stay on completed GW11
  assert.equal(preferredMatchweek(matches, new Date('2026-11-23T12:30:00Z')), 11)
})

test('preferredMatchweek prioritizes any LIVE match from DB or FPL live state', () => {
  const now = new Date('2026-11-23T20:15:00Z')
  const matches = [
    ...Array.from({ length: 9 }, (_, i) => ({
      id: 100 + i, matchweek: 10, status: 'FINISHED', kickoffUtc: '2026-11-08T15:00:00Z',
    })),
    { id: 109, matchweek: 10, status: 'SCHEDULED', kickoffUtc: '2026-12-01T20:00:00Z' },
    ...Array.from({ length: 9 }, (_, i) => ({
      id: 200 + i, matchweek: 11, status: 'FINISHED', kickoffUtc: '2026-11-22T15:00:00Z',
    })),
    { id: 209, matchweek: 11, status: 'SCHEDULED', kickoffUtc: '2026-11-23T20:00:00Z' },
  ]
  // Without LIVE, smallest in-progress week is GW10
  assert.equal(preferredMatchweek(matches, now), 10)
  // With FPL live state LIVE on fixture 209 -> opens GW11
  assert.equal(preferredMatchweek(matches, now, { 209: { fixtureId: 209, status: 'LIVE', minute: 15 } }), 11)
  // With DB status LIVE on fixture 209 -> opens GW11
  const dbLiveMatches = matches.map((m) => (m.id === 209 ? { ...m, status: 'LIVE' } : m))
  assert.equal(preferredMatchweek(dbLiveMatches, now), 11)
})

test('explicit URL week parameter takes precedence over preferredMatchweek', () => {
  const route = parseMatchRoute('#matches?season=2026&week=4')
  const matches = [
    ...Array.from({ length: 9 }, (_, i) => ({ id: 200 + i, matchweek: 11, status: 'FINISHED' })),
    { id: 209, matchweek: 11, status: 'LIVE' },
  ]
  const resolvedWeek = route.week ?? preferredMatchweek(matches, new Date('2026-11-23T20:15:00Z'))
  assert.equal(resolvedWeek, 4)
})

test('abbreviates the first name while preserving compound surnames, accents and single names', () => {
  for (const [name, expected] of [['Harvey Barnes', 'H. Barnes'], ['Cole Palmer', 'C. Palmer'],
    ['Virgil van Dijk', 'V. van Dijk'], ['  João   Pedro ', 'J. Pedro'], ['C. Palmer', 'C. Palmer'], ['Rodri', 'Rodri']]) {
    assert.equal(shortPlayerName(name), expected)
  }
})

test('uses explicit participation before minutes and preserves missing statistics', () => {
  assert.equal(matchPlayerState({ participationStatus: 'PLAYED', minutes: null }), 'played')
  assert.equal(matchPlayerState({ participationStatus: 'DID_NOT_PLAY', minutes: null }), 'did-not-play')
  assert.equal(matchPlayerState({ minutes: null, inferred: null }), 'unknown')
  assert.equal(matchPlayerValue({ goals: null, inferred: { goals: 1 } }, 'goals', 'MISSING'), null)
  assert.equal(matchPlayerValue({ goals: null, inferred: { goals: 1 } }, 'goals', 'VERIFIED'), 1)
})

test('pitch estimate selects eleven unique played players and keeps substitutes and DNP on bench', () => {
  const players = [
    { playerId: 1, position: 'G', participationStatus: 'PLAYED', minutes: 90 },
    ...Array.from({ length: 11 }, (_, index) => ({
      playerId: index + 2, position: index < 4 ? 'D' : index < 8 ? 'M' : 'F',
      participationStatus: 'PLAYED', minutes: 89 - index,
    })),
    { playerId: 13, position: 'D', participationStatus: 'DID_NOT_PLAY', minutes: null },
  ]
  const { selected, bench } = estimatedPitchPlayers(players, 'MANUAL_VERIFIED')
  assert.equal(selected.length, 11)
  assert.equal(selected[0].playerId, 1)
  assert.deepEqual(bench.map((player) => player.playerId), [12, 13])
  const nodes = pitchPositions(selected, 'home')
  assert.equal(nodes.length, 11)
  assert.ok(nodes.every(({ x, y }) => x > 0 && x < 50 && y > 0 && y < 100))
  assert.deepEqual([8, 20, 32, 44].map((x) => nodes.filter((node) => node.x === x).length), [1, 4, 3, 3])
})

test('matchweekStartDate picks earliest kickoff in Vietnam time including 00:30 next-day rollover', () => {
  const matches = [
    // GW11 earliest kickoff is 2026-11-21T17:30:00Z -> 00:30 on 22/11 Vietnam time!
    { id: 1, matchweek: 11, kickoffUtc: '2026-11-21T17:30:00Z', date: '2026-11-21' },
    { id: 2, matchweek: 11, kickoffUtc: '2026-11-22T14:00:00Z', date: '2026-11-22' },
    // GW12 has an earlier Saturday 19:30 VN kickoff
    { id: 3, matchweek: 12, kickoffUtc: '2026-11-28T15:00:00Z', date: '2026-11-28' },
    { id: 4, matchweek: 12, kickoffUtc: '2026-11-28T12:30:00Z', date: '2026-11-28' },
    // GW13 has no valid date
    { id: 5, matchweek: 13, kickoffUtc: null, date: null },
  ]
  assert.equal(matchweekStartDate(matches, 11, 2026), '22/11')
  assert.equal(matchweekStartDate(matches, 12, 2026), '28/11')
  assert.equal(matchweekStartDate(matches, 13, 2026), '')
  assert.equal(matchweekStartDate(matches, 99, 2026), '')
})

test('buildScheduleHeroChips handles live, upcoming, finished-all, and legacy season states', () => {
  const now = new Date('2026-11-23T18:20:00Z') // 01:20 VN on Tue 24/11
  const gw11 = [
    ...Array.from({ length: 9 }, (_, i) => ({
      id: 100 + i,
      matchweek: 11,
      status: 'FINISHED',
      homeClub: 'Manchester City FC',
      awayClub: 'Fulham FC',
      homeGoals: 1,
      awayGoals: 1,
      kickoffUtc: '2026-11-21T15:00:00Z',
    })),
    {
      id: 200,
      matchweek: 11,
      status: 'SCHEDULED',
      homeClub: 'Brentford FC',
      awayClub: 'Everton FC',
      homeGoals: null,
      awayGoals: null,
      kickoffUtc: '2026-11-23T20:00:00Z', // 03:00 Thứ Ba 24/11 VN
    },
  ]

  // 1. No live match -> chip 1 + upcoming chip 2
  const upcomingChips = buildScheduleHeroChips(gw11, { season: 2026, now, liveByFixture: {} })
  assert.equal(upcomingChips.length, 2)
  assert.deepEqual(upcomingChips[0], { kind: 'week', tone: 'accent', text: 'Vòng 11 · 9/10 trận đã đá' })
  assert.deepEqual(upcomingChips[1], { kind: 'next', tone: 'up', text: 'Trận tiếp: 03:00 Thứ Ba 24/11' })

  // 2. Single live match via liveByFixture -> chip 2 shows live score
  const liveChips = buildScheduleHeroChips(gw11, {
    season: 2026,
    now,
    liveByFixture: {
      200: { fixtureId: 200, status: 'LIVE', minute: 67, homeScore: 1, awayScore: 0, goals: [], assists: [], redCards: [] },
    },
  })
  assert.equal(liveChips.length, 2)
  assert.deepEqual(liveChips[1], { kind: 'live', tone: 'live', text: 'Đang đá: BRE 1–0 EVE' })

  // 3. Multiple live matches -> appends "+2 trận"
  const multiLive = [
    ...gw11,
    { id: 201, matchweek: 11, status: 'LIVE', homeClub: 'Arsenal FC', awayClub: 'Chelsea FC', homeGoals: 2, awayGoals: 1, kickoffUtc: '2026-11-23T20:00:00Z' },
    { id: 202, matchweek: 11, status: 'LIVE', homeClub: 'Liverpool FC', awayClub: 'Leeds United FC', homeGoals: 0, awayGoals: 0, kickoffUtc: '2026-11-23T20:00:00Z' },
  ]
  const multiLiveChips = buildScheduleHeroChips(multiLive, {
    season: 2026,
    now,
    liveByFixture: {
      200: { fixtureId: 200, status: 'LIVE', minute: 67, homeScore: 1, awayScore: 0, goals: [], assists: [], redCards: [] },
    },
  })
  assert.equal(multiLiveChips[1].text, 'Đang đá: BRE 1–0 EVE +2 trận')

  // 4. All matches finished -> chip 2 hidden
  const allDone = gw11.map((m) => ({ ...m, status: 'FINISHED', homeGoals: 1, awayGoals: 0 }))
  const doneChips = buildScheduleHeroChips(allDone, { season: 2026, now, liveByFixture: {} })
  assert.equal(doneChips.length, 1)
  assert.equal(doneChips[0].text, 'Vòng 11 · 10/10 trận đã đá')

  // 5. Legacy season -> hidden completely
  assert.deepEqual(buildScheduleHeroChips(gw11, { season: 2024, now }), [])
})
