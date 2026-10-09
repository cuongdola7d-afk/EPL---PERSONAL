import test from 'node:test'
import assert from 'node:assert/strict'
import { formatDeadline, remainingMilliseconds, countdownLabel, shouldRefreshDeadline,
  displayedGameweek, nextGameweekToOpen } from './gameweek.js'
import { fetchGameweeks, publishGameweek } from '../api/gameweeks.js'

test('UTC deadline crosses the Vietnam date and uses midnight rather than 24h subtraction', () => {
  assert.equal(formatDeadline('2026-10-08T17:00:00Z'), '09/10/2026 · 00:00')
  assert.equal(formatDeadline('2026-10-09T10:00:00Z'), '09/10/2026 · 17:00')
  assert.equal(formatDeadline(null), 'Chưa công bố')
})

test('countdown follows server timestamp and monotonic elapsed time at deadline boundaries', () => {
  const args = ['2026-10-08T17:00:00Z', '2026-10-08T16:59:59Z', 500]
  assert.equal(remainingMilliseconds(...args, 1499), 1)
  assert.equal(remainingMilliseconds(...args, 1500), 0)
  assert.equal(remainingMilliseconds(...args, 1501), 0)
  assert.equal(countdownLabel(1000), '0 ngày 00:00:01')
  assert.equal(countdownLabel(86400000), '1 ngày 00:00:00')
  assert.equal(shouldRefreshDeadline({ configured: true, status: 'OPEN' }, 0), true)
  assert.equal(shouldRefreshDeadline({ configured: true, status: 'LOCKED' }, 0), false)
  assert.equal(shouldRefreshDeadline({ configured: false, status: null }, 0), false)
})

test('GW lookup omits account cookies and rejects invalid authoritative status', async t => {
  const original = global.fetch
  t.after(() => { global.fetch = original })
  const data = { serverTimeUtc: '2026-10-05T00:00:00Z', recommendedGameweek: 6,
    gameweeks: Array.from({ length: 38 }, (_, i) => ({ season: 2026, gameweek: i + 1, configured: false,
      deadlineUtc: null, status: null, canEdit: false, deadlineChanges: [] })) }
  global.fetch = async (url, options) => {
    assert.equal(url, '/api/fantasy/2026/gameweeks')
    assert.equal(options.credentials, 'omit')
    return new Response(JSON.stringify(data), { status: 200 })
  }
  assert.equal((await fetchGameweeks()).recommendedGameweek, 6)
  data.gameweeks[5].canEdit = true
  await assert.rejects(fetchGameweeks(), /không đúng định dạng/)
})

test('deadline expiry and an unpublished future schedule keep GW6 selected, including reload', () => {
  const rounds = [{ gameweek: 6, configured: true, status: 'LOCKED' },
    { gameweek: 7, configured: false, candidateDeadlineUtc: '2026-10-15T17:00:00Z' }]
  assert.equal(displayedGameweek(rounds, null, null), 6)
  assert.equal(displayedGameweek(rounds, 6, 6), 6)
  assert.equal(displayedGameweek(rounds, 7, 6), 6)
  assert.equal(nextGameweekToOpen(rounds).gameweek, 7)
  rounds[0].status = 'OPEN'
  assert.equal(nextGameweekToOpen(rounds), null)
})

test('admin opening advances to GW7; later refreshes preserve a deliberate visit to GW6', () => {
  const rounds = [{ gameweek: 6, configured: true, status: 'AWAITING_RESULTS' },
    { gameweek: 7, configured: true, status: 'OPEN' }, { gameweek: 8, configured: false }]
  assert.equal(displayedGameweek(rounds, 6, 6), 7)
  assert.equal(displayedGameweek(rounds, null, null), 7)
  assert.equal(displayedGameweek(rounds, 6, 7), 6)
  assert.equal(nextGameweekToOpen(rounds), null)
  rounds[1].status = 'LOCKED'
  assert.equal(displayedGameweek(rounds, 7, 7), 7)
  assert.equal(nextGameweekToOpen(rounds).gameweek, 8)
  assert.equal(nextGameweekToOpen([{ gameweek: 38, configured: true, status: 'PUBLISHED' }]), null)
})

test('opening a round sends the admin session and CSRF, leaving the deadline calculation to the server', async t => {
  const original = global.fetch
  t.after(() => { global.fetch = original })
  const calls = []
  global.fetch = async (url, options) => {
    calls.push([url, options])
    return new Response(JSON.stringify(url === '/api/auth/csrf' ?
      { headerName: 'X-CSRF-TOKEN', token: 'synthetic' } : { gameweek: 7, status: 'OPEN' }))
  }
  assert.equal((await publishGameweek(7)).status, 'OPEN')
  assert.equal(calls[0][0], '/api/auth/csrf')
  assert.equal(calls[0][1].credentials, 'include')
  assert.equal(calls[1][0], '/api/fantasy/2026/admin/gameweeks/7/publish-deadline')
  assert.equal(calls[1][1].credentials, 'include')
  assert.equal(calls[1][1].method, 'POST')
  assert.equal(calls[1][1].headers['X-CSRF-TOKEN'], 'synthetic')
  assert.deepEqual(JSON.parse(calls[1][1].body), { reason: 'Bắt đầu GW7 mùa 2026/27 với deadline theo lịch.' })
  global.fetch = async url => new Response(JSON.stringify(url === '/api/auth/csrf' ?
    { headerName: 'X-CSRF-TOKEN', token: 'synthetic' } : { message: 'Deadline đã công bố.' }),
    { status: url === '/api/auth/csrf' ? 200 : 409 })
  await assert.rejects(publishGameweek(7), error => error.status === 409 && error.message === 'Deadline đã công bố.')
})
