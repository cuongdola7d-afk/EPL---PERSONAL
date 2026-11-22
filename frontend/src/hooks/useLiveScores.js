import { useEffect, useRef, useState } from 'react'
import { fetchLiveMatches, openLiveStream } from '../api/liveStream.js'
import { applyLiveScoreUpdate, applyLiveSnapshot } from '../utils/liveScores.js'

const FLASH_DURATION_MS = 3000
const FALLBACK_THRESHOLD_MS = 120000
const FALLBACK_POLL_INTERVAL_MS = 60000

export function useLiveScores(enabled = true) {
  const [liveByFixture, setLiveByFixture] = useState({})
  const [flashingIds, setFlashingIds] = useState({})
  const flashTimersRef = useRef(new Map())

  useEffect(() => {
    const timers = flashTimersRef.current
    if (!enabled) {
      setLiveByFixture({})
      setFlashingIds({})
      for (const timer of timers.values()) clearTimeout(timer)
      timers.clear()
      return undefined
    }

    const abortController = new AbortController()
    let continuousErrorSince = null
    let fallbackPollTimer = null

    function triggerFlash(fixtureId) {
      setFlashingIds((prev) => ({ ...prev, [fixtureId]: true }))
      const existing = timers.get(fixtureId)
      if (existing) clearTimeout(existing)
      const timer = setTimeout(() => {
        timers.delete(fixtureId)
        setFlashingIds((prev) => {
          if (!prev[fixtureId]) return prev
          const copy = { ...prev }
          delete copy[fixtureId]
          return copy
        })
      }, FLASH_DURATION_MS)
      timers.set(fixtureId, timer)
    }

    function applySnapshotData(list) {
      setLiveByFixture((prev) => {
        const { byFixture, flashedIds } = applyLiveSnapshot(prev, list)
        for (const id of flashedIds) triggerFlash(id)
        return byFixture
      })
    }

    function stopFallbackPolling() {
      continuousErrorSince = null
      if (fallbackPollTimer !== null) {
        clearInterval(fallbackPollTimer)
        fallbackPollTimer = null
      }
    }

    function pollSnapshotFallback() {
      fetchLiveMatches(abortController.signal)
        .then((list) => {
          if (!abortController.signal.aborted) {
            applySnapshotData(list)
          }
        })
        .catch(() => {
          // Ignore transient snapshot errors
        })
    }

    // 1. Initial HTTP snapshot via Vercel / proxy
    pollSnapshotFallback()

    // 2. Open SSE stream (via VITE_LIVE_STREAM_BASE when configured)
    let stream = null
    try {
      stream = openLiveStream({
        path: '/api/live/stream',
        onOpen: () => {
          stopFallbackPolling()
        },
        onHello: () => {
          stopFallbackPolling()
        },
        onSnapshot: (list) => {
          stopFallbackPolling()
          applySnapshotData(list)
        },
        onScore: (item) => {
          stopFallbackPolling()
          setLiveByFixture((prev) => {
            const { byFixture, scoreChanged } = applyLiveScoreUpdate(prev, item)
            if (scoreChanged) {
              triggerFlash(item.fixtureId)
            }
            return byFixture
          })
        },
        onError: () => {
          const now = Date.now()
          if (continuousErrorSince === null) {
            continuousErrorSince = now
          } else if (now - continuousErrorSince >= FALLBACK_THRESHOLD_MS && fallbackPollTimer === null) {
            pollSnapshotFallback()
            fallbackPollTimer = setInterval(pollSnapshotFallback, FALLBACK_POLL_INTERVAL_MS)
          }
        },
      })
    } catch {
      // Fallback to 60s snapshot polling if EventSource cannot be created
      fallbackPollTimer = setInterval(pollSnapshotFallback, FALLBACK_POLL_INTERVAL_MS)
    }

    return () => {
      abortController.abort()
      stopFallbackPolling()
      if (stream) {
        stream.close()
      }
      for (const timer of timers.values()) clearTimeout(timer)
      timers.clear()
    }
  }, [enabled])

  return { liveByFixture, flashingIds }
}
