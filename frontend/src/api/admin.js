export class AdminApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
}

async function parseAdminResponse(response, fallbackMessage) {
  let data = null
  try {
    data = await response.json()
  } catch {
    data = null
  }

  if (response.status === 401) {
    throw new AdminApiError(data?.message ?? 'Bạn cần đăng nhập tài khoản quản trị.', 401)
  }
  if (response.status === 403) {
    throw new AdminApiError('Chỉ tài khoản ADMIN mới được truy cập trang Quản trị.', 403)
  }
  if (!response.ok || !data) {
    throw new AdminApiError(data?.message ?? fallbackMessage, response.status)
  }
  return data
}

export async function fetchAdminOverview(range = 30, signal) {
  let response
  try {
    response = await fetch(`/api/admin/overview?range=${encodeURIComponent(range)}`, {
      credentials: 'include',
      headers: { Accept: 'application/json' },
      signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new AdminApiError('Không kết nối được máy chủ quản trị. Vui lòng thử lại.', 0)
  }
  return parseAdminResponse(response, 'Không tải được dữ liệu quản trị.')
}

export async function fetchAdminDataHealth(signal) {
  let response
  try {
    response = await fetch('/api/admin/data-health', {
      credentials: 'include',
      headers: { Accept: 'application/json' },
      signal,
    })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new AdminApiError('Không kết nối được máy chủ để kiểm tra sức khỏe dữ liệu.', 0)
  }
  return parseAdminResponse(response, 'Không tải được kiểm tra sức khỏe dữ liệu.')
}
