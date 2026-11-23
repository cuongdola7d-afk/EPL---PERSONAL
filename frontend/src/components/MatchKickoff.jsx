import { matchDateTimeLabel, matchKickoffParts } from '../utils/matchTime.js'
import './MatchKickoff.css'

export default function MatchKickoff({ match, season, legacyLabel, variant = 'inline' }) {
  if (season !== 2026) return legacyLabel ?? matchDateTimeLabel(match, season)
  const { instant, time, weekdayDate } = matchKickoffParts(match, season)

  if (variant === 'card') {
    return (
      <time className="mx-time" dateTime={instant?.toISOString()} title="Giờ Việt Nam">
        {time ? (
          <>
            <span className="mx-time-hh">{time}</span>
            <span className="mx-time-sep" aria-hidden="true">·</span>
            <span className="mx-time-dd">{weekdayDate}</span>
          </>
        ) : (
          <span className="mx-time-dd">{weekdayDate}</span>
        )}
      </time>
    )
  }

  return (
    <span className="match-kickoff">
      <time dateTime={instant?.toISOString()}>
        {time ? (
          <>
            <strong className="match-kickoff-hh">{time}</strong>
            <span className="match-kickoff-sep" aria-hidden="true"> · </span>
            <span className="match-kickoff-dd">{weekdayDate}</span>
          </>
        ) : (
          matchDateTimeLabel(match, season)
        )}
      </time>
      <small className="match-time-zone">Giờ Việt Nam</small>
    </span>
  )
}
