import { useCallback, useEffect, useRef, useState } from 'react'
import { adjustGameweekDeadline, fetchGameweeks, publishGameweek } from '../api/gameweeks.js'
import { GAMEWEEK_LABELS, countdownLabel, displayedGameweek, formatDeadline, formatVietnamDateTimeInput,
  latestPublishedGameweek, nextGameweekToOpen, parseVietnamDateTimeToUtc, remainingMilliseconds,
  shouldRefreshDeadline } from '../fantasy/gameweek.js'
import './FantasyGameweek.css'

function AdminAdjustDeadline({ view, onAdjusted }) {
  const [open, setOpen] = useState(false)
  const [vnInput, setVnInput] = useState(() => formatVietnamDateTimeInput(view.candidateDeadlineUtc ?? view.deadlineUtc))
  const [reason, setReason] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const request = useRef(null)
  useEffect(() => () => request.current?.abort(), [])

  const utcPreview = parseVietnamDateTimeToUtc(vnInput)
  const canSubmit = Boolean(!busy && utcPreview && reason.trim().length >= 3)

  async function submit(event) {
    event.preventDefault()
    if (!canSubmit) return
    const controller = new AbortController()
    request.current = controller
    setBusy(true); setError('')
    try {
      await adjustGameweekDeadline(view.gameweek, {
        deadlineUtc: utcPreview,
        expectedRevision: view.revision,
        reason: reason.trim(),
      }, controller.signal)
      if (!controller.signal.aborted) {
        setOpen(false)
        setReason('')
        onAdjusted()
      }
    } catch (failure) {
      if (!controller.signal.aborted) setError(failure.message)
    } finally {
      if (!controller.signal.aborted) setBusy(false)
    }
  }

  return <div className="fantasy-gameweek-admin" aria-label="Điều chỉnh deadline" aria-busy={busy}>
    <button type="button" onClick={() => { setOpen(v => !v); setError('') }}>
      {open ? 'Đóng điều chỉnh deadline' : 'Điều chỉnh deadline'}
    </button>
    {open && <form className="fantasy-gameweek-adjust-form" onSubmit={submit}>
      <label>
        Deadline mới (giờ Việt Nam, UTC+7)
        <input type="datetime-local" value={vnInput} disabled={busy}
          onChange={e => setVnInput(e.target.value)} required />
      </label>
      <p className="fantasy-gameweek-adjust-preview">
        {utcPreview
          ? <>Giờ Việt Nam: <strong>{formatDeadline(utcPreview)}</strong> · UTC gửi server: <code>{utcPreview}</code></>
          : 'Nhập đúng định dạng ngày giờ hợp lệ.'}
      </p>
      <label>
        Lý do công khai (3–500 ký tự)
        <input type="text" maxLength={500} value={reason} disabled={busy}
          placeholder={`Ví dụ: Dời hạn GW${view.gameweek} sang 30 phút trước trận đầu theo luật mới`}
          onChange={e => setReason(e.target.value)} required />
      </label>
      <div className="fantasy-gameweek-adjust-actions">
        <button type="submit" disabled={!canSubmit}>{busy ? 'Đang lưu…' : 'Lưu deadline mới'}</button>
        <button type="button" className="fantasy-gameweek-adjust-cancel" disabled={busy} onClick={() => setOpen(false)}>Hủy</button>
      </div>
      {error && <p role="alert">{error}</p>}
    </form>}
  </div>
}

