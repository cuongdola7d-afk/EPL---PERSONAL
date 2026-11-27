import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildContributionPieSlices,
  buildXAxisTicks,
  countCleanSheets,
  cumulativePointsSeries,
  formatSignedDelta,
  normalizeMatchesFromTrends,
  shortSurname,
  splitHomeAway,
  summarizeResults,
  summarizeXgTotals,
} from './clubStats.js'

const LIVERPOOL_GW1_11 = normalizeMatchesFromTrends([
  { fixtureId: 1, matchweek: 1, home: false, opponentName: 'Newcastle United FC', goalsFor: 2, goalsAgainst: 2, teamRating: 6.75, xgFor: 3.01, xgAgainst: 1.58 },
  { fixtureId: 2, matchweek: 2, home: true, opponentName: 'Nottingham Forest FC', goalsFor: 2, goalsAgainst: 2, teamRating: 6.91, xgFor: 1.74, xgAgainst: 2.30 },
  { fixtureId: 3, matchweek: 3, home: false, opponentName: 'Ipswich Town FC', goalsFor: 2, goalsAgainst: 0, teamRating: 7.19, xgFor: 0.69, xgAgainst: 0.76 },
  { fixtureId: 4, matchweek: 4, home: true, opponentName: 'Fulham FC', goalsFor: 0, goalsAgainst: 0, teamRating: 7.03, xgFor: 1.29, xgAgainst: 0.69 },
  { fixtureId: 5, matchweek: 5, home: false, opponentName: 'AFC Bournemouth', goalsFor: 1, goalsAgainst: 0, teamRating: 7.01, xgFor: 1.65, xgAgainst: 0.80 },
  { fixtureId: 6, matchweek: 6, home: true, opponentName: 'Manchester City FC', goalsFor: 0, goalsAgainst: 3, teamRating: 6.34, xgFor: 0.95, xgAgainst: 1.28 },
  { fixtureId: 7, matchweek: 7, home: false, opponentName: 'Brentford FC', goalsFor: 2, goalsAgainst: 1, teamRating: 7.05, xgFor: 1.65, xgAgainst: 0.87 },
  { fixtureId: 8, matchweek: 8, home: true, opponentName: 'Brighton & Hove Albion FC', goalsFor: 2, goalsAgainst: 0, teamRating: 7.21, xgFor: 1.21, xgAgainst: 1.32 },
  { fixtureId: 9, matchweek: 9, home: true, opponentName: 'Arsenal FC', goalsFor: 0, goalsAgainst: 1, teamRating: 6.78, xgFor: 1.04, xgAgainst: 0.97 },
  { fixtureId: 10, matchweek: 10, home: false, opponentName: 'Crystal Palace FC', goalsFor: 3, goalsAgainst: 1, teamRating: 7.29, xgFor: 2.64, xgAgainst: 2.62 },
  { fixtureId: 11, matchweek: 11, home: true, opponentName: 'Manchester United FC', goalsFor: 4, goalsAgainst: 0, teamRating: 7.60, xgFor: 4.35, xgAgainst: 0.64 },
])

