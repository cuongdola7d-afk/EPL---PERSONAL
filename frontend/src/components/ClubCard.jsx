import { clubVisual } from '../utils/matchView.js'
import { clubDetailHash } from '../utils/clubRoute.js'

export function HomeCrest({ name }) {
  const { code, color } = clubVisual(name)
  return <span className="cx-crest" style={{ '--crest-color': color }} aria-hidden="true">{code}</span>
}
function ClubCard({ club, season, query = '' }) {
  const match = query.trim()
  const index = match ? club.name.toLocaleLowerCase('vi').indexOf(match.toLocaleLowerCase('vi')) : -1
  return <a className="cx-club-card" href={clubDetailHash(club.id, { season })} aria-label={`Xem câu lạc bộ ${club.name}`}>
    <HomeCrest name={club.name} />
    <h3>{index < 0 ? club.name : <>{club.name.slice(0, index)}<mark>{club.name.slice(index, index + match.length)}</mark>{club.name.slice(index + match.length)}</>}</h3>
    <span className="cx-club-go">Xem câu lạc bộ <span aria-hidden="true">›</span></span>
  </a>
}
export default ClubCard
