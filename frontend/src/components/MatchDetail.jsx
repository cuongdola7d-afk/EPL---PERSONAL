import { useEffect, useRef, useState } from 'react'
import { fetchMatchDetail } from '../api/matches.js'

const STATS = [
  ['minutes', 'Phút'], ['goals', 'Bàn thắng'], ['assists', 'Kiến tạo'],
  ['yellowCards', 'Thẻ vàng'], ['redCards', 'Thẻ đỏ'], ['rating', 'Đánh giá trận (API)'],
  ['shotsOn', 'Sút trúng đích'], ['passesKey', 'Chuyền quyết định'],
  ['tackles', 'Tắc bóng'], ['saves', 'Cứu thua'],
]

const POSITIONS = { G: 'Thủ môn', D: 'Hậu vệ', M: 'Tiền vệ', F: 'Tiền đạo' }

function signed(points) {
  return points >= 0 ? `+${points}` : String(points)
}

function PlayerScore({ score, inferred }) {
  const complete = score.status === 'COMPLETE'
  const hasKnownPart = score.parts.some((part) => part.points !== null)
  const hasInference = inferred && Object.values(inferred).some((value) => value !== null)
  return (
    <div className={`match-player-score${complete ? ' complete' : ' provisional'}`}>
      <p className="match-score-total">
        {complete ? `Điểm v1: ${score.confirmedPoints}`
          : hasKnownPart ? `Tạm tính: ${score.confirmedPoints} điểm đã xác định`
            : 'Chưa đủ dữ liệu để tính điểm'}
      </p>
      {!complete && <p className="match-score-note">Còn chỉ số thiếu; đây chưa phải điểm cuối cùng.</p>}
      {hasInference && <p className="match-score-note">Một số chỉ số được suy luận từ sự kiện và đội hình đã đối chiếu.</p>}
      <ul className="match-score-parts">
        {score.parts.map((part) => (
          <li key={part.code}>
            <span><strong>{part.label}</strong><small>{part.detail}</small></span>
            <strong>{part.points === null ? 'Chưa có dữ liệu' : signed(part.points)}</strong>
          </li>
        ))}
      </ul>
    </div>
  )
}

function PlayerStats({ player }) {
  return (
    <article className="match-player">
      <div className="match-player-heading">
        <strong>{player.playerName}</strong>
        <span>{POSITIONS[player.position] ?? player.position ?? 'Chưa có dữ liệu'}</span>
      </div>
      <PlayerScore score={player.score} inferred={player.inferred} />
      <dl className="match-player-stats">
        {STATS.map(([field, label]) => {
          const inferredValue = player.inferred?.[field]
          const isInferred = player[field] === null && inferredValue !== null && inferredValue !== undefined
          return (
            <div key={field}><dt>{label}</dt><dd>
              {player[field] ?? inferredValue ?? 'Chưa có dữ liệu'}
              {isInferred && <small className="inferred-label"> suy luận</small>}
            </dd></div>
          )
        })}
      </dl>
    </article>
  )
}

function MatchDetail({ matchId, onClose, season = 2024 }) {
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
    fetchMatchDetail(matchId, controller.signal, season)
      .then((value) => { if (!controller.signal.aborted) { setDetail(value); setStatus('success') } })
      .catch((requestError) => {
        if (controller.signal.aborted) return
        setError(requestError instanceof Error ? requestError.message : 'Không thể tải chi tiết trận đấu.')
        setStatus('error')
      })
    return () => controller.abort()
  }, [matchId, season, reloadCount])

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
      {status === 'success' && detail.evidenceStatus === 'INVALID' && (
        <p className="match-evidence-error" role="alert">Bằng chứng trận không khớp: {detail.evidenceError}. Điểm vẫn tạm tính.</p>
      )}
      {status === 'success' && detail.evidenceStatus === 'VERIFIED' && <p className="match-evidence-verified">Thống kê trận đã được xác minh.</p>}
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
