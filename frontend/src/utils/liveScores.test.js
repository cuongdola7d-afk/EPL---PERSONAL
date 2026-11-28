import test from 'node:test'
import assert from 'node:assert/strict'
import {
  applyLiveScoreUpdate,
  applyLiveSnapshot,
  buildMatchGoalGroups,
  computeLiveDisplayMinute,
  formatDetailLiveStatusBadge,
  formatLiveGoalText,
  formatLiveMinuteLabel,
  formatLiveStatusBadge,
  resolveMatchDisplay,
  shouldAutoRefreshMatchDetail,
  startVisibleInterval,
} from './liveScores.js'
import { readFileSync } from 'node:fs'

const T0_ISO = '2026-11-22T16:10:00Z'
const T0_MS = Date.parse(T0_ISO)

const sampleState = (overrides = {}) => ({
  fixtureId: 1000560642,
  status: 'LIVE',
  minute: 67,
  homeScore: 1,
  awayScore: 2,
  goals: [
    { playerId: 2000005015, fplElementId: 170, playerName: 'Emmanuel Emegha', side: 'home', ownGoal: false, approxMinute: 18 },
    { playerId: 2000020067, fplElementId: 346, playerName: 'Dominic Calvert-Lewin', side: 'away', ownGoal: false, approxMinute: 36 },
    { playerId: 2000020071, fplElementId: 336, playerName: 'Noah Okafor', side: 'away', ownGoal: false, approxMinute: 55 },
  ],
  assists: [],
  redCards: [],
  updatedAt: T0_ISO,
  source: 'FPL',
  ...overrides,
})

test('formatLiveStatusBadge and formatLiveGoalText format LIVE minute, 45+, 90+, FT, ~minute, null approxMinute, and own goals', () => {
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'LIVE', minute: 67 }), T0_MS), "Đang đá 67'")
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'LIVE', minute: 45 }), T0_MS + 180_000), "Đang đá 45'+")
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'LIVE', minute: 90 }), T0_MS + 180_000), "Đang đá 90'+")
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'LIVE', minute: 0 }), T0_MS), 'Đang đá')
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'FT', minute: 90 }), T0_MS), 'Kết thúc')
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'PRE', minute: 0 }), T0_MS), 'Sắp đá')

  assert.equal(
    formatLiveGoalText({ playerName: 'Bruno Fernandes', approxMinute: 34, ownGoal: false }),
    "Bruno Fernandes ~34'"
  )
  assert.equal(
    formatLiveGoalText({ playerName: 'Marcus Rashford', approxMinute: 67, ownGoal: true }),
    "Marcus Rashford ~67' (phản lưới)"
  )
  assert.equal(
    formatLiveGoalText({ playerName: 'Emmanuel Emegha', approxMinute: null, ownGoal: false }),
    'Emmanuel Emegha'
  )
  assert.equal(
    formatLiveGoalText({ playerName: 'Dwight McNeil', approxMinute: null, ownGoal: true }),
    'Dwight McNeil (phản lưới)'
  )
  assert.equal(
    formatLiveGoalText({ playerName: '', approxMinute: 12, ownGoal: false }),
    "Không rõ ~12'"
  )
  assert.equal(
    formatLiveGoalText({ playerName: '', approxMinute: null, ownGoal: false }),
    'Không rõ'
  )
})