test('summarizeResults, countCleanSheets, splitHomeAway and cumulativePointsSeries match Liverpool GW1-11', () => {
  const total = summarizeResults(LIVERPOOL_GW1_11)
  assert.deepEqual(total, {
    played: 11,
    won: 6,
    drawn: 3,
    lost: 2,
    points: 21,
    goalsFor: 18,
    goalsAgainst: 10,
    cleanSheets: 5,
  })
  assert.equal(countCleanSheets(LIVERPOOL_GW1_11), 5)

  const split = splitHomeAway(LIVERPOOL_GW1_11)
  assert.equal(split.home.played, 6)
  assert.equal(split.home.points, 8)
  assert.equal(split.home.won, 2)
  assert.equal(split.home.goalsFor, 8)
  assert.equal(split.home.goalsAgainst, 6)

  assert.equal(split.away.played, 5)
  assert.equal(split.away.points, 13)
  assert.equal(split.away.won, 4)
  assert.equal(split.away.goalsFor, 10)
  assert.equal(split.away.goalsAgainst, 4)

  const series = cumulativePointsSeries(LIVERPOOL_GW1_11)
  assert.deepEqual(series.map(p => p.cumulativePoints), [1, 2, 5, 6, 9, 9, 12, 15, 15, 18, 21])

  const xgForSummary = summarizeXgTotals(LIVERPOOL_GW1_11, 'xgFor', 'goalsFor')
  assert.equal(xgForSummary.goals, 18)
  assert.equal(xgForSummary.xg, 20.22)
  assert.equal(formatSignedDelta(xgForSummary.delta), '−2.2')

  const xgAgainstSummary = summarizeXgTotals(LIVERPOOL_GW1_11, 'xgAgainst', 'goalsAgainst')
  assert.equal(xgAgainstSummary.goals, 10)
  assert.equal(xgAgainstSummary.xg, 13.83)
  assert.equal(formatSignedDelta(xgAgainstSummary.delta), '−3.8')

  // Null xG matches are excluded from xG totals
  const withMissingXg = [
    { goalsFor: 2, xgFor: 1.5 },
    { goalsFor: 3, xgFor: null },
  ]
  const partial = summarizeXgTotals(withMissingXg, 'xgFor', 'goalsFor')
  assert.equal(partial.count, 1)
  assert.equal(partial.goals, 2)
  assert.equal(partial.xg, 1.5)
  assert.equal(formatSignedDelta(partial.delta), '+0.5')
})

test('buildContributionPieSlices applies >=2 rule, caps at 4 named, groups remainder and own goals into Khac, and falls back to top 4 when all are 1', () => {
  const goalsPie = buildContributionPieSlices([
    { name: 'Alexander Isak', value: 7 },
    { name: 'Victor Munoz', value: 3 },
    { name: 'Dominik Szoboszlai', value: 2 },
    { name: 'Florian Wirtz', value: 2 },
    { name: 'Alexis Mac Allister', value: 1 },
    { name: 'Cody Gakpo', value: 1 },
    { name: 'Rio Ngumoha', value: 1 },
  ], { ownGoals: 1 })
  assert.equal(goalsPie.total, 18)
  assert.deepEqual(goalsPie.slices.map(s => [s.label, s.value, s.percent, s.isOther]), [
    ['Isak', 7, 39, false],
    ['Munoz', 3, 17, false],
    ['Szoboszlai', 2, 11, false],
    ['Wirtz', 2, 11, false],
    ['Khác', 4, 22, true],
  ])
  assert.deepEqual(goalsPie.slices.at(-1).tooltipMembers, ['Mac Allister', 'Gakpo', 'Ngumoha', 'phản lưới nhà'])

  // Assisters: Barcola leads ahead of Wirtz in API order -> 27% Barcola
  const assistsPie = buildContributionPieSlices([
    { name: 'Bradley Barcola', value: 4 },
    { name: 'Florian Wirtz', value: 4 },
    { name: 'Cody Gakpo', value: 3 },
    { name: 'Alexander Isak', value: 1 },
    { name: 'Dominik Szoboszlai', value: 1 },
    { name: 'Rio Ngumoha', value: 1 },
    { name: 'Ryan Gravenberch', value: 1 },
  ])
  assert.equal(assistsPie.total, 15)
  assert.deepEqual(assistsPie.slices.map(s => [s.label, s.value, s.percent]), [
    ['Barcola', 4, 27],
    ['Wirtz', 4, 27],
    ['Gakpo', 3, 20],
    ['Khác', 4, 27],
  ])
  assert.deepEqual(assistsPie.slices.at(-1).tooltipMembers, ['Isak', 'Szoboszlai', 'Ngumoha', 'Gravenberch'])

  // More than 4 players with >= 2 -> only top 4 named, 5th goes to Khac
  const fiveOverTwo = buildContributionPieSlices([
    { name: 'A One', value: 5 },
    { name: 'B Two', value: 4 },
    { name: 'C Three', value: 3 },
    { name: 'D Four', value: 2 },
    { name: 'E Five', value: 2 },
  ])
  assert.equal(fiveOverTwo.slices.length, 5)
  assert.equal(fiveOverTwo.slices[4].label, 'Khác')
  assert.equal(fiveOverTwo.slices[4].value, 2)
  assert.deepEqual(fiveOverTwo.slices[4].tooltipMembers, ['Five'])

  // Nobody has >= 2 -> take top 4 players, remaining into Khac
  const allOnes = buildContributionPieSlices([
    { name: 'A Alpha', value: 1 },
    { name: 'B Beta', value: 1 },
    { name: 'C Gamma', value: 1 },
    { name: 'D Delta', value: 1 },
    { name: 'E Epsilon', value: 1 },
  ], { ownGoals: 1 })
  assert.deepEqual(allOnes.slices.map(s => [s.label, s.value]), [
    ['Alpha', 1],
    ['Beta', 1],
    ['Gamma', 1],
    ['Delta', 1],
    ['Khác', 2],
  ])
  assert.deepEqual(allOnes.slices.at(-1).tooltipMembers, ['Epsilon', 'phản lưới nhà'])

  // Empty
  assert.deepEqual(buildContributionPieSlices([]), { total: 0, slices: [] })
  assert.equal(shortSurname('Virgil van Dijk'), 'van Dijk')
})

