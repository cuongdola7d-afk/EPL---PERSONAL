import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchReplayPlayers } from '../api/replay.js'
import { useApiList } from '../hooks/useApiList.js'
import { additionError, lineupError, normalizeSavedIds, POSITION_LABELS,
  POSITION_LIMITS, REPLAY_STORAGE_KEY, replayTotal } from '../fantasy/replay.js'
import ResultPanel from './ResultPanel.jsx'

function readSavedIds() {
  try {
    const saved = JSON.parse(window.localStorage.getItem(REPLAY_STORAGE_KEY) ?? '[]')
    return Array.isArray(saved) ? [...new Set(saved.filter(Number.isInteger))].slice(0, 11) : []
  } catch {
    return []
  }
}

function FantasyReplayPage() {
  const requestPlayers = useCallback((signal) => fetchReplayPlayers(signal), [])
  const { data: players, status, error, reload } = useApiList(requestPlayers)
  const [selectedIds, setSelectedIds] = useState(readSavedIds)
  const [search, setSearch] = useState('')
  const [position, setPosition] = useState('')
  const [clubId, setClubId] = useState('')
  const [selectionError, setSelectionError] = useState('')
  const [revealed, setRevealed] = useState(false)

  useEffect(() => {
    if (status === 'success') setSelectedIds((ids) => normalizeSavedIds(ids, players))
  }, [status, players])

  useEffect(() => {
    try {
      window.localStorage.setItem(REPLAY_STORAGE_KEY, JSON.stringify(selectedIds))
    } catch {
      // Replay vẫn dùng được nếu trình duyệt chặn localStorage.
    }
  }, [selectedIds])

  const byId = useMemo(() => new Map(players.map((player) => [player.id, player])), [players])
  const selected = selectedIds.map((id) => byId.get(id)).filter(Boolean)
  const clubs = useMemo(() => [...new Map(players.map((player) => [player.clubId,
    { id: player.clubId, name: player.clubName }])).values()]
    .sort((a, b) => a.name.localeCompare(b.name, 'vi')), [players])
  const visible = players.filter((player) =>
    (!position || player.position === position) &&
    (!clubId || player.clubId === Number(clubId)) &&
    (!search || `${player.name} ${player.clubName}`.toLocaleLowerCase('vi')
      .includes(search.trim().toLocaleLowerCase('vi'))))

  function add(player) {
    const message = additionError(selectedIds, player, players)
    if (message) { setSelectionError(message); return }
    setSelectedIds((ids) => [...ids, player.id])
    setSelectionError('')
    setRevealed(false)
  }

  function remove(id) {
    setSelectedIds((ids) => ids.filter((item) => item !== id))
    setSelectionError('')
    setRevealed(false)
  }

  function showResult() {
    const message = lineupError(selectedIds, players)
    setSelectionError(message)
    setRevealed(!message)
  }

  return (
    <section className="directory-section replay-page" id="directory" aria-labelledby="replay-heading">
      <div className="container">
        <div className="section-heading">
          <div>
            <p className="section-kicker">FANTASY REPLAY <span>GW1 / 2024–25</span></p>
            <h2 id="replay-heading">Chọn đội hình của bạn</h2>
            <p className="section-description">Bản chơi lại Gameweek 1 mùa 2024/25 đã kết thúc. Chọn 11 cầu thủ từ 10 trận đã xác minh, rồi xem điểm v1 thực tế.</p>
          </div>
        </div>

        <ResultPanel status={status} error={error} count={players.length} itemName="cầu thủ Replay"
          emptyMessage="Chưa có cầu thủ với điểm đã xác minh cho GW1." onRetry={reload}>
          <div className="replay-layout">
            <div>
              <div className="search-form replay-filters">
                <div className="filter-controls">
                  <div className="filter-field">
                    <label htmlFor="replay-search">Tìm cầu thủ</label>
                    <input id="replay-search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Tên cầu thủ hoặc CLB" />
                  </div>
                  <div className="filter-field">
                    <label htmlFor="replay-position">Vị trí</label>
                    <select id="replay-position" value={position} onChange={(event) => setPosition(event.target.value)}>
                      <option value="">Mọi vị trí</option>
                      {Object.entries(POSITION_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
                    </select>
                  </div>
                  <div className="filter-field">
                    <label htmlFor="replay-club">CLB</label>
                    <select id="replay-club" value={clubId} onChange={(event) => setClubId(event.target.value)}>
                      <option value="">Mọi CLB</option>
                      {clubs.map((club) => <option key={club.id} value={club.id}>{club.name}</option>)}
                    </select>
                  </div>
                </div>
              </div>
              <p className="replay-count">{visible.length} / {players.length} cầu thủ từ 10 trận GW1 đã xác minh</p>
              {visible.length === 0 && <p className="match-detail-empty">Không có cầu thủ khớp bộ lọc.</p>}
              <div className="replay-candidates">
                {visible.map((player) => (
                  <div className="replay-candidate" key={player.id}>
                    <div><strong>{player.name}</strong><span>{POSITION_LABELS[player.position]} · {player.clubName}</span></div>
                    <button type="button" disabled={selectedIds.includes(player.id)} onClick={() => add(player)}
                      aria-label={`Chọn ${player.name}, ${player.clubName}`}>
                      {selectedIds.includes(player.id) ? 'Đã chọn' : 'Chọn'}
                    </button>
                  </div>
                ))}
              </div>
            </div>

            <aside className="replay-lineup" aria-labelledby="lineup-heading">
              <h3 id="lineup-heading">Đội hình <span>{selected.length}/11</span></h3>
              <p>1 thủ môn · 3–5 hậu vệ · 2–5 tiền vệ · 1–3 tiền đạo · tối đa 3 người mỗi CLB.</p>
              <div className="replay-position-counts">
                {Object.entries(POSITION_LIMITS).map(([code, [min, max]]) => (
                  <span key={code}>{code}: {selected.filter((player) => player.position === code).length} ({min}–{max})</span>
                ))}
              </div>
              {selected.length === 0 && <p className="replay-empty">Chọn cầu thủ ở danh sách bên trái để bắt đầu.</p>}
              <ul className="replay-selected">
                {selected.map((player) => (
                  <li key={player.id}>
                    <span><strong>{player.name}</strong><small>{POSITION_LABELS[player.position]} · {player.clubName}</small></span>
                    <button type="button" onClick={() => remove(player.id)} aria-label={`Bỏ ${player.name}`}>Bỏ</button>
                  </li>
                ))}
              </ul>
              {selectionError && <p className="filter-note filter-note-error" role="alert">{selectionError}</p>}
              <button className="search-button replay-submit" type="button" onClick={showResult}>Xem kết quả</button>
              {revealed && (
                <div className="replay-result" role="status">
                  <span>Tổng điểm v1 · GW1 2024/25</span>
                  <strong>{replayTotal(selectedIds, players)} điểm</strong>
                  <p>Điểm từ 11 cầu thủ của 10 trận đã kết thúc; không có đội trưởng, bonus hay điểm giữ sạch lưới.</p>
                  <ul>{selected.map((player) => <li key={player.id}><span>{player.name}</span><strong>{player.points}</strong></li>)}</ul>
                </div>
              )}
            </aside>
          </div>
        </ResultPanel>
      </div>
    </section>
  )
}

export default FantasyReplayPage
