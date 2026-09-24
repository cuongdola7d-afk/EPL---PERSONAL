import { useEffect, useRef, useState } from 'react'
import { fetchMatchDetail } from '../api/matches.js'

const STATS = [
  ['minutes', 'Phút'], ['goals', 'Bàn thắng'], ['assists', 'Kiến tạo'],
  ['yellowCards', 'Thẻ vàng'], ['redCards', 'Thẻ đỏ'], ['rating', 'Điểm đánh giá'],
  ['shotsOn', 'Sút trúng đích'], ['passesKey', 'Chuyền quyết định'],
  ['tackles', 'Tắc bóng'], ['saves', 'Cứu thua'],
]

const POSITIONS = { G: 'Thủ môn', D: 'Hậu vệ', M: 'Tiền vệ', F: 'Tiền đạo' }

function PlayerStats({ player }) {
  return (
    <article className="match-player">
      <div className="match-player-heading">
        <strong>{player.playerName}</strong>
        <span>{POSITIONS[player.position] ?? player.position ?? 'Chưa có dữ liệu'}</span>
      </div>
      <dl className="match-player-stats">
        {STATS.map(([field, label]) => (
          <div key={field}><dt>{label}</dt><dd>{player[field] ?? 'Chưa có dữ liệu'}</dd></div>
        ))}
      </dl>
    </article>
  )
}

function MatchDetail({ matchId, onClose }) {
  const [detail, setDetail] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [reloadCount, setReloadCount] = useState(0)
  const headingRef = useRef(null)

  useEffect(() => {
    headingRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    headingRef.current?.focus({ preventScroll: true })
  }, [matchId])

  useEffect(() => {
    const controller = new AbortController()
    setStatus('loading')
    setError('')
    fetchMatchDetail(matchId, controller.signal)
      .then((value) => { setDetail(value); setStatus('success') })
      .catch((requestError) => {
        if (controller.signal.aborted) return
        setError(requestError instanceof Error ? requestError.message : 'Không thể tải chi tiết trận đấu.')
        setStatus('error')
      })
    return () => controller.abort()
  }, [matchId, reloadCount])

  const match = detail?.match
  const noStats = status === 'success' &&
    detail.homePlayers.length === 0 && detail.awayPlayers.length === 0

  return (
    <section className="match-detail" aria-labelledby="match-detail-heading" aria-busy={status === 'loading'}>
      <div className="match-detail-head">
        <div>
          <p className="section-kicker">CHI TIẾT TRẬN ĐẤU</p>
          <h3 id="match-detail-heading" tabIndex="-1" ref={headingRef}>
            {match ? `${match.homeClub} – ${match.awayClub}` : `Trận #${matchId}`}
          </h3>
          {match && <p>Vòng {match.matchweek} · {match.date} · {match.status} ·
            {' '}{match.homeGoals === null || match.awayGoals === null
              ? 'Chưa có tỉ số' : `${match.homeGoals} : ${match.awayGoals}`}</p>}
        </div>
        <button className="match-detail-close" type="button" onClick={onClose}>Đóng chi tiết</button>
      </div>
      {status === 'loading' && <p role="status">Đang tải chi tiết trận đấu...</p>}
      {status === 'error' && <div role="alert"><p>{error}</p><button type="button" onClick={() => setReloadCount((count) => count + 1)}>Thử lại</button></div>}
      {noStats && <p className="match-detail-empty">Trận này chưa có thống kê cầu thủ được lưu.</p>}
      {status === 'success' && !noStats && (
        <div className="match-detail-teams">
          {[
            [match.homeClub, detail.homePlayers],
            [match.awayClub, detail.awayPlayers],
          ].map(([club, players]) => (
            <div className="match-detail-team" key={club}>
              <h4>{club} <span>({players.length} cầu thủ)</span></h4>
              {players.length === 0
                ? <p>Đội này chưa có thống kê cầu thủ được lưu.</p>
                : players.map((player) => <PlayerStats key={player.playerId} player={player} />)}
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

export default MatchDetail
