import { FantasyEntryError } from './fantasyEntries.js'

export async function fantasyAdminRequest(gameweek, action, body, signal) {
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
    const error = new FantasyEntryError(data.message ?? 'Không thực hiện được thao tác quản trị.', response.status, data.code)
    error.readiness = data.readiness
    if (response.status === 401) globalThis.window?.dispatchEvent(new Event('prismaxi-session-refresh'))
    throw error
  }
  return data
}