function AdminGameweek({ next, canStart, onOpened }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const request = useRef(null)
  useEffect(() => () => request.current?.abort(), [])

  async function start() {
    if (busy || !canStart) return
    const controller = new AbortController()
    request.current = controller
    setBusy(true); setError('')
    try {
      await publishGameweek(next.gameweek, controller.signal)
      if (!controller.signal.aborted) onOpened()
    } catch (failure) {
      if (!controller.signal.aborted) setError(failure.message)
    } finally {
      if (!controller.signal.aborted) setBusy(false)
    }
  }

  return <div className="fantasy-gameweek-admin" aria-label="Mở vòng thi" aria-busy={busy}>
    <p>GW{next.gameweek} · Deadline dự kiến: {formatDeadline(next.candidateDeadlineUtc)} · Giờ Việt Nam.</p>
    <button type="button" disabled={!canStart || busy} onClick={start}>
      {busy ? 'Đang mở vòng…' : `Bắt đầu GW${next.gameweek}`}</button>
    {!canStart && <p>Chỉ mở được khi lịch đủ 10 trận và deadline dự kiến còn ở tương lai.</p>}
    {error && <p role="alert">{error}</p>}
  </div>
}

export default function FantasyGameweek({
  account = null,
  onSelectionChange,
  requestedGameweek = null,
  onOpenGameweekChange,
}) {
  const [data, setData] = useState(null)
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tick, setTick] = useState(() => performance.now())
  const [reload, setReload] = useState(0)
  const refreshed = useRef(null)
  const previousLatest = useRef(null)
  const retry = useCallback(() => { refreshed.current = null; setReload(n => n + 1) }, [])

  useEffect(() => {
    const controller = new AbortController()
    setError('')
    fetchGameweeks(controller.signal).then(result => {
      if (controller.signal.aborted) return
      const receivedAt = performance.now()
      setData({ ...result, receivedAt }); setTick(receivedAt)
      const previous = previousLatest.current
      previousLatest.current = latestPublishedGameweek(result.gameweeks)
      setSelected(current => displayedGameweek(result.gameweeks, current, previous))
    }).catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [reload])

  useEffect(() => {
    const timer = setInterval(() => setTick(performance.now()), 1000)
    const refreshOnReturn = () => { if (document.visibilityState === 'visible') retry() }
    document.addEventListener('visibilitychange', refreshOnReturn)
    window.addEventListener('prismaxi-results-published', retry)
    // Public status only: pick up a round opened by an admin in another session.
    const refreshTimer = setInterval(() => { if (document.visibilityState === 'visible') retry() }, 30000)
    return () => { clearInterval(timer); clearInterval(refreshTimer); document.removeEventListener('visibilitychange', refreshOnReturn); window.removeEventListener('prismaxi-results-published', retry) }
  }, [retry])

  useEffect(() => {
    if (requestedGameweek?.gameweek) {
      setSelected(requestedGameweek.gameweek)
    }
  }, [requestedGameweek?.nonce, requestedGameweek?.gameweek])

  const view = data?.gameweeks.find(gw => gw.gameweek === selected)
  const remaining = view?.deadlineUtc ? remainingMilliseconds(view.deadlineUtc, data.serverTimeUtc, data.receivedAt, tick) : 0
  const expired = shouldRefreshDeadline(view, remaining)
  const next = data ? nextGameweekToOpen(data.gameweeks) : null
  const openView = data?.gameweeks.filter(gw => gw.gameweek >= 6 && gw.configured && gw.status === 'OPEN')
    .reduce((latest, gw) => (!latest || gw.gameweek > latest.gameweek ? gw : latest), null) ?? null
  const openExpired = openView?.deadlineUtc
    ? shouldRefreshDeadline(openView, remainingMilliseconds(openView.deadlineUtc, data.serverTimeUtc, data.receivedAt, tick))
    : false
  const canStart = Boolean(next?.scheduleComplete && next.candidateDeadlineUtc && !loading && !error &&
    remainingMilliseconds(next.candidateDeadlineUtc, data.serverTimeUtc, data.receivedAt, tick) > 0)
  useEffect(() => {
    onSelectionChange?.(selected ?? 6, Boolean(view?.canEdit && !expired && !loading && !error), view?.rosterAsOf ?? null, view?.status ?? null, view?.deadlineUtc ?? null, !loading)
  }, [selected, view?.canEdit, view?.rosterAsOf, view?.status, view?.deadlineUtc, expired, loading, error, onSelectionChange])
  useEffect(() => {
    if (!onOpenGameweekChange) return
    onOpenGameweekChange(openView && !openExpired && !loading && !error
      ? { gameweek: openView.gameweek, deadlineUtc: openView.deadlineUtc }
      : null)
  }, [openView?.gameweek, openView?.deadlineUtc, openExpired, loading, error, onOpenGameweekChange])
  useEffect(() => {
    const key = `${selected}:${view?.revision}:${data?.serverTimeUtc}`
    if (!expired || loading || error || refreshed.current === key) return
    refreshed.current = key
    setReload(n => n + 1)
  }, [expired, loading, error, selected, view?.revision, data?.serverTimeUtc])

  return <section className="fantasy-card fantasy-gameweek" aria-labelledby="fantasy-gameweek-heading" aria-busy={loading}>
    <div className="fantasy-gameweek-head">
      <h2 id="fantasy-gameweek-heading">Vòng thi · 2026/27</h2>
      <label>Gameweek <select value={selected ?? 6} disabled={!data} onChange={event => setSelected(Number(event.target.value))}>
        {Array.from({ length: 38 }, (_, i) => <option value={i + 1} key={i + 1}
          disabled={i >= 5 && !data?.gameweeks[i].configured}>GW{i + 1}{i < 5 ? ' · Replay' : !data?.gameweeks[i].configured ? ' · Chưa mở' : ''}</option>)}
      </select></label>
    </div>
    {error && <p role="alert">Không tải được trạng thái: {error} <button type="button" onClick={retry}>Thử lại</button></p>}
    {loading && <p role="status">Đang cập nhật trạng thái từ server…</p>}
    {view && <>
      <div className="fantasy-gameweek-details">
        <div><span>Hạn lưu đội hình · Giờ Việt Nam</span><strong>{formatDeadline(view.deadlineUtc)}</strong></div>
        <div><span>Trạng thái GW{view.gameweek}</span><strong>{expired ? 'Đã đến hạn · đang xác nhận trạng thái' : GAMEWEEK_LABELS[view.status] ?? (view.mode === 'REPLAY' ? 'Replay · chưa mở' : 'Chưa công bố deadline')}</strong></div>
        {view.status === 'OPEN' && !expired && <div><span>Còn lại theo giờ server</span><strong className="fantasy-gameweek-countdown" role="timer">{countdownLabel(remaining)}</strong></div>}
      </div>
      {!view.configured && view.candidateDeadlineUtc && <p>Deadline dự kiến theo lịch: {formatDeadline(view.candidateDeadlineUtc)} · Giờ Việt Nam. Chưa mở cuộc thi.</p>}
      {view.rosterAsOf && <p>Mốc danh sách cầu thủ: {view.rosterAsOf.split('-').reverse().join('/')}</p>}
      {!view.scheduleComplete && <p>Lịch chưa đủ thời điểm UTC để xác định trận đầu và deadline.</p>}
      {view.deadlineChanges.filter(change => change.revision > 1).map(change => <p key={change.revision}>
        Điều chỉnh hạn: {formatDeadline(change.oldDeadlineUtc)} → {formatDeadline(change.newDeadlineUtc)} · {change.reason}</p>)}
    </>}
    {account?.role === 'ADMIN' && view?.configured && view.status === 'OPEN' && !expired &&
      <AdminAdjustDeadline key={`${account.id}:${view.gameweek}:${view.revision}`} view={view} onAdjusted={retry} />}
    {account?.role === 'ADMIN' && next && <AdminGameweek key={`${account.id}:${next.gameweek}`}
      next={next} canStart={canStart} onOpened={retry} />}
    <p className="fantasy-gameweek-note">Hạn chốt là 30 phút trước trận đầu tiên (giờ Việt Nam). Lưu đội hình trước deadline; đội được lưu gần nhất sẽ tự khóa khi hết hạn.</p>
  </section>
}
