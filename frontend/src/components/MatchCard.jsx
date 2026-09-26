import { hasMatchScore } from '../utils/seasons.js'

function MatchCard({ match, onOpen }) {
  const hasScore = hasMatchScore(match)
  const statusLabel = {
    FINISHED: 'Đã kết thúc', SCHEDULED: 'Chưa diễn ra', POSTPONED: 'Bị hoãn',
    CANCELLED: 'Đã hủy', SUSPENDED: 'Tạm dừng', LIVE: 'Đang diễn ra',
    AWARDED: 'Kết quả xử lý',
  }[match.status] ?? match.status
  const dateLabel = match.date.split('-').reverse().join('/')

  return (
    <article className="match-card">
      <div className="match-meta">
        <span>Vòng {match.matchweek}</span>
        <time dateTime={match.date}>{dateLabel}</time>
        <span className={`match-status${match.status === 'FINISHED' ? ' finished' : ''}`}>{statusLabel}</span>
      </div>
      <div className="match-teams">
        <strong className="match-team home-team">{match.homeClub}</strong>
        <div className="match-score">
          {hasScore ? (
            <strong>{match.homeGoals} <span>:</span> {match.awayGoals}</strong>
          ) : (
            <span className="match-no-score">Chưa có tỉ số</span>
          )}
        </div>
        <strong className="match-team away-team">{match.awayClub}</strong>
      </div>
      {onOpen ? <button className="match-detail-button" type="button" onClick={() => onOpen(match.id)}
        aria-label={`Xem chi tiết trận ${match.homeClub} gặp ${match.awayClub}`}>
        Xem chi tiết cầu thủ <span aria-hidden="true">→</span>
      </button> : <p className="match-stat-unavailable">Chưa có thống kê cầu thủ.</p>}
    </article>
  )
}

export default MatchCard
