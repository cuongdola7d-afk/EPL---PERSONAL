import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { fetchFantasyPlayers } from '../api/fantasy.js'
import { useApiList } from '../hooks/useApiList.js'
import { getInitials } from '../utils/initials.js'
import { ESTIMATED_OVR_PLAYER_IDS, FANTASY_STORAGE_KEY, FORMATIONS, GROUP_LABEL, formationSlots,
  movePicks, normalizeLineup, pickError, validateLineup } from '../fantasy/lineup.js'
import ResultPanel from './ResultPanel.jsx'
import './FantasyPage.css'

const GROUPS = ['FORWARD', 'MIDFIELDER', 'DEFENDER', 'GOALKEEPER']
const CLUB_COLORS = ['#b44955', '#425caa', '#3c987e', '#a26c35', '#705aaa', '#497d9b']

function readSavedLineup() {
  try { return JSON.parse(window.localStorage.getItem(FANTASY_STORAGE_KEY) ?? '{}') }
  catch { return {} }
}

function clubColor(id) { return CLUB_COLORS[Math.abs(id) % CLUB_COLORS.length] }
function ratingTier(overall) { return overall >= 85 ? 'elite' : overall >= 78 ? 'strong' : overall >= 70 ? 'good' : 'basic' }

function PlayerAvatar({ player, group, small = false }) {
  return <span className={`fantasy-avatar${small ? ' fantasy-avatar-small' : ''}${player ? '' : ' fantasy-avatar-empty'}`}
    style={player ? { '--club-color': clubColor(player.clubId) } : undefined} aria-hidden="true">
    {player ? getInitials(player.name) : '+'}
    {player && !small && <span className={`fantasy-rating fantasy-rating-${ratingTier(player.fc27Overall)}`}>{player.fc27Overall}</span>}
    {small && group && <span className={`fantasy-position fantasy-position-${group}`}>{group === 'GOALKEEPER' ? 'GK' : group === 'DEFENDER' ? 'DEF' : group === 'MIDFIELDER' ? 'MID' : 'FWD'}</span>}
  </span>
}

function Pitch() {
  return <svg className="fantasy-field" viewBox="0 0 400 500" preserveAspectRatio="none" aria-hidden="true">
    <defs><linearGradient id="fantasy-grass" x1="0" y1="0" x2="0" y2="1"><stop stopColor="#4aae5e" /><stop offset="1" stopColor="#2f8c47" /></linearGradient></defs>
    <polygon points="36,0 364,0 400,500 0,500" fill="url(#fantasy-grass)" />
    {[0, 2, 4].map((stripe) => <path key={stripe} d={`M${36 - stripe * 6} ${stripe * 83} L${364 + stripe * 6} ${stripe * 83} L${376 + stripe * 6} ${(stripe + 1) * 83} L${24 - stripe * 6} ${(stripe + 1) * 83} Z`} fill="#ffffff" opacity=".055" />)}
    <g fill="none" stroke="rgba(255,255,255,.64)" strokeWidth="2" strokeLinejoin="round">
      <polygon points="36,0 364,0 400,500 0,500" />
      <path d="M0 250 H400 M155 0 A45 32 0 0 0 245 0 M155 500 A45 32 0 0 1 245 500" />
      <ellipse cx="200" cy="250" rx="55" ry="55" />
      <path d="M96 0 V92 H304 V0 M139 0 V38 H261 V0 M75 500 V398 H325 V500 M139 500 V455 H261 V500" />
    </g>
    <circle cx="200" cy="250" r="2.5" fill="white" opacity=".75" />
  </svg>
}

