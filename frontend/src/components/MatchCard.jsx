import { useEffect, useState } from 'react'
import { clubVisual } from '../utils/matchView.js'
import { formatLiveStatusBadge, resolveMatchDisplay } from '../utils/liveScores.js'
import MatchKickoff from './MatchKickoff.jsx'

const LIVE_STATUSES = new Set(['LIVE', 'IN_PLAY', 'PAUSED', 'EXTRA_TIME', 'PENALTY_SHOOTOUT'])

const LABELS = {
  FINISHED: 'Kết thúc',
  SCHEDULED: 'Sắp đá',
  TIMED: 'Sắp đá',
  POSTPONED: 'Hoãn',
  CANCELLED: 'Hủy',
  SUSPENDED: 'Tạm dừng',
  LIVE: 'Đang đá',
  IN_PLAY: 'Đang đá',
  PAUSED: 'Đang đá',
  EXTRA_TIME: 'Đang đá',
  PENALTY_SHOOTOUT: 'Đang đá',
  AWARDED: 'Kết quả xử lý',
}

export function matchStatusMeta(status, liveState = null, nowMs = Date.now()) {
  if (liveState?.status === 'LIVE') {
    return {
      tone: 'live',
      label: formatLiveStatusBadge(liveState, nowMs) ?? 'Đang đá',
      isLive: true,
    }
  }
  if (LIVE_STATUSES.has(status)) {
    return {
      tone: 'live',
      label: LABELS[status] ?? 'Đang đá',
      isLive: true,
    }
  }
  if (status === 'FINISHED' || liveState?.status === 'FT') {
    return {
      tone: 'done',
      label: 'Kết thúc',
      isLive: false,
    }
  }
  if (status === 'SCHEDULED' || status === 'TIMED') {
    return {
      tone: 'up',
      label: 'Sắp đá',
      isLive: false,
    }
  }
  if (status === 'POSTPONED') {
    return {
      tone: 'postponed',
      label: 'Hoãn',
      isLive: false,
    }
  }
  return {
    tone: 'cancelled',
    label: LABELS[status] ?? status,
    isLive: false,
  }
}

export function ClubCrest({ name, large = false }) {
  const { color, code } = clubVisual(name)
  return <span className={`mx-crest${large ? ' mx-crest-large' : ''}`} style={{ '--mx-club-color': color }} aria-hidden="true">{code}</span>
}

export function MatchStatus({ status, liveState = null }) {
  const isLiveTicking = liveState?.status === 'LIVE' &&
    Number.isInteger(liveState.minute) &&
    liveState.minute > 0 &&
    liveState.minute !== 45 &&
    liveState.minute !== 90
  const [nowMs, setNowMs] = useState(() => Date.now())

  useEffect(() => {
    setNowMs(Date.now())
    if (!isLiveTicking) {
      return undefined
    }
    const timer = setInterval(() => {
      setNowMs(Date.now())
    }, 5000)
    return () => clearInterval(timer)
  }, [isLiveTicking, liveState?.minute, liveState?.updatedAt, liveState?.minuteUpdatedAt])

  const { tone, label, isLive } = matchStatusMeta(status, liveState, nowMs)
  return (
    <span className={`mx-status mx-status-${tone}`}>
      {isLive && <span className="mx-live-dot" aria-hidden="true" />}
      {label}
    </span>
  )
}

function MatchCard({ match, season, onOpen, liveState = null, isFlashing = false }) {
  const display = resolveMatchDisplay(match, liveState)
  const { scored, homeGoals, awayGoals } = display
  const homeWon = scored && homeGoals > awayGoals
  const awayWon = scored && awayGoals > homeGoals
  const { tone, label: statusText } = matchStatusMeta(match.status, liveState)
  const statusAria = scored ? `${statusText}, ${homeGoals}–${awayGoals}` : statusText

  return (
    <button
      className={`mx-match-card mx-card-${tone}`}
      type="button"
      onClick={() => onOpen(match.id)}
      aria-label={`${match.homeClub} gặp ${match.awayClub}, ${statusAria}`}
    >
      <span className={`mx-match-meta${season === 2026 ? ' mx-match-meta-timed' : ''}`}>
        <MatchStatus status={match.status} liveState={liveState} />
        {season === 2026 && <MatchKickoff match={match} season={season} variant="card" />}
        {season === 2026 && <span className="mx-tz">Giờ Việt Nam</span>}
      </span>
      <span className="mx-match-main">
        <span className={`mx-team mx-team-home${awayWon ? ' mx-team-lost' : ''}${homeWon ? ' mx-team-won' : ''}`}>
          <span className="mx-team-name">{match.homeClub}</span>
          <ClubCrest name={match.homeClub} />
        </span>
        <span className={`mx-card-score${!scored ? ' mx-score-vs' : ''}${isFlashing ? ' mx-score-flash' : ''}`} aria-live="polite">
          <strong>{scored ? `${homeGoals} – ${awayGoals}` : 'vs'}</strong>
        </span>
        <span className={`mx-team mx-team-away${homeWon ? ' mx-team-lost' : ''}${awayWon ? ' mx-team-won' : ''}`}>
          <ClubCrest name={match.awayClub} />
          <span className="mx-team-name">{match.awayClub}</span>
        </span>
      </span>
    </button>
  )
}

export default MatchCard
