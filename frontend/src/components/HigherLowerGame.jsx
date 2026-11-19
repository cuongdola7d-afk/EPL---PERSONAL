import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchPlayers } from '../api/players.js'
import {
  STATS,
  advance,
  applyGuess,
  calculateAge,
  cardTier,
  createGame,
  eligiblePlayers,
  formatFoot,
  readBest,
  saveBest,
  statValue,
} from '../minigame/higherLower.js'
import { getInitials } from '../utils/initials.js'
import { clubVisual } from '../utils/matchView.js'
import './HigherLowerGame.css'

const RULES_ITEMS = [
  <>Mỗi lượt có <b>hai thẻ cầu thủ</b>. Thẻ bên trái hiện chỉ số, thẻ bên phải ẩn chỉ số đó (các thông tin khác đều hiện).</>,
  <>Chỉ số đổi ngẫu nhiên mỗi lượt: <b>OVR</b>, <b>bàn thắng</b> hoặc <b>kiến tạo</b> mùa này. Hai người luôn có chỉ số khác nhau.</>,
  <>Bấm <b>mũi tên lên</b> nếu người bên phải cao hơn, <b>mũi tên xuống</b> nếu thấp hơn.</>,
  <>Đúng: chuỗi +1, thẻ bên phải sang trái và có người mới. Sai: chuỗi kết thúc.</>,
  <>Không cần đăng nhập, không có bảng xếp hạng. <b>Kỷ lục</b> của bạn lưu trên trình duyệt đang dùng.</>,
]

function prefersReducedMotion() {
  return typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function ArrowIcon({ direction }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {direction === 'up' ? <path d="M12 19V5M5 12l7-7 7 7" /> : <path d="M12 5v14M5 12l7 7 7-7" />}
    </svg>
  )
}

function FxIcon({ kind }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {kind === 'check' ? <path d="M5 12l5 5 9-10" /> :
        kind === 'star' ? <path d="M12 3l2.7 5.6 6.1.8-4.4 4.2 1.1 6.1L12 17l-5.5 2.7 1.1-6.1L3.2 9.4l6.1-.8z" strokeWidth="2.2" /> :
          <path d="M6 6l12 12M18 6L6 18" />}
    </svg>
  )
}

function HigherLowerRules({ onClose }) {
  const dialogRef = useRef(null)

  useEffect(() => {
    const previous = document.activeElement
    dialogRef.current?.showModal()
    return () => { previous?.focus?.() }
  }, [])

  return (
    <dialog
      ref={dialogRef}
      className="mg-sheet hl-rules-sheet"
      aria-labelledby="hl-rules-title"
      onCancel={onClose}
      onClose={onClose}
      onClick={(event) => {
        if (event.target === event.currentTarget) {
          const box = event.currentTarget.getBoundingClientRect()
          if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) {
            onClose()
          }
        }
      }}
    >
      <div className="mg-sheet-header">
        <div>
          <h2 id="hl-rules-title">Luật chơi</h2>
          <span className="mg-muted">Cao hay thấp</span>
        </div>
        <button type="button" className="mg-icon-button" onClick={onClose} aria-label="Đóng">×</button>
      </div>
      <ol className="mg-rules">
        {RULES_ITEMS.map((item, index) => (
          <li key={index}><span>{index + 1}</span><div>{item}</div></li>
        ))}
      </ol>
      <p className="mg-example">Bàn thắng và kiến tạo là tổng mùa 2026/27 tính đến vòng đã cập nhật gần nhất.</p>
    </dialog>
  )
}

function GameOverDialog({ score, best, newBest, onPlayAgain }) {
  const dialogRef = useRef(null)
  const playAgainRef = useRef(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return undefined
    if (!dialog.open) dialog.showModal()
    playAgainRef.current?.focus()
    return () => {
      if (dialog.open) dialog.close()
    }
  }, [])

  return (
    <dialog
      ref={dialogRef}
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="hl-over-title"
      className="hl-popup"
      onCancel={(event) => event.preventDefault()}
    >
      <div className="hl-pop-body">
        <span className={`hl-pop-icon ${newBest ? 'hl-best' : ''}`} aria-hidden="true">
          <FxIcon kind={newBest ? 'star' : 'x'} />
        </span>
        <h2 id="hl-over-title">{newBest ? 'Kỷ Lục Mới!' : 'Kết Thúc'}</h2>
        <div className="hl-pop-lbl">Điểm Số</div>
        <div className="hl-pop-big">{score}</div>
        <div className="hl-pop-bst">Kỷ Lục <b>{best}</b></div>
        <div className="hl-pop-acts">
          <button ref={playAgainRef} type="button" className="mg-primary" onClick={onPlayAgain}>Chơi Lại</button>
          <a className="mg-secondary" href="#minigame">Về Minigame</a>
        </div>
      </div>
    </dialog>
  )
}

