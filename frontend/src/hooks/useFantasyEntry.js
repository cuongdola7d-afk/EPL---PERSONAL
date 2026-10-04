import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchFantasyEntry, saveFantasyDraft, submitFantasyEntry } from '../api/fantasyEntries.js'
import { emptyLineup, entryLineup } from '../fantasy/entry.js'

export function useFantasyEntry(account, gameweek, setLineup) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [conflict, setConflict] = useState(false)
  const [reloadVersion, setReloadVersion] = useState(0)
  const generation = useRef(0)
  const writeRequest = useRef(null)
  const reload = useCallback(() => setReloadVersion(value => value + 1), [])
  useEffect(() => {
    const version = ++generation.current
    const controller = new AbortController()
    setData(null); setError(''); setNotice(''); setConflict(false); setBusy(false)
    if (account) setLineup(emptyLineup())
    if (!account || !gameweek || gameweek < 6) { setLoading(false); return () => { generation.current++; controller.abort() } }
    setLoading(true)
    fetchFantasyEntry(account.id, gameweek, controller.signal).then(value => {
      if (controller.signal.aborted || version !== generation.current) return
      setData(value); setLineup(entryLineup(value))
    }).catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => { generation.current++; controller.abort(); writeRequest.current?.abort() }
  }, [account?.id, gameweek, reloadVersion, setLineup])
  const ready = Boolean(data && data.accountId === account?.id && data.gameweek === gameweek && !loading)
  async function persist(lineup, submit) {
    if (!ready || busy || conflict) return
    const version = generation.current
    const controller = new AbortController(); writeRequest.current = controller
    setBusy(true); setError(''); setNotice('')
    try {
      const action = submit ? submitFantasyEntry : saveFantasyDraft
      const value = await action(account.id, gameweek, { formation: lineup.formation,
        picks: lineup.picks, expectedVersion: data.version }, controller.signal)
      if (controller.signal.aborted || generation.current !== version) return
      setData(value); setNotice(submit ? 'Đã chốt đội hình thành công.' : 'Đã lưu bản nháp trên server.')
    } catch (failure) {
      if (!controller.signal.aborted && version === generation.current) {
        setError(failure.message)
        if (failure.status === 409) setConflict(true)
        if (failure.status === 401 || failure.code === 'SESSION_CHANGED') { setData(null); setLineup(emptyLineup()) }
      }
    } finally { if (!controller.signal.aborted && version === generation.current) setBusy(false) }
  }
  return { data: ready ? data : null, ready, loading, busy, error, notice, conflict, reload,
    save: lineup => persist(lineup, false), submit: lineup => persist(lineup, true) }
}
