import { compareMatchSchedule, formatShortDayMonth, kickoffInstant, matchDayKey, matchKickoffParts } from './matchTime.js'
import { resolveMatchDisplay } from './liveScores.js'

const COLORS = [
  ['arsenal', '#e0413a'], ['liverpool', '#c8102e'], ['manchester city', '#5ba3d9'],
  ['manchester united', '#da291c'], ['chelsea', '#1f4fa3'], ['newcastle', '#4a545b'],
  ['aston villa', '#7b1d4a'], ['everton', '#1c5bb0'], ['bournemouth', '#ad252f'],
  ['brentford', '#c92834'], ['brighton', '#2362b0'], ['crystal palace', '#2d5eaa'],
  ['fulham', '#48535e'], ['leeds', '#e0b52c'], ['sunderland', '#c72d37'],
  ['nottingham forest', '#d42329'], ['tottenham', '#29436b'], ['ipswich', '#315ba5'],
  ['hull', '#d89720'], ['coventry', '#58a5d0'],
]

const CODES = [
  ['bournemouth', 'BOU'], ['arsenal', 'ARS'], ['aston villa', 'AVL'],
  ['brentford', 'BRE'], ['brighton', 'BHA'], ['chelsea', 'CHE'],
  ['crystal palace', 'CRY'], ['everton', 'EVE'], ['fulham', 'FUL'],
  ['hull', 'HUL'], ['ipswich', 'IPS'], ['leeds', 'LEE'],
  ['liverpool', 'LIV'], ['manchester city', 'MCI'], ['manchester united', 'MUN'],
  ['newcastle', 'NEW'], ['nottingham forest', 'NFO'], ['sunderland', 'SUN'],
  ['tottenham', 'TOT'], ['coventry', 'COV'],
]

export function shortPlayerName(name) {
  const parts = name.trim().split(/\s+/).filter(Boolean)
  return parts.length < 2 ? parts.join('') : `${Array.from(parts[0])[0]}. ${parts.slice(1).join(' ')}`
}

export function clubVisual(name) {
  const lower = name.toLowerCase()
  return {
    color: COLORS.find(([part]) => lower.includes(part))?.[1] ?? '#487783',
    code: CODES.find(([part]) => lower.includes(part))?.[1] ??
      name.split(/\s+/).filter(Boolean).slice(0, 3).map((part) => part[0]).join('').toUpperCase(),
  }
}

const TWO_DAYS_MS = 2 * 24 * 60 * 60 * 1000
const LIVE_DB_STATUSES = new Set(['LIVE', 'IN_PLAY', 'PAUSED', 'EXTRA_TIME', 'PENALTY_SHOOTOUT'])
const DEFERRED_STATUSES = new Set(['POSTPONED', 'CANCELLED'])

function lookupLiveState(liveByFixtureId, fixtureId) {
  if (!liveByFixtureId || fixtureId == null) return null
  if (liveByFixtureId instanceof Map) {
    return liveByFixtureId.get(fixtureId) ?? liveByFixtureId.get(String(fixtureId)) ?? null
  }
  return liveByFixtureId[fixtureId] ?? liveByFixtureId[String(fixtureId)] ?? null
}

function isMatchLive(match, liveByFixtureId) {
  if (LIVE_DB_STATUSES.has(match.status)) return true
  const liveState = lookupLiveState(liveByFixtureId, match.id)
  return liveState?.status === 'LIVE'
}

function isMatchFinished(match) {
  return match.status === 'FINISHED'
}

function isMatchUnplayed(match) {
  return !isMatchFinished(match) && !DEFERRED_STATUSES.has(match.status)
}

function parseKickoffMs(match) {
  const timestamp = match.kickoffUtc ?? match.utcDate
  if (typeof timestamp === 'string' && timestamp.trim()) {
    const parsed = Date.parse(timestamp)
    if (Number.isFinite(parsed)) return parsed
  }
  if (typeof match.date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(match.date)) {
    const parsed = Date.parse(`${match.date}T00:00:00Z`)
    if (Number.isFinite(parsed)) return parsed
  }
  return null
}

function resolveNowMs(now) {
  if (now instanceof Date) return now.getTime()
  if (typeof now === 'number') return now
  if (typeof now === 'string') return Date.parse(now)
  return Date.now()
}