test('buildXAxisTicks handles 11, 20 and 38 matches at 360px and 1100px with >=26px spacing', () => {
  const makeItems = n => Array.from({ length: n }, (_, i) => ({ matchweek: i + 1, opponentCode: 'ARS' }))

  // 11 matches at 1100px -> full labels GW1 + opponent code
  const t11Wide = buildXAxisTicks(makeItems(11), 1100)
  assert.equal(t11Wide.compact, false)
  assert.equal(t11Wide.showOpponent, true)
  assert.equal(t11Wide.step, 1)
  assert.equal(t11Wide.ticks.length, 11)
  assert.equal(t11Wide.ticks[0].label, 'GW1')
  assert.equal(t11Wide.ticks[0].sublabel, 'ARS')

  // 11 matches at 360px -> compact numeric only, no opponent code, spacing >= 26px
  const t11Narrow = buildXAxisTicks(makeItems(11), 360)
  assert.equal(t11Narrow.compact, true)
  assert.equal(t11Narrow.showOpponent, false)
  assert.equal(t11Narrow.ticks[0].label, '1')
  assert.equal(t11Narrow.ticks[0].sublabel, null)
  assert.ok(t11Narrow.ticks[1].centerOffset - t11Narrow.ticks[0].centerOffset >= 26)

  // 20 matches at 1100px -> full labels (55px per slot)
  const t20Wide = buildXAxisTicks(makeItems(20), 1100)
  assert.equal(t20Wide.compact, false)
  assert.equal(t20Wide.ticks.length, 20)

  // 20 matches at 360px -> compact with step >= 2, spacing >= 26px
  const t20Narrow = buildXAxisTicks(makeItems(20), 360)
  assert.equal(t20Narrow.compact, true)
  assert.ok(t20Narrow.step >= 2)
  assert.ok(t20Narrow.ticks[1].centerOffset - t20Narrow.ticks[0].centerOffset >= 26)

  // 38 matches at 1100px -> compact numeric (28.9px per slot < 38px), spacing >= 26px
  const t38Wide = buildXAxisTicks(makeItems(38), 1100)
  assert.equal(t38Wide.compact, true)
  assert.equal(t38Wide.showOpponent, false)
  assert.equal(t38Wide.step, 1)
  assert.ok(t38Wide.ticks[1].centerOffset - t38Wide.ticks[0].centerOffset >= 26)

  // 38 matches at 360px -> compact numeric with step = 3, spacing >= 26px
  const t38Narrow = buildXAxisTicks(makeItems(38), 360)
  assert.equal(t38Narrow.compact, true)
  assert.equal(t38Narrow.showOpponent, false)
  assert.equal(t38Narrow.step, 3)
  assert.ok(t38Narrow.ticks[1].centerOffset - t38Narrow.ticks[0].centerOffset >= 26)
})
