import { getInitials } from '../utils/initials.js'

function ClubCard({ club, index }) {
  return (
    <article className="club-card">
      <div className="club-card-top">
        <span className="club-number">{String(index + 1).padStart(2, '0')}</span>
        <span className="club-monogram" aria-hidden="true">{getInitials(club.name)}</span>
      </div>
      <div className="club-card-bottom">
        <div>
          <h3>{club.name}</h3>
          <p><span className="location-dot" aria-hidden="true" />{club.city}</p>
        </div>
      </div>
    </article>
  )
}

export default ClubCard
