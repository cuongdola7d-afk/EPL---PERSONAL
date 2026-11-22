import { fetchApiList } from './request.js'
import { isValidLiveMatchState } from '../utils/liveScores.js'

export const DELAY_THRESHOLD_MS = 15000

export function fetchLiveMatches(signal) {
  return fetchApiList('/api/live/matches', signal, isValidLiveMatchState, 'tỉ số trực tiếp')
}

export function resolveLiveStreamUrl(path, baseUrl = import.meta.env?.VITE_LIVE_STREAM_BASE) {
  const apiPath = `/${String(path ?? '').replace(/^\/+/, '')}`
  if (!baseUrl || !String(baseUrl).trim()) {
    return apiPath
  }
  let parsed
  try {
    parsed = new URL(String(baseUrl).trim().replace(/\/+$/, ''))
  } catch {
    throw new Error('VITE_LIVE_STREAM_BASE phải là URL origin hợp lệ.')
  }
  if (!['http:', 'https:'].includes(parsed.protocol) || !parsed.hostname ||
      parsed.pathname !== '/' || parsed.search || parsed.hash || parsed.username || parsed.password) {
    throw new Error('VITE_LIVE_STREAM_BASE phải là origin backend (không chứa đường dẫn, query hoặc hash).')
  }
  return `${parsed.origin}${apiPath}`
}

export function parseHelloPayload(raw) {
  const data = typeof raw === 'string' ? JSON.parse(raw) : raw
  if (!data || typeof data !== 'object' ||
      typeof data.connectionId !== 'string' || !data.connectionId.trim() ||
      typeof data.serverTime !== 'string' || !Number.isFinite(Date.parse(data.serverTime))) {
    throw new Error('Sự kiện hello không đúng định dạng.')
  }
  return {
    connectionId: data.connectionId,
    channel: typeof data.channel === 'string' ? data.channel : 'main',
    serverTime: data.serverTime,
  }
}

export function parseTickPayload(raw) {
  const data = typeof raw === 'string' ? JSON.parse(raw) : raw
  if (!data || typeof data !== 'object' ||
      !Number.isInteger(data.seq) || data.seq < 1 ||
      typeof data.connectionId !== 'string' || !data.connectionId.trim() ||
      typeof data.serverTime !== 'string' || !Number.isFinite(Date.parse(data.serverTime))) {
    throw new Error('Sự kiện tick không đúng định dạng.')
  }
  return {
    seq: data.seq,
    serverTime: data.serverTime,
    connectionId: data.connectionId,
  }
}

export function computeTickMetrics(tick, receivedAtMs, previous = null) {
  const serverTimeMs = Date.parse(tick.serverTime)
  const latencyMs = Number.isFinite(serverTimeMs) ? Math.round(receivedAtMs - serverTimeMs) : null
  const hasPrevTick = previous && Number.isFinite(previous.lastTickAtMs)
  const intervalMs = hasPrevTick ? Math.max(0, Math.round(receivedAtMs - previous.lastTickAtMs)) : null
  const intervalSec = intervalMs == null ? null : Number((intervalMs / 1000).toFixed(1))
  const isDelayed = intervalMs != null && intervalMs > DELAY_THRESHOLD_MS

  const hasPrevSeq = previous && Number.isInteger(previous.lastSeq)
  const sameConnection = !previous?.connectionId || previous.connectionId === tick.connectionId
  const seqGap = hasPrevSeq && sameConnection && tick.seq > previous.lastSeq + 1
    ? tick.seq - previous.lastSeq - 1
    : 0

  return {
    seq: tick.seq,
    connectionId: tick.connectionId,
    serverTime: tick.serverTime,
    serverTimeMs,
    receivedAtMs,
    receivedTimeIso: new Date(receivedAtMs).toISOString(),
    latencyMs,
    intervalMs,
    intervalSec,
    isDelayed,
    seqGap,
  }
}

