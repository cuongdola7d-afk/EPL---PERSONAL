import { effectiveStat, matchRating, matchResult, matchState } from './playerDetail.js'
import { compareMatchSchedule } from './matchTime.js'
import { hasMatchScore } from './seasons.js'

export const RATING_TIER = (rating) =>
  rating == null ? 'na' : rating >= 9 ? 'blue' : rating >= 7 ? 'green' : rating >= 5 ? 'orange' : 'red'

export const ROLE_MATCH_COLUMNS = {
  ATTACK: [
    { key: 'goals', label: 'Bàn', source: 'base', decimals: 0 },
    { key: 'assists', label: 'KT', source: 'base', decimals: 0 },
    { key: 'expectedGoals', label: 'xG', source: 'extra', decimals: 2 },
    { key: 'expectedAssists', label: 'xA', source: 'extra', decimals: 2 },
  ],
  MIXED: [
    { key: 'goals', label: 'Bàn', source: 'base', decimals: 0 },
    { key: 'assists', label: 'KT', source: 'base', decimals: 0 },
    { key: 'tackles', label: 'Tắc bóng', source: 'extra', decimals: 0 },
    { key: 'recoveries', label: 'Đoạt bóng', source: 'extra', decimals: 0 },
  ],
  DEFEND: [
    { key: 'tackles', label: 'Tắc bóng', source: 'extra', decimals: 0 },
    { key: 'clearancesBlocksInterceptions', label: 'Chặn/cắt', source: 'extra', decimals: 0 },
    { key: 'recoveries', label: 'Đoạt bóng', source: 'extra', decimals: 0 },
  ],
  GK: [
    { key: 'saves', label: 'Cứu thua', source: 'extra', decimals: 0 },
    { key: 'goalsConceded', label: 'Thủng lưới', source: 'extra', decimals: 0 },
    { key: 'recoveries', label: 'Đoạt bóng', source: 'extra', decimals: 0 },
  ],
}

export const ROLE_MATCH_COLUMNS_2024 = [
  { key: 'goals', label: 'Bàn', source: 'base', decimals: 0 },
  { key: 'assists', label: 'KT', source: 'base', decimals: 0 },
  { key: 'yellowCards', label: 'Thẻ vàng', source: 'base', decimals: 0 },
]

export const PERCENTILE_META = {
  goalsPer90: { label: 'Ghi bàn', unit: 'bàn / 90 phút', format: (v) => v.toFixed(2) },
  xgPer90: { label: 'Cơ hội ghi bàn', unit: 'xG / 90 phút', format: (v) => v.toFixed(2) },
  assistsPer90: { label: 'Kiến tạo', unit: 'kiến tạo / 90 phút', format: (v) => v.toFixed(2) },
  xaPer90: { label: 'Tạo cơ hội', unit: 'xA / 90 phút', format: (v) => v.toFixed(2) },
  tacklesPer90: { label: 'Tắc bóng', unit: 'tắc / 90 phút', format: (v) => v.toFixed(2) },
  cbiPer90: { label: 'Chặn/cắt bóng', unit: 'chặn/cắt / 90 phút', format: (v) => v.toFixed(2) },
  recoveriesPer90: { label: 'Đoạt bóng', unit: 'đoạt / 90 phút', format: (v) => v.toFixed(2) },
  defendingPer90: { label: 'Phòng ngự', unit: 'tắc + chặn/cắt + đoạt / 90', format: (v) => v.toFixed(2) },
  bpsPer90: { label: 'Ảnh hưởng trận đấu', unit: 'BPS / 90 phút', format: (v) => v.toFixed(1) },
  savesPer90: { label: 'Cứu thua', unit: 'cứu thua / 90 phút', format: (v) => v.toFixed(2) },
  goalsConcededPer90: { label: 'Bàn thua', unit: 'bàn thua / 90 phút', format: (v) => v.toFixed(2) },
  cleanSheetRate: { label: 'Giữ sạch lưới', unit: 'tỉ lệ sạch lưới', format: (v) => `${Math.round(v * 100)}%` },
}

