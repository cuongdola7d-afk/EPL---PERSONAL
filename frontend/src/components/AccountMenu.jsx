import { useEffect, useRef, useState } from 'react'
import { currentAccount, loginAccount, logoutAccount, registerAccount } from '../api/auth.js'
import './AccountMenu.css'

function AccountDialog({ account, initialError, onAccountChange, onClose, onReload }) {
  const dialog = useRef(null)
  const [mode, setMode] = useState('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(initialError)
  const [message, setMessage] = useState('')

  useEffect(() => { dialog.current.showModal() }, [])

  function switchMode(next) {
    setMode(next); setError(''); setMessage(''); setPassword(''); setConfirmation('')
  }

  async function submit(event) {
    event.preventDefault()
    setError(''); setMessage('')
    if (mode === 'register' && password !== confirmation) { setError('Mật khẩu xác nhận chưa khớp.'); return }
    if (new TextEncoder().encode(password).length > 72) { setError('Mật khẩu không được vượt quá 72 byte UTF-8.'); return }
    setBusy(true)
    try {
      if (mode === 'register') {
        const created = await registerAccount({ displayName: name.trim(), email: email.trim(), password })
        setEmail(created.email); setMode('login'); setPassword(''); setConfirmation('')
        setMessage('Đăng ký thành công. Bạn có thể đăng nhập ngay.')
      } else {
        onAccountChange(await loginAccount({ email: email.trim(), password }))
        setPassword(''); setConfirmation(''); setMessage('Đăng nhập thành công.')
      }
    } catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }

  async function logout() {
    setError(''); setMessage(''); setBusy(true)
    try {
      await logoutAccount(); onAccountChange(null)
      setEmail(''); setPassword(''); setConfirmation(''); setMode('login'); setMessage('Đã đăng xuất.')
    } catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }

  return <dialog ref={dialog} className="account-dialog" aria-labelledby="account-title" onCancel={onClose}
    onClose={onClose} onClick={event => { if (event.target === event.currentTarget && !busy) onClose() }}>
    <button className="account-close" type="button" onClick={onClose} aria-label="Đóng tài khoản">×</button>
    <p className="account-eyebrow">prismaXI</p>
    <h2 id="account-title">{account ? 'Tài khoản của bạn' : mode === 'register' ? 'Tạo tài khoản' : 'Chào mừng trở lại'}</h2>
    {error && <p className="account-error" role="alert">{error}</p>}
    {message && <p className="account-message" role="status">{message}</p>}
    {initialError && <button type="button" className="account-retry" onClick={async () => {
      setBusy(true)
      try { await onReload(); setError('') } catch (failure) { setError(failure.message) } finally { setBusy(false) }
    }} disabled={busy}>Thử kết nối lại</button>}
    {account ? <section className="account-profile" aria-label="Thông tin tài khoản">
      <strong>{account.displayName}</strong><p>{account.email}</p>
      <button type="button" className="account-submit" disabled={busy} onClick={logout}>{busy ? 'Đang đăng xuất…' : 'Đăng xuất'}</button>
    </section> : <>
      <div className="account-tabs" aria-label="Chọn đăng nhập hoặc đăng ký">
        <button type="button" aria-pressed={mode === 'login'} onClick={() => switchMode('login')} disabled={busy}>Đăng nhập</button>
        <button type="button" aria-pressed={mode === 'register'} onClick={() => switchMode('register')} disabled={busy}>Đăng ký</button>
      </div>
      <form onSubmit={submit} aria-busy={busy}>
        <fieldset disabled={busy}>
          {mode === 'register' && <label>Tên hiển thị<input name="displayName" autoComplete="nickname" required minLength={2} maxLength={80}
            value={name} onChange={event => setName(event.target.value)} /></label>}
          <label>Email<input name="email" type="email" autoComplete="email" required maxLength={254}
            value={email} onChange={event => setEmail(event.target.value)} /></label>
          <label>Mật khẩu<input name="password" type="password" autoComplete={mode === 'register' ? 'new-password' : 'current-password'} required
            minLength={mode === 'register' ? 8 : 1} maxLength={72} value={password} onChange={event => setPassword(event.target.value)} /></label>
          {mode === 'register' && <><p className="account-hint">Từ 8 ký tự, tối đa 72 byte UTF-8.</p>
            <label>Xác nhận mật khẩu<input name="confirmation" type="password" autoComplete="new-password" required
              value={confirmation} onChange={event => setConfirmation(event.target.value)} /></label></>}
          <button type="submit" className="account-submit">{busy ? 'Đang xử lý…' : mode === 'register' ? 'Đăng ký' : 'Đăng nhập'}</button>
        </fieldset>
      </form>
    </>}
  </dialog>
}

export default function AccountMenu() {
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [open, setOpen] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    currentAccount(controller.signal).then(value => { setAccount(value); setError('') })
      .catch(failure => { if (failure.name !== 'AbortError') setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [])

  function updateAccount(value) { setAccount(value); setError('') }
  async function reload() { const value = await currentAccount(); updateAccount(value) }

  return <div className="account-menu">
    <button className="account-trigger" type="button" disabled={loading} onClick={() => setOpen(true)}
      aria-haspopup="dialog" title={account ? 'Mở tài khoản' : 'Đăng nhập hoặc đăng ký'}>
      {loading ? 'Đang tải…' : account ? account.displayName : 'Đăng nhập'}
    </button>
    {open && <AccountDialog account={account} initialError={error} onAccountChange={updateAccount}
      onClose={() => setOpen(false)} onReload={reload} />}
  </div>
}