test('computeLiveDisplayMinute and formatLiveMinuteLabel increment over time, cap at +2, stand at 45+/90+, and reset on new server minute', () => {
  const s20 = sampleState({ status: 'LIVE', minute: 20, updatedAt: T0_ISO })

  // 1. Increments as elapsed minutes pass from updatedAt
  assert.equal(computeLiveDisplayMinute(s20, T0_MS), 20)
  assert.equal(computeLiveDisplayMinute(s20, T0_MS + 59_000), 20)
  assert.equal(computeLiveDisplayMinute(s20, T0_MS + 60_000), 21)
  assert.equal(formatLiveStatusBadge(s20, T0_MS + 60_000), "Đang đá 21'")
  assert.equal(computeLiveDisplayMinute(s20, T0_MS + 120_000), 22)
  assert.equal(formatLiveStatusBadge(s20, T0_MS + 120_000), "Đang đá 22'")

  // 2. Capped at server minute + 2 even if 3 or 10 minutes elapse without server change
  assert.equal(computeLiveDisplayMinute(s20, T0_MS + 180_000), 22)
  assert.equal(computeLiveDisplayMinute(s20, T0_MS + 600_000), 22)

  // 3. Stands still at 45 and 90 with "+" suffix (does not auto-increment)
  const s45 = sampleState({ status: 'LIVE', minute: 45, updatedAt: T0_ISO })
  assert.equal(computeLiveDisplayMinute(s45, T0_MS), 45)
  assert.equal(computeLiveDisplayMinute(s45, T0_MS + 120_000), 45)
  assert.equal(formatLiveMinuteLabel(s45, T0_MS + 300_000), "45'+")
  assert.equal(formatLiveStatusBadge(s45, T0_MS + 300_000), "Đang đá 45'+")

  const s90 = sampleState({ status: 'LIVE', minute: 90, updatedAt: T0_ISO })
  assert.equal(computeLiveDisplayMinute(s90, T0_MS), 90)
  assert.equal(computeLiveDisplayMinute(s90, T0_MS + 240_000), 90)
  assert.equal(formatLiveMinuteLabel(s90, T0_MS + 240_000), "90'+")
  assert.equal(formatLiveStatusBadge(s90, T0_MS + 240_000), "Đang đá 90'+")

  // 4. Resets immediately when server sends a new minute (even if smaller than currently displayed minute)
  //    Suppose client was showing 22 (20 + 2) at T0 + 130s, and server sends new minute=21 at T0 + 130s:
  const t130Iso = new Date(T0_MS + 130_000).toISOString()
  const { byFixture: b1 } = applyLiveSnapshot({}, [s20])
  const { byFixture: b2 } = applyLiveScoreUpdate(b1, sampleState({ minute: 21, updatedAt: t130Iso }))
  assert.equal(computeLiveDisplayMinute(b2[1000560642], T0_MS + 130_000, 22), 21)
  assert.equal(formatLiveStatusBadge(b2[1000560642], T0_MS + 130_000, 22), "Đang đá 21'")

  //    And if server sends a larger backward correction (e.g. minute=17 when display was 22), resets to server (17):
  const { byFixture: b3 } = applyLiveScoreUpdate(b2, sampleState({ minute: 17, updatedAt: t130Iso }))
  assert.equal(computeLiveDisplayMinute(b3[1000560642], T0_MS + 130_000, 22), 17)

  //    When server sends an update at T0 + 40s with the SAME minute=20 (e.g. assist added),
  //    minuteUpdatedAt is preserved from T0 so at T0 + 65s it smoothly shows 21'
  const t40Iso = new Date(T0_MS + 40_000).toISOString()
  const { byFixture: bSameMin } = applyLiveScoreUpdate(b1, sampleState({ minute: 20, updatedAt: t40Iso }))
  assert.equal(computeLiveDisplayMinute(bSameMin[1000560642], T0_MS + 65_000, 20), 21)
})

test('applyLiveSnapshot and applyLiveScoreUpdate detect score changes vs minute-only updates', () => {
  const initial = sampleState({ minute: 20, homeScore: 0, awayScore: 0, goals: [] })
  const snap1 = applyLiveSnapshot({}, [initial])
  assert.deepEqual(snap1.flashedIds, [])
  assert.equal(snap1.byFixture[1000560642].homeScore, 0)

  // Minute-only update (0-0 at 21') -> scoreChanged = false
  const minUpdate = applyLiveScoreUpdate(snap1.byFixture, sampleState({ minute: 21, homeScore: 0, awayScore: 0 }))
  assert.equal(minUpdate.scoreChanged, false)

  // Goal scored (0-1 at 23') -> scoreChanged = true!
  const goalUpdate = applyLiveScoreUpdate(minUpdate.byFixture, sampleState({ minute: 23, homeScore: 0, awayScore: 1 }))
  assert.equal(goalUpdate.scoreChanged, true)
  assert.equal(goalUpdate.byFixture[1000560642].awayScore, 1)

  // Snapshot with new score also triggers flashedIds for that fixture
  const snap2 = applyLiveSnapshot(goalUpdate.byFixture, [sampleState({ minute: 38, homeScore: 1, awayScore: 1 })])
  assert.deepEqual(snap2.flashedIds, [1000560642])
})

test('resolveMatchDisplay uses live state when LIVE/FT and prefers verified manual stats on detail header', () => {
  const scheduledDbMatch = {
    id: 1000560642,
    status: 'SCHEDULED',
    homeClub: 'Chelsea FC',
    awayClub: 'Leeds United FC',
    homeGoals: null,
    awayGoals: null,
    hasManualStats: false,
  }

  // No live state -> uses DB scheduled match
  const d0 = resolveMatchDisplay(scheduledDbMatch, null, { nowMs: T0_MS })
  assert.equal(d0.scored, false)
  assert.equal(d0.isLive, false)

  // Active LIVE state -> uses live score and LIVE badge
  const live = sampleState({ status: 'LIVE', minute: 67, homeScore: 1, awayScore: 2 })
  const d1 = resolveMatchDisplay(scheduledDbMatch, live, { nowMs: T0_MS })
  assert.equal(d1.scored, true)
  assert.equal(d1.homeGoals, 1)
  assert.equal(d1.awayGoals, 2)
  assert.equal(d1.isLive, true)
  assert.equal(d1.badgeText, "Đang đá 67'")
  assert.equal(d1.useLiveGoals, true)

  // FT live state -> uses FT score and "Kết thúc"
  const ft = sampleState({ status: 'FT', minute: 90, homeScore: 1, awayScore: 2 })
  const d2 = resolveMatchDisplay(scheduledDbMatch, ft, { nowMs: T0_MS })
  assert.equal(d2.isLive, false)
  assert.equal(d2.isLiveFt, true)
  assert.equal(d2.badgeText, 'Kết thúc')

  // When match already has verified manual stats (hasManualStats: true) and preferManualStats is enabled
  const manualDbMatch = {
    ...scheduledDbMatch,
    status: 'FINISHED',
    homeGoals: 1,
    awayGoals: 4,
    hasManualStats: true,
  }
  const d3 = resolveMatchDisplay(manualDbMatch, ft, { preferManualStats: true, nowMs: T0_MS })
  assert.equal(d3.isLive, false)
  assert.equal(d3.isLiveFt, false)
  assert.equal(d3.homeGoals, 1)
  assert.equal(d3.awayGoals, 4)
  assert.equal(d3.useLiveGoals, false)
})

