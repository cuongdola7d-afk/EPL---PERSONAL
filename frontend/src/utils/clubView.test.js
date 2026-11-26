import test from 'node:test'
import assert from 'node:assert/strict'
import { clubDetailHash, parseClubRoute } from './clubRoute.js'
import { matchDetailHash, parseMatchRoute } from './matchRoute.js'
import { playerDetailHash, parsePlayerDetailHash } from './playerRoute.js'
import {
  standingBand,
  clubMatches,
  clubMatchResult,
  FOOT_LABEL,
  formatFormTooltip,
  clampTooltipPosition,
  clubShortName,
  clubPerspectiveScore,
  groupMatchesByVietnamMonth,
  squadColumnsForGroup,
  calculateAge,
  playerPositionChips,
  sortSquadRows,
  squadColumnLeaders,
  isSquadColumnLeader,
  formatSquadStat,
  squadRatingBand,
} from './clubView.js'

test('club routes default to overview tab, support explicit tab=overview and preserve legacy tab links', () => {
  assert.equal(parseClubRoute('#clubs/1000000057?season=2026').tab, 'overview')
  assert.equal(parseClubRoute('#clubs/1000000057?season=2026&tab=overview').tab, 'overview')
  assert.equal(parseClubRoute('#clubs/1000000057?season=2026&tab=standings').tab, 'standings')
  assert.equal(parseClubRoute('#clubs/1000000057?season=2026&tab=matches').tab, 'matches')
  const standingsHash = clubDetailHash(1000000057, { season: 2026, tab: 'standings' })
  assert.match(standingsHash, /tab=standings/)
  assert.equal(parseClubRoute(standingsHash).tab, 'standings')
})

test('formatFormTooltip formats home/away, scoreline with home first and Vietnam kickoff time', () => {
  const awayDraw = formatFormTooltip({
    id: 1,
    matchweek: 11,
    homeClubId: 2,
    homeClub: 'Newcastle United FC',
    awayClubId: 1,
    awayClub: 'Arsenal FC',
    homeGoals: 2,
    awayGoals: 2,
    status: 'FINISHED',
    date: '2026-11-21',
    kickoffUtc: '2026-11-21T17:30:00Z',
  }, 1, 2026)
  assert.equal(awayDraw.header, 'GW11 · Khách')
  assert.equal(awayDraw.scoreline, 'Newcastle United FC 2–2 Arsenal FC')
  assert.equal(awayDraw.timeLabel, 'CN 22/11 · 00:30 giờ VN')
  assert.match(awayDraw.ariaLabel, /Hòa · GW11 · Khách/)

  const homeWin = formatFormTooltip({
    id: 2,
    matchweek: 8,
    homeClubId: 1,
    homeClub: 'Arsenal FC',
    awayClubId: 3,
    awayClub: 'Everton FC',
    homeGoals: 2,
    awayGoals: 0,
    status: 'FINISHED',
    date: '2026-10-24',
    kickoffUtc: '2026-10-24T14:00:00Z',
  }, 1, 2026)
  assert.equal(homeWin.header, 'GW8 · Nhà')
  assert.equal(homeWin.scoreline, 'Arsenal FC 2–0 Everton FC')
  assert.equal(homeWin.timeLabel, 'T7 24/10 · 21:00 giờ VN')
})

test('clampTooltipPosition keeps tooltip inside 390px viewport at both edges', () => {
  const leftEdge = clampTooltipPosition({
    anchorCenterX: 13,
    containerLeft: 16,
    tooltipWidth: 230,
    viewportWidth: 390,
  })
  assert.equal(16 + leftEdge.left, 12)
  assert.ok(leftEdge.arrowLeft >= 14)

  const rightEdge = clampTooltipPosition({
    anchorCenterX: 120,
    containerLeft: 250,
    tooltipWidth: 230,
    viewportWidth: 390,
  })
  assert.equal(250 + rightEdge.left + 230, 390 - 12)
  assert.ok(rightEdge.arrowLeft <= 230 - 14)
})

