export class AdminApiError extends Error {
  constructor(message, status) {
    super(message)
    this.status = status
  }
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
    throw new AdminApiError(data?.message ?? 'Không tải được dữ liệu quản trị.', response.status)
  }
  return data
}