export const CLUB_COLORS = {
  Arsenal: ['#ef0107', '#7a0004'],
  'Arsenal FC': ['#ef0107', '#7a0004'],
  'Aston Villa': ['#670e36', '#3c081f'],
  'Aston Villa FC': ['#670e36', '#3c081f'],
  'AFC Bournemouth': ['#da291c', '#6e1009'],
  Bournemouth: ['#da291c', '#6e1009'],
  Brentford: ['#e30613', '#7a040b'],
  'Brentford FC': ['#e30613', '#7a040b'],
  Brighton: ['#0057b8', '#002c60'],
  'Brighton & Hove Albion FC': ['#0057b8', '#002c60'],
  Chelsea: ['#034694', '#01234a'],
  'Chelsea FC': ['#034694', '#01234a'],
  'Crystal Palace': ['#1b458f', '#0f2752'],
  'Crystal Palace FC': ['#1b458f', '#0f2752'],
  Everton: ['#003399', '#001a4d'],
  'Everton FC': ['#003399', '#001a4d'],
  Fulham: ['#38434a', '#161c20'],
  'Fulham FC': ['#38434a', '#161c20'],
  'Hull City': ['#f59e0b', '#92400e'],
  'Hull City AFC': ['#f59e0b', '#92400e'],
  'Ipswich Town': ['#1d4ed8', '#1e3a8a'],
  'Ipswich Town FC': ['#1d4ed8', '#1e3a8a'],
  'Leeds United': ['#1d4ed8', '#0f2b6e'],
  'Leeds United FC': ['#1d4ed8', '#0f2b6e'],
  Liverpool: ['#c8102e', '#6d0718'],
  'Liverpool FC': ['#c8102e', '#6d0718'],
  'Manchester City': ['#3a9bdc', '#165280'],
  'Manchester City FC': ['#3a9bdc', '#165280'],
  'Manchester United': ['#da291c', '#7d1209'],
  'Manchester United FC': ['#da291c', '#7d1209'],
  'Newcastle United': ['#2b343b', '#0f1417'],
  'Newcastle United FC': ['#2b343b', '#0f1417'],
  'Nottingham Forest': ['#dd0000', '#730000'],
  'Nottingham Forest FC': ['#dd0000', '#730000'],
  Sunderland: ['#eb172b', '#7b0a14'],
  'Sunderland AFC': ['#eb172b', '#7b0a14'],
  Tottenham: ['#132257', '#08102e'],
  'Tottenham Hotspur FC': ['#132257', '#08102e'],
  'West Ham': ['#7a263a', '#42121d'],
  'West Ham United FC': ['#7a263a', '#42121d'],
  Wolves: ['#f2a900', '#8c5d00'],
  'Wolverhampton Wanderers FC': ['#f2a900', '#8c5d00'],
}

export function clubGradient(clubName) {
  const [primary, secondary] = CLUB_COLORS[clubName] ?? ['#0f876f', '#183e61']
  return { primary, secondary }
}

export function normalizePlayerTab(tab) {
  if (tab === 'season') return 'overview'
  if (['overview', 'matches', 'next', 'info'].includes(tab)) return tab
  return 'overview'
}

export function roleMatchColumns(roleGroup, season = 2026) {
  if (season !== 2026) return ROLE_MATCH_COLUMNS_2024
  return ROLE_MATCH_COLUMNS[roleGroup] ?? ROLE_MATCH_COLUMNS.MIXED
}

export function percentileBand(percentile) {
  if (percentile == null || !Number.isFinite(percentile)) return 'na'
  if (percentile >= 67) return 'hi'
  if (percentile >= 34) return 'mid'
  return 'lo'
}

export function percentileLabel(percentile) {
  if (percentile == null || !Number.isFinite(percentile)) return '—'
  if (percentile >= 90) return `Top ${Math.max(1, 100 - Math.round(percentile))}%`
  if (percentile >= 67) return 'Tốt'
  if (percentile >= 34) return 'Trung bình'
  return 'Thấp'
}