test('clubShortName strips FC/AFC suffixes, maps all 20 clubs and falls back cleanly for unknown clubs', () => {
  assert.equal(clubShortName('Manchester United FC'), 'Man Utd')
  assert.equal(clubShortName('Manchester City FC'), 'Man City')
  assert.equal(clubShortName('Crystal Palace FC'), 'C. Palace')
  assert.equal(clubShortName('Nottingham Forest FC'), 'Forest')
  assert.equal(clubShortName('Brighton & Hove Albion FC'), 'Brighton')
  assert.equal(clubShortName('AFC Bournemouth'), 'Bournemouth')
  assert.equal(clubShortName('Hull City AFC'), 'Hull')
  assert.equal(clubShortName('Sunderland AFC'), 'Sunderland')
  assert.equal(clubShortName('Tottenham Hotspur FC'), 'Spurs')
  assert.equal(clubShortName('Newcastle United FC'), 'Newcastle')
  assert.equal(clubShortName('Coventry City FC'), 'Coventry')
  assert.equal(clubShortName('Ipswich Town FC'), 'Ipswich')
  assert.equal(clubShortName('Leeds United FC'), 'Leeds')
  assert.equal(clubShortName('Wolverhampton Wanderers FC'), 'Wolves')
  assert.equal(clubShortName('West Ham United FC'), 'West Ham')
  assert.equal(clubShortName('Liverpool FC'), 'Liverpool')
  assert.equal(clubShortName('Arsenal'), 'Arsenal')
  assert.equal(clubShortName('Bolton Wanderers FC'), 'Bolton Wanderers')
  assert.equal(clubShortName('Wrexham AFC'), 'Wrexham')
})

test('clubPerspectiveScore puts own club goals first for home, away and draw matches', () => {
  // Liverpool (id=64) loses 0-1 at home to Arsenal
  assert.equal(clubPerspectiveScore({ homeClubId: 64, awayClubId: 57, homeGoals: 0, awayGoals: 1 }, 64), '0–1')
  // Liverpool (id=64) wins 3-1 away at Crystal Palace (home=354)
  assert.equal(clubPerspectiveScore({ homeClubId: 354, awayClubId: 64, homeGoals: 1, awayGoals: 3 }, 64), '3–1')
  // Draw 2-2 away at Newcastle
  assert.equal(clubPerspectiveScore({ homeClubId: 67, awayClubId: 64, homeGoals: 2, awayGoals: 2 }, 64), '2–2')
  // Unplayed match -> null
  assert.equal(clubPerspectiveScore({ homeClubId: 64, awayClubId: 57, homeGoals: null, awayGoals: null }, 64), null)
})

test('groupMatchesByVietnamMonth groups by Asia/Ho_Chi_Minh month including UTC-to-VN midnight rollover', () => {
  const groups = groupMatchesByVietnamMonth([
    // 2026-11-22 23:30 VN -> Tháng 11 · 2026
    { id: 3, date: '2026-11-22', kickoffUtc: '2026-11-22T16:30:00Z' },
    // 2026-10-31 19:00 UTC = 2026-11-01 02:00 VN -> MUST be Tháng 11 · 2026!
    { id: 2, date: '2026-10-31', kickoffUtc: '2026-10-31T19:00:00Z' },
    // 2026-10-25 21:00 VN -> Tháng 10 · 2026
    { id: 1, date: '2026-10-25', kickoffUtc: '2026-10-25T14:00:00Z' },
  ], 2026)
  assert.equal(groups.length, 2)
  assert.equal(groups[0].label, 'Tháng 11 · 2026')
  assert.deepEqual(groups[0].matches.map(m => m.id), [3, 2])
  assert.equal(groups[1].label, 'Tháng 10 · 2026')
  assert.deepEqual(groups[1].matches.map(m => m.id), [1])
})

test('club routes retain tabs, ranking and filters through player/match detail and reject unsafe routes', () => {
  const hash = clubDetailHash(64,{season:2026,tab:'players',players:'top',metric:'assists',count:10})
  assert.equal(parseClubRoute(hash).metric,'assists')
  assert.equal(parsePlayerDetailHash(playerDetailHash(10,2026,hash)).backHash,hash)
  assert.equal(parseMatchRoute(matchDetailHash(100,{season:2026,week:5},hash)).backHash,hash)
  for (const bad of ['#clubs/0','#clubs/1?season=2025','#clubs/1?tab=foo','#clubs/1?count=NaN','#clubs/1?count=2']) assert.equal(parseClubRoute(bad),null)
  assert.equal(parsePlayerDetailHash(playerDetailHash(10,2026,'javascript:alert(1)')).backHash,undefined)
})

test('standings mark all five leaders, both Europa places and exactly the last three; legacy season unchanged', () => {
  assert.deepEqual(Array.from({length:20},(_,i)=>standingBand(i+1,2026)),[
    ...Array(5).fill('rank-champions'),...Array(2).fill('rank-europa'),...Array(10).fill(''),...Array(3).fill('rank-relegation')])
  assert.equal(standingBand(1,2024),'')
})