function PlayerCard({ player, mode, statKey, displayValue }) {
  const hidden = mode === 'hidden'
  const reveal = mode === 'reveal'
  const hideOvr = hidden && statKey === 'ovr'
  const stat = STATS[statKey]
  const value = statValue(player, statKey)
  const shownStat = hidden ? '?' : (reveal && displayValue !== null ? displayValue : value)
  const shownOvr = hideOvr ? '?' : (reveal && statKey === 'ovr' && displayValue !== null ? displayValue : player.fc27Overall)
  const age = calculateAge(player.birthDate)
  const foot = formatFoot(player.preferredFoot)
  const color = clubVisual(player.club).color

  const info = [
    ['Quốc tịch', player.nationality],
    ['CLB', player.club],
    ['Tuổi', age === null ? '—' : `${age} tuổi`],
    ['Chiều cao', `${player.heightCm} cm`],
    ['Chân thuận', foot ?? '—'],
    ['Số áo', String(player.shirtNumber)],
  ]

  const ariaLabel = `Thẻ ${player.name}, ${hidden ? `${stat.label} đang ẩn` : `${stat.label} ${value}`}`

  return (
    <div
      className={`hl-card ${hidden ? 'hl-mystery' : `hl-tier-${cardTier(player.fc27Overall)}`} ${reveal ? 'hl-rv' : ''}`}
      aria-label={ariaLabel}
    >
      <div className="hl-card-hd">
        <div className="hl-card-s">
          <span className="hl-card-lbl">OVR</span>
          <div className={`hl-card-ovr ${hideOvr ? 'hl-q' : ''}`}>{shownOvr}</div>
          <div className="hl-card-pos">{player.primaryPosition}</div>
        </div>
        <div className="hl-card-av" style={{ background: color }} aria-hidden="true">
          {getInitials(player.name)}
        </div>
      </div>
      <div className="hl-card-bd">
        <div className="hl-card-name">{player.name}</div>
        <div className="hl-card-info">
          {info.map(([label, text]) => (
            <div key={label}>
              <div className="hl-info-k">{label}</div>
              <div className="hl-info-v">{text}</div>
            </div>
          ))}
        </div>
        {statKey !== 'ovr' && (
          <div className="hl-card-stat">
            <span className="hl-card-stat-k">{stat.label}</span>
            <span className={`hl-card-stat-v ${hidden ? 'hl-q' : ''}`}>{shownStat}</span>
          </div>
        )}
      </div>
    </div>
  )
}

