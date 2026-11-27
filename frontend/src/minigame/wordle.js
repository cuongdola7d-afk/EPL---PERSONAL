export const WORDLE_MAX_TURNS = 6
export const WORDLE_HINT_UNLOCK_TURNS = 4
export const WORDLE_MIN_LENGTH = 4
export const TELEX_BURST_MS = 25

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
    const nextRaw = raw.slice(0, Math.max(0, raw.length - k))
    return { raw: nextRaw, shadow: nextShadow, uncertain: false }
  }

  // 2. Pure insert without delete
  if (k === 0) {
    if (/^\s+$/.test(T)) {
      return { raw, shadow, uncertain: false }
    }
    if (T.length === 1) {
      if (/^[A-Za-z]$/.test(T)) {
        return { raw: clamp(raw + T.toUpperCase()), shadow: nextShadow, uncertain: false }
      }
      // Standalone W key in Telex inserts Ư/ư or Ơ/ơ with no delete.
      const info = analyzeViChar(T)
      if (info.shape === 'HORN' && !info.tone && (info.base === 'U' || info.base === 'O')) {
        return { raw: clamp(raw + 'W'), shadow: nextShadow, uncertain: false }
      }
    }
    if (/^[A-Za-z]+$/.test(T)) {
      return { raw: clamp(raw + T.toUpperCase()), shadow: nextShadow, uncertain: false }
    }
    // Non-letter or unexpected non-ASCII pure insert
    const fallback = baseLetters(T)
    if (!fallback) {
      return { raw, shadow: nextShadow, uncertain: false }
    }
    return { raw: clamp(raw + fallback), shadow: nextShadow, uncertain: true }
  }

  // 2b. Spacebar auto-restore (e.g. Unikey restores 'hâland' -> 'haaland ' on Space): no letter added.
  if (/\s$/.test(T)) {
    const trimmed = T.replace(/\s+$/, '')
    const restoredShadow = shadowChars.slice(0, shadowChars.length - kEff).join('') + trimmed
    return { raw, shadow: restoredShadow, uncertain: false }
  }

  // 3. Delete + Insert burst (k > 0 && T.length > 0)
  const delChars = Array.from(deleted)
  const insChars = Array.from(T)
  const delBase = baseLetters(deleted)
  const insBase = baseLetters(T)

  // 3a. Same base letters -> diacritic/tone transformation swallowed 1 Telex key.
  if (delBase.length > 0 && insBase === delBase && insChars.length === delChars.length) {
    const key = inferSwallowedKey(deleted, T)
    if (key !== null) {
      return { raw: clamp(raw + key), shadow: nextShadow, uncertain: false }
    }
  }

  // 3b. All-ASCII replacement differing from deleted -> Telex undo or auto-restore (e.g. Â+A -> AA, ỬI+T -> WIRT).
  if (/^[A-Za-z]+$/.test(T) && T !== deleted) {
    const lastChar = T[T.length - 1].toUpperCase()
    return { raw: clamp(raw + lastChar), shadow: nextShadow, uncertain: false }
  }

  // 3c. Base letters preserved plus 1+ new ASCII letters appended:
  // handles both Unikey tone-strip on consonant (ƯỈ+T -> ƯIT) and fast typing inside 25ms (A -> ÂL).
  if (delBase.length > 0 && insBase.startsWith(delBase) && insBase.length > delBase.length) {
    const prefix = insChars.slice(0, delChars.length).join('')
    const suffix = insChars.slice(delChars.length).join('')
    if (/^[A-Za-z]+$/.test(suffix)) {
      const addedBeforeSuffix = inferSwallowedKey(deleted, prefix, { allowRemoval: false }) || ''
      return { raw: clamp(raw + addedBeforeSuffix + suffix.toUpperCase()), shadow: nextShadow, uncertain: false }
    }
  }

  // 3d. Fallback: uncertain transformation -> append newly added base letters and surface IME warning.
  const addedBase = insBase.startsWith(delBase) ? insBase.slice(delBase.length) : insBase
  return { raw: clamp(raw + addedBase), shadow: nextShadow, uncertain: true }
}

/**
 * Coalesces delete/insert input operations occurring within windowMs (~25ms)
 * into single bursts for decodeTelexBurst. A delete arriving after an insert
 * flushes the preceding insert first so fast typing never merges separate keypresses.
 */
export function createTelexBurstQueue({ onFlush, windowMs = TELEX_BURST_MS } = {}) {
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

  function schedule() {
    clearTimer()
    timer = setTimeout(flush, windowMs)
  }

  return {
    pushDelete(count = 1) {
      if (pending.insert.length > 0) flush()
      pending.deletes += count
      schedule()
    },
    pushInsert(text) {
      if (!text) return
      if (pending.deletes === 0 && pending.insert.length > 0) flush()
      pending.insert += text
      schedule()
    },
    flush,
    reset() {
      clearTimer()
      pending = { deletes: 0, insert: '' }
    },
  }
}
