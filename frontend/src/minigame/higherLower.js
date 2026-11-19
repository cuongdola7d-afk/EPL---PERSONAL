export const BEST_SCORE_KEY = 'prismaxi-higher-best'
export const STAT_KEYS = ['ovr', 'goals', 'assists']
export const RECENT_LIMIT = 5

export const FOOT_LABELS = {
  LEFT: 'Trái',
  RIGHT: 'Phải',
  BOTH: 'Hai chân',
}

export const STATS = {
  ovr: {
    key: 'ovr',
    label: 'OVR',
    promptPrefix: 'có OVR ',
    promptHighlight: 'cao hơn',
    read: (player) => (Number.isInteger(player?.fc27Overall) ? player.fc27Overall : null),
  },
  goals: {
    key: 'goals',
    label: 'Bàn thắng',
    promptPrefix: 'có ghi ',
    promptHighlight: 'nhiều bàn hơn',
    read: (player) => (typeof player?.goals === 'number' && Number.isFinite(player.goals) ? player.goals : null),
  },
  assists: {
    key: 'assists',
    label: 'Kiến tạo',
    promptPrefix: 'có ',
    promptHighlight: 'kiến tạo nhiều hơn',
    read: (player) => (typeof player?.assists === 'number' && Number.isFinite(player.assists) ? player.assists : null),
  },
}

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/
const VIETNAM_DATE_FORMATTER = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Ho_Chi_Minh',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

function nonEmptyString(value) {
  return typeof value === 'string' && value.trim().length > 0
}

export function formatFoot(foot) {
  return FOOT_LABELS[foot] ?? null
}

export function vietnamDateParts(now = new Date()) {
  const parts = VIETNAM_DATE_FORMATTER.formatToParts(now)
  const year = Number(parts.find((part) => part.type === 'year')?.value)
  const month = Number(parts.find((part) => part.type === 'month')?.value)
  const day = Number(parts.find((part) => part.type === 'day')?.value)
  return { year, month, day }
}

export function calculateAge(birthDate, now = new Date()) {
  if (typeof birthDate !== 'string') return null
  const match = DATE_PATTERN.exec(birthDate.trim())
  if (!match) return null
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const probe = new Date(Date.UTC(year, month - 1, day))
  if (probe.getUTCFullYear() !== year || probe.getUTCMonth() + 1 !== month || probe.getUTCDate() !== day) {
    return null
  }
  const today = vietnamDateParts(now)
  let age = today.year - year
  if (today.month < month || (today.month === month && today.day < day)) {
    age -= 1
  }
  return age >= 0 ? age : null
}

export function cardTier(overall) {
  if (overall >= 90) return 'prism'
  if (overall >= 85) return 'rare'
  if (overall >= 78) return 'gold'
  if (overall >= 70) return 'silver'
  return 'bronze'
}

export function statValue(player, statKey) {
  const stat = STATS[statKey]
  return stat ? stat.read(player) : null
}

export function isEligiblePlayer(player, now = new Date()) {
  return player !== null &&
    typeof player === 'object' &&
    typeof player.id === 'number' &&
    Number.isFinite(player.id) &&
    nonEmptyString(player.name) &&
    nonEmptyString(player.club) &&
    Number.isInteger(player.fc27Overall) &&
    player.fc27Overall >= 1 &&
    player.fc27Overall <= 99 &&
    nonEmptyString(player.nationality) &&
    calculateAge(player.birthDate, now) !== null &&
    Number.isInteger(player.heightCm) &&
    player.heightCm > 0 &&
    formatFoot(player.preferredFoot) !== null &&
    Number.isInteger(player.shirtNumber) &&
    player.shirtNumber >= 1 &&
    nonEmptyString(player.primaryPosition)
}

export function eligiblePlayers(players, now = new Date()) {
  if (!Array.isArray(players)) return []
  const seen = new Set()
  const result = []
  for (const player of players) {
    if (!isEligiblePlayer(player, now) || seen.has(player.id)) continue
    seen.add(player.id)
    result.push(player)
  }
  return result
}

function pickRandom(items, random) {
  const raw = typeof random === 'function' ? random() : Math.random()
  const bounded = Number.isFinite(raw) ? Math.min(Math.max(raw, 0), 0.999999999999) : 0
  return items[Math.floor(bounded * items.length)]
}

function candidatesFor(pool, left, statKey) {
  const leftVal = statValue(left, statKey)
  if (leftVal === null) return []
  if ((statKey === 'goals' || statKey === 'assists') && leftVal < 1) return []
  return pool.filter((candidate) => {
    if (candidate.id === left.id) return false
    const rightVal = statValue(candidate, statKey)
    return rightVal !== null && rightVal !== leftVal
  })
}

