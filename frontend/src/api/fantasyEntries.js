// Private data always stays on the page origin, never the public VITE_API_BASE_URL.
export class FantasyEntryError extends Error {
  constructor(message, status, code) { super(message); this.status = status; this.code = code }
}
function notifySessionChanged() { globalThis.window?.dispatchEvent(new Event('prismaxi-session-refresh')) }
function validPicks(picks) {
  return picks && typeof picks === 'object' && !Array.isArray(picks) &&
    Object.entries(picks).every(([slot, id]) => /^\d-\d$/.test(slot) && Number.isInteger(id) && id > 0)
}
export function validEntry(data, accountId, gameweek) {
  return data?.accountId === accountId && data.season === 2026 && data.gameweek === gameweek &&
    Number.isSafeInteger(data.version) && data.version >= 0 &&
    (data.draft === null || typeof data.draft?.formation === 'string' && validPicks(data.draft.picks)) &&
    (data.submitted === null || typeof data.submitted?.formation === 'string' && validPicks(data.submitted.picks) &&
      Object.keys(data.submitted.picks).length === 11 && Number.isInteger(data.submitted.totalOvr) &&
      Array.isArray(data.submitted.players) && data.submitted.players.length === 11)
}
async function request(accountId, gameweek, action, body, signal) {
  const headers = { Accept: 'application/json', 'X-PrismaXI-Account-ID': String(accountId) }
  if (action) {
    const csrfResponse = await fetch('/api/auth/csrf', { credentials: 'include', signal })
    if (!csrfResponse.ok) throw new FantasyEntryError('Không lấy được mã xác nhận phiên. Vui lòng thử lại.', csrfResponse.status)
    const csrf = await csrfResponse.json()
    headers[csrf.headerName] = csrf.token; headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(`/api/fantasy/2026/me/gameweeks/${gameweek}${action ? `/${action}` : ''}`, {
    credentials: 'include', signal, headers, ...(action ? { method: 'POST', body: JSON.stringify(body) } : {}),
  })
  let data
  try { data = await response.json() }
  catch { throw new FantasyEntryError('Dịch vụ đội hình trả về dữ liệu không hợp lệ.', response.status) }
  if (!response.ok) {
    if (response.status === 401 || data.code === 'SESSION_CHANGED') notifySessionChanged()
    const details = data.issues?.map(issue => issue.message).join(' ')
    throw new FantasyEntryError(details || data.message || 'Không thực hiện được thao tác đội hình.', response.status, data.code)
  }
  if (data.accountId !== accountId) {
    notifySessionChanged(); throw new FantasyEntryError('Phiên đã đổi tài khoản. Vui lòng tải lại.', 409, 'SESSION_CHANGED')
  }
  if (!validEntry(data, accountId, gameweek)) throw new FantasyEntryError('Dữ liệu đội hình không đúng định dạng.', 0)
  return data
}
export const fetchFantasyEntry = (accountId, gameweek, signal) => request(accountId, gameweek, null, null, signal)
export const saveFantasyDraft = (accountId, gameweek, body, signal) => request(accountId, gameweek, 'draft', body, signal)
export const submitFantasyEntry = (accountId, gameweek, body, signal) => request(accountId, gameweek, 'submit', body, signal)
