export const WORDLE_MAX_TURNS = 6
export const WORDLE_HINT_UNLOCK_TURNS = 4
export const WORDLE_MIN_LENGTH = 4
export const TELEX_DELETE_HOLD_MS = 150
export const TELEX_BURST_MS = 25
export const WORDLE_FLIP_DURATION_MS = 320
export const WORDLE_FLIP_TOTAL_MS = 600
export const WORDLE_DEBUG_MAX_ENTRIES = 500
export const WORDLE_SYNC_MAX_RETRIES = 3
export const WORDLE_SYNC_RETRY_DELAYS_MS = [600, 1500, 3000]

export function wordleFlipDelayMs(tileIndex, guessLength) {
  if (!Number.isFinite(tileIndex) || tileIndex <= 0 || !Number.isFinite(guessLength) || guessLength <= 1) {
    return 0
  }
  const budget = Math.max(0, WORDLE_FLIP_TOTAL_MS - WORDLE_FLIP_DURATION_MS)
  const step = Math.min(93, Math.floor(budget / (guessLength - 1)))
  return tileIndex * step
}

/**
 * Decodes the lightweight XOR + Base64 answer key produced by WordleKeyCodec.java.
 * First 4 bytes are the puzzleId-derived mask; remaining bytes are XORed ASCII A-Z letters.
 */
export function decodeWordleKey(key) {
  if (typeof key !== 'string' || !key.trim()) return null
  try {
    let bytes
    if (typeof atob === 'function') {
      const bin = atob(key)
      bytes = new Uint8Array(bin.length)
      for (let i = 0; i < bin.length; i += 1) bytes[i] = bin.charCodeAt(i)
    } else if (typeof Buffer !== 'undefined') {
      bytes = Buffer.from(key, 'base64')
    } else {
      return null
    }
    if (bytes.length <= 4) return null
    let out = ''
    for (let i = 0; i < bytes.length - 4; i += 1) {
      const mix = bytes[i % 4] ^ ((i * 31 + 17) & 0xFF)
      out += String.fromCharCode(bytes[4 + i] ^ mix)
    }
    return /^[A-Z]+$/.test(out) ? out : null
  } catch {
    return null
  }
}

/**
 * Exact two-pass Wordle scorer matching backend WordleScorer.java.
 * Supports guesses of length 1..N against an answer of length N.
 */
export function scoreWordleGuess(guessLetters, answerLetters) {
  if (typeof guessLetters !== 'string' || typeof answerLetters !== 'string') {
    throw new Error('Guess and answer must be strings')
  }
  const l = guessLetters.length
  const n = answerLetters.length
  if (l < 1 || l > n) {
    throw new Error('Guess length must be between 1 and answer length')
  }
  const result = new Array(l).fill('M')
  const remaining = new Int32Array(26)

  for (let i = 0; i < l; i += 1) {
    const g = guessLetters.charCodeAt(i)
    const a = answerLetters.charCodeAt(i)
    if (g < 65 || g > 90 || a < 65 || a > 90) {
      throw new Error('Letters must be uppercase A-Z')
    }
    if (g === a) {
      result[i] = 'H'
    } else {
      remaining[a - 65] += 1
    }
  }
  for (let i = l; i < n; i += 1) {
    const a = answerLetters.charCodeAt(i)
    if (a < 65 || a > 90) {
      throw new Error('Letters must be uppercase A-Z')
    }
    remaining[a - 65] += 1
  }

  for (let i = 0; i < l; i += 1) {
    if (result[i] === 'H') continue
    const idx = guessLetters.charCodeAt(i) - 65
    if (remaining[idx] > 0) {
      result[i] = 'N'
      remaining[idx] -= 1
    }
  }
  return result.join('')
}

/**
 * Validates a guess locally in the exact same order as WordleService.applyGuessTimed
 * so invalid guesses shake and show a toast instantly with zero network calls.
 */
