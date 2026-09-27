import { getInitials } from '../utils/initials.js'
import { playerDetailHash } from '../utils/playerRoute.js'

function PlayerCard({ player, positionLabel, season }) {
  return (
    <a className="player-card" href={playerDetailHash(player.id, season)} aria-label={`Xem chi tiết ${player.name}, mùa ${season}`}>
      <div className="player-card-top">
        <span className="player-id">#{String(player.id).padStart(2, '0')}</span>
        <span className="player-avatar" aria-hidden="true">{getInitials(player.name)}</span>
      </div>
      <div className="player-card-main">
        <span className="position-badge">{positionLabel}</span>
        <h3>{player.name}</h3>
        <p className="player-club"><span className="location-dot" aria-hidden="true" />{player.club}</p>
      </div>
      <div className="player-stats">
        <div><strong aria-label={player.goals === null ? 'Chưa có dữ liệu' : undefined}>{player.goals ?? '—'}</strong><span>Bàn thắng</span></div>
        <div><strong aria-label={player.assists === null ? 'Chưa có dữ liệu' : undefined}>{player.assists ?? '—'}</strong><span>Kiến tạo</span></div>
      </div>
    </a>
  )
}

export default PlayerCard
