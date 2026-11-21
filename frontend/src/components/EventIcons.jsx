import { matchPlayerValue } from '../utils/matchView.js'

export function GoalIcon({ className = '' }) {
  return (
    <svg className={`mx-ic mx-ic-goal${className ? ` ${className}` : ''}`} viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="11" fill="#fff" stroke="#1b2a30" strokeWidth="1.5" />
      <path d="M12 7.2l4.1 3-1.6 4.8h-5l-1.6-4.8z" fill="#1b2a30" />
      <path
        d="M12 7.2V2.6M16.1 10.2l4.4-1.4M14.5 15l2.7 3.8M9.5 15l-2.7 3.8M7.9 10.2L3.5 8.8"
        stroke="#1b2a30"
        strokeWidth="1.4"
      />
    </svg>
  )
}

export function AssistIcon({ className = '' }) {
  return (
    <svg className={`mx-ic mx-ic-ast${className ? ` ${className}` : ''}`} viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="11.5" fill="#46e2b5" />
      <path d="M4 7.5h5.2l.9 3.4 6.2 1.6c2.4.6 3.7 1.8 3.7 3.4v.6H4z" fill="#05241c" />
      <path d="M5.5 18.3v1.2M9 18.3v1.2M13 18.3v1.2M17 18.3v1.2" stroke="#05241c" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M10.6 12.4l1.2-1.6M12.8 13l1.2-1.6" stroke="#46e2b5" strokeWidth="1.1" strokeLinecap="round" />
    </svg>
  )
}

export function CardIcon({ color = 'yellow', className = '' }) {
  const tone = color === 'red' ? 'r' : 'y'
  return <span className={`mx-ic mx-ic-card mx-ic-card-${tone}${className ? ` ${className}` : ''}`} aria-hidden="true" />
}

export function SecondYellowIcon({ className = '' }) {
  return (
    <span className={`mx-ic mx-ic-2y${className ? ` ${className}` : ''}`} aria-hidden="true">
      <span className="mx-ic-card mx-ic-card-y" />
      <span className="mx-ic-card mx-ic-card-r" />
    </span>
  )
}

export function SubInIcon({ className = '' }) {
  return (
    <span className={`mx-ic mx-ic-sub mx-ic-sub-in${className ? ` ${className}` : ''}`} aria-hidden="true">
      <svg viewBox="0 0 10 10">
        <path d="M5 8.5v-7M2 4.5l3-3 3 3" fill="none" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </span>
  )
}

export function SubOutIcon({ className = '' }) {
  return (
    <span className={`mx-ic mx-ic-sub mx-ic-sub-out${className ? ` ${className}` : ''}`} aria-hidden="true">
      <svg viewBox="0 0 10 10">
        <path d="M5 1.5v7M2 5.5l3 3 3-3" fill="none" stroke="#fff" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    </span>
  )
}

export function playerEventSummary(player, evidenceStatus) {
  const goals = matchPlayerValue(player, 'goals', evidenceStatus) ?? 0
  const assists = matchPlayerValue(player, 'assists', evidenceStatus) ?? 0
  const yellowCards = matchPlayerValue(player, 'yellowCards', evidenceStatus) ?? 0
  const redCards = matchPlayerValue(player, 'redCards', evidenceStatus) ?? 0
  const parts = []
  if (goals > 0) parts.push(`${goals} bàn`)
  if (assists > 0) parts.push(`${assists} kiến tạo`)
  if (yellowCards >= 2 && redCards >= 1) {
    parts.push('2 thẻ vàng thành thẻ đỏ')
  } else {
    if (yellowCards > 0) parts.push(`${yellowCards} thẻ vàng`)
    if (redCards > 0) parts.push(`${redCards} thẻ đỏ`)
  }
  return parts.join(', ')
}

export function PlayerEventIcons({ player, evidenceStatus, className = '' }) {
  const goals = matchPlayerValue(player, 'goals', evidenceStatus) ?? 0
  const assists = matchPlayerValue(player, 'assists', evidenceStatus) ?? 0
  const yellowCards = matchPlayerValue(player, 'yellowCards', evidenceStatus) ?? 0
  const redCards = matchPlayerValue(player, 'redCards', evidenceStatus) ?? 0
  const isSecondYellow = yellowCards >= 2 && redCards >= 1
  const summary = playerEventSummary(player, evidenceStatus)
  if (!summary) return null

  return (
    <span className={`mx-ev-group${className ? ` ${className}` : ''}`} aria-label={summary} title={summary}>
      {goals > 0 && (
        <span className="mx-ev-item">
          <GoalIcon />
          {goals > 1 && <b className="mx-ev-n">×{goals}</b>}
        </span>
      )}
      {assists > 0 && (
        <span className="mx-ev-item">
          <AssistIcon />
          {assists > 1 && <b className="mx-ev-n">×{assists}</b>}
        </span>
      )}
      {isSecondYellow ? (
        <span className="mx-ev-item">
          <SecondYellowIcon />
        </span>
      ) : (
        <>
          {yellowCards > 0 && (
            <span className="mx-ev-item">
              <CardIcon color="yellow" />
              {yellowCards > 1 && <b className="mx-ev-n">×{yellowCards}</b>}
            </span>
          )}
          {redCards > 0 && (
            <span className="mx-ev-item">
              <CardIcon color="red" />
              {redCards > 1 && <b className="mx-ev-n">×{redCards}</b>}
            </span>
          )}
        </>
      )}
    </span>
  )
}
