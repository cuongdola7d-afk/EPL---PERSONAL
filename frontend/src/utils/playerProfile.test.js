import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildCleanSheetCells,
  buildFormSeries,
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

  // Null extra stat displays em dash, numeric zero is muted
  assert.deepEqual(formatMatchCellValue(rows[2], { key: 'expectedGoals', source: 'extra', decimals: 2 }), { text: '—', muted: true })
  assert.deepEqual(formatMatchCellValue(rows[1], { key: 'goals', source: 'base', decimals: 0 }), { text: '0', muted: true })
  assert.deepEqual(formatMatchCellValue(rows[1], { key: 'expectedGoals', source: 'extra', decimals: 2 }), { text: '0.12', muted: false })
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
