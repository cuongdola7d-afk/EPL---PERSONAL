const BASE = '/api/minigame/2026/wordle'

const instant = value => typeof value === 'string' && Number.isFinite(Date.parse(value))
const isoDate = value => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)
const integer = (value, min, max = Number.MAX_SAFE_INTEGER) => Number.isSafeInteger(value) && value >= min && value <= max
const text = value => typeof value === 'string' && value.trim().length > 0
const lettersOnly = value => typeof value === 'string' && /^[A-Z]+$/.test(value)
const feedbackCode = value => typeof value === 'string' && /^[HNM]+$/.test(value)

export function isValidWordleStats(stats) {
  return !!stats && typeof stats === 'object' &&
    integer(stats.played, 0) &&
    integer(stats.wins, 0, stats.played) &&
    integer(stats.winPercentage, 0, 100) &&
    integer(stats.currentStreak, 0) &&
    integer(stats.maxStreak, 0) &&
    Array.isArray(stats.distribution) &&
    stats.distribution.length === 6 &&
    stats.distribution.every(count => integer(count, 0))
}

export function isValidWordleGuess(guess, maxLength) {
  return !!guess && typeof guess === 'object' &&
    integer(guess.turn, 1, 6) &&
    text(guess.word) &&
    lettersOnly(guess.letters) &&
    guess.letters.length >= 4 &&
    guess.letters.length <= maxLength &&
    guess.word.replace(/\s+/g, '') === guess.letters &&
    feedbackCode(guess.feedback) &&
    guess.feedback.length === guess.letters.length &&
    text(guess.playerName) &&
    text(guess.club)
}

export function isValidWordleAnswer(answer) {
  return !!answer && typeof answer === 'object' &&
    integer(answer.playerId, 1) &&
    text(answer.name) &&
    integer(answer.clubId, 1) &&
    text(answer.club) &&
    (answer.shirtNumber === null || integer(answer.shirtNumber, 0, 99)) &&
    (answer.position === null || text(answer.position)) &&
    (answer.nationality === null || text(answer.nationality)) &&
    (answer.age === null || integer(answer.age, 14, 60)) &&
    (answer.overall === null || integer(answer.overall, 1, 99)) &&
    text(answer.word)
}

export function isValidWordleShape(shape, length) {
  return Array.isArray(shape) &&
    shape.length >= 1 &&
    shape.every(part => integer(part, 1, length)) &&
    shape.reduce((sum, part) => sum + part, 0) === length
}

export function isValidWordleInfo(data) {
  return !!data && typeof data === 'object' &&
    data.enabled === true &&
    (data.puzzleNumber === null || integer(data.puzzleNumber, 1)) &&
    isoDate(data.date) &&
    (data.length === null || integer(data.length, 4, 11)) &&
    (data.length === null ? data.shape === null : isValidWordleShape(data.shape, data.length)) &&
    instant(data.nextAt) &&
    instant(data.serverTime)
}

export function isValidWordleToday(data) {
  if (!data || typeof data !== 'object') return false
  const terminal = data.status === 'WON' || data.status === 'LOST'
  return integer(data.puzzleNumber, 1) &&
    isoDate(data.date) &&
    integer(data.length, 4, 11) &&
    isValidWordleShape(data.shape, data.length) &&
    instant(data.serverTime) &&
    instant(data.nextAt) &&
    (terminal || data.status === 'PLAYING') &&
    typeof data.hintUsed === 'boolean' &&
    (data.hintUsed ? text(data.hintClub) : data.hintClub === null) &&
    Array.isArray(data.guesses) &&
    data.guesses.length <= 6 &&
    data.guesses.every((guess, index) => guess.turn === index + 1 && isValidWordleGuess(guess, data.length)) &&
    (terminal ? isValidWordleAnswer(data.answer) : data.answer === null) &&
    isValidWordleStats(data.stats)
}

export function isValidWordleDictionary(entries) {
  return Array.isArray(entries) && entries.every(entry =>
    !!entry && typeof entry === 'object' &&
    lettersOnly(entry.letters) &&
    text(entry.word) &&
    entry.word.replace(/\s+/g, '') === entry.letters &&
    text(entry.name) &&
    text(entry.club)
  )
}

export class WordleError extends Error {
  constructor(message, { status = 0, code = '', uncertain = false } = {}) {
    super(message)
    this.status = status
    this.code = code
    this.uncertain = uncertain
  }
}

const MESSAGES = {
  SESSION_CHANGED: 'Tài khoản đã thay đổi. Đang kiểm tra lại phiên đăng nhập.',
  INSUFFICIENT_DATA: 'Chưa có đủ cầu thủ hợp lệ để tạo câu đố Wordle hôm nay.',
}

