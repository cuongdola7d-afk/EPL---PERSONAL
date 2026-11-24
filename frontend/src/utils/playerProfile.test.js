import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildCleanSheetCells,
  buildDefensiveActionsSummary,
  buildFormSeries,
  buildSaveRateSummary,
  buildTeamShareCells,
  expectedVerdict,
  formatFormDetail,
  formatMatchCellValue,
  formatPer90,
  formatPerMatch,
  normalizePlayerTab,
  percentileBand,
  percentileLabel,
  roleMatchColumns,
} from './playerProfile.js'

test('roleMatchColumns maps all four role groups with Vietnamese headers and no abbreviations', () => {
  assert.deepEqual(roleMatchColumns('ATTACK').map((c) => c.label), ['Bàn', 'KT', 'xG', 'xA'])
  assert.deepEqual(roleMatchColumns('MIXED').map((c) => c.label), ['Bàn', 'KT', 'Tắc bóng', 'Đoạt bóng'])
  assert.deepEqual(roleMatchColumns('DEFEND').map((c) => c.label), ['Tắc bóng', 'Chặn/cắt', 'Đoạt bóng'])
  assert.deepEqual(roleMatchColumns('GK').map((c) => c.label), ['Cứu thua', 'Thủng lưới', 'Đoạt bóng'])
  assert.deepEqual(roleMatchColumns('ATTACK', 2024).map((c) => c.label), ['Bàn', 'KT', 'Thẻ vàng'])
})

test('percentileBand and percentileLabel respect thresholds and null safety', () => {
  assert.equal(percentileBand(null), 'na')
  assert.equal(percentileLabel(null), '—')
  assert.equal(percentileBand(100), 'hi')
  assert.equal(percentileLabel(100), 'Top 1%')
  assert.equal(percentileLabel(95), 'Top 5%')
  assert.equal(percentileLabel(90), 'Top 10%')
  assert.equal(percentileBand(67), 'hi')
  assert.equal(percentileLabel(67), 'Tốt')
  assert.equal(percentileBand(66), 'mid')
  assert.equal(percentileLabel(66), 'Trung bình')
  assert.equal(percentileBand(34), 'mid')
  assert.equal(percentileLabel(34), 'Trung bình')
  assert.equal(percentileBand(33), 'lo')
  assert.equal(percentileLabel(33), 'Thấp')
})

test('expectedVerdict handles on-target, overperforming, underperforming, and null', () => {
  assert.deepEqual(expectedVerdict(null, 2.1), { tone: 'eq', label: '—', diff: null })
  assert.deepEqual(expectedVerdict(4, null), { tone: 'eq', label: '—', diff: null })
  assert.equal(expectedVerdict(4, 3.8).label, 'Đúng kỳ vọng')
  assert.equal(expectedVerdict(4, 3.8).tone, 'eq')
  assert.equal(expectedVerdict(5, 3.2).label, 'Hơn kỳ vọng +1.8')
  assert.equal(expectedVerdict(5, 3.2).tone, 'up')
  assert.equal(expectedVerdict(1, 2.6).label, 'Kém kỳ vọng −1.6')
  assert.equal(expectedVerdict(1, 2.6).tone, 'down')
})

test('buildTeamShareCells and buildCleanSheetCells generate expected grids and handle null/zero', () => {
  const share = buildTeamShareCells(14, 4, 2)
  assert.equal(share.pct, 43)
  assert.equal(share.involved, 6)
  assert.equal(share.other, 8)
  assert.equal(share.cells.length, 14)
  assert.equal(share.cells.filter((c) => c === 'goal').length, 4)
  assert.equal(share.cells.filter((c) => c === 'assist').length, 2)
  assert.equal(share.cells.filter((c) => c === 'other').length, 8)

  assert.equal(buildTeamShareCells(0, 0, 0).pct, null)
  assert.equal(buildTeamShareCells(10, null, 2).pct, null)

  const cs = buildCleanSheetCells(4, 10)
  assert.equal(cs.pct, 40)
  assert.equal(cs.cells.length, 10)
  assert.equal(cs.cells.filter((c) => c === 'cs').length, 4)
  assert.equal(buildCleanSheetCells(null, 5).pct, null)
  assert.equal(buildCleanSheetCells(0, 0).pct, null)
})