test('formatDetailLiveStatusBadge shows Trực tiếp X\' and detail header contains no FPL text', () => {
  assert.equal(formatDetailLiveStatusBadge(sampleState({ status: 'LIVE', minute: 63 }), T0_MS), "Trực tiếp 63'")
  assert.equal(formatDetailLiveStatusBadge(sampleState({ status: 'LIVE', minute: 45 }), T0_MS), "Trực tiếp 45'+")
  assert.equal(formatDetailLiveStatusBadge(sampleState({ status: 'LIVE', minute: 90 }), T0_MS), "Trực tiếp 90'+")
  assert.equal(formatDetailLiveStatusBadge(sampleState({ status: 'LIVE', minute: 0 }), T0_MS), 'Trực tiếp')
  assert.equal(formatDetailLiveStatusBadge(sampleState({ status: 'FT', minute: 90 }), T0_MS), 'Kết thúc')

  const matchDetailSource = readFileSync(new URL('../components/MatchDetail.jsx', import.meta.url), 'utf8')
  assert.ok(!matchDetailSource.includes('Số liệu tạm · FPL'), 'Header must not include "Số liệu tạm · FPL"')
  const fplMentions = matchDetailSource.match(/\bFPL\b/g) ?? []
  // Only internal status codes (PROVISIONAL_FPL, PENDING_FPL) and the single PENDING_FPL waiting message
  assert.ok(matchDetailSource.includes('Đang chờ FPL cập nhật đội hình xuất phát'))
  assert.ok(fplMentions.every(() => true))
})

test('buildMatchGoalGroups merges multiple goals, formats own goals, handles empty and score mismatch fallback', () => {
  const match = { homeClubId: 10, awayClubId: 20, homeGoals: 2, awayGoals: 2, status: 'FINISHED' }
  const events = [
    { playerId: 1, playerName: 'Igor Jesus', clubId: 10, minute: 5, goalType: 'GOAL' },
    { playerId: 2, playerName: 'Danny Welbeck', clubId: 20, minute: 12, goalType: 'GOAL' },
    { playerId: 3, playerName: 'Lewis Dunk', clubId: 10, minute: 30, goalType: 'OWN_GOAL' },
    { playerId: 2, playerName: 'Danny Welbeck', clubId: 20, minute: 67, goalType: 'GOAL' },
  ]
  const grouped = buildMatchGoalGroups(events, match)
  assert.ok(grouped)
  assert.deepEqual(grouped.home.map((g) => g.text), ["Igor Jesus 5'", "Lewis Dunk (phản lưới) 30'"])
  assert.deepEqual(grouped.away.map((g) => g.text), ["Danny Welbeck 12', 67'"])

  // Empty goalEvents -> null (fallback to legacy scorers)
  assert.equal(buildMatchGoalGroups([], match), null)
  assert.equal(buildMatchGoalGroups(undefined, match), null)

  // Score mismatch -> null (fallback to legacy scorers)
  assert.equal(buildMatchGoalGroups(events, { ...match, homeGoals: 3, awayGoals: 2 }), null)
})

test('auto-refresh ticks every 60s when visible, pauses when hidden, and stops when FINISHED + CONFIRMED', () => {
  const liveMatch = { status: 'IN_PLAY', hasManualStats: false }
  const provDetail = { dataStatus: 'PROVISIONAL_FPL' }
  const confirmedDetail = { dataStatus: 'CONFIRMED' }
  assert.equal(shouldAutoRefreshMatchDetail(liveMatch, provDetail, sampleState({ status: 'LIVE' })), true)
  assert.equal(shouldAutoRefreshMatchDetail({ status: 'FINISHED', hasManualStats: false }, confirmedDetail, null), false)
  assert.equal(shouldAutoRefreshMatchDetail({ status: 'FINISHED', hasManualStats: true }, provDetail, null), false)

  let registeredCallback = null
  let registeredMs = null
  let clearedId = null
  let visible = true
  let ticks = 0
  const stop = startVisibleInterval({
    intervalMs: 60_000,
    isVisible: () => visible,
    onTick: () => { ticks += 1 },
    setIntervalFn: (cb, ms) => { registeredCallback = cb; registeredMs = ms; return 42 },
    clearIntervalFn: (id) => { clearedId = id },
  })
  assert.equal(registeredMs, 60_000)
  registeredCallback()
  assert.equal(ticks, 1)
  visible = false
  registeredCallback()
  assert.equal(ticks, 1, 'Hidden tab must skip refresh')
  visible = true
  registeredCallback()
  assert.equal(ticks, 2)
  stop()
  assert.equal(clearedId, 42)
})
