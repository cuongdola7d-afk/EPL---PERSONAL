import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { fetchFantasyPlayers } from '../api/fantasy.js'
import { useApiList } from '../hooks/useApiList.js'
import { FANTASY_STORAGE_KEY, FORMATIONS, GROUP_LABEL, MAX_OVR, fitsSlot, formationSlots,
  lineupIssues, movePicks, normalizeLineup, pickError, playerDataError } from '../fantasy/lineup.js'
import ResultPanel from './ResultPanel.jsx'
import PlayerAvatar, { clubColor, ratingTier } from './FantasyPlayerAvatar.jsx'
import Pitch from './FantasyPitch.jsx'
import TeamOfWeek from './TeamOfWeek.jsx'
import FantasyGameweek from './FantasyGameweek.jsx'
import FantasyResults from './FantasyResults.jsx'
import FantasyLeaderboard from './FantasyLeaderboard.jsx'
import FantasyRules from './FantasyRules.jsx'
import { ArrowIcon } from './HomeArtwork.jsx'
import { useFantasyEntry } from '../hooks/useFantasyEntry.js'
import { emptyLineup, entryLineup } from '../fantasy/entry.js'
import { useFantasyResult } from '../hooks/useFantasyResult.js'
import { formatPoints, publishedPlayerPoints } from '../fantasy/results.js'
import './FantasyPage.css'
import './FantasyEntry.css'

const GROUPS = ['FORWARD', 'MIDFIELDER', 'DEFENDER', 'GOALKEEPER']
const VIEWS = [['user', 'Đội hình của bạn'], ['team', 'Đội hình tiêu biểu'], ['leaderboard', 'BXH người chơi']]

function readSavedLineup() {
  try { return JSON.parse(window.localStorage.getItem(FANTASY_STORAGE_KEY) ?? '{}') }
  catch { return {} }
}

