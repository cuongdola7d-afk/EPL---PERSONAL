import { useEffect, useMemo, useRef } from 'react'
import { isMatchDone, isMatchLiveNow, matchweekStartDate } from '../utils/matchView.js'

function prefersReducedMotion() {
  return typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export default function MatchweekStrip({
  weeks,
  currentWeek,
  activeWeek,
  matches,
  season,
  liveByFixture = null,
  onChangeWeek,
}) {
  const railRef = useRef(null)
  const hasCenteredRef = useRef(false)
  const weekIndex = weeks.indexOf(currentWeek)

  const items = useMemo(() => {
    const byWeek = new Map()
    for (const match of matches) {
      if (!byWeek.has(match.matchweek)) byWeek.set(match.matchweek, [])
      byWeek.get(match.matchweek).push(match)
    }
    return weeks.map((week) => {
      const fixtures = byWeek.get(week) ?? []
      const hasLive = fixtures.some((match) => isMatchLiveNow(match, liveByFixture))
      const isComplete = fixtures.length > 0 && fixtures.every((match) => isMatchDone(match, liveByFixture))
      const startDate = matchweekStartDate(fixtures, week, season)
      return { week, hasLive, isComplete, startDate }
    })
  }, [weeks, matches, season, liveByFixture])

  useEffect(() => {
    const rail = railRef.current
    if (!rail || weeks.length === 0) return
    const selected = rail.querySelector('[aria-selected="true"]')
    if (!selected) return
    const railRect = rail.getBoundingClientRect()
    const selRect = selected.getBoundingClientRect()
    const targetLeft = rail.scrollLeft + (selRect.left - railRect.left) - rail.clientWidth / 2 + selRect.width / 2
    const smooth = hasCenteredRef.current && !prefersReducedMotion()
    rail.scrollTo({ left: Math.max(0, targetLeft), behavior: smooth ? 'smooth' : 'auto' })
    hasCenteredRef.current = true
  }, [currentWeek, weeks.length])

  function handleKeyDown(event, index) {
    let nextIndex = null
    if (event.key === 'ArrowLeft') nextIndex = Math.max(0, index - 1)
    else if (event.key === 'ArrowRight') nextIndex = Math.min(weeks.length - 1, index + 1)
    else if (event.key === 'Home') nextIndex = 0
    else if (event.key === 'End') nextIndex = weeks.length - 1
    if (nextIndex === null || nextIndex === index) return
    event.preventDefault()
    const nextWeek = weeks[nextIndex]
    onChangeWeek(nextWeek)
    const buttons = railRef.current?.querySelectorAll('[role="tab"]')
    buttons?.[nextIndex]?.focus()
  }

  if (weeks.length === 0) return null

  return (
    <div className="mx-strip">
      <button
        className="mx-strip-arr"
        type="button"
        aria-label="Vòng trước"
        disabled={weekIndex <= 0}
        onClick={() => onChangeWeek(weeks[weekIndex - 1])}
      >
        ‹
      </button>
      <div
        className="mx-strip-rail"
        ref={railRef}
        role="tablist"
        aria-label="Chọn vòng đấu"
      >
        {items.map(({ week, hasLive, isComplete, startDate }, index) => {
          const isSelected = week === currentWeek
          const isCurrent = season === 2026 && week === activeWeek
          const classes = [
            'mx-gwc',
            isComplete && !isSelected ? 'mx-gwc-past' : '',
            isCurrent ? 'mx-gwc-cur' : '',
            isSelected ? 'mx-gwc-sel' : '',
          ].filter(Boolean).join(' ')

          return (
            <button
              key={week}
              type="button"
              role="tab"
              aria-selected={isSelected}
              tabIndex={isSelected ? 0 : -1}
              className={classes}
              aria-label={startDate ? `Vòng ${week}, bắt đầu ${startDate}` : `Vòng ${week}`}
              onClick={() => onChangeWeek(week)}
              onKeyDown={(event) => handleKeyDown(event, index)}
            >
              <b>
                GW{week}
                {hasLive && <i className="mx-gwc-ld" title="Đang có trận" aria-hidden="true" />}
              </b>
              <small aria-hidden={!startDate || undefined}>{startDate || '\u00A0'}</small>
            </button>
          )
        })}
      </div>
      <button
        className="mx-strip-arr"
        type="button"
        aria-label="Vòng sau"
        disabled={weekIndex < 0 || weekIndex >= weeks.length - 1}
        onClick={() => onChangeWeek(weeks[weekIndex + 1])}
      >
        ›
      </button>
    </div>
  )
}
