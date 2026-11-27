import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  fetchWordleDictionary,
  fetchWordleStats,
  fetchWordleToday,
  revealWordleHint,
  submitWordleGuess,
} from '../api/wordle.js'
import {
  WORDLE_HINT_UNLOCK_TURNS,
  WORDLE_MAX_TURNS,
  buildRowWordGroups,
  buildShareText,
  filterWordleSuggestions,
  formatWordleDateLabel,
  keyboardKeyStates,
  resolveWordleAccess,
  wordleToastMessage,
} from '../minigame/wordle.js'
import { clubVisual } from '../utils/matchView.js'
import { Countdown, ErrorBox, Loading, LoginGate, Sheet } from './MinigamePage.jsx'
import './WordleGame.css'

const KEYBOARD_ROWS = [
  ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'],
  ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L'],
  ['Enter', 'Z', 'X', 'C', 'V', 'B', 'N', 'M', 'Backspace'],
]

async function copyToClipboard(text) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const area = document.createElement('textarea')
  area.value = text
  area.setAttribute('readonly', '')
  area.style.position = 'fixed'
  area.style.opacity = '0'
  document.body.appendChild(area)
  area.select()
  document.execCommand('copy')
  document.body.removeChild(area)
}

function WordleStatsBlock({ stats, highlightTurn = null }) {
  if (!stats) return <p className="mg-muted">Chưa có thống kê.</p>
  const dist = Array.isArray(stats.distribution) ? stats.distribution : [0, 0, 0, 0, 0, 0]
  const maxCount = Math.max(1, ...dist)
  return (
    <>
      <div className="wd-st">
        <div><b>{stats.played}</b><span>Đã chơi</span></div>
        <div><b>{stats.winPercentage}%</b><span>Thắng</span></div>
        <div><b>{stats.currentStreak}</b><span>Chuỗi</span></div>
        <div><b>{stats.maxStreak}</b><span>Dài nhất</span></div>
      </div>
      <div className="wd-dist" aria-label="Phân bố số lượt đoán">
        {dist.map((count, index) => {
          const turn = index + 1
          const widthPct = count > 0 ? Math.max(10, Math.round((count / maxCount) * 100)) : 0
          return (
            <div className="wd-dr" key={turn}>
              <span>{turn}</span>
              <div className="wd-bar-track">
                <i
                  className={highlightTurn === turn ? 'wd-me' : ''}
                  style={widthPct > 0 ? { width: `${widthPct}%` } : undefined}
                >
                  {count}
                </i>
              </div>
            </div>
          )
        })}
      </div>
    </>
  )
}

function WordleRulesSheet({ onClose }) {
  return (
    <Sheet title="Cách chơi" onClose={onClose}>
      <div className="wd-sheet-body">
        <p>Đoán <b>tên cầu thủ</b> Premier League 2026/27 trong 6 lượt. Mỗi lượt phải là tên của một cầu thủ có thật, đủ số chữ cái.</p>
        <div className="wd-ex" aria-hidden="true">
          <span className="wd-tile wd-hit">S</span>
          <span className="wd-tile wd-miss">A</span>
          <span className="wd-tile wd-near">L</span>
          <span className="wd-tile wd-miss">A</span>
          <span className="wd-tile wd-miss">H</span>
        </div>
        <ul>
          <li><b className="wd-c-hit">Xanh</b>: đúng chữ, đúng chỗ.</li>
          <li><b className="wd-c-near">Vàng</b>: có chữ này nhưng sai chỗ.</li>
          <li><b>Xám</b>: không có chữ này.</li>
          <li>Tên dùng <b>họ</b> của cầu thủ. Tên nhiều từ giữ nguyên các từ: MAC ALLISTER, VAN DIJK.</li>
          <li>Dấu và ký tự đặc biệt đổi thành chữ thường: Ødegaard → ODEGAARD, Muñoz → MUNOZ.</li>
          <li>Ô chữ của mỗi lượt giữ đúng hình dạng tên bạn đoán. Đáp án nhiều từ vẫn đoán được bằng tên một từ, miễn là cùng tổng số chữ cái.</li>
          <li>Khi gõ, bên dưới hiện các tên cầu thủ khớp với chữ đã gõ. Bấm vào một tên để điền nhanh.</li>
          <li>Sau lượt 4 được mở 1 gợi ý: CLB của cầu thủ.</li>
          <li>Không tính điểm. Chỉ lưu thống kê của riêng bạn: số ván, % thắng, chuỗi ngày.</li>
        </ul>
        <div className="wd-sheet-acts">
          <button type="button" className="mg-primary" onClick={onClose}>Đã hiểu</button>
        </div>
      </div>
    </Sheet>
  )
}