export function validateWordleGuessLocally({
  letters,
  maxLength,
  dictionaryByLetters,
  guessedLetters = [],
  finished = false,
}) {
  if (finished) return { ok: false, code: 'GAME_FINISHED' }
  const normalized = typeof letters === 'string' ? letters.trim().toUpperCase() : ''
  if (normalized.length > 0 && !/^[A-Z]+$/.test(normalized)) {
    return { ok: false, code: 'INVALID_CHARACTERS' }
  }
  if (normalized.length < WORDLE_MIN_LENGTH) {
    return { ok: false, code: 'TOO_SHORT' }
  }
  if (normalized.length > maxLength) {
    return { ok: false, code: 'TOO_LONG' }
  }
  const entry = dictionaryByLetters?.get?.(normalized) ?? null
  if (!entry) {
    return { ok: false, code: 'NOT_A_PLAYER' }
  }
  if (guessedLetters.includes(normalized)) {
    return { ok: false, code: 'ALREADY_GUESSED' }
  }
  if (guessedLetters.length >= WORDLE_MAX_TURNS) {
    return { ok: false, code: 'GAME_FINISHED' }
  }
  return { ok: true, normalized, entry }
}

/**
 * FIFO background sync queue for client-scored guesses:
 * - Sends turns strictly in order, allowing the player to type the next turn immediately.
 * - Retries transient network / 5xx errors up to maxRetries with increasing delays.
 * - Verifies server feedback/status against client result; on mismatch or rejection, clears
 *   the queue and notifies onMismatchOrRejected so the UI resyncs from /today.
 */
export function createGuessSyncQueue({
  sendGuess,
  onSynced,
  onMismatchOrRejected,
  onPendingChange,
  maxRetries = WORDLE_SYNC_MAX_RETRIES,
  retryDelaysMs = WORDLE_SYNC_RETRY_DELAYS_MS,
} = {}) {
  const queue = []
  let running = false
  let cancelled = false
  let retryTimer = null
  let idleWaiters = []

  const notifyPending = () => {
    onPendingChange?.(queue.length + (running ? 1 : 0))
  }

  const resolveIdleIfDone = () => {
    if (!running && queue.length === 0 && idleWaiters.length > 0) {
      const waiters = idleWaiters
      idleWaiters = []
      for (const resolve of waiters) resolve()
    }
  }

  const waitDelay = ms => new Promise(resolve => {
    retryTimer = setTimeout(() => {
      retryTimer = null
      resolve()
    }, ms)
  })

  const isRetryableError = err => !err?.status || err.status >= 500

  async function drain() {
    if (running || cancelled) return
    while (!cancelled && queue.length > 0) {
      const item = queue.shift()
      running = true
      notifyPending()

      let attempt = 0
      let serverData = null
      let lastError = null

      while (!cancelled) {
        try {
          serverData = await sendGuess(item)
          lastError = null
          break
        } catch (err) {
          lastError = err
          if (cancelled) break
          if (!isRetryableError(err) || attempt >= maxRetries) break
          const delay = retryDelaysMs[Math.min(attempt, retryDelaysMs.length - 1)] ?? 1000
          attempt += 1
          await waitDelay(delay)
        }
      }

      if (cancelled) {
        running = false
        notifyPending()
        resolveIdleIfDone()
        return
      }

      if (lastError) {
        queue.length = 0
        running = false
        notifyPending()
        await onMismatchOrRejected?.('rejected', lastError, item)
        resolveIdleIfDone()
        return
      }

      const serverGuess = serverData?.guesses?.[item.turn - 1]
      const matches = !!serverGuess &&
        serverGuess.letters === item.letters &&
        serverGuess.feedback === item.feedback &&
        (item.status === 'PLAYING' || serverData.status === item.status)

      if (!matches) {
        queue.length = 0
        running = false
        notifyPending()
        await onMismatchOrRejected?.('mismatch', null, item, serverData)
        resolveIdleIfDone()
        return
      }

      onSynced?.(serverData, item)
      running = false
      notifyPending()
    }
    resolveIdleIfDone()
  }

  return {
    enqueue(item) {
      if (cancelled) return
      queue.push(item)
      notifyPending()
      drain()
    },
    pendingCount() {
      return queue.length + (running ? 1 : 0)
    },
    waitUntilIdle() {
      if (!running && queue.length === 0) return Promise.resolve()
      return new Promise(resolve => { idleWaiters.push(resolve) })
    },
    cancel() {
      cancelled = true
      queue.length = 0
      if (retryTimer !== null) {
        clearTimeout(retryTimer)
        retryTimer = null
      }
      running = false
      notifyPending()
      resolveIdleIfDone()
    },
  }
}

