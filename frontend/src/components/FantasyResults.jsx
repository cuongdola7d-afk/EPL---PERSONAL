import { useEffect, useState } from 'react'
import { fetchFantasyReadiness, publishFantasyResults } from '../api/fantasyResults.js'
import { formatPoints } from '../fantasy/results.js'
import { formatDeadline } from '../fantasy/gameweek.js'
import './FantasyResults.css'

function AdminResults({ gameweek, onPublished }) {
  const [ready, setReady] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reason, setReason] = useState('')
  useEffect(() => { setReady(null); setError(''); setNotice(''); setReason('') }, [gameweek])
  async function check() {
    setBusy(true); setError('')
    try { setReady(await fetchFantasyReadiness(gameweek)) }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }
  async function publish() {
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await publishFantasyResults(gameweek, { expectedVersion: ready.currentVersion, reason: reason.trim() }, ready.currentVersion > 0)
      setNotice((result.unchanged ? 'Dữ liệu không đổi' : 'Đã công bố') + ' · phiên bản ' + result.version)
      setReady(null); onPublished()
      window.dispatchEvent(new Event('prismaxi-results-published'))
    } catch (failure) { setError(failure.message); if (failure.readiness) setReady(failure.readiness) }
    finally { setBusy(false) }
  }
  return <section className="fantasy-card fantasy-admin-results" aria-label="Quản trị kết quả" aria-busy={busy}>
    <h2>Quản trị kết quả · GW{gameweek}</h2>
    <button type="button" className="fantasy-clear" disabled={busy} onClick={check}>{busy ? 'Đang xử lý…' : 'Kiểm tra readiness'}</button>
    {error && <p role="alert">{error}</p>}{notice && <p role="status">{notice}</p>}
    {ready && <>
      <p>{ready.ready ? 'Đủ điều kiện công bố' : 'Còn dữ liệu đang chờ'} · {ready.fixtures} trận · {ready.participants} đội đã chốt · phiên bản hiện tại {ready.currentVersion}</p>
      {ready.history?.length > 0 && <details><summary>Lịch sử công bố</summary><ul>
        {ready.history.map(item => <li key={item.version}>Phiên bản {item.version} · {formatDeadline(item.publishedAt)} · Giờ Việt Nam
          {' · '}{item.action === 'RECALCULATE' ? 'Tái tính' : 'Công bố'} · {item.reason} · Quản trị viên #{item.publishedBy}</li>)}
      </ul></details>}
      {ready.issues.length > 0 && <ul>{ready.issues.map((issue, i) => <li key={i}>
        {issue.fixtureId ? 'Trận #' + issue.fixtureId + ' · ' : ''}{issue.playerId ? 'Cầu thủ #' + issue.playerId + ' · ' : ''}{issue.message}</li>)}</ul>}
      <label>Lý do công bố/tái tính<input maxLength={500} value={reason} disabled={busy} onChange={e => setReason(e.target.value)} /></label>
      <button type="button" className="fantasy-primary" disabled={busy || !ready.ready || reason.trim().length < 3} onClick={publish}>
        {ready.currentVersion > 0 ? 'Tái tính và công bố phiên bản mới' : 'Công bố kết quả'}</button>
    </>}
  </section>
}

export default function FantasyResults({ account, gameweek, resultState }) {
  const { data, loading, error, refresh } = resultState
  return <>
    <section className="fantasy-card fantasy-results" aria-label="Kết quả đội của bạn" aria-busy={loading}>
      <div className="fantasy-result-head"><h2>Kết quả · GW{gameweek}</h2>
        <button type="button" className="fantasy-clear" disabled={loading} onClick={refresh}>Cập nhật kết quả</button></div>
      {loading && <p role="status">Đang tải kết quả…</p>}
      {error && <p role="alert">{error} <button type="button" onClick={refresh}>Thử lại</button></p>}
      {data?.status === 'NOT_PARTICIPATING' && <p>Bạn chưa lưu đội hình tham gia GW này.</p>}
      {data?.status === 'AWAITING_RESULTS' && <p role="status">Đang chờ kết quả. Điểm sẽ hiển thị sau khi quản trị viên công bố.</p>}
      {data?.status === 'PUBLISHED' && <p className="fantasy-result-total" role="status">Tổng điểm: <strong>{formatPoints(data.result.totalPoints)}</strong></p>}
    </section>
    {account.role === 'ADMIN' && <AdminResults key={gameweek} gameweek={gameweek} onPublished={refresh} />}
  </>
}
