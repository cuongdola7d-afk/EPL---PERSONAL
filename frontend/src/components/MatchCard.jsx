function MatchCard({ match }) {
  const hasScore = match.homeGoals !== null && match.awayGoals !== null
  const statusLabel = match.status === 'FINISHED' ? 'Đã kết thúc' : 'Chưa diễn ra'
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
    </article>
  )
}

export default MatchCard