export function isWordleDebugEnabled(hash = '', search = '', storage = null) {
  const hasDebugParam = query => {
    if (typeof query !== 'string' || !query) return false
    const idx = query.indexOf('?')
    const raw = idx >= 0 ? query.slice(idx + 1) : query
    if (!raw) return false
    return new URLSearchParams(raw).get('debug') === '1'
  }
  if (hasDebugParam(hash) || hasDebugParam(search)) return true
  try {
    return storage?.getItem?.('wdDebug') === '1'
  } catch {
    return false
  }
}

export function createWordleDebugLog(maxEntries = WORDLE_DEBUG_MAX_ENTRIES) {
  const limit = Math.max(1, maxEntries)
  const buffer = []
  return {
    push(entry) {
      if (!entry || typeof entry !== 'object') return
      if (buffer.length >= limit) buffer.shift()
      buffer.push(entry)
    },
    entries() {
      return buffer.slice()
    },
    toJson() {
      return JSON.stringify(buffer, null, 2)
    },
    clear() {
      buffer.length = 0
    },
  }
}

const FEEDBACK_STATE = { H: 'hit', N: 'near', M: 'miss' }
const STATE_RANK = { miss: 1, near: 2, hit: 3 }
const FEEDBACK_EMOJI = { H: '🟩', N: '🟨', M: '⬛' }
const WEEKDAYS_VI = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

const TONE_KEY_BY_MARK = {
  '\u0301': 'S', // sắc
  '\u0300': 'F', // huyền
  '\u0309': 'R', // hỏi
  '\u0303': 'X', // ngã
  '\u0323': 'J', // nặng
}

export function wordShape(word) {
  if (typeof word !== 'string' || !word.trim()) return []
  return word.trim().split(/\s+/).map(part => part.length)
}

/**
 * Splits a board row into tile groups according to the answer's word shape (e.g. [3, 4]).
 * All 6 rows (submitted, active, and empty) share the answer's shape so columns always align.
 */
export function buildRowWordGroups({ shape = null, letters = '', feedback = null, length = 0 }) {
  const effectiveShape = Array.isArray(shape) && shape.length ? shape : [length]
  let offset = 0
  return effectiveShape.map(groupLen => {
    const tiles = []
    for (let i = 0; i < groupLen; i += 1) {
      const char = letters[offset] ?? ''
      let state
      if (feedback !== null) {
        state = offset < feedback.length
          ? (FEEDBACK_STATE[feedback[offset]] ?? 'miss')
          : 'pad'
      } else {
        state = char ? 'filled' : 'empty'
      }
      tiles.push({ index: offset, letter: state === 'pad' ? '' : char, state })
      offset += 1
    }
    return tiles
  })
}

export function highlightWordPrefix(word, input) {
  const prefix = String(input || '').trim().toUpperCase()
  if (!prefix || typeof word !== 'string') {
    return { matched: '', rest: word || '' }
  }
  let seen = 0
  let cut = 0
  for (let i = 0; i < word.length && seen < prefix.length; i += 1) {
    if (word[i] !== ' ') seen += 1
    cut = i + 1
  }
  return { matched: word.slice(0, cut), rest: word.slice(cut) }
}

export function filterWordleSuggestions(dictionary, input, guessedLetters = [], limit = 5, maxLength = null) {
  const prefix = String(input || '').trim().toUpperCase()
  if (!prefix || !Array.isArray(dictionary)) return []
  const used = new Set(guessedLetters)
  return dictionary
    .filter(entry =>
      entry &&
      typeof entry.letters === 'string' &&
      entry.letters.length >= WORDLE_MIN_LENGTH &&
      (maxLength == null || entry.letters.length <= maxLength) &&
      entry.letters.startsWith(prefix) &&
      !used.has(entry.letters)
    )
    .sort((a, b) => a.letters.localeCompare(b.letters))
    .slice(0, limit)
    .map(entry => ({ ...entry, ...highlightWordPrefix(entry.word, prefix) }))
}

