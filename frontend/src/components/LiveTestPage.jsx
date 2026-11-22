import { useEffect, useRef, useState } from 'react'
import {
  applyHelloEvent,
  applyStreamDisconnect,
  applyStreamOpen,
  applyStreamStart,
  applyStreamStop,
  applyTickEvent,
  createStreamState,
  formatDuration,
  formatStreamSummary,
  openLiveStream,
  resolveLiveStreamUrl,
} from '../api/liveStream.js'
import './LiveTestPage.css'

const DEFAULT_RENDER_ORIGIN = 'https://epl-personal.onrender.com'

const STATUS_LABEL = {
  idle: 'Đã dừng',
  connecting: 'Đang kết nối...',
  connected: 'Đã kết nối',
  disconnected: 'Mất kết nối (đang thử lại)',
}

const timeOnly = (isoString) => {
  if (!isoString) return '—'
  const d = new Date(isoString)
  if (Number.isNaN(d.getTime())) return isoString
  return d.toISOString().slice(11, 23) + 'Z'
}

function StreamPanel({ title, subtitle, path, baseUrl, allowOriginSwitch = false, onOriginChange, state, onStateChange }) {
  const clientRef = useRef(null)
  const [nowMs, setNowMs] = useState(() => Date.now())
  const [copied, setCopied] = useState(false)

  const resolvedUrl = (() => {
    try {
      return resolveLiveStreamUrl(path, baseUrl)
    } catch {
      return `${baseUrl}${path}`
    }
  })()

  useEffect(() => {
    if (state.status !== 'connected' && state.status !== 'connecting' && state.status !== 'disconnected') {
      return undefined
    }
    const timer = setInterval(() => setNowMs(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [state.status])

  useEffect(() => {
    return () => {
      if (clientRef.current) {
        clientRef.current.close()
        clientRef.current = null
      }
    }
  }, [])

  function handleStart() {
    if (clientRef.current) {
      clientRef.current.close()
      clientRef.current = null
    }
    setNowMs(Date.now())
    onStateChange(() => applyStreamStart(createStreamState()))
    clientRef.current = openLiveStream({
      path,
      baseUrl,
      onOpen: () => {
        setNowMs(Date.now())
        onStateChange((prev) => applyStreamOpen(prev, Date.now()))
      },
      onHello: (hello) => {
        setNowMs(Date.now())
        onStateChange((prev) => applyHelloEvent(prev, hello, Date.now()))
      },
      onTick: (tick) => {
        const received = Date.now()
        setNowMs(received)
        onStateChange((prev) => applyTickEvent(prev, tick, received))
      },
      onError: () => {
        setNowMs(Date.now())
        onStateChange((prev) => applyStreamDisconnect(prev, 'Mất kết nối hoặc kênh thử chưa bật (404/CORS). Đang tự kết nối lại...'))
      },
    })
  }

  function handleStop() {
    if (clientRef.current) {
      clientRef.current.close()
      clientRef.current = null
    }
    onStateChange((prev) => applyStreamStop(prev))
  }

  async function handleCopy() {
    const summary = formatStreamSummary({ title, url: resolvedUrl, state, nowMs: Date.now() })
    try {
      await navigator.clipboard.writeText(summary)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      // Fallback
    }
  }

  const isRunning = state.status !== 'idle'
  const elapsedMs = state.connectedAtMs != null ? Math.max(0, nowMs - state.connectedAtMs) : 0

  return (
    <article className="lt-panel">
      <header className="lt-panel-head">
        <div>
          <div className="lt-title-row">
            <h2>{title}</h2>
            <span className={`lt-status lt-status-${state.status}`}>
              <i className="lt-dot" aria-hidden="true" />
              {STATUS_LABEL[state.status] ?? state.status}
            </span>
          </div>
          <p className="lt-sub">{subtitle}</p>
          <code className="lt-url">{resolvedUrl}</code>
          {allowOriginSwitch && (
            <div className="lt-origin-presets">
              <span>Đích:</span>
              <button
                type="button"
                className={baseUrl === DEFAULT_RENDER_ORIGIN ? 'active' : ''}
                disabled={isRunning}
                onClick={() => onOriginChange(DEFAULT_RENDER_ORIGIN)}
              >
                Render Prod
              </button>
              <button
                type="button"
                className={baseUrl === 'http://localhost:8080' ? 'active' : ''}
                disabled={isRunning}
                onClick={() => onOriginChange('http://localhost:8080')}
              >
                Local 8080 (CORS)
              </button>
            </div>
          )}
        </div>

        <div className="lt-actions">
          {!isRunning ? (
            <button type="button" className="lt-btn lt-btn-primary" onClick={handleStart}>
              Bắt đầu
            </button>
          ) : (
            <button type="button" className="lt-btn lt-btn-stop" onClick={handleStop}>
              Dừng
            </button>
          )}
          <button type="button" className="lt-btn lt-btn-ghost" onClick={handleCopy}>
            {copied ? 'Đã chép!' : 'Sao chép kết quả'}
          </button>
        </div>
      </header>

      {state.error && (
        <div className="lt-alert" role="status">
          {state.error}
        </div>
      )}

      <div className="lt-metrics">
        <div className="lt-metric">
          <span>Thời gian đã kết nối</span>
          <strong>{formatDuration(elapsedMs)}</strong>
          <small>{state.connectionId ? `ID: ${state.connectionId}` : 'Chưa có ID'}</small>
        </div>
        <div className="lt-metric">
          <span>Số tick nhận được</span>
          <strong>{state.tickCount}</strong>
          <small>Mỗi 10 giây / tick</small>
        </div>
        <div className="lt-metric">
          <span>Số lần kết nối lại</span>
          <strong>{state.reconnectCount}</strong>
          <small>Hụt seq: {state.gapCount}</small>
        </div>
        <div className={`lt-metric${state.isDelayed ? ' lt-metric-delayed' : ''}`}>
          <span>Khoảng cách 2 tick gần nhất</span>
          <strong>{state.lastIntervalSec == null ? '—' : `${state.lastIntervalSec.toFixed(1)}s`}</strong>
          <small>
            {state.isDelayed
              ? 'CẢNH BÁO: > 15s (bị dồn/trễ)'
              : `Lớn nhất: ${state.maxIntervalSec == null ? '—' : `${state.maxIntervalSec.toFixed(1)}s`} · Trễ: ${state.delayedCount}`}
          </small>
        </div>
      </div>

      <div className="lt-table-wrap">
        <table className="lt-table">
          <thead>
            <tr>
              <th>Seq</th>
              <th>Giờ server (UTC)</th>
              <th>Giờ nhận (UTC)</th>
              <th>Chênh lệch</th>
              <th>Cách tick trước</th>
            </tr>
          </thead>
          <tbody>
            {state.logs.length === 0 ? (
              <tr>
                <td colSpan={5} className="lt-empty">
                  Chưa có dữ liệu tick. Bấm &ldquo;Bắt đầu&rdquo; để mở luồng SSE (tick đầu tiên sau 10 giây).
                </td>
              </tr>
            ) : (
              state.logs.map((row, idx) => (
                <tr key={`${row.connectionId}-${row.seq}-${idx}`} className={row.isDelayed || row.seqGap > 0 ? 'lt-row-delayed' : ''}>
                  <td>
                    <b>#{row.seq}</b>
                    {row.seqGap > 0 && <span className="lt-gap-badge">+{row.seqGap} hụt</span>}
                  </td>
                  <td>{timeOnly(row.serverTime)}</td>
                  <td>{timeOnly(row.receivedTimeIso)}</td>
                  <td>{row.latencyMs == null ? '—' : `${row.latencyMs >= 0 ? '+' : ''}${row.latencyMs} ms`}</td>
                  <td className={row.isDelayed ? 'lt-cell-delayed' : ''}>
                    {row.intervalSec == null ? '—' : `${row.intervalSec.toFixed(1)}s`}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </article>
  )
}

export default function LiveTestPage({ theme = 'dark' }) {
  const [proxyState, setProxyState] = useState(createStreamState)
  const [directState, setDirectState] = useState(createStreamState)
  const [directOrigin, setDirectOrigin] = useState(DEFAULT_RENDER_ORIGIN)
  const [copiedAll, setCopiedAll] = useState(false)

  async function handleCopyBoth() {
    const now = Date.now()
    const text = [
      `=== BÁO CÁO THỬ ĐƯỜNG TRUYỀN SSE (${new Date(now).toISOString()}) ===`,
      '',
      formatStreamSummary({
        title: 'Kênh 1: Qua Vercel / Proxy',
        url: resolveLiveStreamUrl('/api/live/test-stream', ''),
        state: proxyState,
        nowMs: now,
      }),
      '',
      formatStreamSummary({
        title: 'Kênh 2: Đi thẳng Render',
        url: resolveLiveStreamUrl('/api/live/test-stream', directOrigin),
        state: directState,
        nowMs: now,
      }),
    ].join('\n')
    try {
      await navigator.clipboard.writeText(text)
      setCopiedAll(true)
      setTimeout(() => setCopiedAll(false), 2000)
    } catch {
      // Fallback
    }
  }

  return (
    <section className="live-test-page" data-theme={theme}>
      <div className="lt-wrap">
        <header className="lt-header">
          <div>
            <p className="lt-eyebrow">PRISMAXI · HẠ TẦNG TỈ SỐ TRỰC TIẾP (BƯỚC 1)</p>
            <h1>Kiểm tra đường truyền Server-Sent Events (SSE)</h1>
            <p className="lt-desc">
              So sánh độ ổn định của kết nối SSE qua proxy (Vercel / Vite) và đi thẳng Render. Mỗi 10 giây server gửi 1 sự kiện <code>tick</code>, mỗi 25 giây gửi heartbeat <code>: ping</code>. Khoảng cách giữa 2 tick vượt quá 15 giây sẽ được tô đỏ.
            </p>
          </div>
          <button type="button" className="lt-btn lt-btn-primary" onClick={handleCopyBoth}>
            {copiedAll ? 'Đã sao chép cả 2 kênh!' : 'Sao chép kết quả cả 2 kênh'}
          </button>
        </header>

        <div className="lt-grid">
          <StreamPanel
            title="Qua Vercel"
            subtitle="Đường tương đối qua proxy hiện tại (/api/live/test-stream)"
            path="/api/live/test-stream"
            baseUrl=""
            state={proxyState}
            onStateChange={setProxyState}
          />
          <StreamPanel
            title="Đi thẳng Render"
            subtitle="Kết nối trực tiếp tới backend qua CORS"
            path="/api/live/test-stream"
            baseUrl={directOrigin}
            allowOriginSwitch
            onOriginChange={setDirectOrigin}
            state={directState}
            onStateChange={setDirectState}
          />
        </div>
      </div>
    </section>
  )
}
