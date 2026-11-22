import test from 'node:test'
import assert from 'node:assert/strict'
import {
  applyLiveScoreUpdate,
  applyLiveSnapshot,
  formatLiveGoalText,
  formatLiveStatusBadge,
  resolveMatchDisplay,
} from './liveScores.js'

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
  updatedAt: '2026-11-21T16:10:00Z',
  source: 'FPL',
  ...overrides,
})

test('formatLiveStatusBadge and formatLiveGoalText format LIVE minute, FT, ~minute, and own goals', () => {
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'LIVE', minute: 67 })), "LIVE 67'")
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'LIVE', minute: 0 })), 'LIVE')
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'FT', minute: 90 })), 'Hết giờ')
  assert.equal(formatLiveStatusBadge(sampleState({ status: 'PRE', minute: 0 })), 'Sắp diễn ra')

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
  const d0 = resolveMatchDisplay(scheduledDbMatch, null)
  assert.equal(d0.scored, false)
  assert.equal(d0.isLive, false)

  // Active LIVE state -> uses live score and LIVE badge
  const live = sampleState({ status: 'LIVE', minute: 67, homeScore: 1, awayScore: 2 })
  const d1 = resolveMatchDisplay(scheduledDbMatch, live)
  assert.equal(d1.scored, true)
  assert.equal(d1.homeGoals, 1)
  assert.equal(d1.awayGoals, 2)
  assert.equal(d1.isLive, true)
  assert.equal(d1.badgeText, "LIVE 67'")
  assert.equal(d1.useLiveGoals, true)

  // FT live state -> uses FT score and "Hết giờ"
  const ft = sampleState({ status: 'FT', minute: 90, homeScore: 1, awayScore: 2 })
  const d2 = resolveMatchDisplay(scheduledDbMatch, ft)
  assert.equal(d2.isLive, false)
  assert.equal(d2.isLiveFt, true)
  assert.equal(d2.badgeText, 'Hết giờ')

  // When match already has verified manual stats (hasManualStats: true) and preferManualStats is enabled
  const manualDbMatch = {
    ...scheduledDbMatch,
    status: 'FINISHED',
    homeGoals: 1,
    awayGoals: 4,
    hasManualStats: true,
  }
  const d3 = resolveMatchDisplay(manualDbMatch, ft, { preferManualStats: true })
  assert.equal(d3.isLive, false)
  assert.equal(d3.isLiveFt, false)
  assert.equal(d3.homeGoals, 1)
  assert.equal(d3.awayGoals, 4)
  assert.equal(d3.useLiveGoals, false)
})