export function keyboardKeyStates(guesses) {
  const states = {}
  if (!Array.isArray(guesses)) return states
  for (const guess of guesses) {
    if (!guess?.letters || !guess?.feedback) continue
    for (let i = 0; i < guess.letters.length; i += 1) {
      const letter = guess.letters[i]
      const state = FEEDBACK_STATE[guess.feedback[i]]
      if (!letter || !state) continue
      if (!states[letter] || STATE_RANK[state] > STATE_RANK[states[letter]]) {
        states[letter] = state
      }
    }
  }
  return states
}

export function buildShareText({ puzzleNumber, status, guesses, shape = null }) {
  const scoreLabel = status === 'WON' ? `${guesses.length}/${WORDLE_MAX_TURNS}` : `X/${WORDLE_MAX_TURNS}`
  const header = `PrismaXI Wordle #${puzzleNumber} ${scoreLabel}`
  if (!Array.isArray(guesses) || !guesses.length) return header
  const rows = guesses.map(guess => {
    const groups = Array.isArray(shape) && shape.length
      ? shape
      : (wordShape(guess.word).length ? wordShape(guess.word) : [guess.feedback.length])
    let offset = 0
    return groups
      .map(groupLen => {
        let chunk = ''
        for (let i = 0; i < groupLen && offset < guess.feedback.length; i += 1) {
          chunk += FEEDBACK_EMOJI[guess.feedback[offset]] ?? '⬛'
          offset += 1
        }
        return chunk
      })
      .filter(Boolean)
      .join(' ')
  })
  return `${header}\n${rows.join('\n')}`
}

export function formatWordleDateLabel(isoDate) {
  if (typeof isoDate !== 'string') return ''
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate)
  if (!match) return ''
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const date = new Date(Date.UTC(year, month - 1, day))
  if (Number.isNaN(date.getTime())) return ''
  const weekday = WEEKDAYS_VI[date.getUTCDay()]
  return `${weekday} ${String(day).padStart(2, '0')}/${String(month).padStart(2, '0')}`
}

export function wordleToastMessage(code, length, fallback = '') {
  switch (code) {
    case 'TOO_SHORT':
      return 'Cần ít nhất 4 chữ cái'
    case 'TOO_LONG':
      return `Tối đa ${length} chữ cái`
    case 'WRONG_LENGTH':
      return `Cần đủ ${length} chữ cái`
    case 'NOT_A_PLAYER':
      return 'Không có cầu thủ Premier League nào tên như vậy'
    case 'ALREADY_GUESSED':
      return 'Bạn đã đoán tên này rồi'
    case 'INVALID_CHARACTERS':
      return 'Chỉ dùng chữ cái A–Z'
    case 'GAME_FINISHED':
      return 'Ván hôm nay đã kết thúc'
    case 'HINT_LOCKED':
      return 'Gợi ý mở sau 4 lượt đoán'
    case 'HINT_USED':
      return 'Bạn đã mở gợi ý rồi'
    default:
      return fallback || 'Không thực hiện được thao tác. Vui lòng thử lại.'
  }
}

export function resolveWordleAccess({ infoStatus, account }) {
  if (infoStatus === 'disabled') {
    return { showOnHub: false, routeAction: 'redirect' }
  }
  if (infoStatus === 'enabled') {
    return { showOnHub: true, routeAction: account ? 'play' : 'login' }
  }
  if (infoStatus === 'error') {
    return { showOnHub: false, routeAction: 'error' }
  }
  return { showOnHub: false, routeAction: 'loading' }
}

