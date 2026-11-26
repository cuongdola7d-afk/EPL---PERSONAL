import test from 'node:test'
import assert from 'node:assert/strict'
import { clubDetailHash, parseClubRoute } from './clubRoute.js'
import { matchDetailHash, parseMatchRoute } from './matchRoute.js'
import { playerDetailHash, parsePlayerDetailHash } from './playerRoute.js'
import {
  standingBand,
  clubMatches,
  clubMatchResult,
  clubRanking,
  FOOT_LABEL,
  formatFormTooltip,
  clampTooltipPosition,
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

test('rankings use club-specific totals, exclude missing values/DNP and do not import goals from another club', () => {
  const roster=[{id:1,name:'A',goals:100},{id:2,name:'B'},{id:3,name:'C'}]
  const stats={players:[{playerId:1,goals:2,averageRating:7,appearances:2},{playerId:2,goals:null,averageRating:null,appearances:1},
    {playerId:3,goals:0,averageRating:null,appearances:0},{playerId:99,goals:8,appearances:3}]}
  assert.deepEqual(clubRanking(roster,stats,'goals').map(row=>[row.player.id,row.value]),[[1,2]])
  assert.deepEqual(clubRanking(roster,stats,'rating').map(row=>row.value),[7])
  assert.equal(FOOT_LABEL.BOTH,'Hai chân')
})
