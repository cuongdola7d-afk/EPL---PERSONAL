import { useEffect, useState } from 'react'

export function useApiList(request) {
  const [data, setData] = useState([])
  const [status, setStatus] = useState('loading')
  const [error, setError] = useState('')
  const [reloadCount, setReloadCount] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
      setStatus('loading')
      setError('')
      setData([])

      try {
        const result = await request(controller.signal)
        if (controller.signal.aborted) return
        setData(result)
        setStatus('success')
      } catch (requestError) {
        if (controller.signal.aborted) return
        setError(requestError instanceof Error ? requestError.message : 'Không thể tải dữ liệu.')
        setStatus('error')
      }
    }

    load()
    return () => controller.abort()
  }, [request, reloadCount])

  function reload() {
    setReloadCount((count) => count + 1)
  }

  return { data, status, error, reload }
}