export function wordleHubSummary(account, state, infoData = null) {
  const defaultTitle = infoData?.puzzleNumber != null ? `Wordle cầu thủ #${infoData.puzzleNumber}` : 'Wordle cầu thủ'
  if (!account) {
    return { title: defaultTitle, statusText: 'Đăng nhập để chơi', statusDone: false, actionLabel: 'Vào chơi' }
  }
  if (state.loading && !state.data) {
    return { title: defaultTitle, statusText: 'Đang kiểm tra…', statusDone: false, actionLabel: 'Vào chơi' }
  }
  if (!state.data) {
    return { title: defaultTitle, statusText: 'Chưa kiểm tra được tiến trình', statusDone: false, actionLabel: 'Vào chơi' }
  }
  const { puzzleNumber, status, guesses } = state.data
  const title = `Wordle cầu thủ #${puzzleNumber}`
  if (status === 'WON') {
    return { title, statusText: `Đã đoán ra · ${guesses.length}/${WORDLE_MAX_TURNS}`, statusDone: true, actionLabel: 'Xem ván' }
  }
  if (status === 'LOST') {
    return { title, statusText: 'Hết lượt', statusDone: false, actionLabel: 'Xem ván' }
  }
  if (!guesses.length) {
    return { title, statusText: 'Chưa chơi', statusDone: false, actionLabel: 'Vào chơi' }
  }
  const turn = Math.min(WORDLE_MAX_TURNS, guesses.length + 1)
  return { title, statusText: `Đang chơi dở · lượt ${turn}/${WORDLE_MAX_TURNS}`, statusDone: false, actionLabel: 'Tiếp tục' }
}

function analyzeViChar(ch) {
  if (!ch) return { base: '', shape: null, tone: null }
  if (ch === 'Đ' || ch === 'đ') {
    return { base: 'D', shape: 'STROKE', tone: null }
  }
  const nfd = ch.normalize('NFD')
  const first = nfd[0]?.toUpperCase() ?? ''
  const base = first >= 'A' && first <= 'Z' ? first : ''
  let shape = null
  let tone = null
  for (let i = 1; i < nfd.length; i += 1) {
    const mark = nfd[i]
    if (mark === '\u0302') shape = 'CIRCUMFLEX'
    else if (mark === '\u0306') shape = 'BREVE'
    else if (mark === '\u031B') shape = 'HORN'
    else if (TONE_KEY_BY_MARK[mark]) tone = TONE_KEY_BY_MARK[mark]
  }
  return { base, shape, tone }
}

function baseLetters(str) {
  let out = ''
  for (const ch of str) {
    out += analyzeViChar(ch).base
  }
  return out
}

function letterCharsOnly(str) {
  return Array.from(str).filter(ch => analyzeViChar(ch).base !== '')
}

function decodePureInsert({ raw, shadowChars, nextShadow, text, clamp }) {
  if (/^\s+$/.test(text)) {
    return { raw, shadow: shadowChars.join(''), uncertain: false }
  }
  const insOnlyChars = Array.from(text)
  if (insOnlyChars.length === 1) {
    if (/^[A-Za-z]$/.test(text)) {
      return { raw: clamp(raw + text.toUpperCase()), shadow: nextShadow, uncertain: false }
    }
    const info = analyzeViChar(text)
    // Non-letter placeholder (e.g. UniKey U+00B7 "·"): keep in shadow for delete counting, ignore in raw.
    if (!info.base) {
      return { raw, shadow: nextShadow, uncertain: false }
    }
    // Safety net: 1 accented char inserted without a Backspace whose base letter matches
    // the last letter in raw (or last letter char in shadow) -> treat as k=1 transform.
    if (info.tone || info.shape) {
      const shadowLetters = letterCharsOnly(shadowChars.join(''))
      const lastShadowChar = shadowLetters.length > 0 ? shadowLetters[shadowLetters.length - 1] : ''
      const lastShadowBase = analyzeViChar(lastShadowChar).base
      if ((raw.length > 0 && raw[raw.length - 1] === info.base) || lastShadowBase === info.base) {
        const beforeChar = lastShadowBase === info.base ? lastShadowChar : info.base
        const key = inferSwallowedKey(beforeChar, text)
        if (key) {
          let replacedShadow = nextShadow
          if (lastShadowBase === info.base) {
            let cut = shadowChars.length - 1
            while (cut >= 0 && analyzeViChar(shadowChars[cut]).base === '') cut -= 1
            replacedShadow = shadowChars.slice(0, Math.max(0, cut)).join('') + text
          }
          return { raw: clamp(raw + key), shadow: replacedShadow, uncertain: false }
        }
      }
    }
    // Standalone W key in Telex inserts Ư/ư or Ơ/ơ with no letter deleted.
    if (info.shape === 'HORN' && !info.tone && (info.base === 'U' || info.base === 'O')) {
      return { raw: clamp(raw + 'W'), shadow: nextShadow, uncertain: false }
    }
  }
  if (/^[A-Za-z]+$/.test(text)) {
    return { raw: clamp(raw + text.toUpperCase()), shadow: nextShadow, uncertain: false }
  }
  // Non-letter placeholder string or unexpected non-ASCII pure insert
  const fallback = baseLetters(text)
  if (!fallback) {
    return { raw, shadow: nextShadow, uncertain: false }
  }
  return { raw: clamp(raw + fallback), shadow: nextShadow, uncertain: true }
}