function refreshSession() {
  globalThis.window?.dispatchEvent(new Event('prismaxi-session-refresh'))
}

function malformed(uncertain = false) {
  throw new WordleError('Dữ liệu Wordle không đúng định dạng. Vui lòng tải lại.', { uncertain })
}

async function request(path, { accountId, body, signal, omitCredentials = false } = {}) {
  const headers = { Accept: 'application/json' }
  if (accountId != null) headers['X-PrismaXI-Account-ID'] = String(accountId)

  if (body !== undefined) {
    let csrfResponse, csrf
    try {
      csrfResponse = await fetch('/api/auth/csrf', { credentials: 'include', cache: 'no-store', signal })
    } catch (failure) {
      if (failure.name === 'AbortError') throw failure
      throw new WordleError('Không kết nối được để xác nhận phiên. Vui lòng thử lại.')
    }
    if (!csrfResponse.ok) {
      if ([401, 403].includes(csrfResponse.status)) refreshSession()
      throw new WordleError('Không xác nhận được phiên đăng nhập. Vui lòng thử lại.', { status: csrfResponse.status })
    }
    try { csrf = await csrfResponse.json() }
    catch { throw new WordleError('Không nhận được mã xác nhận phiên. Vui lòng thử lại.') }
    if (!text(csrf?.headerName) || !text(csrf.token)) {
      throw new WordleError('Không nhận được mã xác nhận phiên. Vui lòng thử lại.')
    }
    headers[csrf.headerName] = csrf.token
    headers['Content-Type'] = 'application/json'
  }

  let response, data
  try {
    response = await fetch(`${BASE}${path}`, {
      credentials: omitCredentials ? 'omit' : 'include',
      cache: 'no-store',
      headers,
      signal,
      ...(body !== undefined ? { method: 'POST', body: JSON.stringify(body) } : {}),
    })
  } catch (failure) {
    if (failure.name === 'AbortError') throw failure
    throw new WordleError(
      body !== undefined
        ? 'Chưa xác nhận được lượt chơi đã lưu hay chưa. Vui lòng tải lại.'
        : 'Không kết nối được Wordle cầu thủ. Vui lòng thử lại.',
      { uncertain: body !== undefined }
    )
  }

  try { data = await response.json() }
  catch {
    if (!omitCredentials && [401, 403].includes(response.status)) refreshSession()
    if (response.status === 404) {
      throw new WordleError('Wordle cầu thủ chưa được bật trên máy chủ này.', { status: 404, code: 'NOT_FOUND' })
    }
    throw new WordleError(
      'Máy chủ không trả về dữ liệu Wordle hợp lệ. Vui lòng thử lại.',
      { status: response.status, uncertain: body !== undefined && (response.ok || response.status >= 500) }
    )
  }

  if (!data || typeof data !== 'object') {
    malformed(body !== undefined && (response.ok || response.status >= 500))
  }

  if (!response.ok) {
    if (!omitCredentials && ([401, 403].includes(response.status) || data.code === 'SESSION_CHANGED')) refreshSession()
    throw new WordleError(
      MESSAGES[data.code] ||
        (response.status === 404 ? 'Wordle cầu thủ chưa được bật trên máy chủ này.' : data.message) ||
        'Không thực hiện được thao tác. Vui lòng thử lại.',
      { status: response.status, code: data.code || '', uncertain: body !== undefined && response.status >= 500 }
    )
  }

  return data
}

export async function fetchWordleInfo(signal) {
  const data = await request('/info', { signal, omitCredentials: true })
  if (!isValidWordleInfo(data)) malformed()
  return data
}

export async function fetchWordleToday(accountId, signal) {
  const data = await request('/today', { accountId, signal })
  if (!isValidWordleToday(data)) malformed()
  return data
}

export async function submitWordleGuess(accountId, letters, { date, signal } = {}) {
  const body = date ? { letters, date } : { letters }
  const data = await request('/today/guesses', { accountId, body, signal })
  if (!isValidWordleToday(data)) malformed(true)
  return data
}

export async function revealWordleHint(accountId, { date, signal } = {}) {
  const body = date ? { date } : {}
  const data = await request('/today/hint', { accountId, body, signal })
  if (!isValidWordleToday(data)) malformed(true)
  return data
}

export async function fetchWordleDictionary(accountId, signal) {
  const data = await request('/dictionary', { accountId, signal })
  if (!isValidWordleDictionary(data)) malformed()
  return data
}

export async function fetchWordleStats(accountId, signal) {
  const data = await request('/stats', { accountId, signal })
  if (!isValidWordleStats(data)) malformed()
  return data
}
