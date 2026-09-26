import { useCallback, useState } from 'react'
import { fetchClubs } from '../api/clubs.js'
import { fetchMatches } from '../api/matches.js'
import { useApiList } from '../hooks/useApiList.js'
import MatchCard from './MatchCard.jsx'
import MatchDetail from './MatchDetail.jsx'
import ResultPanel from './ResultPanel.jsx'
import SeasonPicker from './SeasonPicker.jsx'
import { SEASONS, canOpenMatchStats } from '../utils/seasons.js'

const EMPTY_FILTERS = { club: '', matchweek: '', status: '' }

function MatchPage({ season, onSeasonChange }) {
  const [draftClub, setDraftClub] = useState('')
  const [draftWeek, setDraftWeek] = useState('1')
  const [draftStatus, setDraftStatus] = useState('')
  const [filters, setFilters] = useState({ ...EMPTY_FILTERS, matchweek: '1' })
  const [selectedMatchId, setSelectedMatchId] = useState(null)
  const requestClubs = useCallback((signal) => fetchClubs('', signal, season), [season])
  const requestMatches = useCallback((signal) => fetchMatches({ ...filters, season }, signal), [filters, season])
  const clubList = useApiList(requestClubs)
  const { data: matches, status, error, reload } = useApiList(requestMatches)
  const hasFilters = Boolean(filters.club || filters.matchweek || filters.status)
  const gameweeks = season === 2024 ? [1] : Array.from({ length: 38 }, (_, index) => index + 1)

  function applyFilters(event) {
    event.preventDefault()
    setSelectedMatchId(null)
    setFilters({ club: draftClub, matchweek: draftWeek, status: draftStatus })
  }

  function clearFilters() {
    setDraftClub('')
    setDraftWeek('')
    setDraftStatus('')
    setSelectedMatchId(null)
    setFilters(EMPTY_FILTERS)
  }

  return (
    <section className="directory-section" id="directory" aria-labelledby="matches-heading">
      <div className="container">
        <div className="section-heading">
          <div>
            <p className="section-kicker">KHÁM PHÁ GIẢI ĐẤU <span>03 / MATCHES</span></p>
            <h2 id="matches-heading">Lịch đấu &amp; Kết quả</h2>
            <p className="section-description">Premier League {SEASONS[season]} · {filters.matchweek ? `Gameweek ${filters.matchweek}` : 'Tất cả vòng đã lưu'}.</p>
            <p className="section-description">{season === 2024
              ? 'Dữ liệu trận đấu hiện có: Gameweek 1 với thống kê cầu thủ đã xác minh.'
              : 'Lịch đấu và kết quả cơ bản. Chưa có thống kê cầu thủ hoặc điểm Fantasy mùa 2026/27.'}</p>
          </div>
          {status === 'success' && (
            <p className="result-count" aria-live="polite"><strong>{matches.length}</strong> trận đấu</p>
          )}
        </div>

        <SeasonPicker season={season} onChange={onSeasonChange} />
        <form className="search-form" onSubmit={applyFilters}>
          <div className="filter-controls match-filters">
            <div className="filter-field">
              <label htmlFor="match-club">Câu lạc bộ</label>
              <select id="match-club" value={draftClub} onChange={(event) => setDraftClub(event.target.value)} disabled={clubList.status !== 'success'}>
                <option value="">Tất cả câu lạc bộ</option>
                {clubList.data.map((item) => <option key={item.id} value={item.name}>{item.name}</option>)}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="match-week">Gameweek</label>
              <select id="match-week" value={draftWeek} onChange={(event) => setDraftWeek(event.target.value)}>
                <option value="">Tất cả vòng đã lưu</option>
                {gameweeks.map((week) => <option key={week} value={week}>Gameweek {week}</option>)}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="match-status">Trạng thái</label>
              <select id="match-status" value={draftStatus} onChange={(event) => setDraftStatus(event.target.value)}>
                <option value="">Tất cả trạng thái</option>
                <option value="FINISHED">Đã kết thúc</option>
                <option value="SCHEDULED">Chưa diễn ra</option>
                <option value="POSTPONED">Bị hoãn</option>
                <option value="CANCELLED">Đã hủy</option>
                <option value="SUSPENDED">Tạm dừng</option>
                <option value="LIVE">Đang diễn ra</option>
                <option value="AWARDED">Kết quả xử lý</option>
              </select>
            </div>
            <button className="search-button filter-apply" type="submit">Áp dụng</button>
            {hasFilters && <button className="clear-button filter-clear" type="button" onClick={clearFilters}>Xóa lọc</button>}
          </div>
          {clubList.status === 'loading' && <p className="filter-note">Đang tải danh sách câu lạc bộ...</p>}
          {clubList.status === 'error' && (
            <p className="filter-note filter-note-error" role="alert">
              Không tải được lựa chọn câu lạc bộ. <button type="button" onClick={clubList.reload}>Thử lại</button>
            </p>
          )}
        </form>

        <ResultPanel
          status={status}
          error={error}
          count={matches.length}
          itemName="trận đấu"
          emptyMessage={hasFilters ? 'Không có trận đấu khớp với bộ lọc hiện tại.' : 'API hiện chưa có trận đấu nào.'}
          onRetry={reload}
          onClear={hasFilters ? clearFilters : undefined}
        >
          <div className="match-list">
            {matches.map((match) => <MatchCard key={match.id} match={match}
              onOpen={canOpenMatchStats(season, match) ? setSelectedMatchId : undefined} />)}
          </div>
        </ResultPanel>
        {selectedMatchId !== null && (
          <MatchDetail key={selectedMatchId} matchId={selectedMatchId} season={season} onClose={() => setSelectedMatchId(null)} />
        )}
      </div>
    </section>
  )
}

export default MatchPage
