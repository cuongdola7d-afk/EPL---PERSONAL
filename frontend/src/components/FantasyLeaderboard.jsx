import { useEffect, useState } from 'react'
import { fetchFantasyLeaderboard } from '../api/fantasyLeaderboard.js'
import { formatPoints } from '../fantasy/results.js'
import './FantasyLeaderboard.css'

export default function FantasyLeaderboard({ gameweek, account }) {
  const [scope, setScope] = useState('gameweek')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setData(null); setError(''); setLoading(true)
    if (scope === 'gameweek' && gameweek < 6) { setLoading(false); return () => controller.abort() }
    fetchFantasyLeaderboard(scope === 'season' ? null : gameweek, controller.signal)
      .then(value => { if (!controller.signal.aborted) setData(value) })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [scope, gameweek, revision])
  useEffect(() => {
    const refresh = () => { if (document.visibilityState === 'visible') setRevision(n => n + 1) }
    const timer = setInterval(refresh, 60000)
    document.addEventListener('visibilitychange', refresh)
    return () => { clearInterval(timer); document.removeEventListener('visibilitychange', refresh) }
  }, [])
  return <section className="fantasy-card fantasy-leaderboard" aria-label="BXH người chơi" aria-busy={loading}>
    <div className="fantasy-leaderboard-head"><h2>BXH người chơi · {scope === 'season' ? '2026/27' : 'GW' + gameweek}</h2>
      <div role="group" aria-label="Phạm vi bảng xếp hạng">
        <button type="button" aria-pressed={scope === 'gameweek'} onClick={() => setScope('gameweek')}>Gameweek</button>
        <button type="button" aria-pressed={scope === 'season'} onClick={() => setScope('season')}>Cả mùa</button>
      </div>
    </div>
    <p>Chỉ tính điểm đã công bố. Bằng điểm thì đồng hạng.</p>
    {scope === 'gameweek' && gameweek < 6 && <p>GW1–GW5 là Replay, không có BXH cuộc thi chính thức.</p>}
    {loading && <p role="status">Đang tải bảng xếp hạng…</p>}
    {error && <p role="alert">{error} <button type="button" onClick={() => setRevision(n => n + 1)}>Thử lại</button></p>}
    {data?.status === 'AWAITING_RESULTS' && <p role="status">Đang chờ công bố kết quả. BXH sẽ hiển thị sau khi có điểm.</p>}
    {data?.status === 'PUBLISHED' && <>
      <p>{data.publishedGameweeks} GW đã công bố{data.version ? ' · phiên bản ' + data.version : ''}</p>
      {data.players.length === 0 ? <p>Chưa có người chơi tham gia.</p> : <table>
        <thead><tr><th scope="col">Hạng</th><th scope="col">Người chơi</th><th scope="col">Điểm</th>{scope === 'season' && <th scope="col">GW</th>}</tr></thead>
        <tbody>{data.players.map(player => <tr key={player.accountId} className={player.accountId === account?.id ? 'is-me' : ''}>
          <td>{player.rank}</td><th scope="row">{player.displayName}{player.accountId === account?.id && <small> · Bạn</small>}</th>
          <td>{formatPoints(player.totalPoints)}</td>{scope === 'season' && <td>{player.gameweeksPlayed}</td>}
        </tr>)}</tbody>
      </table>}
    </>}
  </section>
}
