const messages = {
  cancelled: 'Bạn đã hủy đăng nhập Google.',
  provider_error: 'Không đăng nhập được với Google. Vui lòng thử lại hoặc dùng email và mật khẩu.',
  invalid_identity: 'Google chưa cung cấp danh tính hoặc email đã xác minh hợp lệ.',
  invalid_flow: 'Phiên Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.',
  session_changed: 'Phiên đăng nhập đã thay đổi. Hãy bắt đầu liên kết Google lại.',
  try_again: 'Có thao tác đồng thời với tài khoản này. Vui lòng thử lại.',
  identity_linked: 'Google này đã được liên kết với tài khoản khác.',
  link_required: 'Email này đã có tài khoản. Hãy đăng nhập tài khoản đó, chọn Liên kết Google và xác nhận liên kết.',
}

export function googleResult(search) {
  const result = new URLSearchParams(search).get('google')
  if (result === 'success' || result === 'confirm_link') return { result, message: '' }
  return messages[result] ? { result, message: messages[result] } : null
}

export function googleReturnPath(location) {
  const params = new URLSearchParams(location.search)
  params.delete('google')
  const query = params.toString()
  return `${location.pathname}${query ? '?' + query : ''}${location.hash}`
}
