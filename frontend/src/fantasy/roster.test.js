import test from 'node:test'
import assert from 'node:assert/strict'
import { fetchFantasyPlayers, checkFantasyLineup } from '../api/fantasy.js'
import { fetchGameweeks } from '../api/gameweeks.js'
import { todayVietnamDate } from './lineup.js'

test('two multiplayer GWs fetch their server rosters and validate in the same GW', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  const calls = []
  global.fetch = async (url, options) => {
    calls.push([url, options])
    const gw = url.includes('/7/') || url.includes('gameweek=7') ? 7 : 6
    return new Response(JSON.stringify(url.includes('/players') ?
      { season: 2026, gameweek: gw, rosterAsOf: gw === 6 ? '2026-10-05' : '2026-10-07', players: [] } :
      { valid: true, totalOvr: 858, issues: [] }))
  }
  assert.deepEqual(await fetchFantasyPlayers(null, 6, '2026-10-05'), [])
  assert.deepEqual(await fetchFantasyPlayers(null, 7, '2026-10-07'), [])
  await checkFantasyLineup('4-2-1-3', {}, 7)
  assert.deepEqual(calls.map(([url]) => url), [
    '/api/fantasy/2026/gameweeks/6/players', '/api/fantasy/2026/gameweeks/7/players',
    '/api/fantasy/2026/validate?gameweek=7',
  ])
  assert.ok(calls.every(([url]) => !url.includes('2026-10-02')))
})

test('guest mode uses configured GW roster endpoint or Vietnam current date, never hardcoded 2026-10-02', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  const calls = []
  const lewisPotter = {
    id: 2000030002, name: 'Keane Lewis-Potter', clubId: 1000000402, club: 'Brentford FC',
    position: 'DEFENDER', goals: 4, assists: 1, fc27Overall: 77,
    primaryPosition: 'LB', eligiblePositions: ['LB', 'CAM', 'LM'], positionStatus: 'VERIFIED',
  }
  global.fetch = async url => {
    calls.push(url)
    if (url.includes('/gameweeks/12/players')) {
      return new Response(JSON.stringify({
        season: 2026, gameweek: 12, rosterAsOf: '2026-11-25', players: [lewisPotter],
      }))
    }
    return new Response(JSON.stringify([lewisPotter]))
  }
  // Configured GW12 -> reuses public GW roster endpoint
  const gwPlayers = await fetchFantasyPlayers(null, 12, '2026-11-25')
  assert.equal(gwPlayers.length, 1)
  assert.equal(gwPlayers[0].name, 'Keane Lewis-Potter')
  // Unconfigured fallback -> uses Vietnam local date (2026-11-25T18:00:00Z == 2026-11-26 01:00 VN)
  const fallbackPlayers = await fetchFantasyPlayers(null, null, null, new Date('2026-11-25T18:00:00Z'))
  assert.equal(fallbackPlayers.length, 1)
  assert.deepEqual(calls, [
    '/api/fantasy/2026/gameweeks/12/players',
    '/api/players?season=2026&asOf=2026-11-26',
  ])
  assert.equal(todayVietnamDate(new Date('2026-11-25T16:59:59Z')), '2026-11-25')
  assert.equal(todayVietnamDate(new Date('2026-11-25T17:00:00Z')), '2026-11-26')
  assert.ok(calls.every(url => !url.includes('2026-10-02')))
})

test('roster from a stale or different gameweek cannot be applied to the selector', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  global.fetch = async () => new Response(JSON.stringify({
    season: 2026, gameweek: 6, rosterAsOf: '2026-10-05', players: [],
  }))
  await assert.rejects(fetchFantasyPlayers(null, 7, '2026-10-07'), /roster/)
  await assert.rejects(fetchFantasyPlayers(null, 6, '2026-10-07'), /roster/)
})

test('a configured GW must supply its authoritative roster reference date', async t => {
  const original = global.fetch; t.after(() => { global.fetch = original })
  const data = { serverTimeUtc: '2026-10-05T00:00:00Z', recommendedGameweek: 6,
    gameweeks: Array.from({ length: 38 }, (_, i) => ({ season: 2026, gameweek: i + 1,
      configured: false, canEdit: false, deadlineUtc: null, status: null, deadlineChanges: [], rosterAsOf: null })) }
  Object.assign(data.gameweeks[5], { configured: true, canEdit: true, status: 'OPEN',
    deadlineUtc: '2026-10-08T17:00:00Z', deadlinePublishedAt: '2026-10-05T00:00:00Z', rosterAsOf: '2026-10-05' })
  global.fetch = async () => new Response(JSON.stringify(data))
  assert.equal((await fetchGameweeks()).gameweeks[5].rosterAsOf, '2026-10-05')
  delete data.gameweeks[5].rosterAsOf
  await assert.rejects(fetchGameweeks(), /không đúng định dạng/)
})