test('club matches isolate a club and choose real past/future fixtures with postponed status preserved', () => {
  const fixture=(id,date,status,homeClubId=1,awayClubId=2)=>({id,date,status,homeClubId,awayClubId,homeGoals:1,awayGoals:2})
  const result=clubMatches([fixture(1,'2026-08-01','FINISHED'),fixture(2,'2026-09-01','FINISHED',2,1),
    fixture(3,'2026-10-10','SCHEDULED'),fixture(4,'2026-10-05','POSTPONED'),fixture(5,'2026-10-08','SCHEDULED',3,4)],1,'2026-10-04')
  assert.deepEqual(result.done.map(row=>row.id),[2,1])
  assert.deepEqual(result.soon.map(row=>row.id),[3])
  assert.deepEqual(result.pending.map(row=>row.id),[4,3])
  assert.equal(clubMatchResult(result.done[0],1),'W')
  assert.equal(clubMatchResult(result.done[1],1),'L')
})

test('FOOT_LABEL maps preferred foot values to Vietnamese labels', () => {
  assert.equal(FOOT_LABEL.LEFT, 'Chân trái')
  assert.equal(FOOT_LABEL.RIGHT, 'Chân phải')
  assert.equal(FOOT_LABEL.BOTH, 'Hai chân')
})

test('squadColumnsForGroup selects position-specific columns and highlight flags', () => {
  assert.deepEqual(
    squadColumnsForGroup('GOALKEEPER').map(c => [c.key, c.label, c.highlight]),
    [
      ['appearances', 'Trận', false],
      ['minutes', 'Phút', false],
      ['cleanSheets', 'Sạch lưới', true],
      ['saves', 'Cứu thua', true],
      ['goalsConceded', 'Thủng lưới', false],
    ],
  )
  assert.deepEqual(
    squadColumnsForGroup('DEFENDER').map(c => [c.key, c.label, c.highlight]),
    [
      ['appearances', 'Trận', false],
      ['minutes', 'Phút', false],
      ['cleanSheets', 'Sạch lưới', true],
      ['tackles', 'Tắc bóng', true],
      ['clearancesBlocksInterceptions', 'Cắt chặn phá', true],
    ],
  )
  assert.deepEqual(
    squadColumnsForGroup('MIDFIELDER').map(c => [c.key, c.label, c.highlight]),
    [
      ['appearances', 'Trận', false],
      ['minutes', 'Phút', false],
      ['goals', 'Bàn', true],
      ['assists', 'Kiến tạo', true],
      ['recoveries', 'Thu hồi', true],
    ],
  )
  assert.deepEqual(
    squadColumnsForGroup('FORWARD').map(c => [c.key, c.label, c.format, c.highlight]),
    [
      ['appearances', 'Trận', 'int', false],
      ['minutes', 'Phút', 'int', false],
      ['goals', 'Bàn', 'int', true],
      ['assists', 'Kiến tạo', 'int', true],
      ['expectedGoals', 'xG', 'dec1', true],
    ],
  )
  assert.deepEqual(squadColumnsForGroup('UNKNOWN'), squadColumnsForGroup('MIDFIELDER'))
})

test('sortSquadRows orders played players by minutes then OVR and puts unplayed players last by OVR', () => {
  const rows = [
    { player: { id: 1, name: 'Idle High', fc27Overall: 85 }, stats: null, played: false },
    { player: { id: 2, name: 'Mid Minutes', fc27Overall: 80 }, stats: { appearances: 5, minutes: 400 }, played: true },
    { player: { id: 3, name: 'Top Minutes Low Ovr', fc27Overall: 76 }, stats: { appearances: 9, minutes: 800 }, played: true },
    { player: { id: 4, name: 'Top Minutes High Ovr', fc27Overall: 88 }, stats: { appearances: 9, minutes: 800 }, played: true },
    { player: { id: 5, name: 'Idle Low', fc27Overall: 63 }, stats: { appearances: 0, minutes: 0 }, played: false },
  ]
  assert.deepEqual(sortSquadRows(rows, true).map(r => r.player.id), [4, 3, 2, 1, 5])
  assert.deepEqual(sortSquadRows(rows, false).map(r => r.player.id), [4, 1, 2, 3, 5])
})

