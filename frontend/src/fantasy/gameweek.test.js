import test from 'node:test'
import assert from 'node:assert/strict'
import { formatDeadline, remainingMilliseconds, countdownLabel, shouldRefreshDeadline } from './gameweek.js'
import { fetchGameweeks } from '../api/gameweeks.js'

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