function inferSwallowedKey(deleted, inserted, { allowRemoval = true } = {}) {
  const delChars = Array.from(deleted)
  const insChars = Array.from(inserted)
  if (delChars.length !== insChars.length) return null

  let addedTone = null
  let addedShapeKey = null
  let removedAnyMark = false

  for (let i = 0; i < delChars.length; i += 1) {
    const before = analyzeViChar(delChars[i])
    const after = analyzeViChar(insChars[i])
    if (before.base !== after.base) return null

    if (after.tone && after.tone !== before.tone) {
      addedTone = after.tone
    } else if (before.tone && !after.tone) {
      removedAnyMark = true
    }

    if (after.shape && after.shape !== before.shape) {
      if (after.shape === 'CIRCUMFLEX') addedShapeKey = after.base
      else if (after.shape === 'BREVE' || after.shape === 'HORN') addedShapeKey = 'W'
      else if (after.shape === 'STROKE') addedShapeKey = 'D'
    } else if (before.shape && !after.shape) {
      removedAnyMark = true
    }
  }

  if (addedTone) return addedTone
  if (addedShapeKey) return addedShapeKey
  if (removedAnyMark) return allowRemoval ? 'Z' : ''
  return ''
}

/**
 * Pure decoder that maps one coalesced input burst { deletes: k, insert: T }
 * from a Vietnamese Telex IME (Unikey / EVKey) back into raw A-Z keypresses.
 * Maintains parallel { raw, shadow } strings and flags uncertain fallbacks.
 */
export function decodeTelexBurst({ raw = '', shadow = '' }, { deletes = 0, insert = '' }, maxLength = null) {
  const k = Math.max(0, deletes)
  const T = String(insert || '')
  const shadowChars = Array.from(shadow)
  const kEff = Math.min(k, shadowChars.length)
  const deleted = shadowChars.slice(shadowChars.length - kEff).join('')
  const nextShadow = shadowChars.slice(0, shadowChars.length - kEff).join('') + T
  const clamp = value => (maxLength != null ? value.slice(0, maxLength) : value)

  // 1. Pure delete (real Backspace)
  if (k > 0 && T.length === 0) {
    const deletedLetterCount = letterCharsOnly(deleted).length
    const removeCount = kEff === 0 ? k : deletedLetterCount + Math.max(0, k - kEff)
    const nextRaw = raw.slice(0, Math.max(0, raw.length - removeCount))
    return { raw: nextRaw, shadow: nextShadow, uncertain: false }
  }

  // 2. Pure insert without delete
  if (k === 0) {
    return decodePureInsert({ raw, shadowChars, nextShadow, text: T, clamp })
  }

  // 2b. Spacebar auto-restore (e.g. Unikey restores 'hâland' -> 'haaland ' on Space): no letter added.
  if (/\s$/.test(T)) {
    const trimmed = T.replace(/\s+$/, '')
    const restoredShadow = shadowChars.slice(0, shadowChars.length - kEff).join('') + trimmed
    return { raw, shadow: restoredShadow, uncertain: false }
  }

  // 3. Delete + Insert burst (k > 0 && T.length > 0)
  // Ignore non-letter placeholder chars (e.g. UniKey U+00B7 "·") when comparing deleted vs inserted letters.
  const delLetters = letterCharsOnly(deleted)
  const insLetters = letterCharsOnly(T)
  const delLetterStr = delLetters.join('')
  const insLetterStr = insLetters.join('')
  const delBase = baseLetters(delLetterStr)
  const insBase = baseLetters(insLetterStr)

  // 3-pre. Only placeholders were deleted (e.g. "·" + ⌫ + "ư" for standalone W): decode insert as pure insert.
  if (delLetters.length === 0) {
    if (insLetters.length === 0) {
      return { raw, shadow: nextShadow, uncertain: false }
    }
    const keptShadowChars = shadowChars.slice(0, shadowChars.length - kEff)
    return decodePureInsert({ raw, shadowChars: keptShadowChars, nextShadow, text: insLetterStr, clamp })
  }

  // 3a. Same base letters -> diacritic/tone transformation swallowed 1 Telex key.
  if (delBase.length > 0 && insBase === delBase && insLetters.length === delLetters.length) {
    const key = inferSwallowedKey(delLetterStr, insLetterStr)
    if (key !== null) {
      return { raw: clamp(raw + key), shadow: nextShadow, uncertain: false }
    }
  }

  // 3b. All-ASCII replacement differing from deleted -> Telex undo or auto-restore (e.g. Â+A -> AA, Ả·+⌫⌫+AR -> ARR).
  if (/^[A-Za-z]+$/.test(insLetterStr) && insLetterStr !== delLetterStr) {
    const lastChar = insLetterStr[insLetterStr.length - 1].toUpperCase()
    return { raw: clamp(raw + lastChar), shadow: nextShadow, uncertain: false }
  }

  // 3c. Base letters preserved plus 1+ new ASCII letters appended:
  // handles both Unikey tone-strip on consonant (ƯỈ+T -> ƯIT) and fast typing inside 25ms (A· -> ÂL).
  if (delBase.length > 0 && insBase.startsWith(delBase) && insBase.length > delBase.length) {
    const prefix = insLetters.slice(0, delLetters.length).join('')
    const suffix = insLetters.slice(delLetters.length).join('')
    if (/^[A-Za-z]+$/.test(suffix)) {
      const addedBeforeSuffix = inferSwallowedKey(delLetterStr, prefix, { allowRemoval: false }) || ''
      return { raw: clamp(raw + addedBeforeSuffix + suffix.toUpperCase()), shadow: nextShadow, uncertain: false }
    }
  }

  // 3d. Fallback: uncertain transformation -> append newly added base letters and surface IME warning.
  const addedBase = insBase.startsWith(delBase) ? insBase.slice(delBase.length) : insBase
  return { raw: clamp(raw + addedBase), shadow: nextShadow, uncertain: true }
}

