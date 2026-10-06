import { useCallback, useEffect, useState } from 'react'
import { fetchFantasyResult } from '../api/fantasyEntries.js'

// Share one private result request between the pitch and its total score.
export function useFantasyResult(account, gameweek, contestStatus, submittedVersion, active = true) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [revision, setRevision] = useState(0)
  const refresh = useCallback(() => setRevision(n => n + 1), [])
  const current = data?.accountId === account?.id && data?.gameweek === gameweek ? data : null
  useEffect(() => {
    if (!active || current?.status !== 'AWAITING_RESULTS' || !['LOCKED', 'AWAITING_RESULTS', 'PUBLISHED'].includes(contestStatus)) return
    const update = () => { if (document.visibilityState === 'visible') refresh() }
    const timer = setInterval(update, 60000)
    document.addEventListener('visibilitychange', update)
    return () => { clearInterval(timer); document.removeEventListener('visibilitychange', update) }
  }, [active, current?.status, contestStatus, refresh])
  useEffect(() => {
    const controller = new AbortController()
    setData(null); setError('')
    if (!account || !gameweek || gameweek < 6) { setLoading(false); return () => controller.abort() }
    setLoading(true)
    fetchFantasyResult(account.id, gameweek, controller.signal).then(value => {
      if (!controller.signal.aborted) {
        setData(value)
        if (value.status === 'PUBLISHED' && contestStatus !== 'PUBLISHED') window.dispatchEvent(new Event('prismaxi-results-published'))
      }
    }).catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [account?.id, gameweek, contestStatus, submittedVersion, revision])
  return { data: current, loading, error, refresh }
}