export default function HigherLowerGame() {
  const [poolState, setPoolState] = useState({ status: 'loading', pool: [], error: '' })
  const [reloadCount, setReloadCount] = useState(0)
  const [game, setGame] = useState(null)
  const [displayValue, setDisplayValue] = useState(null)
  const [fx, setFx] = useState(null)
  const [popupOpen, setPopupOpen] = useState(false)
  const [rulesOpen, setRulesOpen] = useState(false)
  const [liveMessage, setLiveMessage] = useState('')
  const timersRef = useRef({ t1: null, t2: null, count: null })

  const clearTimers = useCallback(() => {
    const timers = timersRef.current
    if (timers.t1) clearTimeout(timers.t1)
    if (timers.t2) clearTimeout(timers.t2)
    if (timers.count) clearInterval(timers.count)
    timers.t1 = null
    timers.t2 = null
    timers.count = null
  }, [])

  useEffect(() => () => clearTimers(), [clearTimers])

  useEffect(() => {
    const controller = new AbortController()
    clearTimers()
    setPoolState({ status: 'loading', pool: [], error: '' })
    setGame(null)
    setFx(null)
    setPopupOpen(false)
    setDisplayValue(null)

    fetchPlayers({}, controller.signal, 2026)
      .then((players) => {
        if (controller.signal.aborted) return
        const pool = eligiblePlayers(players)
        const initial = createGame(pool, readBest())
        if (!initial) {
          setPoolState({ status: 'insufficient', pool, error: '' })
          setGame(null)
          return
        }
        setPoolState({ status: 'ready', pool, error: '' })
        setGame(initial)
      })
      .catch((failure) => {
        if (controller.signal.aborted) return
        setPoolState({ status: 'error', pool: [], error: failure.message || 'Không tải được danh sách cầu thủ.' })
      })

    return () => controller.abort()
  }, [reloadCount, clearTimers])

  const startCountUp = useCallback((target, statKey, reduced) => {
    if (timersRef.current.count) {
      clearInterval(timersRef.current.count)
      timersRef.current.count = null
    }
    if (reduced) {
      setDisplayValue(target)
      return
    }
    const start = statKey === 'ovr' ? Math.max(40, target - 16) : 0
    if (start >= target) {
      setDisplayValue(target)
      return
    }
    let current = start
    setDisplayValue(current)
    const stepMs = target - start > 12 ? 38 : 90
    timersRef.current.count = setInterval(() => {
      current += 1
      setDisplayValue(current)
      if (current >= target) {
        clearInterval(timersRef.current.count)
        timersRef.current.count = null
      }
    }, stepMs)
  }, [])

  const handlePick = useCallback((direction) => {
    if (!game || game.status !== 'play' || poolState.status !== 'ready') return
    clearTimers()
    const next = applyGuess(game, direction)
    if (!next || next === game) return

    let finalBest = next.best
    if (next.status === 'reveal') {
      const saved = saveBest(next.score)
      finalBest = Math.max(next.best, saved.best)
    }
    const nextWithBest = finalBest === next.best ? next : { ...next, best: finalBest }
    setGame(nextWithBest)

    const reduced = prefersReducedMotion()
    startCountUp(nextWithBest.result.rightValue, nextWithBest.result.stat, reduced)

    if (nextWithBest.status === 'reveal') {
      const isNewRecord = nextWithBest.newBest && nextWithBest.score === nextWithBest.best
      const label = isNewRecord ? 'Đúng! Kỷ lục mới' : 'Đúng!'
      setLiveMessage(label)
      timersRef.current.t1 = setTimeout(() => {
        setFx({ kind: 'ok', title: label, id: nextWithBest.round })
      }, reduced ? 0 : 600)
      timersRef.current.t2 = setTimeout(() => {
        setFx(null)
        setDisplayValue(null)
        const advanced = advance(nextWithBest, poolState.pool)
        if (!advanced) {
          setPoolState((prev) => ({ ...prev, status: 'insufficient' }))
          setGame(null)
          return
        }
        setGame(advanced)
      }, reduced ? 900 : 2000)
    } else {
      const sub = `${nextWithBest.result.rightValue} / ${nextWithBest.result.leftValue}`
      setLiveMessage(`Sai rồi. ${sub}`)
      timersRef.current.t1 = setTimeout(() => {
        setFx({ kind: 'bad', title: 'Sai rồi!', sub, id: nextWithBest.round })
      }, reduced ? 0 : 450)
      timersRef.current.t2 = setTimeout(() => {
        setFx(null)
        setPopupOpen(true)
      }, reduced ? 600 : 1650)
    }
  }, [game, poolState.pool, poolState.status, clearTimers, startCountUp])

  useEffect(() => {
    function handleKeyDown(event) {
      if (rulesOpen || popupOpen) return
      if (!game || game.status !== 'play') return
      const tag = event.target?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return
      if (event.key === 'ArrowUp' || event.key === 'ArrowDown') {
        event.preventDefault()
        handlePick(event.key === 'ArrowUp' ? 'up' : 'down')
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [game, rulesOpen, popupOpen, handlePick])

  const handlePlayAgain = useCallback(() => {
    clearTimers()
    setFx(null)
    setPopupOpen(false)
    setDisplayValue(null)
    const freshBest = Math.max(readBest(), game?.best ?? 0)
    const fresh = createGame(poolState.pool, freshBest)
    if (!fresh) {
      setPoolState((prev) => ({ ...prev, status: 'insufficient' }))
      setGame(null)
      return
    }
    setGame(fresh)
  }, [clearTimers, game?.best, poolState.pool])

  const revealed = !!game && game.status !== 'play'
  const bestDisplay = game ? Math.max(game.best, game.score) : readBest()
  const arrowStateClass = (dir) => {
    if (!game || game.pick !== dir || !game.result) return ''
    return game.result.ok ? ' hl-sel hl-ok' : ' hl-sel hl-bad'
  }
  const activeStat = game ? STATS[game.stat] : null

  return (
    <div className="hl-root">
      <div className="mg-game-heading">
        <a className="mg-back" href="#minigame">← Minigame</a>
        <div className="mg-heading-copy">
          <h1>Cao hay thấp</h1>
        </div>
        <button type="button" className="mg-icon-button" aria-label="Xem luật chơi" onClick={() => setRulesOpen(true)}>?</button>
      </div>

      {poolState.status === 'loading' && (
        <div className="mg-panel" role="status" aria-label="Đang tải Cao hay thấp">
          <div className="mg-skeleton hl-sk-bar" />
          <div className="mg-skeleton hl-sk-board" />
          <span className="sr-only">Đang tải…</span>
        </div>
      )}

      {poolState.status === 'error' && (
        <div className="mg-error" role="alert">
          <p>{poolState.error}</p>
          <button type="button" className="mg-secondary" onClick={() => setReloadCount((value) => value + 1)}>Thử lại</button>
        </div>
      )}

      {poolState.status === 'insufficient' && (
        <div className="mg-panel mg-empty">
          <h2>Chưa đủ dữ liệu cầu thủ để chơi</h2>
          <a className="mg-secondary" href="#minigame">Về Minigame</a>
        </div>
      )}

      {poolState.status === 'ready' && game && activeStat && (
        <section className="mg-panel hl-panel" aria-label="Bàn chơi Cao hay thấp">
          <div className="hl-stat">
            <div>
              <div className="hl-stat-v">{game.score}</div>
              <div className="hl-stat-l">Điểm Số</div>
            </div>
            <div className="hl-stat-right">
              <div className="hl-stat-v2">{bestDisplay}</div>
              <div className="hl-stat-l">Kỷ Lục</div>
            </div>
          </div>

          <div className="hl-ask">
            <b>{game.right.name}</b> {activeStat.promptPrefix}<em>{activeStat.promptHighlight}</em> <b>{game.left.name}</b> không?
          </div>

          <div className={`hl-row ${game.status === 'over' ? 'hl-shake' : ''}`}>
            <div key={`l-${game.round}-${game.left.id}`} className={`hl-c ${game.shifted ? 'hl-shift' : ''}`}>
              <PlayerCard player={game.left} mode="known" statKey={game.stat} displayValue={null} />
            </div>

            <div className="hl-mid">
              <button
                type="button"
                className={`hl-arw hl-up${arrowStateClass('up')}`}
                disabled={revealed}
                onClick={() => handlePick('up')}
                aria-label="Cao hơn"
                title="Cao hơn (phím ↑)"
              >
                <ArrowIcon direction="up" />
              </button>
              <div className="hl-vs" aria-hidden="true">VS</div>
              <button
                type="button"
                className={`hl-arw hl-down${arrowStateClass('down')}`}
                disabled={revealed}
                onClick={() => handlePick('down')}
                aria-label="Thấp hơn"
                title="Thấp hơn (phím ↓)"
              >
                <ArrowIcon direction="down" />
              </button>
            </div>

            <div key={`r-${game.round}-${game.right.id}`} className={`hl-c ${game.shifted ? 'hl-enter' : ''}`}>
              <PlayerCard
                player={game.right}
                mode={revealed ? 'reveal' : 'hidden'}
                statKey={game.stat}
                displayValue={revealed ? displayValue : null}
              />
            </div>
          </div>
        </section>
      )}

      {fx && (
        <div className="hl-fx" aria-hidden="true" key={`${fx.kind}-${fx.id}`}>
          <div className={`hl-fx-flash ${fx.kind === 'ok' ? 'hl-fx-win' : 'hl-fx-bad'}`} />
          <div className={`hl-fx-card ${fx.kind === 'ok' ? 'hl-fx-mini' : 'hl-fx-card-bad'}`}>
            <span className="hl-fx-icon"><FxIcon kind={fx.kind === 'ok' ? 'check' : 'x'} /></span>
            <b>{fx.title}</b>
            {fx.sub && <span className="hl-fx-sub">{fx.sub}</span>}
          </div>
        </div>
      )}

      {rulesOpen && <HigherLowerRules onClose={() => setRulesOpen(false)} />}

      {popupOpen && game && (
        <GameOverDialog
          score={game.score}
          best={bestDisplay}
          newBest={game.newBest}
          onPlayAgain={handlePlayAgain}
        />
      )}

      <div className="hl-sr-only" aria-live="polite">{liveMessage}</div>
    </div>
  )
}