export function preferredMatchweek(matches, now = new Date(), liveByFixtureId = null) {
  if (!Array.isArray(matches) || matches.length === 0) return 1

  const byWeek = new Map()
  for (const match of matches) {
    if (!byWeek.has(match.matchweek)) byWeek.set(match.matchweek, [])
    byWeek.get(match.matchweek).push(match)
  }
  const weeks = [...byWeek.keys()].sort((a, b) => a - b)

  // 1. Any gameweek with a LIVE match (from DB status or FPL live state).
  const liveWeek = weeks.find((week) =>
    byWeek.get(week).some((match) => isMatchLive(match, liveByFixtureId)))
  if (liveWeek != null) return liveWeek

  // 2. Smallest in-progress gameweek (has at least one FINISHED and at least one unplayed match).
  const inProgressWeek = weeks.find((week) => {
    const fixtures = byWeek.get(week)
    return fixtures.some(isMatchFinished) && fixtures.some(isMatchUnplayed)
  })
  if (inProgressWeek != null) return inProgressWeek

  // 3. Latest completed gameweek, unless the next gameweek starts within 2 days.
  const complete = weeks.filter((week) => {
    const fixtures = byWeek.get(week)
    return fixtures.length > 0 &&
      fixtures.some(isMatchFinished) &&
      fixtures.every((match) => isMatchFinished(match) || DEFERRED_STATUSES.has(match.status))
  })
  const latestComplete = complete.at(-1)
  if (latestComplete != null) {
    const nextWeek = weeks.find((week) => week > latestComplete)
    if (nextWeek != null) {
      const nowMs = resolveNowMs(now)
      const kickoffs = byWeek.get(nextWeek)
        .map(parseKickoffMs)
        .filter((ms) => ms != null)
      if (Number.isFinite(nowMs) && kickoffs.length > 0) {
        const earliestKickoff = Math.min(...kickoffs)
        if (earliestKickoff - nowMs <= TWO_DAYS_MS) {
          return nextWeek
        }
      }
    }
    return latestComplete
  }

  return weeks[0] ?? 1
}

export function isMatchLiveNow(match, liveByFixtureId = null) {
  return isMatchLive(match, liveByFixtureId)
}

export function isMatchDone(match, liveByFixtureId = null) {
  if (match.status === 'FINISHED') return true
  const liveState = lookupLiveState(liveByFixtureId, match.id)
  return liveState?.status === 'FT'
}

/**
 * Computes the earliest Vietnam start date (dd/mm) for a given matchweek from match kickoffs.
 * Returns '' if no match in that week has a valid date.
 */
export function matchweekStartDate(matches, week, season = 2026) {
  if (!Array.isArray(matches)) return ''
  const weekMatches = matches.filter((match) => match.matchweek === week)
  if (weekMatches.length === 0) return ''

  let earliestInstant = null
  let earliestInstantMatch = null
  for (const match of weekMatches) {
    const instant = kickoffInstant(match, season)
    if (instant && (earliestInstant === null || instant < earliestInstant)) {
      earliestInstant = instant
      earliestInstantMatch = match
    }
  }
  if (earliestInstantMatch) {
    return formatShortDayMonth(matchDayKey(earliestInstantMatch, season))
  }

  const dayKeys = weekMatches
    .map((match) => matchDayKey(match, season))
    .filter((key) => typeof key === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(key))
    .sort()
  return dayKeys.length > 0 ? formatShortDayMonth(dayKeys[0]) : ''
}

/**
 * Builds the 1 or 2 quick-info chips displayed in the 2026/27 schedule hero.
 * Returns [] for legacy seasons or empty match lists.
 */
