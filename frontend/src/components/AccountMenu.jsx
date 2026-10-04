import { useEffect, useRef, useState } from 'react'
import { currentAccount, loginAccount, logoutAccount, registerAccount, googleStatus, startGoogle, confirmGoogleLink, cancelGoogleLink } from '../api/auth.js'
import { googleResult, googleReturnPath } from '../utils/googleAuth.js'
import './AccountMenu.css'

function AccountDialog({ account, initialError, googleOutcome, onAccountChange, onClose, onReload }) {
  const dialog = useRef(null)
  const [mode, setMode] = useState('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(initialError || googleOutcome?.message ||
    (googleOutcome?.result === 'success' && !account ? 'Chưa nhận được phiên đăng nhập Google. Vui lòng kiểm tra cookie hoặc thử lại.' : ''))
  const [message, setMessage] = useState(googleOutcome?.result === 'success' && account ? 'Đăng nhập Google thành công.' : '')
  const [google, setGoogle] = useState(null)
  const [googleError, setGoogleError] = useState('')

  useEffect(() => { dialog.current.showModal() }, [])
  useEffect(() => {
    const controller = new AbortController()
    googleStatus(controller.signal).then(value => { setGoogle(value); setGoogleError('') })
      .catch(failure => { if (failure.name !== 'AbortError') setGoogleError(failure.message) })
    return () => controller.abort()
  }, [account?.id])

  async function continueGoogle(mode) {
    setError(''); setMessage(''); setBusy(true)
    try { window.location.assign(await startGoogle(mode, googleReturnPath(window.location))) }
    catch (failure) { setError(failure.message); setBusy(false) }
  }

  async function finishLink(confirm) {
    setError(''); setMessage(''); setBusy(true)
    try {
      if (confirm) { setGoogle(await confirmGoogleLink()); setMessage('Đã liên kết Google với tài khoản của bạn.') }
      else { await cancelGoogleLink(); setGoogle(await googleStatus()); setMessage('Đã hủy liên kết Google.') }
    } catch (failure) { setError(failure.message); setGoogle(await googleStatus().catch(() => null)) }
    finally { setBusy(false) }
  }

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
    {googleError && <p className="account-error" role="alert">{googleError}</p>}
    {initialError && <button type="button" className="account-retry" onClick={async () => {
      setBusy(true)
      try { await onReload(); setError('') } catch (failure) { setError(failure.message) } finally { setBusy(false) }
    }} disabled={busy}>Thử kết nối lại</button>}
    {account ? <section className="account-profile" aria-label="Thông tin tài khoản">
      <strong>{account.displayName}</strong><p>{account.email}</p>
      {google?.pendingEmail ? <div className="account-google-confirm">
        <p>Liên kết Google <strong>{google.pendingEmail}</strong> với tài khoản đang đăng nhập?</p>
        <button type="button" className="account-google" disabled={busy} onClick={() => finishLink(true)}>Xác nhận liên kết Google</button>
        <button type="button" className="account-retry" disabled={busy} onClick={() => finishLink(false)}>Hủy liên kết</button>
      </div> : google?.linked ? <p className="account-hint">Đã liên kết Google</p> :
        <button type="button" className="account-google" disabled={busy || !google?.enabled} onClick={() => continueGoogle('LINK')}>Liên kết Google</button>}
      {google && !google.enabled && <p className="account-hint account-google-hint">Google chưa được cấu hình trên máy chủ.</p>}
      <button type="button" className="account-submit" disabled={busy} onClick={logout}>{busy ? 'Đang đăng xuất…' : 'Đăng xuất'}</button>
    </section> : <>
      <div className="account-tabs" aria-label="Chọn đăng nhập hoặc đăng ký">
        <button type="button" aria-pressed={mode === 'login'} onClick={() => switchMode('login')} disabled={busy}>Đăng nhập</button>
        <button type="button" aria-pressed={mode === 'register'} onClick={() => switchMode('register')} disabled={busy}>Đăng ký</button>
      </div>
      <button type="button" className="account-google" disabled={busy || !google?.enabled} onClick={() => continueGoogle('LOGIN')}>Tiếp tục với Google</button>
      <p className="account-hint account-google-hint">{!google ? googleError ? 'Chưa kiểm tra được Google. Bạn vẫn có thể dùng email và mật khẩu.' : 'Đang kiểm tra Google…' : google.enabled ? 'Hoặc dùng email và mật khẩu' : 'Google chưa được cấu hình. Bạn có thể dùng email và mật khẩu.'}</p>
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

export default function AccountMenu({ onSessionChange }) {
  const [account, setAccount] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [googleOutcome] = useState(() => googleResult(window.location.search))
  const [open, setOpen] = useState(false)
  const sessionRequest = useRef(null)
  const tabId = useRef(crypto.randomUUID())

  useEffect(() => {
    const controller = new AbortController()
    sessionRequest.current = controller
    currentAccount(controller.signal).then(value => {
      if (controller.signal.aborted) return
      setAccount(value); setError('')
      if (googleOutcome) {
        setOpen(true)
        window.history.replaceState(null, '', googleReturnPath(window.location))
      }
    })
      .catch(failure => { if (failure.name !== 'AbortError') setError(failure.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [])

  useEffect(() => { onSessionChange?.({ account, loading, error }) }, [account, loading, error, onSessionChange])

  useEffect(() => {
    const channel = typeof BroadcastChannel === 'function' ? new BroadcastChannel('prismaxi-session') : null
    const refresh = () => {
      sessionRequest.current?.abort()
      const controller = new AbortController(); sessionRequest.current = controller
      setAccount(null); setLoading(true); setError('')
      onSessionChange?.({ account: null, loading: true, error: '' })
      currentAccount(controller.signal).then(value => { if (!controller.signal.aborted) setAccount(value) })
        .catch(failure => { if (failure.name !== 'AbortError') setError(failure.message) })
        .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    }
    window.addEventListener('prismaxi-session-refresh', refresh)
    if (channel) channel.onmessage = event => { if (event.data?.source !== tabId.current) refresh() }
    return () => { sessionRequest.current?.abort(); channel?.close(); window.removeEventListener('prismaxi-session-refresh', refresh) }
  }, [onSessionChange])

  function updateAccount(value) {
    setAccount(value); setError('')
    onSessionChange?.({ account: value, loading: false, error: '' })
    if (typeof BroadcastChannel === 'function') {
      const channel = new BroadcastChannel('prismaxi-session'); channel.postMessage({ changed: true, source: tabId.current }); channel.close()
    }
  }
  async function reload() { const value = await currentAccount(); updateAccount(value) }

  return <div className="account-menu">
    <button className="account-trigger" type="button" disabled={loading} onClick={() => setOpen(true)}
      aria-haspopup="dialog" title={account ? 'Mở tài khoản' : 'Đăng nhập hoặc đăng ký'}>
      {loading ? 'Đang tải…' : account ? account.displayName : 'Đăng nhập'}
    </button>
    {open && <AccountDialog account={account} initialError={error} googleOutcome={googleOutcome} onAccountChange={updateAccount}
      onClose={() => setOpen(false)} onReload={reload} />}
  </div>
}
