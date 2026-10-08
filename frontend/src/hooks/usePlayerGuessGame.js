import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchCurrentGame, mutateGuessGame } from '../api/playerGuess.js'
import { createAction } from '../minigame/playerGuess.js'

export default function usePlayerGuessGame(accountId, mode) {
  const [state, setState] = useState({ current: null, loading: false, busy: false, error: '', pending: null, effect: null })
  const latest = useRef(state); latest.current = state
  const gate = useRef(false)
  const generation = useRef(0)
  const read = useRef(null), write = useRef(null)
  const refresh = useCallback(async () => {
    if (!accountId || !mode || gate.current) return
    read.current?.abort()
    const controller = new AbortController(); read.current = controller
    const run = generation.current
    setState(old => ({ ...old, loading: true, error: '', effect: null }))
    try {
      const current = await fetchCurrentGame(accountId, mode, controller.signal)
      if (!controller.signal.aborted && run === generation.current) {
        setState({ current, loading: false, busy: false, error: '', pending: null, effect: null })
      }
    } catch (failure) {
      if (!controller.signal.aborted && run === generation.current) setState(old => ({ ...old, loading: false, error: failure.message }))
    }
  }, [accountId, mode])
  useEffect(() => {
    generation.current++
    gate.current = false
    setState({ current: null, loading: !!accountId && !!mode, busy: false, error: '', pending: null, effect: null })
    refresh()
    const focused = () => { if (document.visibilityState === 'visible' && !latest.current.pending) refresh() }
    window.addEventListener('focus', focused); document.addEventListener('visibilitychange', focused)
    return () => {
      generation.current++; read.current?.abort(); write.current?.abort()
      window.removeEventListener('focus', focused); document.removeEventListener('visibilitychange', focused)
    }
  }, [refresh, accountId, mode])
  const perform = async (kind, playerId, retry = false) => {
    if (gate.current || !accountId || !mode || latest.current.loading || (!retry && latest.current.pending)) return
    if (!latest.current.current) return
    gate.current = true
    read.current?.abort()
    const controller = new AbortController(); write.current = controller
    const run = generation.current
    const action = retry ? latest.current.pending : createAction(kind, latest.current.current.game, playerId)
    if (!action) { gate.current = false; return }
    setState(old => ({ ...old, busy: true, error: '', effect: null }))
    try {
      const result = await mutateGuessGame(accountId, mode, action, controller.signal)
      if (!controller.signal.aborted && run === generation.current) setState(old => ({ ...old, busy: false, pending: null, error: '',
        current: { status: result.game.status, date: result.game.questionDate, serverTime: result.game.serverTime, nextDailyAt: result.game.nextDailyAt, game: result.game },
        effect: result.effect?.replayed ? null : { ...result.effect, actionId: action.body.actionId } }))
    } catch (failure) {
      if (!controller.signal.aborted && run === generation.current) setState(old => ({ ...old, busy: false,
        error: failure.message, pending: failure.uncertain ? action : null,
        current: failure.game ? { ...old.current, game: failure.game, status: failure.game.status, serverTime: failure.game.serverTime, nextDailyAt: failure.game.nextDailyAt } : old.current }))
    } finally { if (run === generation.current) gate.current = false }
  }
  return { ...state, game: state.current?.game ?? null, refresh,
    start: () => perform('start'), guess: playerId => perform('guess', playerId), reveal: () => perform('hint'), retry: () => perform(null, null, true) }
}
