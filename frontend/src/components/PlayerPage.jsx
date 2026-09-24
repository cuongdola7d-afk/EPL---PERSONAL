import { useCallback, useState } from 'react'
import { fetchClubs } from '../api/clubs.js'
import { fetchPlayers } from '../api/players.js'
import { useApiList } from '../hooks/useApiList.js'
import PlayerCard from './PlayerCard.jsx'
import ResultPanel from './ResultPanel.jsx'

const POSITIONS = [
  { value: 'GOALKEEPER', label: 'Thủ môn' },
  { value: 'DEFENDER', label: 'Hậu vệ' },
  { value: 'MIDFIELDER', label: 'Tiền vệ' },
  { value: 'FORWARD', label: 'Tiền đạo' },
]

function PlayerPage() {
  const [club, setClub] = useState('')
  const [position, setPosition] = useState('')
  const [sortByGoals, setSortByGoals] = useState(false)
  const requestClubs = useCallback((signal) => fetchClubs('', signal), [])
  const requestPlayers = useCallback(
    (signal) => fetchPlayers({ club, position }, signal),
    [club, position],
  )
  const clubList = useApiList(requestClubs)
  const { data: players, status, error, reload } = useApiList(requestPlayers)
  const hasFilters = Boolean(club || position)
  const visiblePlayers = [...players]
  if (sortByGoals) {
    visiblePlayers.sort((a, b) => b.goals - a.goals || a.name.localeCompare(b.name))
  }
  const totals = players.reduce((sum, player) => ({
    goals: sum.goals + player.goals,
    assists: sum.assists + player.assists,
  }), { goals: 0, assists: 0 })

  function clearFilters() {
    setClub('')
    setPosition('')
  }

  return (
    <section className="directory-section" id="directory" aria-labelledby="players-heading">
      <div className="container">
        <div className="section-heading">
          <div>
            <p className="section-kicker">KHÁM PHÁ GIẢI ĐẤU <span>02 / PLAYERS</span></p>
            <h2 id="players-heading">Cầu thủ</h2>
            <p className="section-description">Cầu thủ, vị trí và chỉ số từ API PremierHub.</p>
          </div>
          {status === 'success' && (
            <p className="result-count" aria-live="polite">
              <strong>{players.length}</strong> {hasFilters ? 'kết quả' : 'cầu thủ'}
            </p>
          )}
        </div>

        <div className="search-form filters-form" role="group" aria-label="Bộ lọc cầu thủ">
          <div className="filter-controls">
            <div className="filter-field">
              <label htmlFor="player-club">Câu lạc bộ</label>
              <select
                id="player-club"
                value={club}
                onChange={(event) => setClub(event.target.value)}
                disabled={clubList.status !== 'success'}
              >
                <option value="">Tất cả câu lạc bộ</option>
                {clubList.data.map((item) => <option key={item.id} value={item.name}>{item.name}</option>)}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="player-position">Vị trí</label>
              <select id="player-position" value={position} onChange={(event) => setPosition(event.target.value)}>
                <option value="">Tất cả vị trí</option>
                {POSITIONS.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="player-sort">Sắp xếp</label>
              <select id="player-sort" value={sortByGoals ? 'goals' : 'default'} onChange={(event) => setSortByGoals(event.target.value === 'goals')}>
                <option value="default">Thứ tự API</option>
                <option value="goals">Bàn thắng giảm dần</option>
              </select>
            </div>
            {hasFilters && <button className="clear-button filter-clear" type="button" onClick={clearFilters}>Xóa lọc</button>}
          </div>
          {clubList.status === 'loading' && <p className="filter-note">Đang tải danh sách câu lạc bộ...</p>}
          {clubList.status === 'error' && (
            <p className="filter-note filter-note-error" role="alert">
              Không tải được lựa chọn câu lạc bộ. <button type="button" onClick={clubList.reload}>Thử lại</button>
            </p>
          )}
        </div>

        {status === 'success' && (
          <div className="result-summary" aria-live="polite">
            <span>Tổng trong kết quả lọc</span>
            <strong>{totals.goals} <small>bàn thắng</small></strong>
            <strong>{totals.assists} <small>kiến tạo</small></strong>
          </div>
        )}

        <ResultPanel
          status={status}
          error={error}
          count={visiblePlayers.length}
          itemName="cầu thủ"
          emptyMessage={hasFilters ? 'Không có cầu thủ khớp với bộ lọc hiện tại.' : 'API hiện chưa có cầu thủ nào.'}
          onRetry={reload}
          onClear={hasFilters ? clearFilters : undefined}
        >
          <div className="player-grid">
            {visiblePlayers.map((player) => (
              <PlayerCard
                key={player.id}
                player={player}
                positionLabel={POSITIONS.find((item) => item.value === player.position)?.label ?? player.position}
              />
            ))}
          </div>
        </ResultPanel>
      </div>
    </section>
  )
}

export default PlayerPage
