export function buildApiUrl(path, baseUrl, isDevelopment) {
  const apiPath = `/${path.replace(/^\/+/, '')}`
  if (isDevelopment) return apiPath

  if (!baseUrl?.trim()) {
    throw new Error('Chưa cấu hình VITE_API_BASE_URL cho bản production.')
  }

  let url
  try {
    url = new URL(baseUrl.trim().replace(/\/+$/, ''))
  } catch {
    throw new Error('VITE_API_BASE_URL phải là URL backend hợp lệ.')
  }

  if (!['http:', 'https:'].includes(url.protocol) || !url.hostname ||
      url.pathname !== '/' || url.search || url.hash || url.username || url.password) {
    throw new Error('VITE_API_BASE_URL phải là origin backend, không chứa /api hoặc đường dẫn khác.')
  }

  return `${url.origin}${apiPath}`
}

export async function fetchApiList(path, signal, isValidItem, itemName) {
  const url = buildApiUrl(path, import.meta.env.VITE_API_BASE_URL, import.meta.env.DEV)
  const response = await fetch(url, {
    headers: { Accept: 'application/json' },
    signal,
  })

  if (!response.ok) {
    throw new Error(`API trả về mã HTTP ${response.status}.`)
  }

  let items
  try {
    items = await response.json()
  } catch {
    throw new Error('API không trả về JSON hợp lệ.')
  }

  if (!Array.isArray(items) || items.some((item) => !isValidItem(item))) {
    throw new Error(`Dữ liệu ${itemName} từ API không đúng định dạng.`)
  }

  return items
}
