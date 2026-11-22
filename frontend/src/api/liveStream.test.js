import test from 'node:test'
import assert from 'node:assert/strict'
import {
  applyHelloEvent,
  applyStreamDisconnect,
  applyStreamOpen,
  applyStreamStart,
  applyTickEvent,
  computeTickMetrics,
  createStreamState,
  formatDuration,
  formatStreamSummary,
  resolveLiveStreamUrl,
} from './liveStream.js'

test('resolveLiveStreamUrl defaults to relative path and validates explicit backend origin', () => {
  assert.equal(resolveLiveStreamUrl('/api/live/test-stream', ''), '/api/live/test-stream')
  assert.equal(resolveLiveStreamUrl('api/live/stream', undefined), '/api/live/stream')
  assert.equal(
    resolveLiveStreamUrl('/api/live/test-stream', 'https://epl-personal.onrender.com/'),
    'https://epl-personal.onrender.com/api/live/test-stream'
  )
  assert.throws(() => resolveLiveStreamUrl('/api/live/stream', 'https://epl-personal.onrender.com/api'))
  assert.throws(() => resolveLiveStreamUrl('/api/live/stream', 'ftp://epl-personal.onrender.com'))
})

test('computeTickMetrics calculates server-client latency, interval, >15s delay flag, and sequence gaps', () => {
  const t1 = { seq: 1, serverTime: '2026-11-22T10:00:00.000Z', connectionId: 'sse-1' }
  const m1 = computeTickMetrics(t1, Date.parse('2026-11-22T10:00:00.120Z'), null)
  assert.equal(m1.latencyMs, 120)
  assert.equal(m1.intervalMs, null)
  assert.equal(m1.isDelayed, false)
  assert.equal(m1.seqGap, 0)

  const prev1 = { lastTickAtMs: Date.parse('2026-11-22T10:00:00.120Z'), lastSeq: 1, connectionId: 'sse-1' }
  const t2 = { seq: 2, serverTime: '2026-11-22T10:00:10.000Z', connectionId: 'sse-1' }
  const m2 = computeTickMetrics(t2, Date.parse('2026-11-22T10:00:10.150Z'), prev1)
  assert.equal(m2.latencyMs, 150)
  assert.equal(m2.intervalMs, 10030)
  assert.equal(m2.intervalSec, 10.0)
  assert.equal(m2.isDelayed, false)
  assert.equal(m2.seqGap, 0)

  // Delayed tick (>15s) and missing seq=3 (seq jumps from 2 to 4 -> seqGap=1)
  const prev2 = { lastTickAtMs: Date.parse('2026-11-22T10:00:10.150Z'), lastSeq: 2, connectionId: 'sse-1' }
  const t4 = { seq: 4, serverTime: '2026-11-22T10:00:28.000Z', connectionId: 'sse-1' }
  const m4 = computeTickMetrics(t4, Date.parse('2026-11-22T10:00:28.400Z'), prev2)
  assert.equal(m4.intervalMs, 18250)
  assert.equal(m4.intervalSec, 18.3)
  assert.equal(m4.isDelayed, true)
  assert.equal(m4.seqGap, 1)
})

test('stream state reducer tracks ticks, delays, gaps, and reconnect counts across disconnects', () => {
  let state = applyStreamStart(createStreamState())
  assert.equal(state.status, 'connecting')
  assert.equal(state.reconnectCount, 0)

  const t0 = Date.parse('2026-11-22T10:00:00.000Z')
  state = applyStreamOpen(state, t0)
  state = applyHelloEvent(state, { connectionId: 'sse-1', channel: 'test', serverTime: '2026-11-22T10:00:00.000Z' }, t0)
  assert.equal(state.status, 'connected')
  assert.equal(state.reconnectCount, 0)

  state = applyTickEvent(state, { seq: 1, serverTime: '2026-11-22T10:00:10.000Z', connectionId: 'sse-1' }, t0 + 10050)
  state = applyTickEvent(state, { seq: 2, serverTime: '2026-11-22T10:00:26.000Z', connectionId: 'sse-1' }, t0 + 26250)
  assert.equal(state.tickCount, 2)
  assert.equal(state.isDelayed, true)
  assert.equal(state.delayedCount, 1)
  assert.equal(state.maxIntervalSec, 16.2)

  // Disconnect and reconnect
  state = applyStreamDisconnect(state, 'Network lost')
  assert.equal(state.status, 'disconnected')

  state = applyHelloEvent(state, { connectionId: 'sse-2', channel: 'test', serverTime: '2026-11-22T10:00:30.000Z' }, t0 + 30000)
  assert.equal(state.status, 'connected')
  assert.equal(state.reconnectCount, 1)
  assert.equal(state.connectionId, 'sse-2')

  const summary = formatStreamSummary({
    title: 'Qua Vercel',
    url: '/api/live/test-stream',
    state,
    nowMs: t0 + 65000,
  })
  assert.match(summary, /\[Qua Vercel\]/)
  assert.match(summary, /Thời gian đã kết nối: 01:05/)
  assert.match(summary, /Số tick nhận được: 2/)
  assert.match(summary, /Số lần kết nối lại: 1/)
  assert.equal(formatDuration(125000), '02:05')
  assert.equal(formatDuration(3661000), '01:01:01')
})
