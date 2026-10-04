import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchGameweeks } from '../api/gameweeks.js'
import { GAMEWEEK_LABELS, countdownLabel, formatDeadline, remainingMilliseconds, shouldRefreshDeadline } from '../fantasy/gameweek.js'
import './FantasyGameweek.css'

export default function FantasyGameweek({ onSelectionChange }) {
  const [data, setData] = useState(null)
  const [selected, setSelected] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tick, setTick] = useState(() => performance.now())
  const [reload, setReload] = useState(0)
  const refreshed = useRef(null)
  const retry = useCallback(() => { refreshed.current = null; setReload(n => n + 1) }, [])

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true); setError('')
    fetchGameweeks(controller.signal).then(result => {
      if (controller.signal.aborted) return
      const receivedAt = performance.now()
      setData({ ...result, receivedAt }); setTick(receivedAt)
      setSelected(current => current ?? result.recommendedGameweek ?? 6)
    }).catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [reload])

  useEffect(() => {
    const timer = setInterval(() => setTick(performance.now()), 1000)
    const refreshOnReturn = () => { if (document.visibilityState === 'visible') retry() }
    document.addEventListener('visibilitychange', refreshOnReturn)
    return () => { clearInterval(timer); document.removeEventListener('visibilitychange', refreshOnReturn) }
  }, [retry])

  const view = data?.gameweeks.find(gw => gw.gameweek === selected)
  const remaining = view?.deadlineUtc ? remainingMilliseconds(view.deadlineUtc, data.serverTimeUtc, data.receivedAt, tick) : 0
  const expired = shouldRefreshDeadline(view, remaining)
  useEffect(() => {
    onSelectionChange?.(selected, Boolean(view?.canEdit && !expired && !loading && !error), view?.rosterAsOf ?? null)
  }, [selected, view?.canEdit, view?.rosterAsOf, expired, loading, error, onSelectionChange])
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
        {Array.from({ length: 38 }, (_, i) => <option value={i + 1} key={i + 1}>GW{i + 1}{i < 5 ? ' · Replay' : ''}</option>)}
      </select></label>
    </div>
    {error && <p role="alert">Không tải được trạng thái: {error} <button type="button" onClick={retry}>Thử lại</button></p>}
    {loading && <p role="status">Đang cập nhật trạng thái từ server…</p>}
    {view && <>
      <div className="fantasy-gameweek-details">
        <div><span>Hạn chỉnh/chốt đội · Giờ Việt Nam</span><strong>{formatDeadline(view.deadlineUtc)}</strong></div>
        <div><span>Trạng thái GW{view.gameweek}</span><strong>{expired ? 'Đã đến hạn · đang xác nhận trạng thái' : GAMEWEEK_LABELS[view.status] ?? (view.mode === 'REPLAY' ? 'Replay · chưa mở' : 'Chưa công bố deadline')}</strong></div>
        {view.status === 'OPEN' && !expired && <div><span>Còn lại theo giờ server</span><strong className="fantasy-gameweek-countdown" role="timer">{countdownLabel(remaining)}</strong></div>}
      </div>
      {!view.configured && view.candidateDeadlineUtc && <p>Deadline dự kiến theo lịch: {formatDeadline(view.candidateDeadlineUtc)} · Giờ Việt Nam. Chưa mở cuộc thi.</p>}
      {view.rosterAsOf && <p>Mốc danh sách cầu thủ: {view.rosterAsOf.split('-').reverse().join('/')}</p>}
      {!view.scheduleComplete && <p>Lịch chưa đủ thời điểm UTC để xác định trận đầu và deadline.</p>}
      {view.deadlineChanges.filter(change => change.revision > 1).map(change => <p key={change.revision}>
        Điều chỉnh hạn: {formatDeadline(change.oldDeadlineUtc)} → {formatDeadline(change.newDeadlineUtc)} · {change.reason}</p>)}
    </>}
    <p className="fantasy-gameweek-note">Đội dự thi chỉ có hiệu lực sau khi bạn chủ động chốt thành công trước hạn.</p>
  </section>
}
