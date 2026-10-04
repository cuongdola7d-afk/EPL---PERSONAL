// Auth always uses the page's origin, including Google authorization and callback.
// A legacy VITE_API_BASE_URL may still be used for public football lookups.
const url = path => `/api/auth/${path}`

export class AuthError extends Error {
  constructor(message, status, retryAfterSeconds = null) {
    super(message); this.status = status; this.retryAfterSeconds = retryAfterSeconds
  }
}

function rateLimitError(response, data) {
  const header = response.headers.get('Retry-After')
  let seconds = header && /^\d+$/.test(header) ? Number(header) : null
  if (header && seconds === null) {
    const date = Date.parse(header)
    if (Number.isFinite(date)) seconds = Math.ceil((date - Date.now()) / 1000)
  }
  if (!(seconds > 0) && Number.isFinite(data?.retryAfterSeconds)) seconds = data.retryAfterSeconds
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return new AuthError('Bạn đã thử quá nhiều lần. Vui lòng chờ một lúc rồi thử lại.', 429)
  }
  seconds = Math.ceil(seconds)
  return new AuthError(`Bạn đã thử quá nhiều lần. Vui lòng thử lại sau ${seconds} giây.`, 429, seconds)
}

async function accountRequest(path, options = {}) {
  let response
  try { response = await fetch(url(path), { credentials: 'include', ...options }) }
  catch (error) {
    if (error.name === 'AbortError') throw error
    throw new AuthError('Không kết nối được dịch vụ tài khoản. Vui lòng thử lại.', 0)
  }
  if (response.status === 204) return null
  let data
  try { data = await response.json() }
  catch {
    if (response.status === 429) throw rateLimitError(response, null)
    throw new AuthError('Dịch vụ tài khoản trả về dữ liệu không hợp lệ.', response.status)
  }
  if (response.status === 429) throw rateLimitError(response, data)
  if (!response.ok) throw new AuthError(data.message ?? 'Không thực hiện được thao tác tài khoản.', response.status)
  return data
}

export async function currentAccount(signal) {
  try { return await accountRequest('me', { signal }) }
  catch (error) { if (error.status === 401) return null; throw error }
}

async function changeAccount(path, body) {
  // CSRF is held only for this request; login/logout rotate it on the server.
  const csrf = await accountRequest('csrf')
  return accountRequest(path, {
    method: 'POST', headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    ...(body ? { body: JSON.stringify(body) } : {}),
  })
}

export const registerAccount = body => changeAccount('register', body)
export const loginAccount = body => changeAccount('login', body)
export const logoutAccount = () => changeAccount('logout')
export const googleStatus = signal => accountRequest('google/status', { signal })
export const confirmGoogleLink = () => changeAccount('google/link/confirm', { confirmed: true })
export const cancelGoogleLink = () => changeAccount('google/link/cancel')
export async function startGoogle(mode, returnPath) {
  const started = await changeAccount('google/start', { mode, returnPath })
  // Accept only this fixed server authorization endpoint, never a provider/client-supplied arbitrary URL.
  if (started.authorizationPath !== '/api/auth/google/authorize/google') throw new AuthError('Đường dẫn Google không hợp lệ.', 0)
  return url('google/authorize/google')
}