export function buildScheduleHeroChips(matches, { season = 2026, now = new Date(), liveByFixture = null } = {}) {
  if (season !== 2026 || !Array.isArray(matches) || matches.length === 0) return []

  const activeWeek = preferredMatchweek(matches, now, liveByFixture)
  const activeMatches = matches.filter((match) => match.matchweek === activeWeek)
  const doneCount = activeMatches.filter((match) => isMatchDone(match, liveByFixture)).length
  const chips = [
    {
      kind: 'week',
      tone: 'accent',
      text: `Vòng ${activeWeek} · ${doneCount}/${activeMatches.length} trận đã đá`,
    },
  ]

  const liveMatches = matches
    .filter((match) => isMatchLive(match, liveByFixture))
    .sort((a, b) => compareMatchSchedule(a, b, season))

  if (liveMatches.length > 0) {
    const first = liveMatches[0]
    const liveState = lookupLiveState(liveByFixture, first.id)
    const display = resolveMatchDisplay(first, liveState)
    const homeCode = first.homeClubShort || clubVisual(first.homeClub).code
    const awayCode = first.awayClubShort || clubVisual(first.awayClub).code
    const score = display.scored ? `${display.homeGoals}–${display.awayGoals}` : 'vs'
    const extra = liveMatches.length > 1 ? ` +${liveMatches.length - 1} trận` : ''
    chips.push({
      kind: 'live',
      tone: 'live',
      text: `Đang đá: ${homeCode} ${score} ${awayCode}${extra}`,
    })
    return chips
  }

  const nowMs = resolveNowMs(now)
  const upcoming = matches
    .filter((match) => (match.status === 'SCHEDULED' || match.status === 'TIMED') && !isMatchDone(match, liveByFixture))
    .map((match) => ({ match, parts: matchKickoffParts(match, season) }))
    .filter(({ parts }) => parts.instant !== null)
    .sort((a, b) => a.parts.instant - b.parts.instant || a.match.id - b.match.id)

  const nextUpcoming = (Number.isFinite(nowMs)
    ? upcoming.find(({ parts }) => parts.instant.getTime() >= nowMs)
    : null) ?? upcoming[0] ?? null

  if (nextUpcoming) {
    const { time, weekdayDate } = nextUpcoming.parts
    chips.push({
      kind: 'next',
      tone: 'up',
      text: `Trận tiếp: ${time} ${weekdayDate}`,
    })
  }

  return chips
}

export function matchPlayerState(player) {
  if (player.participationStatus === 'PLAYED') return 'played'
  if (player.participationStatus === 'DID_NOT_PLAY') return 'did-not-play'
  const minutes = player.minutes ?? player.inferred?.minutes
  if (minutes === 0) return 'did-not-play'
  if (minutes != null) return 'played'
  return 'unknown'
}

export function matchPlayerValue(player, field, evidenceStatus) {
  return player[field] ?? (evidenceStatus === 'VERIFIED' ? player.inferred?.[field] : null) ?? null
}

export function matchRating(player) {
  if (player.rating == null) return null
  const value = Number(player.rating)
  return Number.isFinite(value) ? value : null
}

// The API has no starting-XI flag. This produces a clearly labelled visual estimate.
export function estimatedPitchPlayers(players, evidenceStatus) {
  const played = players.filter((player) => matchPlayerState(player) === 'played')
  const ranked = [...played].sort((a, b) =>
    (matchPlayerValue(b, 'minutes', evidenceStatus) ?? -1) -
    (matchPlayerValue(a, 'minutes', evidenceStatus) ?? -1) || a.playerId - b.playerId)
  const goalkeeper = ranked.find((player) => player.position === 'G')
  const selected = (goalkeeper ? [goalkeeper, ...ranked.filter((player) => player !== goalkeeper)] : ranked).slice(0, 11)
  const selectedIds = new Set(selected.map((player) => player.playerId))
  return { selected, bench: players.filter((player) => !selectedIds.has(player.playerId)) }
}

export function pitchPositions(players, side) {
  const positions = ['G', 'D', 'M', 'F']
  const columns = side === 'home' ? [8, 20, 32, 44] : [92, 80, 68, 56]
  const slots = [1, 4, 3, 3]
  const remaining = [...players]
  return positions.flatMap((position, column) => {
    const group = remaining.filter((player) => player.position === position).slice(0, slots[column])
    for (const player of group) remaining.splice(remaining.indexOf(player), 1)
    while (group.length < slots[column] && remaining.length) group.push(remaining.shift())
    return group.map((player, index) => ({
      player, x: columns[column], y: (index + 1) * 100 / (group.length + 1),
    }))
  })
}