function preferNonRecent(candidates, recentIds) {
  if (!recentIds.length || candidates.length <= 1) return candidates
  for (let count = Math.min(recentIds.length, RECENT_LIMIT); count >= 1; count -= 1) {
    const blocked = new Set(recentIds.slice(0, count))
    const filtered = candidates.filter((candidate) => !blocked.has(candidate.id))
    if (filtered.length > 0) return filtered
  }
  return candidates
}

function pushRecent(recentIds, ...ids) {
  const next = []
  for (const id of [...ids, ...recentIds]) {
    if (id != null && !next.includes(id)) next.push(id)
    if (next.length >= RECENT_LIMIT) break
  }
  return next
}

export function pickRound(pool, leftPlayer = null, recentIds = [], random = Math.random) {
  if (!Array.isArray(pool) || pool.length < 2) return null

  if (leftPlayer !== null) {
    const validStats = STAT_KEYS.filter((statKey) => candidatesFor(pool, leftPlayer, statKey).length > 0)
    if (!validStats.length) return null
    const stat = pickRandom(validStats, random)
    const candidates = candidatesFor(pool, leftPlayer, stat)
    const right = pickRandom(preferNonRecent(candidates, recentIds), random)
    return { left: leftPlayer, right, stat }
  }

  const validStats = STAT_KEYS.filter((statKey) =>
    pool.some((player) => candidatesFor(pool, player, statKey).length > 0))
  if (!validStats.length) return null

  const stat = pickRandom(validStats, random)
  const validLefts = pool.filter((player) => candidatesFor(pool, player, stat).length > 0)
  const left = pickRandom(preferNonRecent(validLefts, recentIds), random)
  const candidates = candidatesFor(pool, left, stat)
  const right = pickRandom(preferNonRecent(candidates, pushRecent(recentIds, left.id)), random)
  return { left, right, stat }
}

export function createGame(pool, best = 0, random = Math.random) {
  const round = pickRound(pool, null, [], random)
  if (!round) return null
  const safeBest = Number.isInteger(best) && best >= 0 ? best : 0
  return {
    left: round.left,
    right: round.right,
    stat: round.stat,
    recentIds: pushRecent([], round.right.id, round.left.id),
    score: 0,
    best: safeBest,
    startBest: safeBest,
    newBest: false,
    status: 'play',
    pick: null,
    result: null,
    round: 1,
    shifted: false,
  }
}

export function applyGuess(game, direction) {
  if (!game || game.status !== 'play') return game
  if (direction !== 'up' && direction !== 'down') return game
  const leftValue = statValue(game.left, game.stat)
  const rightValue = statValue(game.right, game.stat)
  if (leftValue === null || rightValue === null) return game

  const ok = direction === 'up' ? rightValue > leftValue : rightValue < leftValue
  const result = { ok, leftValue, rightValue, stat: game.stat }
  if (!ok) {
    return { ...game, status: 'over', pick: direction, result, shifted: false }
  }

  const score = game.score + 1
  const newBest = game.newBest || score > game.startBest
  const best = Math.max(game.best, score)
  return { ...game, score, best, newBest, status: 'reveal', pick: direction, result, shifted: false }
}

export function advance(game, pool, random = Math.random) {
  if (!game || game.status !== 'reveal') return game
  const nextLeft = game.right
  const recentBeforePick = pushRecent(game.recentIds ?? [], nextLeft.id, game.left.id)
  const next = pickRound(pool, nextLeft, recentBeforePick, random)
  if (!next) return null
  return {
    ...game,
    left: nextLeft,
    right: next.right,
    stat: next.stat,
    recentIds: pushRecent(recentBeforePick, next.right.id),
    status: 'play',
    pick: null,
    result: null,
    round: game.round + 1,
    shifted: true,
  }
}

function defaultStorage() {
  try {
    return typeof globalThis !== 'undefined' ? globalThis.localStorage : null
  } catch {
    return null
  }
}

export function readBest(storage = defaultStorage()) {
  try {
    if (!storage || typeof storage.getItem !== 'function') return 0
    const raw = storage.getItem(BEST_SCORE_KEY)
    if (raw === null || raw === undefined || raw === '') return 0
    const parsed = Number(raw)
    return Number.isInteger(parsed) && parsed >= 0 ? parsed : 0
  } catch {
    return 0
  }
}

export function saveBest(score, storage = defaultStorage()) {
  const safeScore = Number.isInteger(score) && score >= 0 ? score : 0
  const current = readBest(storage)
  if (safeScore <= current) return { best: current, updated: false }
  try {
    if (storage && typeof storage.setItem === 'function') {
      storage.setItem(BEST_SCORE_KEY, String(safeScore))
    }
  } catch {
    // Storage may be blocked in private mode; keep score in memory.
  }
  return { best: safeScore, updated: true }
}
