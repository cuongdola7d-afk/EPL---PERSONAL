import { HINT_KEYS } from '../minigame/playerGuess.js'

const BASE = '/api/minigame/2026/player-guess'
const instant = value => typeof value === 'string' && Number.isFinite(Date.parse(value))
const integer = (value, min, max = Number.MAX_SAFE_INTEGER) => Number.isSafeInteger(value) && value >= min && value <= max
const text = value => typeof value === 'string' && value.trim().length > 0
const uuid = value => typeof value === 'string' && /^[\da-f]{8}(-[\da-f]{4}){3}-[\da-f]{12}$/i.test(value)
export function validGame(game, accountId) {
  const terminal = ['WON', 'LOST', 'EXPIRED'].includes(game?.status)
  return game?.accountId === accountId && game.season === 2026 && uuid(game.gameId) &&
    ['PRACTICE', 'DAILY'].includes(game.mode) && (terminal || game.status === 'IN_PROGRESS') &&
    integer(game.version, 0) && integer(game.currentScore, 0, 100) &&
    (terminal ? integer(game.finalScore, 0, 100) : game.finalScore === null) &&
    integer(game.guessesUsed, 0, 3) && game.guessesRemaining === 3 - game.guessesUsed &&
    integer(game.revealedHintCount, 2, 8) && game.totalHints === 8 &&
    typeof game.canGuess === 'boolean' && typeof game.canRevealHint === 'boolean' &&
    (game.nextHintKey === null || HINT_KEYS.includes(game.nextHintKey)) &&
    instant(game.serverTime) && instant(game.nextDailyAt) && (game.expiresAt === null || instant(game.expiresAt)) &&
    Array.isArray(game.hints) && game.hints.length === 8 && game.hints.every((hint, index) =>
      hint.key === HINT_KEYS[index] && text(hint.label) && typeof hint.revealed === 'boolean' &&
      hint.revealed === (terminal || index < game.revealedHintCount) &&
      (hint.revealed ? text(hint.value) : hint.value === null)) &&
    Array.isArray(game.guesses) && game.guesses.length === game.guessesUsed &&
    game.guesses.every(guess => integer(guess.playerId, 1) && text(guess.name) && typeof guess.correct === 'boolean') &&
    (terminal ? !game.canGuess && !game.canRevealHint && integer(game.answer?.playerId, 1) && text(game.answer.name) && text(game.answer.club) : game.answer === null)
}
const messages = {
  INSUFFICIENT_DATA: 'Chưa có đủ cầu thủ với 8 gợi ý hợp lệ để tạo câu hỏi. Vui lòng thử lại sau khi dữ liệu được bổ sung.',
  PRACTICE_POOL_TOO_SMALL: 'Chưa có cầu thủ phù hợp cho luyện tập sau khi loại đáp án daily. Vui lòng thử lại sau.',
  VERSION_CONFLICT: 'Tiến trình đã thay đổi ở tab khác. Đã cập nhật ván mới nhất; hãy kiểm tra trước khi tiếp tục.',
  SESSION_CHANGED: 'Tài khoản đã thay đổi. Đang kiểm tra lại phiên đăng nhập.',
  DAY_CHANGED_RETRY: 'Đã sang ngày mới theo giờ Việt Nam. Hãy kiểm tra tiến trình rồi bắt đầu lại.',
}
export class PlayerGuessError extends Error {
  constructor(message, { status = 0, code, game = null, uncertain = false } = {}) {
    super(message); this.status = status; this.code = code; this.game = game; this.uncertain = uncertain
  }
}
function refreshSession() { globalThis.window?.dispatchEvent(new Event('prismaxi-session-refresh')) }
async function request(path, { accountId, body, signal } = {}) {
  const headers = { Accept: 'application/json' }
  if (accountId != null) headers['X-PrismaXI-Account-ID'] = String(accountId)
  if (body) {
    let csrfResponse, csrf
    try {
      csrfResponse = await fetch('/api/auth/csrf', { credentials: 'include', cache: 'no-store', signal })
    } catch (failure) {
      if (failure.name === 'AbortError') throw failure
      throw new PlayerGuessError('Không kết nối được để xác nhận phiên. Thao tác chưa được gửi; vui lòng thử lại.')
    }
    if (!csrfResponse.ok) {
      if ([401, 403].includes(csrfResponse.status)) refreshSession()
      throw new PlayerGuessError('Không xác nhận được phiên đăng nhập. Vui lòng kiểm tra kết nối và thử lại.', { status: csrfResponse.status })
    }
    try { csrf = await csrfResponse.json() }
    catch { throw new PlayerGuessError('Không nhận được mã xác nhận phiên. Thao tác chưa được gửi.') }
    if (!text(csrf?.headerName) || !text(csrf.token)) throw new PlayerGuessError('Không nhận được mã xác nhận phiên.')
    headers[csrf.headerName] = csrf.token; headers['Content-Type'] = 'application/json'
  }
  let response, data
  try {
    response = await fetch(`${BASE}${path}`, { credentials: 'include', cache: 'no-store', headers, signal,
      ...(body ? { method: 'POST', body: JSON.stringify(body) } : {}) })
  } catch (failure) {
    if (failure.name === 'AbortError') throw failure
    throw new PlayerGuessError(body ? 'Chưa xác nhận được thao tác đã được lưu hay chưa. Kiểm tra tiến trình hoặc gửi lại cùng thao tác.' : 'Không kết nối được Minigame. Vui lòng thử lại.', { uncertain: !!body })
  }
  try { data = await response.json() }
  catch {
    if ([401, 403].includes(response.status)) refreshSession()
    throw new PlayerGuessError('Máy chủ không trả về dữ liệu Minigame hợp lệ. Hãy kiểm tra tiến trình hoặc thử lại.',
      { status: response.status, uncertain: !!body && (response.ok || response.status >= 500) })
  }
  if (!data || typeof data !== 'object') malformed(!!body && (response.ok || response.status >= 500))
  if (data.game && !validGame(data.game, accountId)) {
    if (data.game.accountId !== accountId) refreshSession()
    throw new PlayerGuessError('Tiến trình trả về không hợp lệ. Hãy kiểm tra lại phiên và tải lại ván.', { uncertain: !!body })
  }
  if (!response.ok) {
    if ([401, 403].includes(response.status) || data.code === 'SESSION_CHANGED') refreshSession()
    throw new PlayerGuessError(messages[data.code] || (response.status === 404 ? 'Minigame chưa sẵn sàng trên máy chủ này.' : data.message) || 'Không thực hiện được thao tác. Vui lòng thử lại.',
      { status: response.status, code: data.code, game: data.game, uncertain: !!body && response.status >= 500 })
  }
  return data
}
function malformed(uncertain = false) { throw new PlayerGuessError('Dữ liệu Minigame không đúng định dạng. Vui lòng tải lại.', { uncertain }) }
export async function fetchGuessInfo(signal) {
  const data = await request('/info', { signal })
  if (data.season !== 2026 || !instant(data.serverTime) || !instant(data.nextDailyAt)) malformed()
  return data
}
export async function fetchCurrentGame(accountId, mode, signal) {
  const data = await request(`/${mode.toLowerCase()}/current`, { accountId, signal })
  if (!instant(data.serverTime) || !instant(data.nextDailyAt) ||
    !(data.game === null && data.status === 'NOT_STARTED' || validGame(data.game, accountId) && data.game.mode === mode)) malformed()
  return data
}
export async function mutateGuessGame(accountId, mode, action, signal) {
  const path = action.kind === 'start' ? `/${mode.toLowerCase()}/start` :
    `/games/${action.gameId}/${action.kind === 'guess' ? 'guesses' : 'hints/next'}`
  const data = await request(path, { accountId, body: action.body, signal })
  if (data.code !== 'OK' || !validGame(data.game, accountId) || data.game.mode !== mode) malformed(true)
  return data
}
export async function fetchGuessPlayers(accountId, gameId, signal) {
  const data = await request(`/players?gameId=${encodeURIComponent(gameId)}`, { accountId, signal })
  if (!Array.isArray(data) || !data.every(player => integer(player.playerId, 1) && text(player.name))) malformed()
  return data
}
export async function fetchGuessLeaderboard(offset = 0, signal) {
  const data = await request(`/leaderboard?offset=${offset}&limit=20`, { signal })
  if (data.season !== 2026 || !Array.isArray(data.players) || data.offset !== offset ||
    !data.players.every(player => integer(player.rank, 1) && integer(player.accountId, 1) && text(player.displayName) &&
      integer(player.totalPoints, 0) && integer(player.dailyGames, 0) && typeof player.tied === 'boolean')) malformed()
  return data
}
export async function fetchGuessHistory(accountId, signal) {
  const data = await request('/daily/history?offset=0&limit=20', { accountId, signal })
  if (!Array.isArray(data.games) || !data.games.every(game => validGame(game, accountId) && game.mode === 'DAILY')) malformed()
  return data.games
}
