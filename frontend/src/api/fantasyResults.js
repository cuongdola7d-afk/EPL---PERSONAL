import { fantasyAdminRequest } from './fantasyAdmin.js'

async function adminRequest(gameweek, action, body, signal) {
  const data = await fantasyAdminRequest(gameweek, action, body, signal)
  if (action === 'readiness' && (data.season !== 2026 || data.gameweek !== gameweek || typeof data.ready !== 'boolean' ||
      !Array.isArray(data.issues) || !Number.isInteger(data.currentVersion))) throw new Error('Readiness không đúng định dạng.')
  return data
}
export const fetchFantasyReadiness = (gw, signal) => adminRequest(gw, 'readiness', null, signal)
export const publishFantasyResults = (gw, body, recalculate, signal) => adminRequest(gw,
  recalculate ? 'recalculate-results' : 'publish-results', body, signal)
