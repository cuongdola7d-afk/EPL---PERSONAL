import { useEffect, useRef, useState } from 'react'
import { fetchGuessHistory, fetchGuessInfo, fetchGuessLeaderboard, fetchGuessPlayers } from '../api/playerGuess.js'
import usePlayerGuessGame from '../hooks/usePlayerGuessGame.js'
import { formatCountdown, gameHash, isFinished, remainingSeconds } from '../minigame/playerGuess.js'
import { matchesPlayerSearch } from '../utils/playerSort.js'
import { getInitials } from '../utils/initials.js'
import './MinigamePage.css'

const openAccount = () => window.dispatchEvent(new Event('prismaxi-open-account'))
const ICONS = {
  height: <><path d="M8 3v18M5 6l3-3 3 3M5 18l3 3 3-3M16 5h4M16 10h3M16 15h4M16 20h3" /></>,
  foot: <><path d="M8 3v10l-4 4v4h16v-4l-6-3V3M4 18h16" /></>,
  age: <><rect x="4" y="6" width="16" height="15" rx="3" /><path d="M8 3v6M16 3v6M4 12h16M9 16h6" /></>,
  ovr: <><path d="m12 3 3 6 6 1-4 5 1 6-6-3-6 3 1-6-4-5 6-1z" /></>,
  nationality: <><circle cx="12" cy="12" r="9" /><ellipse cx="12" cy="12" rx="4" ry="9" /><path d="M3 12h18" /></>,
  position: <><rect x="4" y="3" width="16" height="18" rx="2" /><path d="M4 12h16M8 3v4h8V3M8 21v-4h8v4" /><circle cx="12" cy="12" r="3" /></>,
  club: <><path d="M12 3 4 6v6c0 5 8 9 8 9s8-4 8-9V6zM8 12l3 3 5-6" /></>,
  shirtNumber: <><path d="m8 3 4 2 4-2 5 4-3 4-2-1v11H8V10l-2 1-3-4z" /></>,
  search: <><circle cx="10" cy="10" r="6" /><path d="m15 15 6 6" /></>,
  trophy: <><path d="M7 3h10v6a5 5 0 0 1-10 0zM7 5H3v3a4 4 0 0 0 4 4M17 5h4v3a4 4 0 0 1-4 4M12 14v7M8 21h8" /></>,
  arrow: <path d="m8 5 7 7-7 7" />,
}
function Icon({ name, ...props }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" {...props}>{ICONS[name] ?? ICONS.age}</svg>
}
function Countdown({ source, onExpire }) {
  const [elapsed, setElapsed] = useState(0)
  const expire = useRef(onExpire); expire.current = onExpire
  const notified = useRef(null)
  const serverTime = source?.serverTime, deadline = source?.nextDailyAt
  useEffect(() => {
    const start = performance.now()
    setElapsed(0)
    const tick = () => {
      const elapsedMs = performance.now() - start
      setElapsed(elapsedMs)
      if (remainingSeconds(serverTime, deadline, elapsedMs) === 0 && notified.current !== deadline) {
        notified.current = deadline; expire.current?.()
      }
    }
    tick()
    const timer = setInterval(tick, 1000)
    return () => clearInterval(timer)
  }, [serverTime, deadline])
  return <time className="mg-clock" title="Câu hỏi mới lúc 00:00, giờ Việt Nam">{formatCountdown(remainingSeconds(serverTime, deadline, elapsed))}</time>
}
function Sheet({ title, onClose, children }) {
  const dialog = useRef(null)
  useEffect(() => {
    const previous = document.activeElement
    dialog.current.showModal()
    return () => { previous?.focus?.() }
  }, [])
  return <dialog ref={dialog} className="mg-sheet" aria-labelledby="mg-sheet-title" onCancel={onClose} onClose={onClose}
    onClick={event => { if (event.target === event.currentTarget) {
      const box = event.currentTarget.getBoundingClientRect()
      if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) onClose()
    } }}>
    <div className="mg-sheet-header"><h2 id="mg-sheet-title">{title}</h2><button className="mg-icon-button" onClick={onClose} aria-label="Đóng">×</button></div>
    {children}
  </dialog>
}
function Rules({ onClose }) {
  return <Sheet title="Cách chơi" onClose={onClose}>
    <ol className="mg-rules">{[
      'Bắt đầu với 100 điểm, mở sẵn chiều cao, chân thuận và tuổi; tối đa 3 lần đoán. Cả daily và luyện tập đều cần đăng nhập.',
      'Gợi ý mở theo thứ tự: chiều cao, chân thuận, tuổi, OVR FC 27, quốc tịch, vị trí, câu lạc bộ, số áo.',
      'Mở gợi ý tiếp theo mất 10 điểm. Đoán sai mất 20 điểm, 1 lượt và tự mở thêm 1 gợi ý miễn phí.',
      'Chọn cầu thủ trong danh sách rồi xác nhận. Đoán lại cùng cầu thủ hoặc tên chưa được chọn không trừ điểm hay lượt.',
      'Đoán đúng giữ số điểm hiện tại, kể cả 0 điểm. Sai cả 3 lần nhận 0 điểm. Điểm luôn từ 0 trở lên.',
      'Daily có một câu hỏi chung mỗi ngày; chỉ điểm daily hoàn tất được cộng vào BXH. Ván dở hết hạn lúc 00:00 giờ Việt Nam nhận 0 điểm. Luyện tập không hết hạn theo ngày và không cộng BXH.',
    ].map((rule, index) => <li key={rule}><span>{index + 1}</span><div>{rule}</div></li>)}</ol>
    <p className="mg-example">Ví dụ: 100 điểm → mở gợi ý: 90 → đoán sai: 70 → đoán đúng: nhận 70 điểm.</p>
  </Sheet>
}
function ErrorBox({ message, onRetry, disabled, children, retryLabel = 'Thử lại' }) {
  if (!message) return null
  return <div className="mg-error" role="alert"><p>{message}</p>{onRetry && <button className="mg-secondary" disabled={disabled} onClick={onRetry}>{retryLabel}</button>}{children}</div>
}
function Loading() {
  return <div className="mg-panel" role="status" aria-label="Đang tải Minigame"><div className="mg-skeleton" /><div className="mg-skeleton" /><span className="sr-only">Đang tải…</span></div>
}
function LoginGate({ authLoading, authError }) {
  return <div className="mg-panel mg-empty"><Icon name="club" /><h2>{authLoading ? 'Đang kiểm tra tài khoản…' : 'Đăng nhập để bắt đầu'}</h2>
    <p>{authError || 'Tiến trình được lưu theo tài khoản. Bạn có thể trở lại ván đang chơi trên local.'}</p>
    <button className="mg-primary" disabled={authLoading} onClick={openAccount}>Đăng nhập / Đăng ký</button></div>
}
function ModePicker({ account, source, onClose }) {
  return <Sheet title="Đoán cầu thủ qua gợi ý" onClose={onClose}>
    <p className="mg-muted">Chọn cách chơi của bạn · Premier League 2026/27</p>
    {['DAILY', 'PRACTICE'].map(mode => <a key={mode} className={`mg-mode mg-mode-${mode.toLowerCase()}`} href={gameHash(mode)} onClick={onClose}>
      <div className="mg-mode-top"><span className="mg-mode-icon"><Icon name={mode === 'DAILY' ? 'age' : 'position'} /></span>
        <div><b>{mode === 'DAILY' ? 'Thử thách hằng ngày' : 'Luyện tập tự do'}</b><span className="mg-muted">{mode === 'DAILY' ? 'Cùng một câu hỏi, cùng thử tài trí nhớ.' : 'Chơi nhiều ván, khám phá thêm cầu thủ.'}</span></div></div>
      <div className="mg-tags"><span> {account ? 'Đã đăng nhập' : 'Cần đăng nhập'}</span><span>{mode === 'DAILY' ? '1 ván / ngày · Có BXH' : 'Không giới hạn · Không cộng BXH'}</span></div>
      <div className="mg-mode-go">{mode === 'DAILY' ? <>Câu hỏi mới sau <Countdown source={source} /></> : <>Vào luyện tập <Icon name="arrow" /></>}</div>
    </a>)}
  </Sheet>
}
function Hub({ account, source, onRefresh, onPick }) {
  const daily = usePlayerGuessGame(account?.id, 'DAILY')
  const game = daily.game
  const status = !account ? 'Đăng nhập để chơi thử thách hằng ngày.' : daily.loading ? 'Đang kiểm tra tiến trình hôm nay…' :
    isFinished(game) ? `Hôm nay bạn đã chơi xong: ${game.finalScore} điểm. Hẹn gặp lại ngày mai.` : game ? 'Bạn đang chơi dở. Tiếp tục để hoàn thành ván hôm nay.' : 'Mọi người nhận cùng một cầu thủ bí ẩn. Mỗi tài khoản có một ván mỗi ngày.'
  return <>
    <section className="mg-hero"><svg className="mg-deco" viewBox="0 0 200 200" fill="none" stroke="currentColor" strokeWidth="3" aria-hidden="true"><circle cx="100" cy="100" r="80" /><circle cx="100" cy="100" r="46" /><circle cx="100" cy="100" r="6" /><path d="M100 20v160" /></svg>
      <p className="mg-eyebrow">PRISMAXI / PLAY</p><h1>Minigame</h1><p>Chơi vui, thử trí nhớ bóng đá của bạn và leo bảng xếp hạng riêng của từng trò.</p></section>
    <div className="mg-daily"><span className="mg-mode-icon"><Icon name="age" /></span><div className="mg-daily-copy"><b>Thử thách hôm nay: Đoán cầu thủ</b><span>{status}</span><a href={gameHash('DAILY')}>{game ? 'Xem ván hôm nay' : 'Vào thử thách'} <span aria-hidden="true">→</span></a></div>
      <div className="mg-daily-clock"><span>Câu hỏi mới sau</span><Countdown source={daily.current ?? source} onExpire={() => { daily.refresh(); onRefresh() }} /></div></div>
    <ErrorBox message={daily.error} onRetry={daily.refresh} disabled={daily.loading} retryLabel="Kiểm tra tiến trình" />
    <h2 className="mg-section-title">Chọn trò chơi</h2><div className="mg-game-grid">
      {[
        ['guess', 'Đoán cầu thủ qua gợi ý', 'Bắt đầu với 100 điểm và ba gợi ý. Mở thêm gợi ý hoặc đoán, càng ít gợi ý bạn càng được nhiều điểm.'],
        ['higher', 'Cao hay thấp', 'Hai cầu thủ xuất hiện, bạn đoán ai có chỉ số cao hơn. Đoán đúng liên tiếp để lập kỷ lục.'],
        ['lineup', 'Đội hình bí ẩn', 'Nhận diện các cầu thủ trong đội hình. Thử trí nhớ của bạn qua những gương mặt quen thuộc.'],
      ].map(([id, title, description], index) => <article className={`mg-game-card ${index ? 'mg-soon' : ''}`} key={id}>
        <div className="mg-art"><img src={`/minigame/${id}.svg`} alt="" /><span className="mg-badge">{index ? 'Sắp ra mắt' : 'Có thử thách hằng ngày'}</span></div>
        <div className="mg-card-body"><h3>{title}</h3><p>{description}</p><button className="mg-primary" disabled={!!index} onClick={onPick}>{index ? 'Sắp ra mắt' : <>Chơi ngay <Icon name="arrow" /></>}</button></div>
      </article>)}
    </div><p className="mg-footnote">Mỗi trò có bảng xếp hạng riêng. Điểm Minigame độc lập với Fantasy.</p>
  </>
}
function Leaderboard({ account, revision }) {
  const [offset, setOffset] = useState(0), [retry, setRetry] = useState(0)
  const [state, setState] = useState({ data: null, loading: true, error: '' })
  useEffect(() => {
    const controller = new AbortController()
    setState({ data: null, loading: true, error: '' })
    fetchGuessLeaderboard(offset, controller.signal).then(data => { if (!controller.signal.aborted) setState({ data, loading: false, error: '' }) })
      .catch(failure => { if (!controller.signal.aborted) setState({ data: null, loading: false, error: failure.message }) })
    return () => controller.abort()
  }, [offset, retry, revision])
  return <section className="mg-panel"><div className="mg-board-heading"><div><h2><Icon name="trophy" /> Bảng xếp hạng</h2><p className="mg-muted">Tổng điểm daily · Đoán cầu thủ · 2026/27</p></div><button className="mg-secondary" onClick={() => setRetry(value => value + 1)} disabled={state.loading}>Làm mới</button></div>
    {state.loading ? <Loading /> : state.error ? <ErrorBox message={state.error} onRetry={() => setRetry(value => value + 1)} /> : <>
      {!state.data.players.length && <div className="mg-empty"><h3>Chưa có kết quả ở trang này</h3><p>Hoàn tất thử thách daily để xuất hiện trên bảng xếp hạng.</p></div>}
      <ol className="mg-rank-list">{state.data.players.map(player => <li key={player.accountId} className={`mg-rank ${player.accountId === account?.id ? 'mg-me' : ''}`}>
        <span className="mg-rank-number">{player.rank}</span><span className="mg-avatar">{getInitials(player.displayName)}</span>
        <div className="mg-rank-name"><b>{player.displayName}{player.accountId === account?.id ? ' (Bạn)' : ''}</b><span>{player.dailyGames} ván daily{player.tied ? ' · Đồng hạng' : ''}</span></div><b className="mg-rank-points">{player.totalPoints}<small>điểm</small></b>
      </li>)}</ol>
      <div className="mg-pagination"><button className="mg-secondary" disabled={offset === 0} onClick={() => setOffset(value => value - 20)}>← Trước</button><span>Trang {offset / 20 + 1}</span><button className="mg-secondary" disabled={state.data.players.length < 20 || offset >= 100000} onClick={() => setOffset(value => value + 20)}>Sau →</button></div>
    </>}
  </section>
}
function GuessForm({ accountId, game, locked, onGuess }) {
  const [players, setPlayers] = useState(null), [error, setError] = useState(''), [reload, setReload] = useState(0)
  const [query, setQuery] = useState(''), [selected, setSelected] = useState(null), [highlight, setHighlight] = useState(-1)
  const [focused, setFocused] = useState(false)
  useEffect(() => {
    const controller = new AbortController()
    setPlayers(null); setError('')
    fetchGuessPlayers(accountId, game.gameId, controller.signal).then(data => { if (!controller.signal.aborted) setPlayers(data) })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
    return () => controller.abort()
  }, [accountId, game.gameId, reload])
  useEffect(() => { setSelected(null); setQuery(''); setHighlight(-1) }, [game.version])
  const guessed = new Set(game.guesses.map(guess => guess.playerId))
  const options = (players ?? []).filter(player => matchesPlayerSearch(player.name, query)).slice(0, 6)
  const disabled = locked || !game.canGuess
  const expanded = focused && !!query.trim() && !selected && !disabled
  const choose = player => { if (!guessed.has(player.playerId)) { setSelected(player); setQuery(player.name); setFocused(false) } }
  return <><h2>Đoán cầu thủ</h2><p className="mg-muted">Tìm tên và chọn một cầu thủ để xác nhận.</p>
    <form onSubmit={event => { event.preventDefault(); if (selected && !disabled && !guessed.has(selected.playerId)) onGuess(selected.playerId) }}>
      <div className="mg-search"><Icon name="search" /><input role="combobox" aria-label="Tìm cầu thủ" aria-autocomplete="list" aria-expanded={expanded} aria-controls="mg-suggestions"
        aria-activedescendant={expanded && highlight >= 0 ? `mg-option-${highlight}` : undefined} autoComplete="off" placeholder={players ? 'Nhập tên cầu thủ…' : 'Đang tải danh sách…'}
        disabled={disabled || !players} value={query} onFocus={() => setFocused(true)} onBlur={event => { if (!event.currentTarget.parentElement.parentElement.contains(event.relatedTarget)) setFocused(false) }}
        onChange={event => { setQuery(event.target.value); setSelected(null); setHighlight(-1); setFocused(true) }}
        onKeyDown={event => {
          if (event.key === 'Escape') { setFocused(false); return }
          if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
            event.preventDefault(); setFocused(true)
            setHighlight(value => Math.max(0, Math.min(options.length - 1, value + (event.key === 'ArrowDown' ? 1 : -1))))
          }
          if (event.key === 'Enter' && !selected) { event.preventDefault(); if (expanded && highlight >= 0 && options[highlight]) choose(options[highlight]) }
        }} /></div>
      {expanded && <div className="mg-suggestions" role="listbox" id="mg-suggestions" aria-label="Cầu thủ phù hợp">
        {options.length ? options.map((player, index) => <button type="button" role="option" aria-selected={index === highlight} aria-disabled={guessed.has(player.playerId)} id={`mg-option-${index}`} key={player.playerId}
          className={index === highlight ? 'mg-highlight' : ''} onMouseDown={event => event.preventDefault()} onClick={() => choose(player)} disabled={guessed.has(player.playerId)}>
          <span>{player.name}</span>{guessed.has(player.playerId) && <small>Đã đoán</small>}</button>) : <p className="mg-muted">Không tìm thấy cầu thủ trong danh sách của ván này.</p>}
      </div>}
      {selected && <div className="mg-selected"><b>{selected.name}</b><button type="button" disabled={disabled} onClick={() => { setSelected(null); setQuery('') }}>Bỏ chọn</button></div>}
      {error && <ErrorBox message={error} onRetry={() => setReload(value => value + 1)} />}
      <button className="mg-primary mg-confirm" type="submit" disabled={disabled || !selected || guessed.has(selected.playerId)}>{locked ? 'Đang đồng bộ…' : 'Xác nhận dự đoán'}</button>
    </form></>
}
function GuessHistory({ game }) {
  return <div className="mg-history"><h3>Lịch sử đoán</h3>{!game.guesses.length && <p className="mg-no-guesses">Chưa có lượt đoán nào. Bạn sẽ chọn ai?</p>}
    {game.guesses.map(guess => <div className={`mg-guess ${guess.correct ? 'mg-correct' : ''}`} key={guess.number}><span className="mg-guess-mark">{guess.correct ? '✓' : '×'}</span>
      <div><b>{guess.name}</b><small>{guess.correct ? 'Chính xác!' : 'Sai'}</small></div><strong>{guess.correct ? `+${game.finalScore}` : `−${guess.penalty}`}</strong></div>)}
  </div>
}
function GameBoard({ game, accountId, state, historical = false, onBoard }) {
  const finished = isFinished(game), locked = historical || state.loading || state.busy || !!state.pending
  const score = finished ? game.finalScore : game.currentScore
  return <div className="mg-play-layout">
    <section className="mg-panel" aria-label="Điểm và gợi ý"><div className="mg-stats">
      <div className={`mg-score ${score === 0 ? 'mg-zero' : score < 40 ? 'mg-low' : ''}`}><b>{score}</b><span>{finished ? 'Điểm kết quả' : 'Điểm hiện tại'}</span></div>
      <div className="mg-turns"><div className="mg-pips">{[0, 1, 2].map(index => <span className={index < game.guessesUsed ? 'mg-used' : 'mg-live'} key={index} aria-hidden="true">{index < game.guessesUsed ? '×' : index + 1}</span>)}</div><span>{game.guessesRemaining} lượt còn lại</span></div>
      <div className="mg-hint-count"><b>{game.hints.filter(hint => hint.revealed).length}<span>/8</span></b><small>Gợi ý đã mở</small></div></div>
      <div className="mg-hint-grid">{game.hints.map(hint => <div key={hint.key} className={`mg-hint ${hint.revealed ? 'mg-open' : 'mg-locked'} ${hint.key === game.nextHintKey ? 'mg-next' : ''} ${state.effect?.revealedHintKey === hint.key ? 'mg-new' : ''}`}>
        <span className="mg-hint-icon"><Icon name={hint.key} /></span><div><span className="mg-hint-label">{hint.label}</span><b className={hint.key === 'ovr' && hint.revealed ? 'mg-ovr' : ''}>{hint.revealed ? hint.value : '•••'}</b></div>
      </div>)}</div>
      <button className="mg-reveal" disabled={locked || !game.canRevealHint} onClick={state.reveal}>{finished ? 'Đã hiển thị toàn bộ gợi ý' : game.canRevealHint ? 'Mở gợi ý tiếp theo · −10 điểm' : 'Đã mở hết gợi ý'}</button>
    </section>
    <section className={`mg-panel ${state.effect?.type === 'WRONG' ? 'mg-shake' : ''}`} aria-label="Dự đoán và kết quả">
      {finished ? <div className={`mg-result ${game.status === 'WON' ? 'mg-win' : ''}`}><div className="mg-result-points">{game.status === 'WON' ? '+' : ''}{game.finalScore}</div>
        <h2>{game.status === 'WON' ? 'Chính xác!' : game.status === 'EXPIRED' ? 'Ván daily đã hết hạn' : 'Hết lượt đoán rồi!'}</h2>
        <p className="mg-muted">{game.status === 'WON' ? game.mode === 'DAILY' ? 'Điểm của bạn đã được ghi vào BXH.' : 'Một ván luyện tập đã hoàn thành.' : game.status === 'EXPIRED' ? 'Ván chưa hoàn tất trước 00:00 giờ Việt Nam nhận 0 điểm.' : 'Cầu thủ bí ẩn lần này là…'}</p>
        <div className="mg-answer"><span className="mg-avatar">{getInitials(game.answer.name)}</span><div><b>{game.answer.name}</b><span>{game.answer.club} · {game.answer.primaryPosition} · #{game.answer.shirtNumber}</span></div></div>
        {!historical && <div className="mg-result-actions">{game.mode === 'PRACTICE' ? <button className="mg-primary" disabled={locked} onClick={state.start}>Chơi ván mới</button> : <a className="mg-primary" href={gameHash('PRACTICE')}>Luyện tập thêm</a>}<button className="mg-secondary" onClick={onBoard}>Xem BXH</button></div>}
        {game.mode === 'DAILY' && !historical && <p className="mg-muted">Thử thách tiếp theo sau <Countdown source={game} onExpire={state.refresh} /></p>}
      </div> : <GuessForm key={game.gameId} accountId={accountId} game={game} locked={locked} onGuess={state.guess} />}
      <GuessHistory game={game} />
    </section>
  </div>
}
function DailyHistory({ accountId, onView, revision }) {
  const [state, setState] = useState({ games: [], loading: true, error: '' }), [retry, setRetry] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    setState({ games: [], loading: true, error: '' })
    fetchGuessHistory(accountId, controller.signal).then(games => { if (!controller.signal.aborted) setState({ games, loading: false, error: '' }) })
      .catch(failure => { if (!controller.signal.aborted) setState({ games: [], loading: false, error: failure.message }) })
    return () => controller.abort()
  }, [accountId, retry, revision])
  return <section className="mg-panel mg-daily-history"><h2>Daily gần đây</h2><ErrorBox message={state.error} onRetry={() => setRetry(value => value + 1)} />
    {state.loading ? <p className="mg-muted">Đang tải lịch sử…</p> : !state.games.length ? <p className="mg-muted">Bạn chưa có ván daily nào.</p> : <div className="mg-history-list">{state.games.map(game => <button className="mg-secondary" key={game.gameId} disabled={!isFinished(game)} onClick={() => onView(game)}><span>{game.questionDate}</span><b>{game.status === 'IN_PROGRESS' ? 'Đang chơi' : `${game.finalScore} điểm${game.status === 'EXPIRED' ? ' · Hết hạn' : ''}`}</b></button>)}</div>}
  </section>
}
function GamePage({ account, authLoading, authError, mode, source, onRules }) {
  const state = usePlayerGuessGame(account?.id, mode)
  const [tab, setTab] = useState('play'), [historical, setHistorical] = useState(null), [historyOpen, setHistoryOpen] = useState(false)
  const [fx, setFx] = useState(null)
  useEffect(() => {
    if (!['CORRECT', 'WRONG'].includes(state.effect?.type)) { setFx(null); return }
    setFx(state.effect)
    const timer = setTimeout(() => setFx(null), 1900)
    return () => clearTimeout(timer)
  }, [state.effect])
  const blocked = state.busy || state.loading || !!state.pending
  return <>
    <div className="mg-game-heading"><a className="mg-back" href="#minigame">← Minigame</a><div className="mg-heading-copy"><h1>Đoán cầu thủ qua gợi ý</h1><span className={`mg-mode-chip ${mode === 'DAILY' ? 'mg-daily-chip' : ''}`}>{mode === 'DAILY' ? <>Daily <span aria-hidden="true">·</span> <Countdown source={state.current ?? source} onExpire={state.refresh} /></> : 'Luyện tập · Không giới hạn'}</span></div>
      <button className="mg-icon-button" aria-label="Xem cách chơi" onClick={onRules}>?</button></div>
    <div className="mg-tabs" aria-label="Nội dung trò chơi"><button aria-pressed={tab === 'play'} onClick={() => setTab('play')}>Chơi</button><button aria-pressed={tab === 'board'} onClick={() => setTab('board')}>Bảng xếp hạng</button></div>
    {tab === 'board' ? <Leaderboard account={account} revision={state.game?.version} /> : <>
      {!account ? <LoginGate authLoading={authLoading} authError={authError} /> : <>
        <ErrorBox message={state.error} onRetry={state.refresh} disabled={state.loading || state.busy} retryLabel="Kiểm tra tiến trình">{state.pending && <button className="mg-secondary" disabled={state.busy || state.loading} onClick={state.retry}>Gửi lại cùng thao tác</button>}</ErrorBox>
        {historical ? <><div className="mg-history-heading"><span>Daily ngày {historical.questionDate} · Lịch sử</span><button className="mg-secondary" onClick={() => setHistorical(null)}>Về ván hôm nay</button></div><GameBoard game={historical} accountId={account.id} state={state} historical onBoard={() => setTab('board')} /></> :
          state.game ? <GameBoard game={state.game} accountId={account.id} state={state} onBoard={() => setTab('board')} /> : state.loading ? <Loading /> : <div className="mg-panel mg-empty"><Icon name={mode === 'DAILY' ? 'age' : 'position'} />
            <h2>{mode === 'DAILY' ? 'Sẵn sàng cho thử thách hôm nay?' : 'Thử tài trí nhớ bóng đá'}</h2><p>100 điểm · 3 gợi ý đầu tiên · 3 lượt đoán</p><p className="mg-muted">{mode === 'DAILY' ? 'Mỗi tài khoản một ván, hết hạn lúc 00:00 giờ Việt Nam.' : 'Chơi bao nhiêu ván tùy bạn. Tiến trình ván dở sẽ được lưu.'}</p>
            <button className="mg-primary" disabled={blocked || !state.current} onClick={state.start}>Bắt đầu {mode === 'DAILY' ? 'daily' : 'luyện tập'}</button></div>}
        {mode === 'DAILY' && <><button className="mg-history-toggle mg-secondary" onClick={() => setHistoryOpen(value => !value)} aria-expanded={historyOpen}>{historyOpen ? 'Ẩn lịch sử daily' : 'Xem lịch sử daily'}</button>{historyOpen && <DailyHistory accountId={account.id} onView={setHistorical} revision={state.game?.version} />}</>}
      </>}
    </>}
    {fx && <div className={`mg-fx ${fx.type === 'CORRECT' ? 'mg-win' : ''}`} aria-hidden="true" key={fx.actionId}><div>{fx.type === 'CORRECT' ? '✓ Chính xác!' : 'Chưa đúng rồi'}<small>{fx.type === 'CORRECT' ? `+${state.game?.finalScore ?? 0} điểm` : `${fx.scoreChange} điểm`}</small></div></div>}
  </>
}
export default function MinigamePage({ account, authLoading, authError, route }) {
  const [info, setInfo] = useState(null), [error, setError] = useState(''), [reload, setReload] = useState(0)
  const [sheet, setSheet] = useState(null), [theme, setTheme] = useState('dark')
  useEffect(() => {
    const controller = new AbortController()
    fetchGuessInfo(controller.signal).then(data => { if (!controller.signal.aborted) { setInfo(data); setError('') } })
      .catch(failure => { if (!controller.signal.aborted) setError(failure.message) })
    return () => controller.abort()
  }, [reload])
  const mode = route?.mode
  return <div className="minigame-page" data-mg-theme={theme}><div className="mg-wrap">
    <div className="mg-toolbar"><span>PREMIER LEAGUE · 2026/27</span><button className="mg-icon-button" onClick={() => setTheme(value => value === 'dark' ? 'light' : 'dark')} aria-label={`Chuyển sang chế độ ${theme === 'dark' ? 'sáng' : 'tối'}`}>◐</button></div>
    <ErrorBox message={error} onRetry={() => setReload(value => value + 1)} />
    {mode ? <GamePage key={mode} account={account} authLoading={authLoading} authError={authError} mode={mode} source={info} onRules={() => setSheet('rules')} /> : <Hub account={account} source={info} onRefresh={() => setReload(value => value + 1)} onPick={() => setSheet('modes')} />}
    {sheet === 'modes' && <ModePicker account={account} source={info} onClose={() => setSheet(null)} />}
    {sheet === 'rules' && <Rules onClose={() => setSheet(null)} />}
  </div></div>
}
