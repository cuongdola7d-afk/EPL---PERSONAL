import test from 'node:test'
import assert from 'node:assert/strict'
import { clubDetailHash, parseClubRoute } from './clubRoute.js'
import { matchDetailHash, parseMatchRoute } from './matchRoute.js'
import { playerDetailHash, parsePlayerDetailHash } from './playerRoute.js'
import { standingBand, clubMatches, clubMatchResult, clubRanking, FOOT_LABEL } from './clubView.js'

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
