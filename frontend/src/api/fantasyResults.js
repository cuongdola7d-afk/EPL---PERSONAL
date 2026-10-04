import { FantasyEntryError } from './fantasyEntries.js'

async function adminRequest(gameweek, action, body, signal) {
  const headers = { Accept: 'application/json' }
  if (body) {
    const response = await fetch('/api/auth/csrf', { credentials: 'include', signal })
    if (!response.ok) throw new FantasyEntryError('Không lấy được xác nhận phiên quản trị.', response.status)
    const csrf = await response.json()
    headers[csrf.headerName] = csrf.token; headers['Content-Type'] = 'application/json'
  }
  const response = await fetch('/api/fantasy/2026/admin/gameweeks/' + gameweek + '/' + action, {
    credentials: 'include', headers, signal, ...(body ? { method: 'POST', body: JSON.stringify(body) } : {}),
  })
  const data = await response.json()
  if (!response.ok) {
    const error = new FantasyEntryError(data.message ?? 'Không thực hiện được thao tác kết quả.', response.status, data.code)
    error.readiness = data.readiness
    if (response.status === 401) globalThis.window?.dispatchEvent(new Event('prismaxi-session-refresh'))
    throw error
  }
  if (action === 'readiness' && (data.season !== 2026 || data.gameweek !== gameweek || typeof data.ready !== 'boolean' ||
      !Array.isArray(data.issues) || !Number.isInteger(data.currentVersion))) throw new Error('Readiness không đúng định dạng.')
  return data
}
export const fetchFantasyReadiness = (gw, signal) => adminRequest(gw, 'readiness', null, signal)
export const publishFantasyResults = (gw, body, recalculate, signal) => adminRequest(gw,
  recalculate ? 'recalculate-results' : 'publish-results', body, signal)
