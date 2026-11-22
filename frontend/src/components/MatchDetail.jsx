import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { fetchMatchDetail } from '../api/matches.js'
import { playerDetailHash } from '../utils/playerRoute.js'
import { clubVisual, estimatedPitchPlayers, matchPlayerState, matchPlayerValue, matchRating, pitchPositions, shortPlayerName } from '../utils/matchView.js'
import { hasMatchScore } from '../utils/seasons.js'
import { formatLiveGoalText, resolveMatchDisplay } from '../utils/liveScores.js'
import { formationLines, matchLineup } from '../utils/matchLineup.js'
import { ClubCrest, MatchStatus } from './MatchCard.jsx'
import MatchKickoff from './MatchKickoff.jsx'
import {
  AssistIcon,
  CardIcon,
  GoalIcon,
  PlayerEventIcons,
  SecondYellowIcon,
  SubInIcon,
  SubOutIcon,
  playerEventSummary,
} from './EventIcons.jsx'
import MatchStats from './MatchStats.jsx'
import PlayerMatchSheet from './PlayerMatchSheet.jsx'

const POSITIONS = { G: 'GK', D: 'DEF', M: 'MID', F: 'FWD' }
const EVENT_FIELDS = [
  ['goals', 'Bàn thắng'],
  ['assists', 'Kiến tạo'],
  ['yellowCards', 'Thẻ vàng'],
  ['redCards', 'Thẻ đỏ'],
]
const dateLabel = (date) => new Intl.DateTimeFormat('vi-VN', {
  weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${date}T00:00:00Z`))
const numberLabel = (value) => value == null ? '—' : String(value)
const ratingTier = (value) => value == null ? 'na' : value >= 9 ? 'blue' : value >= 7 ? 'green' : value >= 5 ? 'orange' : 'red'
const playerLabel = (name, season) => season === 2026 ? shortPlayerName(name) : name

function renderEventIcon(field, player, evidenceStatus) {
  if (field === 'goals') return <GoalIcon />
  if (field === 'assists') return <AssistIcon />
  if (field === 'yellowCards') return <CardIcon color="yellow" />
  if (field === 'redCards') {
    const yc = player ? (matchPlayerValue(player, 'yellowCards', evidenceStatus) ?? 0) : 0
    return yc >= 2 ? <SecondYellowIcon /> : <CardIcon color="red" />
  }
  return null
}

function enrichWithLineup(players, lineup) {
  const meta = lineup?.players
  if (!Array.isArray(meta) || meta.length === 0) return players
  const byId = new Map(meta.map((item) => [item.playerId, item]))
  return players.map((player) => {
    const extraMeta = byId.get(player.playerId)
    return extraMeta ? { ...player, ...extraMeta } : player
  })
}

function Avatar({ name, club }) {
  const initials = name.replace(/\./g, '').split(/\s+/).filter(Boolean)
  const label = initials.length < 2 ? name.slice(0, 2) : `${initials[0][0]}${initials.at(-1)[0]}`
  return <span className="mx-avatar" style={{ '--mx-club-color': clubVisual(club).color }} aria-hidden="true">{label.toUpperCase()}</span>
}

function RatingBadge({ player }) {
  const rating = matchRating(player)
  return <span className={`mx-pitch-rating mx-rating-${ratingTier(rating)}`} title={rating == null ? 'Chưa có rating' : `Rating ${rating.toFixed(1)}`}>
    {rating == null ? '—' : rating.toFixed(1)}
  </span>
}

function PlayerRow({ player, club, season, evidenceStatus, onSelectPlayer }) {
  const state = matchPlayerState(player)
  const rating = state === 'played' ? matchRating(player) : null
  const minutes = matchPlayerValue(player, 'minutes', evidenceStatus)
  const points = player.participationStatus != null
    ? `Fantasy: ${player.fantasyPoints == null ? 'chưa được chấm' : player.fantasyPoints}`
    : player.score?.status === 'COMPLETE' ? `Điểm v1: ${player.score.confirmedPoints}`
      : player.score ? `Tạm tính v1: ${player.score.confirmedPoints}` : null
  const interactive2026 = season === 2026 && state === 'played' && Boolean(onSelectPlayer)

  const content = (
    <>
      <span className="mx-player-identity">
        <Avatar name={player.playerName} club={club} />
        <span>
          <strong title={player.playerName}>
            {playerLabel(player.playerName, season)}
            {season === 2026 && state === 'played' && (
              <PlayerEventIcons player={player} evidenceStatus={evidenceStatus} className="mx-inline-ev" />
            )}
          </strong>
          <small>
            <span className={`mx-position mx-position-${player.position}`}>
              {POSITIONS[player.position] ?? player.position ?? '—'}
            </span>
            {state === 'played' && rating == null ? ' · chưa có rating' : ''}
            {points && ` · ${points}`}
          </small>
        </span>
      </span>
      {player.role === 'SUB_USED' && (
        <span className="mx-substitution mx-sub-in" title="Thay vào đã xác nhận">
          <SubInIcon />
          {player.substitutionInMinute != null && <span>{player.substitutionInMinute}′</span>}
        </span>
      )}
      {player.substitutionOutMinute != null && (
        <span className="mx-substitution mx-sub-out" title="Thay ra đã xác nhận">
          <SubOutIcon />
          <span>{player.substitutionOutMinute}′</span>
        </span>
      )}
      {state === 'did-not-play' ? <span className="mx-dnp">Không ra sân</span> :
        state === 'unknown' ? <span className="mx-dnp">Chưa rõ ra sân</span> :
          <span className="mx-player-numbers">
            <span className="mx-minutes" title="Số phút thi đấu">{minutes == null ? '—' : `${minutes}′`}</span>
            <span className={`mx-rating mx-rating-${ratingTier(rating)}`} title={rating == null ? 'Chưa có rating' : 'Rating trận'}>
              {rating == null ? '—' : rating.toFixed(1)}
            </span>
            {EVENT_FIELDS.map(([field, label]) => {
              const value = matchPlayerValue(player, field, evidenceStatus)
              return (
                <span
                  className={`mx-stat-pill${value === 0 ? ' mx-stat-zero' : ''}`}
                  key={field}
                  title={`${label}: ${value == null ? 'chưa có dữ liệu' : value}`}
                >
                  {renderEventIcon(field, player, evidenceStatus)}
                  {numberLabel(value)}
                </span>
              )
            })}
          </span>}
    </>
  )

  return (
    <div className="mx-player-entry">
      {interactive2026 ? (
        <button
          type="button"
          className="mx-player-row mx-player-row-btn"
          onClick={(event) => onSelectPlayer(player, club, event.currentTarget)}
        >
          {content}
        </button>
      ) : season === 2026 ? (
        <div className="mx-player-row mx-player-row-dnp">{content}</div>
      ) : (
        <a className="mx-player-row" href={playerDetailHash(player.playerId, season, window.location.hash)}>
          {content}
        </a>
      )}
      {player.score && (
        <details className="mx-v1-score">
          <summary>Chi tiết điểm v1 · {player.score.status === 'COMPLETE' ? 'đã đủ dữ liệu' : 'tạm tính'}</summary>
          <ul>
            {player.score.parts.map((part) => (
              <li key={part.code}>
                <span><strong>{part.label}</strong><small>{part.detail}</small></span>
                <strong>{part.points == null ? 'Chưa có dữ liệu' : part.points >= 0 ? `+${part.points}` : part.points}</strong>
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  )
}

function PlayerGroup({ title, rows, club, season, evidenceStatus, onSelectPlayer }) {
  return (
    <section className="mx-player-group">
      <h4>{title} · {rows.length}</h4>
      {rows.length ? rows.map((player) => (
        <PlayerRow
          key={player.playerId}
          player={player}
          club={club}
          season={season}
          evidenceStatus={evidenceStatus}
          onSelectPlayer={onSelectPlayer}
        />
      )) : <p>Không có cầu thủ.</p>}
    </section>
  )
}

function PitchNode({ player, club, season, evidenceStatus, x, y, layoutPosition, onSelectPlayer }) {
  const initials = player.playerName.replace(/\./g, '').split(/\s+/).filter(Boolean)
  const label = initials.length < 2 ? player.playerName.slice(0, 2) : `${initials[0][0]}${initials.at(-1)[0]}`
  const minutes = matchPlayerValue(player, 'minutes', evidenceStatus)
  const positionLabel = season === 2026 ? layoutPosition : player.matchPosition
  const eventSummary = playerEventSummary(player, evidenceStatus)
  const hasEvents = Boolean(eventSummary)
  const ratingText = matchRating(player) == null ? 'chưa có' : matchRating(player).toFixed(1)
  const ariaLabel = `${player.playerName}, ${club}, rating ${ratingText}${eventSummary ? `, ${eventSummary}` : ''}`
  const style = { '--mx-x': `${x}%`, '--mx-y': `${y}%`, '--mx-club-color': clubVisual(club).color }

  const inner = (
    <>
      <span className="mx-pitch-avatar">
        {label.toUpperCase()}
        {hasEvents && (
          <PlayerEventIcons player={player} evidenceStatus={evidenceStatus} className="mx-pitch-evs" />
        )}
        {player.substitutionOutMinute != null && (
          <span className="mx-pitch-sub-out" title={`Rời sân phút ${player.substitutionOutMinute}`}>
            <SubOutIcon />
            <span>{player.substitutionOutMinute}′</span>
          </span>
        )}
        {player.substitutionOutMinute == null && minutes != null && minutes < 90 && (
          <span className="mx-pitch-minutes" title="Số phút thi đấu">{minutes}′</span>
        )}
      </span>
      <RatingBadge player={player} />
      {positionLabel && (
        <span className="mx-match-position" title={season === 2026 ? `Ô ${positionLabel} trong sơ đồ` : 'Vị trí trận đã xác minh'}>
          {positionLabel}
        </span>
      )}
      <span className="mx-pitch-name" title={player.playerName}>{playerLabel(player.playerName, season)}</span>
    </>
  )

  if (season === 2026 && onSelectPlayer) {
    return (
      <button
        type="button"
        className="mx-pitch-node mx-pitch-node-btn"
        data-layout-position={layoutPosition}
        style={style}
        aria-label={ariaLabel}
        onClick={(event) => onSelectPlayer(player, club, event.currentTarget)}
      >
        {inner}
      </button>
    )
  }

  return (
    <a
      className="mx-pitch-node"
      href={playerDetailHash(player.playerId, season, window.location.hash)}
      data-layout-position={season === 2026 ? layoutPosition : undefined}
      style={style}
      aria-label={ariaLabel}
    >
      {inner}
    </a>
  )
}

function LegacyLineup({ match, detail, season }) {
  const home = estimatedPitchPlayers(detail.homePlayers, detail.evidenceStatus)
  const away = estimatedPitchPlayers(detail.awayPlayers, detail.evidenceStatus)
  const homeNodes = pitchPositions(home.selected, 'home')
  const awayNodes = pitchPositions(away.selected, 'away')
  const teamHeading = (club, side) => <div className={`mx-lineup-team mx-lineup-team-${side}`}>
    <ClubCrest name={club} /><span><strong>{club}</strong><span>Sơ đồ 4-3-3</span></span>
  </div>
  return <div className="mx-lineup">
    <div className="mx-lineup-heading">{teamHeading(match.homeClub, 'home')}
      {teamHeading(match.awayClub, 'away')}</div>
    <div className="mx-pitch" aria-label="Sơ đồ sân bóng hai đội">
      <svg className="mx-pitch-horizontal" viewBox="0 0 1000 620" preserveAspectRatio="none" aria-hidden="true">
        <rect x="14" y="14" width="972" height="592" /><path d="M500 14v592" /><circle cx="500" cy="310" r="72" />
        <rect x="14" y="145" width="165" height="330" /><rect x="14" y="235" width="55" height="150" />
        <rect x="821" y="145" width="165" height="330" /><rect x="931" y="235" width="55" height="150" />
        <circle cx="500" cy="310" r="5" className="mx-pitch-center-dot" />
      </svg>
      <svg className="mx-pitch-vertical" viewBox="0 0 620 1000" preserveAspectRatio="none" aria-hidden="true">
        <rect x="14" y="14" width="592" height="972" /><path d="M14 500h592" /><circle cx="310" cy="500" r="72" />
        <rect x="145" y="14" width="330" height="165" /><rect x="235" y="14" width="150" height="55" />
        <rect x="145" y="821" width="330" height="165" /><rect x="235" y="931" width="150" height="55" />
        <circle cx="310" cy="500" r="5" className="mx-pitch-center-dot" />
      </svg>
      {homeNodes.map(({ player, x, y }) => <PitchNode key={player.playerId} player={player}
        club={match.homeClub} season={season} evidenceStatus={detail.evidenceStatus} x={x} y={y} />)}
      {awayNodes.map(({ player, x, y }) => <PitchNode key={player.playerId} player={player}
        club={match.awayClub} season={season} evidenceStatus={detail.evidenceStatus} x={x} y={y} />)}
    </div>
    <div className="mx-bench">
      <PlayerGroup title={`Dự bị · ${match.homeClub}`} rows={home.bench} club={match.homeClub}
        season={season} evidenceStatus={detail.evidenceStatus} />
      <PlayerGroup title={`Dự bị · ${match.awayClub}`} rows={away.bench} club={match.awayClub}
        season={season} evidenceStatus={detail.evidenceStatus} />
    </div>
  </div>
}

function Lineup({ match, detail, season, onSelectPlayer }) {
  return season === 2026
    ? <SeasonLineup match={match} detail={detail} season={season} onSelectPlayer={onSelectPlayer} />
    : <LegacyLineup match={match} detail={detail} season={season} />
}

function SeasonLineup({ match, detail, season, onSelectPlayer }) {
  const home = matchLineup(detail.homePlayers, detail.homeLineup, 'home')
  const away = matchLineup(detail.awayPlayers, detail.awayLineup, 'away')
  const sides = [home, away]
  const clubs = [match.homeClub, match.awayClub]
  const pitchRows = Math.max(...sides.map((team) => formationLines(team.formation)?.length ?? 0), 4)
  return <div className="mx-lineup mx-lineup-2026">
    <div className="mx-lineup-heading">{sides.map((team, index) => <div key={index} className={`mx-lineup-team mx-lineup-team-${index ? 'away' : 'home'}`}>
      <ClubCrest name={clubs[index]} /><span><strong>{clubs[index]}</strong><span>{team.formation ? `Sơ đồ ${team.formation}` : 'Chưa có sơ đồ'}</span>
        {team.formationSource === 'FIXTURE' && <span>Sơ đồ trận đã xác minh</span>}</span>
    </div>)}</div>
    {sides.map((team, index) => (team.startersStatus !== 'VERIFIED' || !team.formation || ['INVALID', 'INSUFFICIENT_POSITIONS'].includes(team.positionSource)) && <div key={index} className="mx-lineup-notice" role="status">
      <strong>{clubs[index]}: </strong>{team.startersStatus !== 'VERIFIED' ? 'Chưa có đủ dữ liệu xác nhận 11 cầu thủ đá chính.' :
        !team.formation ? 'Đã xác nhận 11 đá chính; chưa có sơ đồ để bố trí trên sân.' :
          team.positionSource === 'INVALID' ? 'Dữ liệu vị trí trận không hợp lệ; cần kiểm tra lại.' :
            'Chưa đủ vị trí phù hợp để bố trí sơ đồ này; cần bổ sung vị trí trận đã xác minh.'}
    </div>)}
    {sides.some((team) => team.nodes.length > 0) && <div className="mx-pitch mx-pitch-2026" style={{ '--mx-lineup-rows': pitchRows }} aria-label="Sơ đồ sân bóng hai đội">
      <svg className="mx-pitch-horizontal" viewBox="0 0 1000 620" preserveAspectRatio="none" aria-hidden="true">
        <rect x="14" y="14" width="972" height="592" /><path d="M500 14v592" /><circle cx="500" cy="310" r="72" />
        <rect x="14" y="145" width="165" height="330" /><rect x="14" y="235" width="55" height="150" />
        <rect x="821" y="145" width="165" height="330" /><rect x="931" y="235" width="55" height="150" />
        <circle cx="500" cy="310" r="5" className="mx-pitch-center-dot" />
      </svg>
      <svg className="mx-pitch-vertical" viewBox="0 0 620 1000" preserveAspectRatio="none" aria-hidden="true">
        <rect x="14" y="14" width="592" height="972" /><path d="M14 500h592" /><circle cx="310" cy="500" r="72" />
        <rect x="145" y="14" width="330" height="165" /><rect x="235" y="14" width="150" height="55" />
        <rect x="145" y="821" width="330" height="165" /><rect x="235" y="931" width="150" height="55" />
        <circle cx="310" cy="500" r="5" className="mx-pitch-center-dot" />
      </svg>
      {sides.flatMap((team, index) => team.nodes.map((node) => (
        <PitchNode
          key={`${index}-${node.player.playerId}`}
          {...node}
          club={clubs[index]}
          season={season}
          evidenceStatus={detail.evidenceStatus}
          onSelectPlayer={onSelectPlayer}
        />
      )))}
    </div>}
    <div className="mx-bench">{sides.map((team, index) => <div key={index}>
      {team.startersStatus === 'VERIFIED' ? <>
        <PlayerGroup
          title={`Dự bị đã vào sân · ${clubs[index]}`}
          rows={team.substitutes}
          club={clubs[index]}
          season={season}
          evidenceStatus={detail.evidenceStatus}
          onSelectPlayer={onSelectPlayer}
        />
        <PlayerGroup
          title={`Không ra sân · ${clubs[index]}`}
          rows={team.bench}
          club={clubs[index]}
          season={season}
          evidenceStatus={detail.evidenceStatus}
          onSelectPlayer={onSelectPlayer}
        />
      </> : (
        <PlayerGroup
          title={`Chưa xác nhận vai trò · ${clubs[index]}`}
          rows={team.unknown}
          club={clubs[index]}
          season={season}
          evidenceStatus={detail.evidenceStatus}
          onSelectPlayer={onSelectPlayer}
        />
      )}
    </div>)}</div>
  </div>
}

function EventColumn({ club, players, evidenceStatus, season }) {
  return <div className="mx-event-column"><strong>{club}</strong>
    {EVENT_FIELDS.map(([field, label]) => {
      const contributors = players.filter((player) =>
        (matchPlayerValue(player, field, evidenceStatus) ?? 0) > 0)
      return <div className="mx-event-type" key={field}><small>{label}</small>
        {contributors.length ? contributors.map((player) => {
          const count = matchPlayerValue(player, field, evidenceStatus)
          return (
            <span key={player.playerId} title={player.playerName}>
              {renderEventIcon(field, player, evidenceStatus)}
              <span>{playerLabel(player.playerName, season)}{count > 1 && ` ×${count}`}</span>
            </span>
          )
        }) : <em>—</em>}</div>
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

function Overview({ match, detail, season }) {
  const all = [...detail.homePlayers.map((player) => ({ player, club: match.homeClub })),
    ...detail.awayPlayers.map((player) => ({ player, club: match.awayClub }))]
  const best = all.filter(({ player }) => matchPlayerState(player) === 'played' && matchRating(player) != null)
    .sort((a, b) => matchRating(b.player) - matchRating(a.player))[0]
  return <div className="mx-overview">
    <section className="mx-block"><h3>Cầu thủ xuất sắc nhất trận</h3>
      {best ? <div className="mx-best"><Avatar name={best.player.playerName} club={best.club} />
        <span><strong title={best.player.playerName}>{playerLabel(best.player.playerName, season)}</strong><small>{best.club}</small></span>
        <span className={`mx-rating mx-best-rating mx-rating-${ratingTier(matchRating(best.player))}`}>
          {matchRating(best.player).toFixed(1)}</span></div> : <p>Chưa có điểm đánh giá nào.</p>}
    </section>
    <section className="mx-block"><h3>Sự kiện chính</h3>
      <div className="mx-event-grid"><EventColumn club={match.homeClub} players={detail.homePlayers} evidenceStatus={detail.evidenceStatus} season={season} />
        <EventColumn club={match.awayClub} players={detail.awayPlayers} evidenceStatus={detail.evidenceStatus} season={season} /></div>
      <p className="mx-detail-note">Nguồn hiện chưa lưu phút xảy ra sự kiện.</p>
    </section>
    <section className="mx-block"><h3>Rating trung bình của đội</h3>
      <TeamAverage club={match.homeClub} players={detail.homePlayers} />
      <TeamAverage club={match.awayClub} players={detail.awayPlayers} />
    </section>
  </div>
}

function Scorers({ detail, season }) {
  const sides = [detail.homePlayers, detail.awayPlayers]
  if (!sides.some((players) => players.some((player) =>
    (matchPlayerValue(player, 'goals', detail.evidenceStatus) ?? 0) > 0))) return null
  return <div className="mx-scorers">{sides.map((players, index) => <div key={index}>
    {players.filter((player) => (matchPlayerValue(player, 'goals', detail.evidenceStatus) ?? 0) > 0)
      .map((player) => {
        const count = matchPlayerValue(player, 'goals', detail.evidenceStatus)
        return (
          <span key={player.playerId} className="mx-scorer-item" title={player.playerName}>
            <GoalIcon />
            <span>{playerLabel(player.playerName, season)}{count > 1 && ` ×${count}`}</span>
          </span>
        )
      })}
  </div>)}</div>
}

function LiveScorers({ goals }) {
  if (!Array.isArray(goals) || goals.length === 0) return null
  const homeGoals = goals.filter((goal) => goal.side === 'home')
  const awayGoals = goals.filter((goal) => goal.side === 'away')
  if (homeGoals.length === 0 && awayGoals.length === 0) return null
  return (
    <div className="mx-scorers mx-live-scorers">
      <div>
        {homeGoals.map((goal, idx) => (
          <span key={`live-h-${idx}`} className="mx-scorer-item">
            <GoalIcon />
            <span>{formatLiveGoalText(goal)}</span>
          </span>
        ))}
      </div>
      <div>
        {awayGoals.map((goal, idx) => (
          <span key={`live-a-${idx}`} className="mx-scorer-item">
            <GoalIcon />
            <span>{formatLiveGoalText(goal)}</span>
          </span>
        ))}
      </div>
    </div>
  )
}

function MatchDetail({ matchId, summary, onClose, season, backLabel = 'Lịch đấu', liveState = null, isFlashing = false }) {
  const [detail, setDetail] = useState(null)
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [reloadCount, setReloadCount] = useState(0)
  const [tab, setTab] = useState('lineup')
  const [selectedPlayer, setSelectedPlayer] = useState(null)
  const headingRef = useRef(null)
  const sheetTriggerRef = useRef(null)

  useEffect(() => {
    headingRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    headingRef.current?.focus({ preventScroll: true })
  }, [matchId])

  useEffect(() => {
    setSelectedPlayer(null)
    if (season !== 2026) {
      setTab((prev) => (prev === 'stats' ? 'lineup' : prev))
    }
  }, [matchId, season])

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

  const openPlayerSheet = useCallback((player, club, triggerEl) => {
    if (season !== 2026 || matchPlayerState(player) !== 'played') return
    sheetTriggerRef.current = triggerEl ?? document.activeElement
    setSelectedPlayer({ player, club })
  }, [season])

  const closePlayerSheet = useCallback(() => {
    setSelectedPlayer(null)
  }, [])

  const enrichedHomePlayers = useMemo(
    () => (detail ? enrichWithLineup(detail.homePlayers, detail.homeLineup) : []),
    [detail],
  )
  const enrichedAwayPlayers = useMemo(
    () => (detail ? enrichWithLineup(detail.awayPlayers, detail.awayLineup) : []),
    [detail],
  )

  const match = detail?.match ?? summary
  const noStats = status === 'success' &&
    detail.homePlayers.length === 0 && detail.awayPlayers.length === 0
  const hasVerifiedManualStats = Boolean(match?.hasManualStats) || (status === 'success' && !noStats)
  const effectiveLiveState = hasVerifiedManualStats ? null : liveState
  const display = resolveMatchDisplay(match, effectiveLiveState, { preferManualStats: true })
  const scored = display.scored
  const activeTab = season === 2026 ? tab : (tab === 'stats' ? 'lineup' : tab)

  return <div className="mx-detail" aria-busy={status === 'loading'}>
    <button className="mx-back" type="button" onClick={onClose}>‹ <span>{backLabel}</span></button>
    {match && <header className="mx-detail-hero">
      <div className="mx-detail-meta"><span className="mx-gw">GW{match.matchweek}</span>
        {season === 2026 ? <MatchKickoff match={match} season={season} /> : <time dateTime={match.date}>{dateLabel(match.date)}</time>}<MatchStatus status={match.status} liveState={effectiveLiveState} /></div>
      <div className="mx-detail-score"><div className="mx-detail-club"><ClubCrest name={match.homeClub} large />
        <strong>{match.homeClub}</strong></div><div className={`mx-big-score${isFlashing && !hasVerifiedManualStats ? ' mx-score-flash' : ''}`} ref={headingRef} tabIndex="-1" aria-live="polite"
          aria-label={`${match.homeClub} ${scored ? display.homeGoals : 'đấu'} ${match.awayClub} ${scored ? display.awayGoals : ''}`}>
          {scored ? `${display.homeGoals} – ${display.awayGoals}` : 'vs'}
          {!scored && <small>Chưa có tỉ số</small>}</div>
        <div className="mx-detail-club"><ClubCrest name={match.awayClub} large />
          <strong>{match.awayClub}</strong></div></div>
      {status === 'success' && !noStats
        ? <Scorers detail={detail} season={season} />
        : display.useLiveGoals && effectiveLiveState && <LiveScorers goals={effectiveLiveState.goals} />}
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
        <button type="button" role="tab" aria-selected={activeTab === 'lineup'} onClick={() => setTab('lineup')}>Đội hình</button>
        {season === 2026 && (
          <button type="button" role="tab" aria-selected={activeTab === 'stats'} onClick={() => setTab('stats')}>Thống kê</button>
        )}
        <button type="button" role="tab" aria-selected={activeTab === 'overview'} onClick={() => setTab('overview')}>Tổng quan</button>
      </div>
      <section className="mx-detail-panel" role="tabpanel">
        {activeTab === 'lineup' ? (
          <Lineup match={match} detail={detail} season={season} onSelectPlayer={season === 2026 ? openPlayerSheet : null} />
        ) : activeTab === 'stats' && season === 2026 ? (
          <MatchStats
            match={match}
            homePlayers={enrichedHomePlayers}
            awayPlayers={enrichedAwayPlayers}
            evidenceStatus={detail.evidenceStatus}
            onSelectPlayer={openPlayerSheet}
          />
        ) : (
          <Overview match={match} detail={detail} season={season} />
        )}
      </section>
    </>}

    {season === 2026 && selectedPlayer && detail && (
      <PlayerMatchSheet
        player={selectedPlayer.player}
        club={selectedPlayer.club}
        season={season}
        evidenceStatus={detail.evidenceStatus}
        onClose={closePlayerSheet}
        returnFocusRef={sheetTriggerRef}
      />
    )}
  </div>
}

export default MatchDetail