test('squadColumnLeaders requires at least 270 minutes, skips zero max and highlights all ties', () => {
  const cols = squadColumnsForGroup('DEFENDER')
  const rows = [
    { player: { id: 1 }, played: true, stats: { appearances: 11, minutes: 990, cleanSheets: 5, tackles: 11, clearancesBlocksInterceptions: 92 } },
    { player: { id: 2 }, played: true, stats: { appearances: 11, minutes: 952, cleanSheets: 5, tackles: 28, clearancesBlocksInterceptions: 69 } },
    // Under 270 minutes with higher tackles must NOT become leader or be highlighted
    { player: { id: 3 }, played: true, stats: { appearances: 3, minutes: 210, cleanSheets: 5, tackles: 40, clearancesBlocksInterceptions: 10 } },
    { player: { id: 4 }, played: false, stats: null },
  ]
  const leaders = squadColumnLeaders(rows, cols, 270)
  assert.equal(leaders.appearances, null)
  assert.equal(leaders.minutes, null)
  assert.equal(leaders.cleanSheets, 5)
  assert.equal(leaders.tackles, 28)
  assert.equal(leaders.clearancesBlocksInterceptions, 92)

  // Both tied players >= 270 min highlighted; sub-270 player NOT highlighted
  const csCol = cols[2]
  const tklCol = cols[3]
  assert.equal(isSquadColumnLeader(rows[0], csCol, leaders, 270), true)
  assert.equal(isSquadColumnLeader(rows[1], csCol, leaders, 270), true)
  assert.equal(isSquadColumnLeader(rows[2], csCol, leaders, 270), false)
  assert.equal(isSquadColumnLeader(rows[1], tklCol, leaders, 270), true)
  assert.equal(isSquadColumnLeader(rows[2], tklCol, leaders, 270), false)

  // Zero max is NOT highlighted
  const zeroRows = [
    { player: { id: 10 }, played: true, stats: { appearances: 5, minutes: 450, cleanSheets: 0, tackles: 0, clearancesBlocksInterceptions: 0 } },
  ]
  const zeroLeaders = squadColumnLeaders(zeroRows, cols, 270)
  assert.equal(zeroLeaders.cleanSheets, null)
  assert.equal(isSquadColumnLeader(zeroRows[0], csCol, zeroLeaders, 270), false)

  // Goals conceded is never highlighted for goalkeepers
  const gkCols = squadColumnsForGroup('GOALKEEPER')
  const gkRows = [
    { player: { id: 20 }, played: true, stats: { appearances: 11, minutes: 990, cleanSheets: 5, saves: 29, goalsConceded: 10 } },
  ]
  const gkLeaders = squadColumnLeaders(gkRows, gkCols, 270)
  assert.equal(gkLeaders.cleanSheets, 5)
  assert.equal(gkLeaders.saves, 29)
  assert.equal(gkLeaders.goalsConceded, null)
})

test('calculateAge handles birthdays before, on and after today without timezone drift', () => {
  assert.equal(calculateAge('1992-10-02', '2026-11-26'), 34)
  // Birthday not yet reached in 2026 (Dec 24 vs Nov 26)
  assert.equal(calculateAge('1998-12-24', '2026-11-26'), 27)
  // Exact birthday today
  assert.equal(calculateAge('2000-11-26', '2026-11-26'), 26)
  // Day before birthday
  assert.equal(calculateAge('2000-11-27', '2026-11-26'), 25)
  assert.equal(calculateAge(null, '2026-11-26'), null)
  assert.equal(calculateAge('bad-date', '2026-11-26'), null)
})

test('playerPositionChips limits to two chips and falls back to primaryPosition', () => {
  assert.deepEqual(playerPositionChips({ eligiblePositions: ['CAM', 'CM', 'LM', 'LW'], primaryPosition: 'CAM' }), ['CAM', 'CM'])
  assert.deepEqual(playerPositionChips({ eligiblePositions: [], primaryPosition: 'CB' }), ['CB'])
  assert.deepEqual(playerPositionChips({ eligiblePositions: [], primaryPosition: null }), [])
  assert.equal(formatSquadStat(6.2, 'dec1'), '6.2')
  assert.equal(formatSquadStat(null, 'dec1'), '–')
  assert.equal(squadRatingBand(7.42), 'high')
  assert.equal(squadRatingBand(7.07), 'good')
  assert.equal(squadRatingBand(6.71), 'mid')
  assert.equal(squadRatingBand(6.38), 'low')
})
