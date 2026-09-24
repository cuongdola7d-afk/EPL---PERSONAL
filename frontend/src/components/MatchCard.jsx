function MatchCard({ match, onOpen }) {
  const hasScore = match.homeGoals !== null && match.awayGoals !== null
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
      <button className="match-detail-button" type="button" onClick={() => onOpen(match.id)}
        aria-label={`Xem chi tiết trận ${match.homeClub} gặp ${match.awayClub}`}>
        Xem chi tiết cầu thủ <span aria-hidden="true">→</span>
      </button>
    </article>
  )
}

export default MatchCard
