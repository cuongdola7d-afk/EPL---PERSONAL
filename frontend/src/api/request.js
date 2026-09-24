export async function fetchApiList(path, signal, isValidItem, itemName) {
  const response = await fetch(path, {
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
