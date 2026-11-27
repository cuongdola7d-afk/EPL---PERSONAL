export const WORDLE_MAX_TURNS = 6
export const WORDLE_HINT_UNLOCK_TURNS = 4

const FEEDBACK_STATE = { H: 'hit', N: 'near', M: 'miss' }
const STATE_RANK = { miss: 1, near: 2, hit: 3 }
const FEEDBACK_EMOJI = { H: '🟩', N: '🟨', M: '⬛' }
const WEEKDAYS_VI = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7']

export function wordShape(word) {
  if (typeof word !== 'string' || !word.trim()) return []
  return word.trim().split(/\s+/).map(part => part.length)
}

export function buildRowWordGroups({ word = null, letters = '', feedback = null, length = 0 }) {
  const shape = word ? wordShape(word) : [length]
  let offset = 0
  return shape.map(groupLen => {
    const tiles = []
    for (let i = 0; i < groupLen; i += 1) {
      const char = letters[offset] ?? ''
      const state = feedback
        ? (FEEDBACK_STATE[feedback[offset]] ?? 'miss')
        : (char ? 'filled' : 'empty')
      tiles.push({ index: offset, letter: char, state })
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

export function filterWordleSuggestions(dictionary, input, guessedLetters = [], limit = 5, length = null) {
  const prefix = String(input || '').trim().toUpperCase()
  if (!prefix || !Array.isArray(dictionary)) return []
  const used = new Set(guessedLetters)
  return dictionary
    .filter(entry =>
      entry &&
      typeof entry.letters === 'string' &&
      (length == null || entry.letters.length === length) &&
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

export function buildShareText({ puzzleNumber, status, guesses }) {
  const scoreLabel = status === 'WON' ? `${guesses.length}/${WORDLE_MAX_TURNS}` : `X/${WORDLE_MAX_TURNS}`
  const header = `PrismaXI Wordle #${puzzleNumber} ${scoreLabel}`
  if (!Array.isArray(guesses) || !guesses.length) return header
  const rows = guesses.map(guess => {
    const shape = wordShape(guess.word)
    const groups = shape.length ? shape : [guess.feedback.length]
    let offset = 0
    return groups.map(groupLen => {
      let chunk = ''
      for (let i = 0; i < groupLen; i += 1) {
        chunk += FEEDBACK_EMOJI[guess.feedback[offset]] ?? '⬛'
        offset += 1
      }
      return chunk
    }).join(' ')
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
