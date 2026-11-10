import { useCallback, useEffect, useState } from 'react'
import { fetchMatches } from '../api/matches.js'
import { completedTeamOfWeekGameweeks } from '../fantasy/teamOfWeek.js'

export function useCompletedGameweeks() {
  const [result, setResult] = useState({ weeks: [], status: 'loading', error: '' })
  const [revision, setRevision] = useState(0)
  const reload = useCallback(() => setRevision(value => value + 1), [])
  useEffect(() => {
    const controller = new AbortController()
    // Retain the last verified calendar while polling; do not reset a valid selection.
    setResult(current => ({ ...current, status: 'loading', error: '' }))
    fetchMatches({ season: 2026 }, controller.signal).then(matches => {
      if (!controller.signal.aborted) setResult({ weeks: completedTeamOfWeekGameweeks(matches), status: 'success', error: '' })
    }).catch(error => {
      if (!controller.signal.aborted) setResult(current => ({ ...current, status: 'error', error: error.message }))
    })
    return () => controller.abort()
  }, [revision])
  return { ...result, reload }
}
