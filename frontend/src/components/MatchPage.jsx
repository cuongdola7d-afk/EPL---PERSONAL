import { useCallback, useMemo, useState } from 'react'
import { fetchMatches } from '../api/matches.js'
import { useApiList } from '../hooks/useApiList.js'
import { preferredMatchweek } from '../utils/matchView.js'
import { SEASONS } from '../utils/seasons.js'
import MatchCard from './MatchCard.jsx'
import MatchDetail from './MatchDetail.jsx'
import './MatchPage.css'

const dateLabel = (date) => new Intl.DateTimeFormat('vi-VN', {
  weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${date}T00:00:00Z`))

function MatchPage({ season, onSeasonChange }) {
  const [week, setWeek] = useState(null)
  const [filter, setFilter] = useState('all')
  const [club, setClub] = useState('')
  const [selectedMatchId, setSelectedMatchId] = useState(null)
  const requestMatches = useCallback((signal) => fetchMatches({ season }, signal), [season])
  const { data: matches, status, error, reload } = useApiList(requestMatches)

  const weeks = useMemo(() => [...new Set(matches.map((match) => match.matchweek))].sort((a, b) => a - b), [matches])
  const currentWeek = week ?? preferredMatchweek(matches)
  const clubs = useMemo(() => [...new Set(matches.flatMap((match) => [match.homeClub, match.awayClub]))].sort(), [matches])
  const weekMatches = matches.filter((match) => match.matchweek === currentWeek &&
    (!club || match.homeClub === club || match.awayClub === club))
  const finished = weekMatches.filter((match) => match.status === 'FINISHED').length
  const visible = weekMatches.filter((match) => filter === 'all' ||
    (filter === 'finished' ? match.status === 'FINISHED' : match.status !== 'FINISHED'))
    .sort((a, b) => a.date.localeCompare(b.date) || a.id - b.id)
  const selectedMatch = matches.find((match) => match.id === selectedMatchId)
  const weekIndex = weeks.indexOf(currentWeek)

  function changeWeek(next) {
    setWeek(next)
    setSelectedMatchId(null)
  }

  return <section className="mx-page" id="directory" aria-label="Lịch đấu và kết quả">
    <div className="mx-wrap">
      <div className="mx-season-switch" role="group" aria-label="Mùa giải">
        {Object.entries(SEASONS).map(([year, label]) => <button key={year} type="button"
          aria-pressed={season === Number(year)} onClick={() => onSeasonChange(Number(year))}>{label}</button>)}
      </div>

      {selectedMatchId !== null ? <MatchDetail key={selectedMatchId} matchId={selectedMatchId}
        summary={selectedMatch} season={season} onClose={() => setSelectedMatchId(null)} /> : <>
        <header className="mx-hero">
          <svg viewBox="0 0 200 200" fill="none" stroke="currentColor" strokeWidth="3" aria-hidden="true">
            <circle cx="100" cy="100" r="80" /><circle cx="100" cy="100" r="46" />
            <circle cx="100" cy="100" r="6" fill="currentColor" /><path d="M100 20v160" />
          </svg>
          <h1>Lịch đấu</h1><p>Premier League {SEASONS[season]} · kết quả và các trận sắp tới</p>
        </header>

        <div className="mx-filters">
          <div className="mx-round-nav">
            <button type="button" aria-label="Vòng trước" disabled={weekIndex <= 0}
              onClick={() => changeWeek(weeks[weekIndex - 1])}>‹</button>
            <select value={currentWeek} aria-label="Chọn vòng đấu"
              onChange={(event) => changeWeek(Number(event.target.value))}>
              {(weeks.length ? weeks : [currentWeek]).map((item) => <option key={item} value={item}>Gameweek {item}</option>)}
            </select>
            <button type="button" aria-label="Vòng sau" disabled={weekIndex < 0 || weekIndex >= weeks.length - 1}
              onClick={() => changeWeek(weeks[weekIndex + 1])}>›</button>
          </div>
          <div className="mx-status-filters" role="group" aria-label="Lọc trạng thái trận">
            {[
              ['all', 'Tất cả', weekMatches.length],
              ['finished', 'Đã kết thúc', finished],
              ['upcoming', 'Chưa kết thúc', weekMatches.length - finished],
            ].map(([value, label, count]) => <button key={value} type="button"
              aria-pressed={filter === value} onClick={() => setFilter(value)}>{label}<span>{count}</span></button>)}
          </div>
          <select className="mx-club-filter" value={club} aria-label="Lọc theo câu lạc bộ"
            onChange={(event) => setClub(event.target.value)}>
            <option value="">Tất cả CLB</option>
            {clubs.map((name) => <option key={name} value={name}>{name}</option>)}
          </select>
        </div>

        {status === 'loading' && <div className="mx-skeletons" role="status" aria-label="Đang tải lịch đấu">
          {Array.from({ length: 4 }, (_, index) => <div key={index} />)}
        </div>}
        {status === 'error' && <div className="mx-empty" role="alert"><strong>Không tải được lịch đấu</strong>
          <p>{error}</p><button type="button" onClick={reload}>Thử lại</button></div>}
        {status === 'success' && <>
          <div className="mx-count">Gameweek {currentWeek} · <strong>{visible.length}</strong> trận</div>
          {visible.length === 0 ? <div className="mx-empty"><strong>Không có trận nào</strong>
            <p>Thử đổi vòng đấu hoặc bộ lọc.</p><button type="button" onClick={() => { setFilter('all'); setClub('') }}>Xem tất cả</button>
          </div> : <div className="mx-match-list">
            {visible.map((match, index) => <div key={match.id}>
              {(index === 0 || visible[index - 1].date !== match.date) &&
                <h2 className="mx-date-heading">{dateLabel(match.date)}</h2>}
              <MatchCard match={match} onOpen={setSelectedMatchId} />
            </div>)}
          </div>}
          <p className="mx-attribution">Lịch và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>.</p>
        </>}
      </>}
    </div>
  </section>
}

export default MatchPage
