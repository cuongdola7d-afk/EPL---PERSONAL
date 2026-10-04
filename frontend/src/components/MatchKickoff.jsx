import { kickoffInstant, matchDateTimeLabel } from '../utils/matchTime.js'
import './MatchKickoff.css'

export default function MatchKickoff({ match, season, legacyLabel }) {
  if (season !== 2026) return legacyLabel ?? matchDateTimeLabel(match, season)
  const instant = kickoffInstant(match, season)
  return <span className="match-kickoff">
    <time dateTime={instant?.toISOString()}>{matchDateTimeLabel(match, season)}</time>
    <small className="match-time-zone">Giờ Việt Nam</small>
  </span>
}