export function createStreamState() {
  return {
    status: 'idle',
    connectionId: null,
    connectedAtMs: null,
    hasEverConnected: false,
    awaitingReconnect: false,
    tickCount: 0,
    reconnectCount: 0,
    gapCount: 0,
    delayedCount: 0,
    lastSeq: null,
    lastTickAtMs: null,
    lastIntervalMs: null,
    lastIntervalSec: null,
    maxIntervalSec: null,
    isDelayed: false,
    logs: [],
    error: '',
  }
}

export function applyStreamStart(state) {
  return {
    ...createStreamState(),
    status: 'connecting',
  }
}

export function applyStreamOpen(state, nowMs = Date.now()) {
  const isReconnect = state.hasEverConnected && (state.status !== 'connected' || state.awaitingReconnect)
  return {
    ...state,
    status: 'connected',
    connectedAtMs: state.connectedAtMs ?? nowMs,
    hasEverConnected: true,
    awaitingReconnect: false,
    reconnectCount: isReconnect ? state.reconnectCount + 1 : state.reconnectCount,
    error: '',
  }
}

export function applyHelloEvent(state, rawHello, nowMs = Date.now()) {
  const hello = parseHelloPayload(rawHello)
  const connectionChanged = state.connectionId !== null && state.connectionId !== hello.connectionId
  const shouldIncrementReconnect = state.awaitingReconnect || (state.hasEverConnected && connectionChanged && state.status === 'connected')
  return {
    ...state,
    status: 'connected',
    connectionId: hello.connectionId,
    connectedAtMs: state.connectedAtMs ?? nowMs,
    hasEverConnected: true,
    awaitingReconnect: false,
    reconnectCount: shouldIncrementReconnect ? state.reconnectCount + 1 : state.reconnectCount,
    lastSeq: connectionChanged ? null : state.lastSeq,
    error: '',
  }
}

export function applyTickEvent(state, rawTick, receivedAtMs = Date.now(), maxLogs = 120) {
  const tick = parseTickPayload(rawTick)
  const metrics = computeTickMetrics(tick, receivedAtMs, state)
  const nextLogs = [metrics, ...state.logs].slice(0, maxLogs)
  const maxIntervalSec = metrics.intervalSec == null
    ? state.maxIntervalSec
    : state.maxIntervalSec == null
      ? metrics.intervalSec
      : Math.max(state.maxIntervalSec, metrics.intervalSec)

  return {
    ...state,
    status: 'connected',
    connectionId: tick.connectionId,
    connectedAtMs: state.connectedAtMs ?? receivedAtMs,
    hasEverConnected: true,
    awaitingReconnect: false,
    tickCount: state.tickCount + 1,
    gapCount: state.gapCount + metrics.seqGap,
    delayedCount: state.delayedCount + (metrics.isDelayed ? 1 : 0),
    lastSeq: tick.seq,
    lastTickAtMs: receivedAtMs,
    lastIntervalMs: metrics.intervalMs,
    lastIntervalSec: metrics.intervalSec,
    maxIntervalSec,
    isDelayed: metrics.isDelayed,
    logs: nextLogs,
    error: '',
  }
}

export function applyStreamDisconnect(state, errorMessage = 'Mất kết nối tới luồng SSE') {
  return {
    ...state,
    status: 'disconnected',
    awaitingReconnect: state.hasEverConnected,
    error: errorMessage,
  }
}

export function applyStreamStop(state) {
  return {
    ...state,
    status: 'idle',
    awaitingReconnect: false,
  }
}

export function formatDuration(durationMs) {
  if (durationMs == null || !Number.isFinite(durationMs) || durationMs < 0) {
    return '00:00'
  }
  const totalSec = Math.floor(durationMs / 1000)
  const hours = Math.floor(totalSec / 3600)
  const minutes = Math.floor((totalSec % 3600) / 60)
  const seconds = totalSec % 60
  const mm = String(minutes).padStart(2, '0')
  const ss = String(seconds).padStart(2, '0')
  return hours > 0 ? `${String(hours).padStart(2, '0')}:${mm}:${ss}` : `${mm}:${ss}`
}