function WordleStatsSheet({ accountId, initialStats, highlightTurn, onClose }) {
  const [state, setState] = useState({ stats: initialStats, loading: !initialStats, error: '' })

  useEffect(() => {
    if (!accountId) return undefined
    const controller = new AbortController()
    fetchWordleStats(accountId, controller.signal)
      .then(stats => {
        if (!controller.signal.aborted) setState({ stats, loading: false, error: '' })
      })
      .catch(failure => {
        if (!controller.signal.aborted && !initialStats) {
          setState({ stats: null, loading: false, error: failure.message })
        }
      })
    return () => controller.abort()
  }, [accountId, initialStats])

  return (
    <Sheet title="Thống kê của bạn" onClose={onClose}>
      <div className="wd-sheet-body">
        {state.loading ? <Loading /> : state.error ? <ErrorBox message={state.error} /> : (
          <WordleStatsBlock stats={state.stats} highlightTurn={highlightTurn} />
        )}
        <div className="wd-sheet-acts">
          <button type="button" className="mg-secondary" onClick={onClose}>Đóng</button>
        </div>
      </div>
    </Sheet>
  )
}

export default function WordleGame({ account, authLoading, authError, wordleInfo, onRetryInfo }) {
  const { routeAction } = resolveWordleAccess({ infoStatus: wordleInfo?.status ?? 'loading', account })
  const [data, setData] = useState(null)
  const [dictionary, setDictionary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [reloadTick, setReloadTick] = useState(0)

  const [input, setInput] = useState('')
  const [sugOpen, setSugOpen] = useState(false)
  const [shaking, setShaking] = useState(false)
  const [flipRow, setFlipRow] = useState(-1)
  const [toast, setToast] = useState({ message: '', open: false })
  const [sheet, setSheet] = useState(null)

  const curRowRef = useRef(null)
  const toastTimerRef = useRef(null)
  const shakeTimerRef = useRef(null)
  const flipTimerRef = useRef(null)

  const showToast = useCallback(message => {
    clearTimeout(toastTimerRef.current)
    setToast({ message, open: true })
    toastTimerRef.current = setTimeout(() => {
      setToast(prev => ({ ...prev, open: false }))
    }, 1800)
  }, [])

  const triggerShake = useCallback(() => {
    clearTimeout(shakeTimerRef.current)
    setShaking(false)
    requestAnimationFrame(() => {
      setShaking(true)
      shakeTimerRef.current = setTimeout(() => setShaking(false), 420)
    })
  }, [])

  useEffect(() => () => {
    clearTimeout(toastTimerRef.current)
    clearTimeout(shakeTimerRef.current)
    clearTimeout(flipTimerRef.current)
  }, [])

  useEffect(() => {
    if (routeAction === 'redirect') {
      window.location.hash = '#minigame'
    }
  }, [routeAction])

  useEffect(() => {
    if (routeAction !== 'play' || !account?.id) {
      setData(null)
      setDictionary(null)
      setLoading(false)
      setError('')
      setInput('')
      setSugOpen(false)
      return undefined
    }
    const controller = new AbortController()
    setLoading(true)
    setError('')

    Promise.all([
      fetchWordleToday(account.id, controller.signal),
      fetchWordleDictionary(account.id, controller.signal).catch(err => {
        if (err.status === 404 || err.name === 'AbortError') throw err
        return null
      }),
    ])
      .then(([todayData, dictData]) => {
        if (controller.signal.aborted) return
        setData(todayData)
        if (dictData) setDictionary(dictData)
        setLoading(false)
        setError('')
      })
      .catch(failure => {
        if (controller.signal.aborted) return
        if (failure.status === 404) {
          window.location.hash = '#minigame'
          return
        }
        setLoading(false)
        setError(failure.message)
      })

    return () => controller.abort()
  }, [account?.id, routeAction, reloadTick])

  const finished = !!data && data.status !== 'PLAYING'
  const guessedLetters = useMemo(() => (data?.guesses ?? []).map(g => g.letters), [data?.guesses])
  const keyStates = useMemo(() => keyboardKeyStates(data?.guesses ?? []), [data?.guesses])
  const suggestions = useMemo(
    () => (data && sugOpen && input.length > 0 && dictionary
      ? filterWordleSuggestions(dictionary, input, guessedLetters, 5, data.length)
      : []),
    [data, dictionary, guessedLetters, input, sugOpen]
  )

  // Close floating suggestions when clicking outside the active row.
  useEffect(() => {
    if (!sugOpen) return undefined
    const handlePointerDown = event => {
      if (curRowRef.current && !curRowRef.current.contains(event.target)) {
        setSugOpen(false)
      }
    }
    document.addEventListener('pointerdown', handlePointerDown)
    return () => document.removeEventListener('pointerdown', handlePointerDown)
  }, [sugOpen])

  const handleMidnightRollover = useCallback(() => {
    setInput('')
    setSugOpen(false)
    setFlipRow(-1)
    setReloadTick(v => v + 1)
  }, [])

  const submitCurrentGuess = useCallback(async () => {
    if (!account?.id || !data || finished || busy) return
    setSugOpen(false)
    setBusy(true)
    try {
      const next = await submitWordleGuess(account.id, input, { date: data.date })
      const submittedIndex = next.guesses.length - 1
      setData(next)
      setInput('')
      setFlipRow(submittedIndex)
      clearTimeout(flipTimerRef.current)
      flipTimerRef.current = setTimeout(() => setFlipRow(-1), 950)
    } catch (failure) {
      if (failure.status === 404) {
        window.location.hash = '#minigame'
        return
      }
      if (['WRONG_LENGTH', 'NOT_A_PLAYER', 'ALREADY_GUESSED', 'INVALID_CHARACTERS'].includes(failure.code)) {
        showToast(wordleToastMessage(failure.code, data.length, failure.message))
        triggerShake()
      } else if (failure.code === 'GAME_FINISHED') {
        showToast(wordleToastMessage('GAME_FINISHED', data.length, failure.message))
        setReloadTick(v => v + 1)
      } else {
        showToast(failure.message)
      }
    } finally {
      setBusy(false)
    }
  }, [account?.id, busy, data, finished, input, showToast, triggerShake])

  const handleKey = useCallback(key => {
    if (!data || finished || busy) return
    if (key === 'Backspace') {
      setInput(prev => {
        const next = prev.slice(0, -1)
        setSugOpen(next.length > 0)
        return next
      })
      return
    }
    if (key === 'Enter') {
      submitCurrentGuess()
      return
    }
    if (/^[A-Z]$/.test(key)) {
      setInput(prev => {
        if (prev.length >= data.length) return prev
        setSugOpen(true)
        return prev + key
      })
    }
  }, [busy, data, finished, submitCurrentGuess])

  // Physical keyboard support (A-Z, Enter, Backspace); ignore shortcuts & open modals/inputs.
  useEffect(() => {
    const onKeyDown = event => {
      if (!data || finished || busy || sheet) return
      if (event.ctrlKey || event.metaKey || event.altKey) return
      const tag = event.target?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || event.target?.isContentEditable) return
      const key = event.key.length === 1 ? event.key.toUpperCase() : event.key
      if (key === 'Enter' || key === 'Backspace' || /^[A-Z]$/.test(key)) {
        event.preventDefault()
        handleKey(key)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [busy, data, finished, handleKey, sheet])

  const handleRevealHint = async () => {
    if (!account?.id || !data || finished || busy || data.hintUsed || data.guesses.length < WORDLE_HINT_UNLOCK_TURNS) return
    setBusy(true)
    try {
      const next = await revealWordleHint(account.id, { date: data.date })
      setData(next)
    } catch (failure) {
      if (failure.status === 404) {
        window.location.hash = '#minigame'
        return
      }
      showToast(wordleToastMessage(failure.code, data.length, failure.message))
    } finally {
      setBusy(false)
    }
  }

  const handleShare = async () => {
    if (!data || !finished) return
    const text = buildShareText({ puzzleNumber: data.puzzleNumber, status: data.status, guesses: data.guesses })
    try {
      await copyToClipboard(text)
      showToast('Đã chép kết quả')
    } catch {
      showToast('Không thể sao chép vào bộ nhớ tạm')
    }
  }

  const metaDate = data?.date ?? wordleInfo?.data?.date ?? null
  const dateLabel = metaDate ? formatWordleDateLabel(metaDate) : ''
  const countdownSource = data
    ? { serverTime: data.serverTime, nextDailyAt: data.nextAt }
    : wordleInfo?.data
      ? { serverTime: wordleInfo.data.serverTime, nextDailyAt: wordleInfo.data.nextAt }
      : null
  const puzzleNumber = data?.puzzleNumber ?? wordleInfo?.data?.puzzleNumber ?? null
  const currentTurn = data ? Math.min(WORDLE_MAX_TURNS, data.guesses.length + 1) : 1
  const hintUnlocked = !!data && data.guesses.length >= WORDLE_HINT_UNLOCK_TURNS && !finished
  const hintVisual = data?.hintClub ? clubVisual(data.hintClub) : null
  const answer = data?.answer ?? null
  const answerVisual = answer ? clubVisual(answer.club) : null
  const answerMeta = answer
    ? [answer.club, answer.position, answer.nationality, answer.age != null ? `${answer.age} tuổi` : null]
        .filter(Boolean)
        .join(' · ')
    : ''

  if (routeAction === 'redirect') return null

  return (
    <div className="wd-game">
      <div className="mg-game-heading wd-heading">
        <a className="mg-back" href="#minigame">← Minigame</a>
        <div className="mg-heading-copy">
          <h1>{puzzleNumber != null ? `Wordle cầu thủ #${puzzleNumber}` : 'Wordle cầu thủ'}</h1>
          <span className="mg-mode-chip mg-daily-chip">
            Daily{dateLabel ? ` · ${dateLabel}` : ''} ·{' '}
            <span className="wd-clock-inline">
              ⏱ <Countdown source={countdownSource} onExpire={handleMidnightRollover} />
            </span>
          </span>
        </div>
        <div className="wd-head-actions">
          <button
            type="button"
            className="mg-icon-button wd-ib"
            aria-label="Cách chơi"
            title="Cách chơi"
            onClick={() => setSheet('rules')}
          >
            ?
          </button>
          <button
            type="button"
            className="mg-icon-button wd-ib"
            aria-label="Thống kê của bạn"
            title="Thống kê của bạn"
            disabled={!account}
            onClick={() => setSheet('stats')}
          >
            📊
          </button>
        </div>
      </div>

      {routeAction === 'loading' ? (
        <Loading />
      ) : routeAction === 'error' ? (
        <ErrorBox message={wordleInfo?.error || 'Không kiểm tra được trạng thái Wordle.'} onRetry={onRetryInfo} />
      ) : routeAction === 'login' ? (
        <LoginGate authLoading={authLoading} authError={authError} />
      ) : loading && !data ? (
        <Loading />
      ) : error && !data ? (
        <ErrorBox message={error} onRetry={() => setReloadTick(v => v + 1)} disabled={loading} />
      ) : data ? (
        <section className="wd-card" aria-label="Bảng chơi Wordle cầu thủ">
          <div className="wd-meta">
            <span className="wd-pill">
              {finished ? 'Đã xong' : <>Lượt <b>{currentTurn}</b>/{WORDLE_MAX_TURNS}</>}
            </span>
          </div>

          <div className="wd-board" style={{ '--wd-len': data.length }}>
            {Array.from({ length: WORDLE_MAX_TURNS }, (_, rowIndex) => {
              if (rowIndex < data.guesses.length) {
                const guess = data.guesses[rowIndex]
                const groups = buildRowWordGroups({
                  word: guess.word,
                  letters: guess.letters,
                  feedback: guess.feedback,
                  length: data.length,
                })
                const shouldFlip = flipRow === rowIndex
                return (
                  <div className="wd-row" key={rowIndex}>
                    <div className="wd-tiles">
                      {groups.map((tiles, gIdx) => (
                        <div className="wd-word" key={gIdx}>
                          {tiles.map(tile => (
                            <span
                              key={tile.index}
                              className={`wd-tile wd-${tile.state}${shouldFlip ? ' wd-flip' : ''}`}
                              style={shouldFlip ? { animationDelay: `${tile.index * 70}ms` } : undefined}
                            >
                              {tile.letter}
                            </span>
                          ))}
                        </div>
                      ))}
                    </div>
                    <div className="wd-who">
                      <b>{guess.playerName}</b> · {guess.club}
                    </div>
                  </div>
                )
              }

              if (rowIndex === data.guesses.length && !finished) {
                const groups = buildRowWordGroups({ letters: input, length: data.length })
                const showSug = sugOpen && input.length > 0 && dictionary !== null
                return (
                  <div
                    className={`wd-row wd-cur${shaking ? ' wd-shake' : ''}`}
                    ref={curRowRef}
                    key={rowIndex}
                  >
                    <div className="wd-tiles">
                      {groups.map((tiles, gIdx) => (
                        <div className="wd-word" key={gIdx}>
                          {tiles.map(tile => (
                            <span
                              key={tile.index}
                              className={`wd-tile${tile.state === 'filled' ? ' wd-f' : ''}`}
                            >
                              {tile.letter}
                            </span>
                          ))}
                        </div>
                      ))}
                    </div>
                    <div className="wd-who" aria-hidden="true" />
                    {showSug && (
                      <div className="wd-sug" role="listbox" aria-label="Gợi ý tên cầu thủ">
                        {suggestions.length > 0 ? (
                          suggestions.map(entry => (
                            <button
                              type="button"
                              role="option"
                              aria-selected="false"
                              key={entry.letters}
                              onClick={() => {
                                setInput(entry.letters)
                                setSugOpen(false)
                              }}
                            >
                              <b>
                                <span className="wd-mt">{entry.matched}</span>
                                {entry.rest}
                              </b>
                              <small>{entry.name} · {entry.club}</small>
                            </button>
                          ))
                        ) : (
                          <span className="wd-none">
                            Không có cầu thủ {data.length} chữ cái nào bắt đầu bằng “{input}”
                          </span>
                        )}
                      </div>
                    )}
                  </div>
                )
              }

              const emptyGroups = buildRowWordGroups({ letters: '', length: data.length })
              return (
                <div className="wd-row" key={rowIndex}>
                  <div className="wd-tiles">
                    {emptyGroups.map((tiles, gIdx) => (
                      <div className="wd-word" key={gIdx}>
                        {tiles.map(tile => (
                          <span key={tile.index} className="wd-tile" />
                        ))}
                      </div>
                    ))}
                  </div>
                  <div className="wd-who" aria-hidden="true" />
                </div>
              )
            })}
          </div>

          <div className="wd-hint">
            {data.hintUsed && data.hintClub ? (
              <span className="wd-hint-got">
                <span className="wd-crest" style={{ background: hintVisual.color }}>
                  {hintVisual.code}
                </span>
                Cầu thủ của {data.hintClub}
              </span>
            ) : (
              <button
                type="button"
                disabled={!hintUnlocked || busy}
                onClick={handleRevealHint}
              >
                💡 Gợi ý: xem CLB{data.guesses.length >= WORDLE_HINT_UNLOCK_TURNS ? '' : ' · mở sau lượt 4'}
              </button>
            )}
          </div>

          <div className="wd-kb" role="group" aria-label="Bàn phím ảo">
            {KEYBOARD_ROWS.map((row, rIdx) => (
              <div className="wd-kr" key={rIdx}>
                {row.map(key => {
                  const isWide = key === 'Enter' || key === 'Backspace'
                  const label = key === 'Enter' ? 'ĐOÁN' : key === 'Backspace' ? '⌫' : key
                  const state = !isWide ? keyStates[key] : ''
                  return (
                    <button
                      type="button"
                      key={key}
                      className={`wd-k${isWide ? ' wd-wide' : ''}${state ? ` wd-${state}` : ''}`}
                      disabled={finished || busy}
                      aria-label={key === 'Enter' ? 'Đoán' : key === 'Backspace' ? 'Xóa chữ' : key}
                      onClick={() => handleKey(key)}
                    >
                      {label}
                    </button>
                  )
                })}
              </div>
            ))}
          </div>

          {finished && answer && (
            <div className="wd-res" aria-live="polite">
              <div className="wd-rh">
                <span className="wd-rh-ico" aria-hidden="true">{data.status === 'WON' ? '🎉' : '😢'}</span>
                <div>
                  <span className={`wd-ok${data.status === 'WON' ? '' : ' wd-lose'}`}>
                    {data.status === 'WON' ? `Chính xác! ${data.guesses.length}/${WORDLE_MAX_TURNS}` : 'Hết lượt'}
                  </span>
                  <small>
                    {data.status === 'WON' ? `Bạn đoán ra sau ${data.guesses.length} lượt` : 'Đáp án hôm nay là'}
                  </small>
                </div>
              </div>

              <div className="wd-pc">
                <span className="wd-no" style={{ background: answerVisual.color }}>
                  {answer.shirtNumber ?? '—'}
                </span>
                <div className="wd-pc-copy">
                  <b>{answer.name}</b>
                  <small>{answerMeta}</small>
                </div>
                {answer.overall != null && (
                  <span className="wd-ovr" title="OVR FC 27">{answer.overall}</span>
                )}
              </div>

              <WordleStatsBlock
                stats={data.stats}
                highlightTurn={data.status === 'WON' ? data.guesses.length : null}
              />

              <div className="wd-acts">
                <button type="button" className="wd-share-btn" onClick={handleShare}>
                  Chia sẻ kết quả
                </button>
              </div>
            </div>
          )}
        </section>
      ) : null}

      <div className={`wd-toast${toast.open ? ' wd-on' : ''}`} role="status" aria-live="polite">
        {toast.message}
      </div>

      {sheet === 'rules' && <WordleRulesSheet onClose={() => setSheet(null)} />}
      {sheet === 'stats' && account && (
        <WordleStatsSheet
          accountId={account.id}
          initialStats={data?.stats ?? null}
          highlightTurn={data?.status === 'WON' ? data.guesses.length : null}
          onClose={() => setSheet(null)}
        />
      )}
    </div>
  )
}
