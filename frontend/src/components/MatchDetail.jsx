import { useEffect, useRef, useState } from 'react'
import { fetchMatchDetail } from '../api/matches.js'
import { playerDetailHash } from '../utils/playerRoute.js'
import { clubVisual, matchPlayerState, matchPlayerValue, matchRating } from '../utils/matchView.js'
import { hasMatchScore } from '../utils/seasons.js'
import { ClubCrest, MatchStatus } from './MatchCard.jsx'

const POSITIONS = { G: 'GK', D: 'DEF', M: 'MID', F: 'FWD' }
const EVENTS = [
  ['goals', '⚽', 'Bàn thắng'], ['assists', '↗', 'Kiến tạo'],
  ['yellowCards', '🟨', 'Thẻ vàng'], ['redCards', '🟥', 'Thẻ đỏ'],
]
const dateLabel = (date) => new Intl.DateTimeFormat('vi-VN', {
  weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${date}T00:00:00Z`))
const numberLabel = (value) => value == null ? '—' : String(value)
const ratingTier = (value) => value == null ? 'na' : value >= 9 ? 'blue' : value >= 7 ? 'green' : value >= 5 ? 'orange' : 'red'

function Avatar({ name, club }) {
  const initials = name.replace(/\./g, '').split(/\s+/).filter(Boolean)
  const label = initials.length < 2 ? name.slice(0, 2) : `${initials[0][0]}${initials.at(-1)[0]}`
  return <span className="mx-avatar" style={{ '--mx-club-color': clubVisual(club).color }} aria-hidden="true">{label.toUpperCase()}</span>
}

function PlayerRow({ player, club, season, evidenceStatus }) {
  const state = matchPlayerState(player)
  const rating = state === 'played' ? matchRating(player) : null
  const minutes = matchPlayerValue(player, 'minutes', evidenceStatus)
  const points = player.participationStatus != null
    ? `Fantasy: ${player.fantasyPoints == null ? 'chưa được chấm' : player.fantasyPoints}`
    : player.score?.status === 'COMPLETE' ? `Điểm v1: ${player.score.confirmedPoints}`
      : player.score ? `Tạm tính v1: ${player.score.confirmedPoints}` : null
  return <div className="mx-player-entry"><a className="mx-player-row" href={playerDetailHash(player.playerId, season)}>
    <span className="mx-player-identity"><Avatar name={player.playerName} club={club} />
      <span><strong>{player.playerName}</strong><small><span className={`mx-position mx-position-${player.position}`}>
        {POSITIONS[player.position] ?? player.position ?? '—'}</span>{state === 'played' && rating == null ? ' · chưa có rating' : ''}
        {points && ` · ${points}`}</small></span>
    </span>
    {state === 'did-not-play' ? <span className="mx-dnp">Không ra sân</span> :
      state === 'unknown' ? <span className="mx-dnp">Chưa rõ ra sân</span> :
        <span className="mx-player-numbers">
          <span className="mx-minutes" title="Số phút thi đấu">{minutes == null ? '—' : `${minutes}′`}</span>
          <span className={`mx-rating mx-rating-${ratingTier(rating)}`} title={rating == null ? 'Chưa có rating' : 'Rating trận'}>
            {rating == null ? '—' : rating.toFixed(1)}</span>
          {EVENTS.map(([field, symbol, label]) => {
            const value = matchPlayerValue(player, field, evidenceStatus)
            return <span className={`mx-stat-pill${value === 0 ? ' mx-stat-zero' : ''}`} key={field}
              title={`${label}: ${value == null ? 'chưa có dữ liệu' : value}`}>
              <span aria-hidden="true">{symbol}</span>{numberLabel(value)}</span>
          })}
        </span>}
  </a>
    {player.score && <details className="mx-v1-score"><summary>Chi tiết điểm v1 · {player.score.status === 'COMPLETE' ? 'đã đủ dữ liệu' : 'tạm tính'}</summary>
      <ul>{player.score.parts.map((part) => <li key={part.code}><span><strong>{part.label}</strong><small>{part.detail}</small></span>
        <strong>{part.points == null ? 'Chưa có dữ liệu' : part.points >= 0 ? `+${part.points}` : part.points}</strong></li>)}</ul>
    </details>}
  </div>
}

function PlayerGroup({ title, rows, club, season, evidenceStatus }) {
  return <section className="mx-player-group"><h4>{title} · {rows.length}</h4>
    {rows.length ? rows.map((player) => <PlayerRow key={player.playerId} player={player} club={club}
      season={season} evidenceStatus={evidenceStatus} />) : <p>Không có cầu thủ.</p>}
  </section>
}

function TeamPlayers({ match, detail, season }) {
  const [side, setSide] = useState('home')
  const club = side === 'home' ? match.homeClub : match.awayClub
  const players = side === 'home' ? detail.homePlayers : detail.awayPlayers
  const played = players.filter((player) => matchPlayerState(player) === 'played')
  const dnp = players.filter((player) => matchPlayerState(player) === 'did-not-play')
  const unknown = players.filter((player) => matchPlayerState(player) === 'unknown')
  const props = { club, season, evidenceStatus: detail.evidenceStatus }
  return <>
    <div className="mx-team-switch" role="group" aria-label="Chọn đội bóng">
      <button type="button" aria-pressed={side === 'home'} onClick={() => setSide('home')}>{match.homeClub}</button>
      <button type="button" aria-pressed={side === 'away'} onClick={() => setSide('away')}>{match.awayClub}</button>
    </div>
    <p className="mx-detail-note">Dữ liệu hiện lưu trạng thái ra sân và vị trí cầu thủ; chưa lưu đội hình xuất phát hoặc sơ đồ chiến thuật.</p>
    <PlayerGroup title="Đã thi đấu" rows={played} {...props} />
    <PlayerGroup title="Không ra sân" rows={dnp} {...props} />
    {unknown.length > 0 && <PlayerGroup title="Chưa rõ trạng thái" rows={unknown} {...props} />}
  </>
}

function EventColumn({ club, players, evidenceStatus }) {
  return <div className="mx-event-column"><strong>{club}</strong>
    {EVENTS.map(([field, symbol, label]) => {
      const contributors = players.filter((player) =>
        (matchPlayerValue(player, field, evidenceStatus) ?? 0) > 0)
      return <div className="mx-event-type" key={field}><small>{label}</small>
        {contributors.length ? contributors.map((player) => <span key={player.playerId}>
          <span aria-hidden="true">{symbol}</span>{player.playerName}
          {matchPlayerValue(player, field, evidenceStatus) > 1 && ` ×${matchPlayerValue(player, field, evidenceStatus)}`}
        </span>) : <em>—</em>}</div>
    })}
  </div>
}

function TeamAverage({ club, players }) {
  const rated = players.filter((player) => matchPlayerState(player) === 'played')
    .map(matchRating).filter((rating) => rating != null)
  const average = rated.length ? rated.reduce((sum, rating) => sum + rating, 0) / rated.length : null
  return <div className="mx-average-row"><span>{club}</span><span className="mx-average-track">
    <span className={`mx-average-fill mx-rating-${ratingTier(average)}`} style={{ width: `${average == null ? 0 : average * 10}%` }} />
  </span><strong>{average == null ? '—' : average.toFixed(1)}</strong></div>
}

function Overview({ match, detail }) {
  const all = [...detail.homePlayers.map((player) => ({ player, club: match.homeClub })),
    ...detail.awayPlayers.map((player) => ({ player, club: match.awayClub }))]
  const best = all.filter(({ player }) => matchPlayerState(player) === 'played' && matchRating(player) != null)
    .sort((a, b) => matchRating(b.player) - matchRating(a.player))[0]
  return <div className="mx-overview">
    <section className="mx-block"><h3>Cầu thủ xuất sắc nhất trận</h3>
      {best ? <div className="mx-best"><Avatar name={best.player.playerName} club={best.club} />
        <span><strong>{best.player.playerName}</strong><small>{best.club}</small></span>
        <span className={`mx-rating mx-best-rating mx-rating-${ratingTier(matchRating(best.player))}`}>
          {matchRating(best.player).toFixed(1)}</span></div> : <p>Chưa có điểm đánh giá nào.</p>}
    </section>
    <section className="mx-block"><h3>Sự kiện chính</h3>
      <div className="mx-event-grid"><EventColumn club={match.homeClub} players={detail.homePlayers} evidenceStatus={detail.evidenceStatus} />
        <EventColumn club={match.awayClub} players={detail.awayPlayers} evidenceStatus={detail.evidenceStatus} /></div>
      <p className="mx-detail-note">Nguồn hiện chưa lưu phút xảy ra sự kiện.</p>
    </section>
    <section className="mx-block"><h3>Rating trung bình của đội</h3>
      <TeamAverage club={match.homeClub} players={detail.homePlayers} />
      <TeamAverage club={match.awayClub} players={detail.awayPlayers} />
    </section>
  </div>
}

function Scorers({ match, detail }) {
  const sides = [detail.homePlayers, detail.awayPlayers]
  if (!sides.some((players) => players.some((player) =>
    (matchPlayerValue(player, 'goals', detail.evidenceStatus) ?? 0) > 0))) return null
  return <div className="mx-scorers">{sides.map((players, index) => <div key={index}>
    {players.filter((player) => (matchPlayerValue(player, 'goals', detail.evidenceStatus) ?? 0) > 0)
      .map((player) => <span key={player.playerId}>⚽ {player.playerName}
        {matchPlayerValue(player, 'goals', detail.evidenceStatus) > 1 && ` ×${matchPlayerValue(player, 'goals', detail.evidenceStatus)}`}</span>)}
  </div>)}</div>
}

function MatchDetail({ matchId, summary, onClose, season }) {
  const [detail, setDetail] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [reloadCount, setReloadCount] = useState(0)
  const [tab, setTab] = useState('players')
  const headingRef = useRef(null)

  useEffect(() => {
    headingRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    headingRef.current?.focus({ preventScroll: true })
  }, [matchId])

  useEffect(() => {
    const controller = new AbortController()
    fetchMatchDetail(matchId, controller.signal, season)
      .then((value) => { if (!controller.signal.aborted) { setDetail(value); setStatus('success') } })
      .catch((requestError) => {
        if (controller.signal.aborted) return
        setError(requestError instanceof Error ? requestError.message : 'Không thể tải chi tiết trận.')
        setStatus('error')
      })
    return () => controller.abort()
  }, [matchId, season, reloadCount])

  const match = detail?.match ?? summary
  const scored = match && hasMatchScore(match)
  const noStats = status === 'success' &&
    detail.homePlayers.length === 0 && detail.awayPlayers.length === 0

  return <div className="mx-detail" aria-busy={status === 'loading'}>
    <button className="mx-back" type="button" onClick={onClose}>‹ <span>Lịch đấu</span></button>
    {match && <header className="mx-detail-hero">
      <div className="mx-detail-meta"><span className="mx-gw">GW{match.matchweek}</span>
        <time dateTime={match.date}>{dateLabel(match.date)}</time><MatchStatus status={match.status} /></div>
      <div className="mx-detail-score"><div className="mx-detail-club"><ClubCrest name={match.homeClub} large />
        <strong>{match.homeClub}</strong></div><div className="mx-big-score" ref={headingRef} tabIndex="-1"
          aria-label={`${match.homeClub} ${scored ? match.homeGoals : 'đấu'} ${match.awayClub} ${scored ? match.awayGoals : ''}`}>
          {scored ? `${match.homeGoals} – ${match.awayGoals}` : 'vs'}
          {!scored && <small>Chưa có tỉ số</small>}</div>
        <div className="mx-detail-club"><ClubCrest name={match.awayClub} large />
          <strong>{match.awayClub}</strong></div></div>
      {status === 'success' && !noStats && <Scorers match={match} detail={detail} />}
    </header>}

    {status === 'loading' && <div className="mx-empty" role="status">Đang tải chi tiết trận...</div>}
    {status === 'error' && <div className="mx-empty" role="alert"><strong>Không tải được chi tiết trận</strong>
      <p>{error}</p><button type="button" onClick={() => { setStatus('loading'); setReloadCount((count) => count + 1) }}>Thử lại</button></div>}
    {status === 'success' && detail.evidenceStatus === 'INVALID' &&
      <p className="mx-evidence-error" role="alert">Bằng chứng trận không khớp: {detail.evidenceError}. Điểm vẫn tạm tính.</p>}
    {status === 'success' && (noStats || match.status !== 'FINISHED') &&
      <div className="mx-empty"><strong>{match.status === 'FINISHED' ? 'Chưa có chỉ số cầu thủ' : 'Trận chưa kết thúc'}</strong>
        <p>{match.status === 'FINISHED' ? 'Tỉ số đã có, thống kê cầu thủ của trận này chưa được lưu.' : 'Thông tin cầu thủ sẽ hiện khi trận kết thúc và có dữ liệu.'}</p></div>}
    {status === 'success' && !noStats && match.status === 'FINISHED' && <>
      <div className="mx-detail-tabs" role="tablist" aria-label="Chi tiết trận đấu">
        <button type="button" role="tab" aria-selected={tab === 'players'} onClick={() => setTab('players')}>Cầu thủ</button>
        <button type="button" role="tab" aria-selected={tab === 'overview'} onClick={() => setTab('overview')}>Tổng quan</button>
      </div>
      <section className="mx-detail-panel" role="tabpanel">
        {tab === 'players' ? <TeamPlayers match={match} detail={detail} season={season} /> :
          <Overview match={match} detail={detail} />}
      </section>
    </>}
  </div>
}

export default MatchDetail