test('buildFormSeries orders matches chronologically, keeps DNP, picks highest rating, and formats null-safe values', () => {
  const mkRow = (id, gw, date, st, rating, g, a, extra = null) => ({
    clubId: 1,
    evidenceStatus: 'MANUAL_VERIFIED',
    match: {
      id, matchweek: gw, date, status: 'FINISHED',
      homeClubId: 1, homeClub: 'Man United', awayClubId: 2, awayClub: 'Opponent',
      homeGoals: 2, awayGoals: 1,
    },
    stats: {
      playerId: 10, participationStatus: st,
      minutes: st === 'PLAYED' ? 90 : 0,
      goals: g, assists: a, yellowCards: 0, redCards: 0,
      rating, extra,
    },
  })
  const rows = [
    mkRow(3, 3, '2026-08-29', 'DID_NOT_PLAY', null, 0, 0, null),
    mkRow(1, 1, '2026-08-15', 'PLAYED', '7.2', 0, 0, { expectedGoals: 0.12, expectedAssists: 0.05 }),
    mkRow(2, 2, '2026-08-22', 'PLAYED', '9.6', 2, 1, null),
  ]

  const series = buildFormSeries(rows, 2026)
  assert.deepEqual(series.items.map((i) => i.matchweek), [1, 2, 3])
  assert.equal(series.bestId, 2)
  assert.equal(series.defaultActiveId, 2)
  assert.equal(series.items[2].state, 'did-not-play')

  const bestDetail = formatFormDetail(series.items[1])
  assert.equal(bestDetail.headline, 'GW2 · Man United 2–1 Opponent')
  assert.match(bestDetail.suffix, /rating 9\.6 · 2 bàn · 1 kiến tạo/)
  assert.doesNotMatch(bestDetail.suffix, /xG/)

  const dnpDetail = formatFormDetail(series.items[2])
  assert.equal(dnpDetail.suffix, ' · Không ra sân.')

  const missingRow = {
    clubId: 1,
    evidenceStatus: 'PENDING',
    match: {
      id: 4, matchweek: 4, date: '2026-09-05', status: 'FINISHED',
      homeClubId: 1, homeClub: 'Man United', awayClubId: 2, awayClub: 'Opponent',
      homeGoals: 0, awayGoals: 0,
    },
    stats: null,
  }
  const seriesWithMissing = buildFormSeries([...rows, missingRow], 2026)
  const missingItem = seriesWithMissing.items.find((i) => i.matchweek === 4)
  assert.equal(missingItem.state, 'missing')
  assert.equal(formatFormDetail(missingItem).suffix, ' · Chưa có số liệu.')

  // Null extra stat displays em dash, numeric zero is muted
  assert.deepEqual(formatMatchCellValue(rows[2], { key: 'expectedGoals', source: 'extra', decimals: 2 }), { text: '—', muted: true })
  assert.deepEqual(formatMatchCellValue(rows[1], { key: 'goals', source: 'base', decimals: 0 }), { text: '0', muted: true })
  assert.deepEqual(formatMatchCellValue(rows[1], { key: 'expectedGoals', source: 'extra', decimals: 2 }), { text: '0.12', muted: false })
})

test('buildSaveRateSummary and buildDefensiveActionsSummary compute summaries and return null when extra stats are null', () => {
  assert.equal(buildSaveRateSummary(null, 13), null)
  assert.equal(buildSaveRateSummary(34, null), null)
  const saveSummary = buildSaveRateSummary(34, 13)
  assert.equal(saveSummary.saves, 34)
  assert.equal(saveSummary.goalsConceded, 13)
  assert.equal(saveSummary.shotsOnTarget, 47)
  assert.equal(saveSummary.pct, 72)
  assert.equal(saveSummary.subtitle, '34 cứu thua · 13 bàn thua')
  assert.equal(Math.round(saveSummary.saveWidthPct), 72)
  assert.equal(Math.round(saveSummary.concededWidthPct), 28)
  const zeroShots = buildSaveRateSummary(0, 0)
  assert.equal(zeroShots.pct, null)
  assert.equal(zeroShots.shotsOnTarget, 0)
  assert.equal(zeroShots.subtitle, '0 cứu thua · 0 bàn thua')

  assert.equal(buildDefensiveActionsSummary(null, 50, 40, 10), null)
  assert.equal(buildDefensiveActionsSummary(12, null, 40, 10), null)
  assert.equal(buildDefensiveActionsSummary(12, 50, null, 10), null)
  const defSummary = buildDefensiveActionsSummary(14, 62, 41, 10)
  assert.equal(defSummary.tackles, 14)
  assert.equal(defSummary.cbi, 62)
  assert.equal(defSummary.recoveries, 41)
  assert.equal(defSummary.total, 117)
  assert.equal(defSummary.perMatch, '11.7')
  assert.equal(Math.round(defSummary.tacklesPct), 12)
  assert.equal(Math.round(defSummary.cbiPct), 53)
  assert.equal(Math.round(defSummary.recoveriesPct), 35)
})

test('normalizePlayerTab and rate formatters handle legacy season tab and null inputs', () => {
  assert.equal(normalizePlayerTab('season'), 'overview')
  assert.equal(normalizePlayerTab('unknown'), 'overview')
  assert.equal(normalizePlayerTab('matches'), 'matches')
  assert.equal(formatPer90(null, 900), null)
  assert.equal(formatPer90(4, 0), null)
  assert.equal(formatPer90(4, 810), '0.44')
  assert.equal(formatPerMatch(220, 9), '24.4')
  assert.equal(formatPerMatch(null, 9), null)
})

test('formatFormDetail labels PROVISIONAL_FPL unrated match cleanly', () => {
  const provisionalRow = {
    clubId: 1,
    dataStatus: 'PROVISIONAL_FPL',
    evidenceStatus: 'MANUAL_PENDING',
    match: {
      id: 99, matchweek: 11, date: '2026-11-22', status: 'FINISHED',
      homeClubId: 1, homeClub: 'Liverpool FC', awayClubId: 2, awayClub: 'Manchester United FC',
      homeGoals: 4, awayGoals: 0,
    },
    stats: {
      playerId: 10, participationStatus: 'PLAYED',
      minutes: 90, goals: 2, assists: 1, yellowCards: 0, redCards: 0,
      rating: null, extra: { expectedGoals: 1.15, expectedAssists: 0.42 },
    },
  }
  const series = buildFormSeries([provisionalRow], 2026)
  assert.equal(series.items[0].rating, null)
  const detail = formatFormDetail(series.items[0])
  assert.equal(detail.headline, 'GW11 · Liverpool FC 4–0 Manchester United FC')
  assert.match(detail.suffix, /số liệu tạm \(FPL\) · 2 bàn · 1 kiến tạo/)
})