function FantasyPage() {
  const requestPlayers = useCallback((signal) => fetchFantasyPlayers(signal), [])
  const { data: players, status, error, reload } = useApiList(requestPlayers)
  const [lineup, setLineup] = useState(readSavedLineup)
  const [activeKey, setActiveKey] = useState(null)
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('')
  const [clubFilter, setClubFilter] = useState('')
  const [message, setMessage] = useState('')
  const [result, setResult] = useState(false)
  const [theme, setTheme] = useState('dark')
  const searchRef = useRef(null)

  const currentLineup = useMemo(() => status === 'success' ? normalizeLineup(lineup, players) : lineup,
    [status, lineup, players])
  const formation = FORMATIONS[currentLineup?.formation] ? currentLineup.formation : '4-2-1-3'
  const picks = currentLineup?.picks ?? {}
  const slots = useMemo(() => formationSlots(formation), [formation])
  const byId = useMemo(() => new Map(players.map((player) => [player.id, player])), [players])
  const selected = slots.map((slot) => byId.get(picks[slot.key])).filter(Boolean)
  const total = selected.reduce((sum, player) => sum + player.fc27Overall, 0)
  const clubCounts = selected.reduce((counts, player) => counts.set(player.clubId,
    { name: player.club, count: (counts.get(player.clubId)?.count ?? 0) + 1 }), new Map())
  const activeSlot = slots.find((slot) => slot.key === activeKey)
  const clubs = useMemo(() => [...new Map(players.map((player) => [player.clubId,
    { id: player.clubId, name: player.club }])).values()].sort((a, b) => a.name.localeCompare(b.name, 'vi')), [players])
  const candidates = players.filter((player) =>
    (!filter || player.position === filter) && (!clubFilter || player.clubId === Number(clubFilter)) &&
    `${player.name} ${player.club}`.toLocaleLowerCase('vi').includes(search.trim().toLocaleLowerCase('vi')))
    .sort((a, b) => (b.fc27Overall ?? -1) - (a.fc27Overall ?? -1) || a.name.localeCompare(b.name, 'vi'))

  useEffect(() => {
    if (status === 'success') setLineup((current) => normalizeLineup(current, players))
  }, [status, players])

  useEffect(() => {
    if (status !== 'success') return
    try { window.localStorage.setItem(FANTASY_STORAGE_KEY, JSON.stringify({ formation, picks })) }
    catch { /* Fantasy vẫn dùng được nếu trình duyệt chặn localStorage. */ }
  }, [status, formation, picks])

  useEffect(() => {
    if (!activeKey) return undefined
    searchRef.current?.focus()
    function onKeyDown(event) { if (event.key === 'Escape') setActiveKey(null) }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [activeKey])

  function openPicker(key) {
    const slot = slots.find((item) => item.key === key)
    setActiveKey(key)
    setFilter(slot.group)
    setClubFilter('')
    setSearch('')
    setMessage('')
  }

  function choose(player) {
    const issue = pickError(player, activeSlot, picks, players)
    if (issue) { setMessage(issue); return }
    setLineup({ formation, picks: { ...picks, [activeKey]: player.id } })
    setActiveKey(null)
    setResult(false)
    setMessage('')
  }

  function remove(key) {
    const next = { ...picks }
    delete next[key]
    setLineup({ formation, picks: next })
    setResult(false)
    setMessage('')
  }

  function changeFormation(nextFormation) {
    setLineup({ formation: nextFormation, picks: movePicks(formation, nextFormation, picks) })
    setResult(false)
    setMessage('')
  }

  function showResult() {
    const issue = validateLineup(formation, picks, players)
    setMessage(issue)
    setResult(!issue)
  }

  return <section className="fantasy-page" id="directory" aria-labelledby="fantasy-heading" data-theme={theme}>
    <div className="fantasy-wrap">
      <header className="fantasy-head">
        <div><p className="fantasy-kicker">prismaXI · FANTASY 2026/27</p><h1 id="fantasy-heading">Đội hình của bạn</h1>
          <p>Chọn 11 cầu thủ · tổng điểm theo OVR FC 27</p></div>
        <button className="fantasy-theme" type="button" onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
          aria-label={theme === 'light' ? 'Đổi sang giao diện tối' : 'Đổi sang giao diện sáng'}>◐</button>
      </header>
      <ResultPanel status={status} error={error} count={players.length} itemName="cầu thủ Fantasy"
        emptyMessage="Chưa có cầu thủ trong roster mùa 2026/27." onRetry={reload}>
        <div className="fantasy-summary">
          <div className="fantasy-card fantasy-total"><span className="fantasy-label">Tổng overall</span>
            <div className="fantasy-total-body"><span className="fantasy-ring" style={{ '--progress': `${Math.min(100, total / 990 * 100)}%` }}><span>{selected.length}/11</span></span>
              <strong>{total}<small> / 990</small></strong></div>
            <p>{selected.length < 11 ? `Còn ${11 - selected.length} vị trí cần chọn` : 'Đội hình đã đủ 11 người'}</p>
          </div>
          <div className="fantasy-card fantasy-averages"><span className="fantasy-label">Overall trung bình theo tuyến</span>
            {GROUPS.map((group) => { const ratings = selected.filter((player) => player.position === group).map((player) => player.fc27Overall)
              const average = ratings.length ? Math.round(ratings.reduce((sum, value) => sum + value, 0) / ratings.length) : 0
              return <div className="fantasy-average" key={group}><span>{GROUP_LABEL[group]}</span><span className={`fantasy-track fantasy-track-${group}`}><i style={{ width: `${average}%` }} /></span><b>{ratings.length ? average : '—'}</b></div> })}
          </div>
          <div className="fantasy-card fantasy-clubs"><span className="fantasy-label">Đã chọn</span><strong>{selected.length}<small>/11</small></strong>
            <div>{clubCounts.size ? [...clubCounts].map(([id, club]) => <span className={`fantasy-club-chip${club.count === 3 ? ' full' : ''}`} key={id}>
              <i style={{ background: clubColor(id) }} />{club.name} {club.count}/3</span>) : <p>Tối đa 3 cầu thủ mỗi CLB</p>}</div>
          </div>
        </div>
        <div className="fantasy-controls">
          <div className="fantasy-formations" role="group" aria-label="Sơ đồ chiến thuật">
            {Object.keys(FORMATIONS).map((name) => <button type="button" key={name} aria-pressed={formation === name}
              onClick={() => changeFormation(name)}>{name}</button>)}
          </div>
          <button className="fantasy-clear" type="button" onClick={() => { setLineup({ formation, picks: {} }); setResult(false); setMessage('') }}>Xóa hết</button>
          <button className="fantasy-primary" type="button" onClick={showResult}>Xem kết quả</button>
        </div>
        {message && <p className="fantasy-message" role="alert">{message}</p>}
        {result && <p className="fantasy-result" role="status">Đội hình {formation} đạt <strong>{total} OVR</strong> từ 11 cầu thủ.</p>}
        <div className="fantasy-main">
          <div className="fantasy-pitch" aria-label="Sân bóng với đội hình đã chọn"><Pitch />
            {slots.map((slot) => { const player = byId.get(picks[slot.key])
              return <button className="fantasy-slot" type="button" key={slot.key} style={{ left: `${slot.x}%`, top: `${slot.y}%` }}
                onClick={() => openPicker(slot.key)} aria-label={`${slot.position}: ${player ? player.name : 'chọn cầu thủ'}`}>
                <span className={`fantasy-position fantasy-position-${slot.group}`}>{slot.position}</span>
                <PlayerAvatar player={player} /><span className="fantasy-slot-name">{player?.name ?? 'Chọn cầu thủ'}</span>
              </button> })}
          </div>
          <aside className="fantasy-card fantasy-side" aria-labelledby="fantasy-lineup-heading">
            <div className="fantasy-side-head"><h2 id="fantasy-lineup-heading">Đội hình</h2><span>{selected.length}/11</span></div>
            <div className="fantasy-side-list">{slots.map((slot) => { const player = byId.get(picks[slot.key])
              return <div className="fantasy-side-row" key={slot.key}>
                <button type="button" className="fantasy-side-pick" onClick={() => openPicker(slot.key)}
                  aria-label={`${slot.position}: ${player ? `thay ${player.name}` : 'chọn cầu thủ'}`}>
                  <PlayerAvatar player={player} group={slot.group} small />
                  <span className="fantasy-side-name"><strong>{player?.name ?? 'Chọn cầu thủ'}</strong><small>{player?.club ?? slot.position}</small></span>
                  {player && <span className={`fantasy-rating fantasy-rating-${ratingTier(player.fc27Overall)}`}>{player.fc27Overall}</span>}
                </button>
                {player && <button className="fantasy-remove" type="button" onClick={() => remove(slot.key)} aria-label={`Bỏ ${player.name}`}>×</button>}
              </div> })}</div>
          </aside>
        </div>
      </ResultPanel>
    </div>
    {activeSlot && <div className="fantasy-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setActiveKey(null) }}>
      <div className="fantasy-sheet" role="dialog" aria-modal="true" aria-labelledby="fantasy-picker-title">
        <div className="fantasy-sheet-head"><div><h2 id="fantasy-picker-title">Chọn cầu thủ</h2><span>Vị trí {activeSlot.position} · {GROUP_LABEL[activeSlot.group]}</span></div>
          <button type="button" className="fantasy-theme" onClick={() => setActiveKey(null)} aria-label="Đóng">×</button></div>
        <label className="fantasy-search"><span className="visually-hidden">Tìm theo tên cầu thủ hoặc CLB</span><span aria-hidden="true">⌕</span>
          <input ref={searchRef} value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tên cầu thủ hoặc CLB" /></label>
        <div className="fantasy-filters">
          {['', ...GROUPS.slice().reverse()].map((group) => <button type="button" key={group} aria-pressed={filter === group}
            onClick={() => setFilter(group)}>{group ? GROUP_LABEL[group] : 'Mọi vị trí'}</button>)}
          <select value={clubFilter} onChange={(event) => setClubFilter(event.target.value)} aria-label="Lọc câu lạc bộ">
            <option value="">Mọi CLB</option>{clubs.map((club) => <option key={club.id} value={club.id}>{club.name}</option>)}
          </select>
        </div>
        <div className="fantasy-results">{candidates.length ? candidates.map((player) => { const issue = pickError(player, activeSlot, picks, players)
          return <div className="fantasy-candidate" key={player.id}><PlayerAvatar player={player} small />
            <div><strong>{player.name}</strong><small>{player.club} · {GROUP_LABEL[player.position]}</small></div>
            <span className={player.fc27Overall == null ? 'fantasy-no-rating' : `fantasy-rating fantasy-rating-${ratingTier(player.fc27Overall)}`}>
              {player.fc27Overall ?? '—'}</span>
            <button type="button" disabled={Boolean(issue)} title={issue || undefined} onClick={() => choose(player)}>
              {issue ? (ESTIMATED_OVR_PLAYER_IDS.has(player.id) ? 'OVR ước tính' : player.fc27Overall == null ? 'Thiếu OVR' : player.position !== activeSlot.group ? 'Sai vị trí' : issue.includes('CLB') ? 'Đủ 3/CLB' : 'Đã chọn') : 'Chọn'}</button>
          </div> }) : <p className="fantasy-no-results">Không tìm thấy cầu thủ. Thử đổi từ khóa hoặc bộ lọc.</p>}</div>
        <div className="fantasy-sheet-foot"><span>{candidates.length} cầu thủ · {players.filter((player) => player.fc27Overall == null).length} chưa có OVR</span>
          {picks[activeKey] != null && <button type="button" onClick={() => { remove(activeKey); setActiveKey(null) }}>Bỏ chọn vị trí này</button>}</div>
      </div>
    </div>}
  </section>
}

export default FantasyPage