/**
 * Coalesces delete/insert input operations from Vietnamese Telex IMEs (Unikey / EVKey):
 * - Plain inserts (deletes === 0) flush immediately so normal typing has zero latency.
 * - Deletes are held up to deleteHoldMs (default 150ms) so slower Unikey delete->insert
 *   pairs (e.g. 60-100ms apart) still merge into a single { deletes, insert } burst.
 * - Once an insert follows pending deletes, a short insertCoalesceMs (default 25ms) window
 *   groups multi-character replacements (e.g. "án", "ar") before flushing.
 * - flush() synchronously resolves any held Backspace (called before Enter / submit).
 */
export function createTelexBurstQueue({
  onFlush,
  deleteHoldMs = TELEX_DELETE_HOLD_MS,
  insertCoalesceMs = TELEX_BURST_MS,
} = {}) {
  let pending = { deletes: 0, insert: '' }
  let timer = null

  function clearTimer() {
    if (timer !== null) {
      clearTimeout(timer)
      timer = null
    }
  }

  function flush() {
    clearTimer()
    if (pending.deletes === 0 && pending.insert.length === 0) return
    const burst = pending
    pending = { deletes: 0, insert: '' }
    onFlush?.(burst)
  }

  function schedule(delayMs) {
    clearTimer()
    timer = setTimeout(flush, delayMs)
  }

  return {
    pushDelete(count = 1) {
      if (pending.insert.length > 0) flush()
      pending.deletes += count
      schedule(deleteHoldMs)
    },
    pushInsert(text) {
      if (!text) return
      if (pending.deletes === 0) {
        if (pending.insert.length > 0) flush()
        onFlush?.({ deletes: 0, insert: text })
        return
      }
      pending.insert += text
      schedule(insertCoalesceMs)
    },
    hasPending() {
      return pending.deletes > 0 || pending.insert.length > 0
    },
    flush,
    reset() {
      clearTimer()
      pending = { deletes: 0, insert: '' }
    },
  }
}