function FantasyPage({ account = null, authLoading = false, authError = '' }) {
  const [contestGameweek, setContestGameweek] = useState(null)
  const [contestOpen, setContestOpen] = useState(false)
  const [contestStatus, setContestStatus] = useState(null)
  const [contestDeadline, setContestDeadline] = useState(null)
  const [rosterAsOf, setRosterAsOf] = useState(null)
  const requestPlayers = useCallback((signal) => {
    if (authLoading || account && (!contestGameweek || !rosterAsOf)) return Promise.resolve([])
    return fetchFantasyPlayers(signal, account ? contestGameweek : null, account ? rosterAsOf : null)
  }, [account?.id, authLoading, contestGameweek, rosterAsOf])
  const { data: rosterPlayers, status, error, reload } = useApiList(requestPlayers)
  const [lineup, setLineup] = useState(() => account || authLoading ? emptyLineup() : readSavedLineup())
  const entry = useFantasyEntry(account, contestGameweek, setLineup)
  const contestSelection = useCallback((gw, open, asOf, status, deadline) => {
    setContestGameweek(gw); setContestOpen(open); setRosterAsOf(asOf); setContestStatus(status); setContestDeadline(deadline)
  }, [])
  const lockedSubmission = account && entry.ready && !contestOpen && entry.data?.submitted
  const snapshotPlayers = lockedSubmission ? lockedSubmission.players.map(player => ({
    id: player.playerId, name: player.name, clubId: player.clubId, club: player.club,
    fc27Overall: player.ovr, primaryPosition: player.primaryPosition, eligiblePositions: player.eligiblePositions,
    position: ({ GK: 'GOALKEEPER', LB: 'DEFENDER', CB: 'DEFENDER', RB: 'DEFENDER', LW: 'FORWARD', ST: 'FORWARD', RW: 'FORWARD' })[player.requiredPosition] ?? 'MIDFIELDER',
  })) : []
  const players = lockedSubmission ? [...snapshotPlayers, ...rosterPlayers.filter(player => !snapshotPlayers.some(p => p.id === player.id))] : rosterPlayers
  const privateBlocked = Boolean(authLoading || account && (!contestOpen || !entry.ready || entry.busy || entry.conflict || status !== 'success' || !rosterAsOf))
  const [activeKey, setActiveKey] = useState(null)
  const [search, setSearch] = useState('')
  const [clubFilter, setClubFilter] = useState('')
  const [message, setMessage] = useState('')
  const [theme, setTheme] = useState('dark')
  const [view, setView] = useState('user')
  const [gameweek, setGameweek] = useState(1)
  const searchRef = useRef(null)
  const resultState = useFantasyResult(account, contestGameweek, contestStatus, entry.data?.submitted?.version ?? null, view === 'user')
  const playerPoints = publishedPlayerPoints(resultState.data, account?.id, contestGameweek)

  const currentLineup = account ? !entry.ready ? emptyLineup() : lockedSubmission ? entryLineup({ submitted: lockedSubmission }) : lineup :
    status === 'success' ? normalizeLineup(lineup, players) : lineup
  const formation = FORMATIONS[currentLineup?.formation] ? currentLineup.formation : '4-2-1-3'
  const picks = currentLineup?.picks ?? {}
  const unassigned = currentLineup?.unassigned ?? []
  const slots = useMemo(() => formationSlots(formation), [formation])
  const byId = useMemo(() => new Map(players.map((player) => [player.id, player])), [players])
  const selectedIds = [...new Set([...Object.values(picks), ...unassigned])]
  const selected = selectedIds.map(id => byId.get(id)).filter(Boolean)
  const total = selected.reduce((sum, player) => sum + (player.fc27Overall ?? 0), 0)
  const issues = lineupIssues(formation, picks, players, unassigned)
  const clubCounts = selected.reduce((counts, player) => counts.set(player.clubId,
    { name: player.club, count: (counts.get(player.clubId)?.count ?? 0) + 1 }), new Map())
  const activeSlot = slots.find((slot) => slot.key === activeKey)
  const clubs = useMemo(() => [...new Map(players.map((player) => [player.clubId,
    { id: player.clubId, name: player.club }])).values()].sort((a, b) => a.name.localeCompare(b.name, 'vi')), [players])
  const candidates = players.filter((player) =>
    fitsSlot(player, activeSlot) && (!clubFilter || player.clubId === Number(clubFilter)) &&
    `${player.name} ${player.club}`.toLocaleLowerCase('vi').includes(search.trim().toLocaleLowerCase('vi')))
    .sort((a, b) => (b.fc27Overall ?? -1) - (a.fc27Overall ?? -1) || a.name.localeCompare(b.name, 'vi'))

  useEffect(() => {
    if (!account && !authLoading && status === 'success') setLineup((current) => normalizeLineup(current, rosterPlayers))
  }, [status, rosterPlayers, account, authLoading])

  useEffect(() => {
    if (account || authLoading || status !== 'success') return
    try { window.localStorage.setItem(FANTASY_STORAGE_KEY, JSON.stringify(currentLineup)) }
    catch { /* Fantasy vẫn dùng được nếu trình duyệt chặn localStorage. */ }
  }, [status, currentLineup, account, authLoading])

  useEffect(() => { setActiveKey(null); setMessage('') }, [contestGameweek, account?.id])
  useEffect(() => { if (privateBlocked) setActiveKey(null) }, [privateBlocked])

  useEffect(() => {
    if (!activeKey) return undefined
    searchRef.current?.focus()
    function onKeyDown(event) { if (event.key === 'Escape') setActiveKey(null) }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [activeKey])

  function openPicker(key) {
    if (privateBlocked) return
    setActiveKey(key)
    setClubFilter('')
    setSearch('')
    setMessage('')
  }

  function choose(player) {
    if (privateBlocked) return
    const issue = pickError(player, activeSlot, picks, players, unassigned)
    if (issue) { setMessage(issue); return }
    setLineup({ formation, picks: { ...picks, [activeKey]: player.id }, unassigned })
    setActiveKey(null)
    setMessage('')
  }

  function remove(key) {
    if (privateBlocked) return
    const next = { ...picks }
    delete next[key]
    setLineup({ formation, picks: next, unassigned })
    setMessage('')
  }

  function changeFormation(nextFormation) {
    if (privateBlocked) return
    setLineup(movePicks(formation, nextFormation, picks, players, unassigned))
    setActiveKey(null)
    setMessage('')
  }

  return <section className="fantasy-page" id="directory" aria-labelledby="fantasy-heading" data-theme={theme}>
    <div className="fantasy-wrap">
      <header className="fantasy-head fantasy-hero">
        <div><p className="fantasy-kicker">prismaXI / Fantasy · 2026/27</p>
          <h1 id="fantasy-heading" className="fantasy-hero-title"><span>Chọn XI trong mơ của bạn.</span><span>Chinh phục từng vòng đấu.</span></h1>
          <p>Chọn 11 cầu thủ theo sơ đồ và giới hạn {MAX_OVR} OVR. Lưu đội hình trước hạn rồi cạnh tranh điểm số qua từng Gameweek.</p>
          <button type="button" className="fantasy-hero-cta" onClick={() => {
            setView('user'); setActiveKey(null)
            requestAnimationFrame(() => {
              const builder = document.getElementById('fantasy-lineup-builder')
              builder?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start' })
              builder?.focus({ preventScroll: true })
            })
          }}>Tạo đội hình<ArrowIcon diagonal /></button>
        </div>
        <button className="fantasy-theme" type="button" onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
          aria-label={theme === 'light' ? 'Đổi sang giao diện tối' : 'Đổi sang giao diện sáng'}>◐</button>
      </header>
      <FantasyGameweek onSelectionChange={contestSelection} />
      <div className="fantasy-feature-tabs" role="tablist" aria-label="Đội hình Fantasy">
        {VIEWS.map(([key, label], index) => <button key={key} type="button" role="tab"
          id={`fantasy-tab-${key}`} aria-controls={`fantasy-panel-${key}`} aria-selected={view === key} tabIndex={view === key ? 0 : -1}
          onClick={() => { setView(key); setActiveKey(null) }} onKeyDown={event => {
            const next = event.key === 'Home' ? 0 : event.key === 'End' ? VIEWS.length - 1 : event.key === 'ArrowLeft' ? (index + VIEWS.length - 1) % VIEWS.length : event.key === 'ArrowRight' ? (index + 1) % VIEWS.length : null
            if (next === null) return
            event.preventDefault(); setView(VIEWS[next][0]); setActiveKey(null)
            event.currentTarget.parentElement.children[next].focus()
          }}>{label}</button>)}
      </div>
      {view === 'team' && <div id="fantasy-panel-team" role="tabpanel" aria-labelledby="fantasy-tab-team">
        <TeamOfWeek gameweek={gameweek} onGameweekChange={setGameweek} /></div>}
      {view === 'leaderboard' && <div id="fantasy-panel-leaderboard" role="tabpanel" aria-labelledby="fantasy-tab-leaderboard">
        <FantasyLeaderboard gameweek={contestGameweek ?? 6} account={account} /></div>}
      <div id="fantasy-panel-user" role="tabpanel" aria-labelledby="fantasy-tab-user" hidden={view !== 'user'}>
      <div className="fantasy-builder-head" id="fantasy-lineup-builder" tabIndex={-1}>
        <h2>Chọn đội hình</h2><FantasyRules gameweek={contestGameweek} deadlineUtc={contestDeadline} />
      </div>
      {authLoading && <p role="status">Đang nhận phiên đăng nhập…</p>}
      {authError && <p className="fantasy-message" role="alert">Chưa xác định được tài khoản: {authError}</p>}
      {!authLoading && !account && <p className="fantasy-message">Đăng nhập để lưu đội hình của bạn cho Gameweek. Lựa chọn thử trong trình duyệt không tự tham gia cuộc thi.</p>}
      {account && <div className="fantasy-entry-state" aria-busy={entry.loading || entry.busy}>
        {entry.loading && <p role="status">Đang tải đội từ server…</p>}
        {contestGameweek !== null && contestGameweek < 6 && <p>Replay chưa triển khai lưu/chốt đội.</p>}
        {!entry.ready && contestGameweek >= 6 && !entry.loading && <p>Chưa tải được đội của vòng này. Sân vẫn hiển thị; chọn, lưu và chốt đội đang tạm khóa.</p>}
        {!rosterAsOf && contestGameweek >= 6 && <p>Chưa nhận được mốc danh sách cầu thủ của vòng thi. Thử lại ở phần Vòng thi phía trên.</p>}
        {entry.error && <p className="fantasy-message" role="alert">{entry.error}</p>}
        {(entry.error || entry.conflict) && <button type="button" className="fantasy-clear" disabled={entry.busy} onClick={entry.reload}>Tải lại đội từ server</button>}
        {entry.ready && <>
          {!contestOpen && ['LOCKED', 'AWAITING_RESULTS'].includes(contestStatus) && <p role="status">Đã hết deadline. Đội hình đã khóa; kết quả sẽ được công bố sau.</p>}
          {!contestOpen && contestStatus === 'PUBLISHED' && <p role="status">Đội hình đã khóa. Kết quả đã được công bố bên dưới và trong tab BXH người chơi.</p>}
          {!contestOpen && !contestStatus && <p>Vòng thi chưa mở hoặc chưa tải được trạng thái. Chưa thể lưu đội hình.</p>}
        </>}
      </div>}
      {!authLoading && <ResultPanel status={status} error={error} count={players.length} itemName="cầu thủ Fantasy" keepContent={Boolean(account)}
        emptyMessage="Chưa có cầu thủ trong roster mùa 2026/27." onRetry={reload}>
      <fieldset className="fantasy-editor" disabled={privateBlocked}>
        <div className="fantasy-summary">
          <div className="fantasy-card fantasy-total"><span className="fantasy-label">Tổng overall</span>
            <div className="fantasy-total-body"><span className="fantasy-ring" style={{ '--progress': `${Math.min(100, total / MAX_OVR * 100)}%` }}><span>{selectedIds.length}/11</span></span>
              <strong>{total}<small> / {MAX_OVR}</small></strong></div>
            <p>{unassigned.length ? `${Object.keys(picks).length}/11 đã xếp · ${unassigned.length} cần thay` :
              selected.length < 11 ? `Còn ${11 - selected.length} vị trí cần chọn` : 'Đội hình đã đủ 11 người'}</p>
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
          <button className="fantasy-clear" type="button" onClick={() => { setLineup({ formation, picks: {}, unassigned: [] }); setMessage('') }}>Xóa hết</button>
          <button className="fantasy-primary" type="button" disabled={!account || privateBlocked || Boolean(issues.length) || Boolean(unassigned.length)} onClick={() => entry.submit(currentLineup)}>{entry.busy ? 'Đang lưu…' : 'Lưu đội hình'}</button>
        </div>
        {entry.notice && <p className="fantasy-result" role="status">{entry.notice}</p>}
        {message && <p className="fantasy-message" role="alert">{message}</p>}
        {currentLineup?.notices?.map(notice => <p className="fantasy-message" role="alert" key={notice}>{notice}</p>)}
        {Object.keys(picks).length > 0 && issues.filter(issue => !issue.startsWith('Chọn đủ')).length > 0 &&
          <p className="fantasy-message" role="alert">{issues.filter(issue => !issue.startsWith('Chọn đủ')).join(' ')}</p>}
        {unassigned.length > 0 && <div className="fantasy-card fantasy-unassigned" role="region" aria-label="Cầu thủ cần thay">
          <h2>Cầu thủ cần thay hoặc chưa xếp được ({unassigned.length})</h2>
          <p>Lựa chọn vẫn được giữ. Đổi sơ đồ phù hợp hoặc bỏ người cần thay trước khi chọn người khác.</p>
          {unassigned.map(id => { const player = byId.get(id)
            return <div key={id}><span><strong>{player?.name ?? `Cầu thủ #${id}`}</strong> · {player?.eligiblePositions?.join('/') || 'Chưa có vị trí'}
              <small>{playerDataError(player) || `Không có ô phù hợp còn trống trong sơ đồ ${formation}.`}</small></span>
              <button className="fantasy-clear" type="button" onClick={() => {
                setLineup({ formation, picks, unassigned: unassigned.filter(value => value !== id) })
              }}>Bỏ {player?.name ?? `#${id}`}</button></div>
          })}
        </div>}
        <div className="fantasy-main">
          <div className="fantasy-pitch" aria-label="Sân bóng với đội hình đã chọn"><Pitch />
            {slots.map((slot) => { const player = byId.get(picks[slot.key])
              return <button className="fantasy-slot" type="button" key={slot.key} style={{ left: `${slot.x}%`, top: `${slot.y}%` }}
                onClick={() => openPicker(slot.key)} aria-label={`${slot.position}: ${player ? player.name : 'chọn cầu thủ'}${playerPoints.has(player?.id) ? ` · ${formatPoints(playerPoints.get(player.id))} điểm` : ''}`}>
                <span className={`fantasy-position fantasy-position-${slot.group}`}>{slot.position}</span>
                <PlayerAvatar player={player} points={playerPoints.get(player?.id)} /><span className="fantasy-slot-name">{player?.name ?? 'Chọn cầu thủ'}</span>
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
      </fieldset>
      </ResultPanel>}
      {account && contestGameweek >= 6 && <FantasyResults key={`${account.id}:${contestGameweek}`} account={account} gameweek={contestGameweek} resultState={resultState} />}
      </div>
    </div>
    {view === 'user' && activeSlot && <div className="fantasy-overlay" onMouseDown={(event) => { if (event.target === event.currentTarget) setActiveKey(null) }}>
      <div className="fantasy-sheet" role="dialog" aria-modal="true" aria-labelledby="fantasy-picker-title">
        <div className="fantasy-sheet-head"><div><h2 id="fantasy-picker-title">Chọn cầu thủ</h2><span>Vị trí {activeSlot.position} · {GROUP_LABEL[activeSlot.group]}</span></div>
          <button type="button" className="fantasy-theme" onClick={() => setActiveKey(null)} aria-label="Đóng">×</button></div>
        <label className="fantasy-search"><span className="visually-hidden">Tìm theo tên cầu thủ hoặc CLB</span><span aria-hidden="true">⌕</span>
          <input ref={searchRef} value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tên cầu thủ hoặc CLB" /></label>
        <div className="fantasy-filters">
          <span>Chỉ cầu thủ được chơi ô {activeSlot.position}</span>
          <select value={clubFilter} onChange={(event) => setClubFilter(event.target.value)} aria-label="Lọc câu lạc bộ">
            <option value="">Mọi CLB</option>{clubs.map((club) => <option key={club.id} value={club.id}>{club.name}</option>)}
          </select>
        </div>
        <div className="fantasy-results">{candidates.length ? candidates.map((player) => { const issue = pickError(player, activeSlot, picks, players, unassigned)
          return <div className="fantasy-candidate" key={player.id}><PlayerAvatar player={player} small />
            <div><strong>{player.name}</strong><small>{player.club} · Chính: {player.primaryPosition}</small>
              <small className="fantasy-eligible">Được chơi: {player.eligiblePositions.join(' / ')}</small>
              {issue && <small className="fantasy-pick-issue">{issue}</small>}</div>
            <span className={player.fc27Overall == null ? 'fantasy-no-rating' : `fantasy-rating fantasy-rating-${ratingTier(player.fc27Overall)}`}>
              {player.fc27Overall ?? '—'}</span>
            <button type="button" disabled={Boolean(issue)} title={issue || undefined} onClick={() => choose(player)}>
              {issue ? (issue.includes('CLB') ? 'Đủ 3/CLB' : issue.includes('OVR') ? 'Vượt OVR' : issue.includes('Đã giữ') ? 'Đủ 11' : 'Đã chọn') : 'Chọn'}</button>
          </div> }) : <p className="fantasy-no-results">Không tìm thấy cầu thủ. Thử đổi từ khóa hoặc bộ lọc.</p>}</div>
        <div className="fantasy-sheet-foot"><span>{candidates.length} phù hợp · {players.filter(player => playerDataError(player)).length} thiếu OVR/vị trí</span>
          {picks[activeKey] != null && <button type="button" onClick={() => { remove(activeKey); setActiveKey(null) }}>Bỏ chọn vị trí này</button>}</div>
      </div>
    </div>}
  </section>
}

export default FantasyPage
