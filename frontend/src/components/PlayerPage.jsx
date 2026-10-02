import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchPlayers } from '../api/players.js'
import { useApiList } from '../hooks/useApiList.js'
import { matchesPlayerSearch, sortPlayers } from '../utils/playerSort.js'
import { SEASONS } from '../utils/seasons.js'
import PlayerCard from './PlayerCard.jsx'
import ResultPanel from './ResultPanel.jsx'
import './PlayerPage.css'

const PAGE_SIZE = 24
const POSITIONS = [
  { value: '', label: 'Tất cả' },
  { value: 'GOALKEEPER', label: 'Thủ môn' },
  { value: 'DEFENDER', label: 'Hậu vệ' },
  { value: 'MIDFIELDER', label: 'Tiền vệ' },
  { value: 'FORWARD', label: 'Tiền đạo' },
]

function PlayerPage({ season, onSeasonChange }) {
  const [query, setQuery] = useState('')
  const [club, setClub] = useState('')
  const [position, setPosition] = useState('')
  const [view, setView] = useState('grid')
  const [sortBy, setSortBy] = useState('overall')
  const [direction, setDirection] = useState('desc')
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE)
  const searchRef = useRef(null)
  const requestPlayers = useCallback((signal) => fetchPlayers({ club: '', position: '' }, signal, season), [season])
  const { data: players, status, error, reload } = useApiList(requestPlayers)

  useEffect(() => {
    function handleShortcut(event) {
      if (event.key !== '/' || event.altKey || event.ctrlKey || event.metaKey ||
          ['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement?.tagName)) return
      event.preventDefault()
      searchRef.current?.focus()
    }
    window.addEventListener('keydown', handleShortcut)
    return () => window.removeEventListener('keydown', handleShortcut)
  }, [])

  const clubs = [...new Set(players.map((player) => player.club))].sort((a, b) => a.localeCompare(b, 'vi'))
  const filtered = players.filter((player) =>
    (!query || matchesPlayerSearch(player.name, query)) && (!club || player.club === club) &&
    (!position || player.position === position))
  const sorted = sortPlayers(filtered, sortBy, direction)
  const shown = sorted.slice(0, visibleCount)
  const hasFilters = Boolean(query || club || position)

  function clearFilters() {
    setQuery('')
    setClub('')
    setPosition('')
    setVisibleCount(PAGE_SIZE)
  }

  return <section className="player-directory" id="directory" aria-labelledby="players-heading">
    <div className="pp-wrap">
      <div className="pp-top"><div className="pp-seasons" role="group" aria-label="Mùa giải">
        {Object.entries(SEASONS).map(([year, label]) => <button key={year} type="button" aria-pressed={season === Number(year)} onClick={() => onSeasonChange(Number(year))}>{label}</button>)}
      </div></div>

      <header className="pp-hero"><div><p>PREMIERHUB / CẦU THỦ</p><h1 id="players-heading">Cầu thủ</h1><span>Premier League {SEASONS[season]}</span></div><div className="pp-hero-art" aria-hidden="true"><i /><i /></div></header>

      <div className="pp-filters" role="group" aria-label="Bộ lọc cầu thủ">
        <div className="pp-search"><span aria-hidden="true">⌕</span><input ref={searchRef} type="search" value={query} onChange={(event) => { setQuery(event.target.value); setVisibleCount(PAGE_SIZE) }} placeholder="Tìm cầu thủ theo tên" aria-label="Tìm cầu thủ theo tên" />
          {query ? <button type="button" onClick={() => { setQuery(''); setVisibleCount(PAGE_SIZE); searchRef.current?.focus() }}>Xóa</button> : <kbd>/</kbd>}
        </div>
        <div className="pp-filter-row"><div className="pp-position-filters" role="group" aria-label="Vị trí">{POSITIONS.map((item) => <button key={item.value} type="button" aria-pressed={position === item.value} onClick={() => { setPosition(item.value); setVisibleCount(PAGE_SIZE) }}>{item.label}</button>)}</div>
          <label className="pp-select"><span className="sr-only">Câu lạc bộ</span><select value={club} onChange={(event) => { setClub(event.target.value); setVisibleCount(PAGE_SIZE) }} disabled={status !== 'success'}><option value="">Mọi CLB</option>{clubs.map((name) => <option key={name} value={name}>{name}</option>)}</select></label>
          <span className="pp-filter-spacer" />
          <label className="pp-select pp-sort-select"><span className="sr-only">Sắp xếp theo</span><select value={sortBy} onChange={(event) => { setSortBy(event.target.value); setVisibleCount(PAGE_SIZE) }}><option value="overall">OVR FC 27</option><option value="name">Tên</option><option value="club">Câu lạc bộ</option><option value="goals">Bàn thắng</option><option value="assists">Kiến tạo</option></select></label>
          <button className="pp-sort-direction" type="button" onClick={() => setDirection(direction === 'desc' ? 'asc' : 'desc')} aria-label={`Sắp xếp ${direction === 'desc' ? 'giảm dần' : 'tăng dần'}. Bấm để đổi chiều`} title={direction === 'desc' ? 'Giảm dần · bấm để tăng dần' : 'Tăng dần · bấm để giảm dần'}>{direction === 'desc' ? '↓' : '↑'}<span>{direction === 'desc' ? 'Giảm' : 'Tăng'}</span></button>
          <div className="pp-view-toggle" role="group" aria-label="Kiểu hiển thị"><button type="button" aria-pressed={view === 'grid'} aria-label="Dạng lưới" onClick={() => setView('grid')}>▦</button><button type="button" aria-pressed={view === 'list'} aria-label="Dạng danh sách" onClick={() => setView('list')}>☷</button></div>
        </div>
      </div>

      {status === 'success' && <div className="pp-results"><span><strong>{filtered.length}</strong> cầu thủ{filtered.length !== players.length ? ` trên ${players.length}` : ''}</span>{hasFilters && <button type="button" onClick={clearFilters}>Xóa bộ lọc</button>}</div>}
      {season === 2024 && <p className="pp-note">Dữ liệu cầu thủ mùa 2024/25 mới được lưu một phần. OVR FC 27 không áp dụng cho mùa này.</p>}
      <ResultPanel status={status} error={error} count={filtered.length} itemName="cầu thủ" emptyMessage={hasFilters ? 'Không có cầu thủ khớp với bộ lọc hiện tại.' : 'API hiện chưa có cầu thủ nào.'} onRetry={reload} onClear={hasFilters ? clearFilters : undefined}>
        <div className={view === 'grid' ? 'pp-grid' : 'pp-list'}>{shown.map((player) => <PlayerCard key={`${player.id}-${player.clubId}`} player={player} season={season} view={view} />)}</div>
        {shown.length < sorted.length && <button className="pp-more" type="button" onClick={() => setVisibleCount((count) => count + PAGE_SIZE)}>Xem thêm {Math.min(PAGE_SIZE, sorted.length - shown.length)} cầu thủ</button>}
      </ResultPanel>
    </div>
  </section>
}

export default PlayerPage
