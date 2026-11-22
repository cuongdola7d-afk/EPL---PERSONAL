import { clubVisual } from '../utils/matchView.js'
import { resolveMatchDisplay } from '../utils/liveScores.js'
import MatchKickoff from './MatchKickoff.jsx'

const LABELS = {
  FINISHED: 'Đã kết thúc', SCHEDULED: 'Sắp diễn ra', POSTPONED: 'Bị hoãn',
  CANCELLED: 'Đã hủy', SUSPENDED: 'Tạm dừng', LIVE: 'Đang diễn ra',
  AWARDED: 'Kết quả xử lý',
}

export function ClubCrest({ name, large = false }) {
  const { color, code } = clubVisual(name)
  return <span className={`mx-crest${large ? ' mx-crest-large' : ''}`} style={{ '--mx-club-color': color }} aria-hidden="true">{code}</span>
}

export function MatchStatus({ status, liveState = null }) {
  if (liveState?.status === 'LIVE') {
    const label = Number.isInteger(liveState.minute) && liveState.minute > 0
      ? `LIVE ${liveState.minute}'`
      : 'LIVE'
    return (
      <span className="mx-status mx-status-live">
        <span className="mx-live-dot" aria-hidden="true" />
        {label}
      </span>
    )
  }
  if (liveState?.status === 'FT') {
    return <span className="mx-status mx-status-done">Hết giờ</span>
  }
  return <span className={`mx-status${status === 'FINISHED' ? ' mx-status-done' : ''}`}>
    {LABELS[status] ?? status}
  </span>
}

function MatchCard({ match, season, onOpen, liveState = null, isFlashing = false }) {
  const display = resolveMatchDisplay(match, liveState)
  const { scored, homeGoals, awayGoals, badgeText } = display
  const homeWon = scored && homeGoals > awayGoals
  const awayWon = scored && awayGoals > homeGoals
  const statusAria = scored ? `${homeGoals}–${awayGoals}` : (badgeText ?? LABELS[match.status] ?? match.status)

  return <button className="mx-match-card" type="button" onClick={() => onOpen(match.id)}
    aria-label={`Xem trận ${match.homeClub} gặp ${match.awayClub}, ${statusAria}`}>
    <span className={`mx-match-meta${season === 2026 ? ' mx-match-meta-timed' : ''}`}>
      <MatchStatus status={match.status} liveState={liveState} />
      {season === 2026 && <MatchKickoff match={match} season={season} />}
      {match.hasManualStats && <span className="mx-stat-tag">Có chỉ số cầu thủ</span>}
    </span>
    <span className="mx-match-main">
      <span className={`mx-team mx-team-home${awayWon ? ' mx-team-lost' : ''}${homeWon ? ' mx-team-won' : ''}`}>
        <span className="mx-team-name">{match.homeClub}</span><ClubCrest name={match.homeClub} />
      </span>
      <span className={`mx-card-score${isFlashing ? ' mx-score-flash' : ''}`} aria-live="polite">
        <strong>{scored ? `${homeGoals} – ${awayGoals}` : 'vs'}</strong>
        {!scored && <small>Chưa có tỉ số</small>}
      </span>
      <span className={`mx-team mx-team-away${homeWon ? ' mx-team-lost' : ''}${awayWon ? ' mx-team-won' : ''}`}>
        <ClubCrest name={match.awayClub} /><span className="mx-team-name">{match.awayClub}</span>
      </span>
    </span>
  </button>
}

export default MatchCard