export function formatStreamSummary({ title, url, state, nowMs = Date.now() }) {
  const elapsedMs = state.connectedAtMs != null ? Math.max(0, nowMs - state.connectedAtMs) : 0
  const statusLabel = {
    idle: 'Đã dừng',
    connecting: 'Đang kết nối',
    connected: 'Đã kết nối',
    disconnected: 'Mất kết nối',
  }[state.status] ?? state.status

  const lines = [
    `[${title}]`,
    `- URL: ${url}`,
    `- Trạng thái: ${statusLabel}${state.connectionId ? ` (${state.connectionId})` : ''}`,
    `- Thời gian đã kết nối: ${formatDuration(elapsedMs)}`,
    `- Số tick nhận được: ${state.tickCount}`,
    `- Số lần kết nối lại: ${state.reconnectCount}`,
    `- Khoảng cách 2 tick gần nhất: ${state.lastIntervalSec == null ? '—' : `${state.lastIntervalSec.toFixed(1)}s`}${state.isDelayed ? ' (CẢNH BÁO > 15s)' : ''}`,
    `- Khoảng cách lớn nhất: ${state.maxIntervalSec == null ? '—' : `${state.maxIntervalSec.toFixed(1)}s`}`,
    `- Số tick trễ (>15s): ${state.delayedCount} | Số tick bị hụt (gap): ${state.gapCount}`,
  ]
  if (state.logs.length > 0) {
    const recent = state.logs.slice(0, 5)
      .map((row) => `#${row.seq} (lệch ${row.latencyMs ?? '—'}ms, cách ${row.intervalSec == null ? '—' : `${row.intervalSec.toFixed(1)}s`})`)
      .join(', ')
    lines.push(`- 5 tick gần nhất: ${recent}`)
  }
  return lines.join('\n')
}

export function openLiveStream({
  path = '/api/live/stream',
  baseUrl,
  onOpen,
  onHello,
  onSnapshot,
  onScore,
  onTick,
  onError,
  reconnectDelayMs = 3000,
  EventSourceImpl = globalThis.EventSource,
}) {
  const url = resolveLiveStreamUrl(path, baseUrl)
  if (typeof EventSourceImpl !== 'function') {
    throw new Error('Trình duyệt không hỗ trợ EventSource.')
  }

  let source = null
  let closedByUser = false
  let reconnectTimer = null

  function clearTimer() {
    if (reconnectTimer !== null) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function connect() {
    if (closedByUser) return
    clearTimer()
    source = new EventSourceImpl(url)

    source.onopen = () => {
      if (!closedByUser) onOpen?.()
    }

    source.addEventListener('hello', (event) => {
      if (closedByUser) return
      try {
        onHello?.(parseHelloPayload(event.data))
      } catch (err) {
        onError?.(err)
      }
    })

    source.addEventListener('snapshot', (event) => {
      if (closedByUser) return
      try {
        const list = JSON.parse(event.data)
        if (Array.isArray(list)) {
          onSnapshot?.(list)
        }
      } catch (err) {
        onError?.(err)
      }
    })

    source.addEventListener('score', (event) => {
      if (closedByUser) return
      try {
        const item = JSON.parse(event.data)
        if (isValidLiveMatchState(item)) {
          onScore?.(item)
        }
      } catch (err) {
        onError?.(err)
      }
    })

    source.addEventListener('tick', (event) => {
      if (closedByUser) return
      try {
        onTick?.(parseTickPayload(event.data))
      } catch (err) {
        onError?.(err)
      }
    })

    source.onerror = (event) => {
      if (closedByUser) return
      onError?.(event)
      // If the browser permanently closed the EventSource (readyState === 2), schedule a fresh connection
      if (source && source.readyState === 2 && reconnectDelayMs > 0 && reconnectTimer === null) {
        reconnectTimer = setTimeout(() => {
          reconnectTimer = null
          connect()
        }, reconnectDelayMs)
      }
    }
  }

  connect()

  return {
    url,
    close() {
      closedByUser = true
      clearTimer()
      if (source) {
        source.close()
        source = null
      }
    },
  }
}
