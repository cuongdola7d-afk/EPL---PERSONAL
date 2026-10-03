import { useEffect, useState } from 'react'

export function useApiResource(request) {
  const [result, setResult] = useState({ data: null, status: 'loading', error: '' })
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setResult({ data: null, status: 'loading', error: '' })
    request(controller.signal).then(data => {
      if (!controller.signal.aborted) setResult({ data, status: 'success', error: '' })
    }).catch(error => {
      if (!controller.signal.aborted) setResult({ data: null, status: 'error', error: error.message })
    })
    return () => controller.abort()
  }, [request, revision])
  return { ...result, reload: () => setRevision(value => value + 1) }
}