export function percentileMeta(key) {
  return PERCENTILE_META[key] ?? { label: key, unit: '', format: (v) => String(v) }
}

export function expectedVerdict(actual, expected) {
  if (actual == null || expected == null || !Number.isFinite(actual) || !Number.isFinite(expected)) {
    return { tone: 'eq', label: '—', diff: null }
  }
  const diff = actual - expected
  if (Math.abs(diff) < 0.5) {
    return { tone: 'eq', label: 'Đúng kỳ vọng', diff }
  }
  if (diff > 0) {
    return { tone: 'up', label: `Hơn kỳ vọng +${diff.toFixed(1)}`, diff }
  }
  return { tone: 'down', label: `Kém kỳ vọng −${Math.abs(diff).toFixed(1)}`, diff }
}

export function buildTeamShareCells(teamGoals, goals, assists) {
  if (teamGoals == null || teamGoals <= 0 || goals == null || assists == null) {
    return { pct: null, involved: null, other: null, cells: [] }
  }
  const safeGoals = Math.max(0, goals)
  const safeAssists = Math.max(0, assists)
  const involved = safeGoals + safeAssists
  const pct = Math.round((involved / teamGoals) * 100)
  const goalCells = Math.min(safeGoals, teamGoals)
  const assistCells = Math.min(safeAssists, Math.max(0, teamGoals - goalCells))
  const other = Math.max(0, teamGoals - goalCells - assistCells)
  const cells = [
    ...Array(goalCells).fill('goal'),
    ...Array(assistCells).fill('assist'),
    ...Array(other).fill('other'),
  ]
  return { pct, involved, other, cells }
}

export function buildCleanSheetCells(cleanSheets, appearances) {
  if (appearances == null || appearances <= 0 || cleanSheets == null) {
    return { pct: null, other: null, cells: [] }
  }
  const capped = Math.max(0, Math.min(cleanSheets, appearances))
  const other = Math.max(0, appearances - capped)
  const pct = Math.round((capped / appearances) * 100)
  const cells = [
    ...Array(capped).fill('cs'),
    ...Array(other).fill('other'),
  ]
  return { pct, other, cells }
}

export function buildSaveRateSummary(saves, goalsConceded) {
  if (
    saves == null ||
    goalsConceded == null ||
    !Number.isFinite(Number(saves)) ||
    !Number.isFinite(Number(goalsConceded))
  ) {
    return null
  }
  const safeSaves = Math.max(0, Number(saves))
  const safeConceded = Math.max(0, Number(goalsConceded))
  const shotsOnTarget = safeSaves + safeConceded
  const pct = shotsOnTarget > 0 ? Math.round((safeSaves / shotsOnTarget) * 100) : null
  const saveWidthPct = shotsOnTarget > 0 ? (safeSaves / shotsOnTarget) * 100 : 0
  const concededWidthPct = shotsOnTarget > 0 ? Math.max(0, 100 - saveWidthPct) : 0
  return {
    pct,
    saves: safeSaves,
    goalsConceded: safeConceded,
    shotsOnTarget,
    saveWidthPct,
    concededWidthPct,
  }
}

export function buildDefensiveActionsSummary(tackles, cbi, recoveries, appearances) {
  if (
    tackles == null ||
    cbi == null ||
    recoveries == null ||
    appearances == null ||
    !Number.isFinite(Number(tackles)) ||
    !Number.isFinite(Number(cbi)) ||
    !Number.isFinite(Number(recoveries)) ||
    !Number.isFinite(Number(appearances)) ||
    Number(appearances) <= 0
  ) {
    return null
  }
  const safeTackles = Math.max(0, Number(tackles))
  const safeCbi = Math.max(0, Number(cbi))
  const safeRecoveries = Math.max(0, Number(recoveries))
  const total = safeTackles + safeCbi + safeRecoveries
  const perMatch = (total / Number(appearances)).toFixed(1)
  const tacklesPct = total > 0 ? (safeTackles / total) * 100 : 0
  const cbiPct = total > 0 ? (safeCbi / total) * 100 : 0
  const recoveriesPct = total > 0 ? Math.max(0, 100 - tacklesPct - cbiPct) : 0
  return {
    perMatch,
    total,
    tackles: safeTackles,
    cbi: safeCbi,
    recoveries: safeRecoveries,
    tacklesPct,
    cbiPct,
    recoveriesPct,
  }
}

