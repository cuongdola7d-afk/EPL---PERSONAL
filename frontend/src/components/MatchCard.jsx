import { clubVisual } from '../utils/matchView.js'
import { hasMatchScore } from '../utils/seasons.js'

const LABELS = {
  FINISHED: 'Đã kết thúc', SCHEDULED: 'Sắp diễn ra', POSTPONED: 'Bị hoãn',
  CANCELLED: 'Đã hủy', SUSPENDED: 'Tạm dừng', LIVE: 'Đang diễn ra',
  AWARDED: 'Kết quả xử lý',
}

export function ClubCrest({ name, large = false }) {
  const { color, code } = clubVisual(name)
  return <span className={`mx-crest${large ? ' mx-crest-large' : ''}`} style={{ '--mx-club-color': color }} aria-hidden="true">{code}</span>
}

export function MatchStatus({ status }) {
  return <span className={`mx-status${status === 'FINISHED' ? ' mx-status-done' : ''}`}>
    {LABELS[status] ?? status}
  </span>
}

function MatchCard({ match, onOpen }) {
  const scored = hasMatchScore(match)
  const homeWon = scored && match.homeGoals > match.awayGoals
  const awayWon = scored && match.awayGoals > match.homeGoals

  return <button className="mx-match-card" type="button" onClick={() => onOpen(match.id)}
    aria-label={`Xem trận ${match.homeClub} gặp ${match.awayClub}, ${scored ? `${match.homeGoals}–${match.awayGoals}` : LABELS[match.status] ?? match.status}`}>
    <span className="mx-match-meta"><MatchStatus status={match.status} />
      {match.hasManualStats && <span className="mx-stat-tag">Có chỉ số cầu thủ</span>}</span>
    <span className="mx-match-main">
      <span className={`mx-team mx-team-home${awayWon ? ' mx-team-lost' : ''}${homeWon ? ' mx-team-won' : ''}`}>
        <span className="mx-team-name">{match.homeClub}</span><ClubCrest name={match.homeClub} />
      </span>
      <span className="mx-card-score"><strong>{scored ? `${match.homeGoals} – ${match.awayGoals}` : 'vs'}</strong>
        {!scored && <small>Chưa có tỉ số</small>}</span>
      <span className={`mx-team mx-team-away${homeWon ? ' mx-team-lost' : ''}${awayWon ? ' mx-team-won' : ''}`}>
        <ClubCrest name={match.awayClub} /><span className="mx-team-name">{match.awayClub}</span>
      </span>
    </span>
  </button>
}

export default MatchCard