export function formatPer90(value, minutes, decimals = 2) {
  if (value == null || minutes == null || minutes <= 0) return null
  return ((Number(value) * 90) / minutes).toFixed(decimals)
}

export function formatPerMatch(value, appearances, decimals = 1) {
  if (value == null || appearances == null || appearances <= 0) return null
  return (Number(value) / appearances).toFixed(decimals)
}

export function formatMatchCellValue(row, col) {
  const raw = col.source === 'extra'
    ? row.stats?.extra?.[col.key]
    : effectiveStat(row, col.key)
  if (raw == null || !Number.isFinite(Number(raw))) {
    return { text: '—', muted: true }
  }
  const num = Number(raw)
  const text = col.decimals > 0 ? num.toFixed(col.decimals) : String(num)
  return { text, muted: num === 0 }
}

export function buildFormSeries(finishedRows, season = 2026) {
  const ordered = [...finishedRows].sort((a, b) =>
    season === 2026
      ? compareMatchSchedule(a.match, b.match, season)
      : (a.match.date.localeCompare(b.match.date) || a.match.id - b.match.id))

  const items = ordered.map((row) => {
    const state = matchState(row)
    const rating = state === 'played' ? matchRating(row) : null
    const home = row.clubId === row.match.homeClubId
    const opponent = home ? row.match.awayClub : row.match.homeClub
    const score = hasMatchScore(row.match)
      ? `${row.match.homeGoals}–${row.match.awayGoals}`
      : '—'
    const fullScore = hasMatchScore(row.match)
      ? `${row.match.homeClub} ${row.match.homeGoals}–${row.match.awayGoals} ${row.match.awayClub}`
      : `${row.match.homeClub} – ${row.match.awayClub}`
    return {
      id: row.match.id,
      matchweek: row.match.matchweek,
      row,
      state,
      rating,
      tier: RATING_TIER(rating),
      home,
      opponent,
      result: matchResult(row),
      score,
      fullScore,
      goals: effectiveStat(row, 'goals'),
      assists: effectiveStat(row, 'assists'),
      yellowCards: effectiveStat(row, 'yellowCards'),
      redCards: effectiveStat(row, 'redCards'),
      extra: row.stats?.extra ?? null,
    }
  })

  let best = null
  for (const item of items) {
    if (item.rating == null) continue
    if (!best || item.rating > best.rating) {
      best = item
    }
  }
  const defaultActiveId = best?.id ?? (items.length ? items[items.length - 1].id : null)
  return { items, bestId: best?.id ?? null, defaultActiveId }
}

export function formatFormDetail(item) {
  if (!item) return null
  const headline = `GW${item.matchweek} · ${item.fullScore}`
  if (item.state === 'did-not-play') {
    return { headline, suffix: ' · Không ra sân.' }
  }
  if (item.state !== 'played') {
    return { headline, suffix: ' · Chưa có số liệu.' }
  }
  const parts = []
  if (item.rating != null) {
    parts.push(`rating ${item.rating.toFixed(1)}`)
  } else {
    parts.push('chưa được chấm điểm')
  }
  if (item.goals > 0) parts.push(`${item.goals} bàn`)
  if (item.assists > 0) parts.push(`${item.assists} kiến tạo`)
  if (item.yellowCards > 0) parts.push(`${item.yellowCards} thẻ vàng`)
  if (item.redCards > 0) parts.push(`${item.redCards} thẻ đỏ`)
  if (item.extra?.expectedGoals != null) parts.push(`xG ${Number(item.extra.expectedGoals).toFixed(2)}`)
  if (item.extra?.expectedAssists != null) parts.push(`xA ${Number(item.extra.expectedAssists).toFixed(2)}`)
  return { headline, suffix: ` · ${parts.join(' · ')}` }
}
