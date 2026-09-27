import { useEffect, useState } from 'react'
import { fetchPlayer, fetchPlayerMatches } from '../api/players.js'
import { getInitials } from '../utils/initials.js'
import { effectiveStat, matchRating, matchResult, matchState, seasonSummary } from '../utils/playerDetail.js'
import { SEASONS, hasMatchScore } from '../utils/seasons.js'

const dateLabel = (date) => new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${date}T00:00:00Z`))
const metric = (value) => value === null ? 'Chưa có dữ liệu' : String(value)
const ratingLabel = (value) => value === null ? 'Chưa có dữ liệu' : value.toFixed(1)

function MatchRow({ row }) {
  const state = matchState(row)
  const result = matchResult(row)
  const score = hasMatchScore(row.match) ? `${row.match.homeGoals} – ${row.match.awayGoals}` : 'Chưa có tỉ số'
  const pendingLabels = { SCHEDULED: 'Trận chưa đá', POSTPONED: 'Trận bị hoãn', CANCELLED: 'Trận bị hủy', SUSPENDED: 'Trận tạm dừng', LIVE: 'Trận đang diễn ra', AWARDED: 'Trận xử thắng' }
  const detail = state === 'upcoming' ? (pendingLabels[row.match.status] ?? 'Trận chưa kết thúc') : state === 'missing' ? 'Thiếu thống kê cầu thủ' :
    state === 'did-not-play' ? 'Không ra sân' : 'Đã thi đấu'
  const value = (field) => state === 'upcoming' || state === 'missing' ? null : effectiveStat(row, field)
  return (
    <li className="profile-match-row">
      <div className="profile-match-main">
        <span className="profile-match-date">GW{row.match.matchweek} · {dateLabel(row.match.date)}</span>
        <strong>{row.match.homeClub} <span className="profile-score">{score}</span> {row.match.awayClub}</strong>
        <span className={`profile-match-state state-${state}`}>{detail}</span>
      </div>
      <div className="profile-match-numbers">
        <div className="profile-match-result"><span>Kết quả</span><b className={`result-${result ?? 'unknown'}`}>{result ?? '—'}</b></div>
        <div><span>Đánh giá</span><b>{ratingLabel(state === 'played' ? matchRating(row) : null)}</b></div>
        <div><span>Phút</span><b>{metric(value('minutes'))}</b></div>
        <div><span>Bàn</span><b>{metric(value('goals'))}</b></div>
        <div><span>Kiến tạo</span><b>{metric(value('assists'))}</b></div>
        <div><span>Vàng</span><b>{metric(value('yellowCards'))}</b></div>
        <div><span>Đỏ</span><b>{metric(value('redCards'))}</b></div>
      </div>
    </li>
  )
}

function SeasonTab({ rows, season }) {
  const summary = seasonSummary(rows)
  if (!rows.some((row) => row.stats)) {
    return <div className="profile-empty"><h2>Chưa có thống kê cầu thủ–trận</h2><p>Mùa {SEASONS[season]} chưa có dữ liệu từng trận cho cầu thủ này. Chỉ số mùa và biểu đồ sẽ xuất hiện khi dữ liệu được nhập.</p></div>
  }
  const items = [
    ['Số trận đã thi đấu', summary.appearances],
    ['Phút trung bình / trận', summary.averageMinutes === null ? null : Math.round(summary.averageMinutes)],
    ['Tổng bàn thắng', summary.goals],
    ['Tổng kiến tạo', summary.assists],
    ['Đánh giá trung bình', summary.averageRating === null ? null : summary.averageRating.toFixed(1)],
  ]
  return <div className="profile-season">
    <p className="profile-context">Premier League · {SEASONS[season]} · Chỉ tính các trận có thống kê cầu thủ đã lưu.</p>
    <div className="profile-metrics">{items.map(([label, value]) =>
      <div className="profile-metric" key={label}><span>{label}</span><strong>{metric(value)}</strong></div>)}</div>
    <h2>Điểm đánh giá từng trận</h2>
    {summary.ratedMatches.length ? <div className="profile-chart" role="img" aria-label={`Biểu đồ điểm đánh giá ${summary.ratedMatches.map(({ row, rating }) => `GW${row.match.matchweek}: ${rating.toFixed(1)}`).join(', ')}`}>
      {summary.ratedMatches.map(({ row, rating }) => <div className="profile-chart-item" key={row.match.id}>
        <span className="profile-chart-score">{rating.toFixed(1)}</span>
        <div className="profile-chart-track"><span style={{ height: `${Math.min(100, rating * 10)}%` }} /></div>
        <strong>GW{row.match.matchweek}</strong><small>{dateLabel(row.match.date)}</small>
      </div>)}
    </div> : <p className="profile-chart-empty">Chưa có điểm đánh giá trận từ nguồn dữ liệu đã lưu.</p>}
    <p className="profile-context">Chỉ số thiếu được để trống; điểm đánh giá là giá trị provider đã lưu, không phải điểm Fantasy.</p>
  </div>
}

function PlayerDetailPage({ playerId, season }) {
  const [tab, setTab] = useState('matches')
  const [reloadKey, setReloadKey] = useState(0)
  const [data, setData] = useState({ status: 'loading', player: null, rows: [], error: '' })

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([fetchPlayer(playerId, season, controller.signal), fetchPlayerMatches(playerId, season, controller.signal)])
      .then(([player, rows]) => { if (!controller.signal.aborted) setData({ status: 'success', player, rows, error: '' }) })
      .catch((error) => { if (!controller.signal.aborted) setData({ status: 'error', player: null, rows: [], error: error.message }) })
    return () => controller.abort()
  }, [playerId, season, reloadKey])

  const { status, player, rows, error } = data
  return <section className="profile-page"><div className="container">
    <a className="profile-back" href="#players">← Danh sách cầu thủ</a>
    {status === 'loading' && <div className="state-panel" role="status">Đang tải chi tiết cầu thủ...</div>}
    {status === 'error' && <div className="state-panel state-error" role="alert"><h2>Không thể tải cầu thủ</h2><p>{error}</p><button type="button" onClick={() => { setData({ status: 'loading', player: null, rows: [], error: '' }); setReloadKey((value) => value + 1) }}>Thử lại</button></div>}
    {status === 'success' && <>
      <div className="profile-header">
        <div className="profile-avatar" aria-label="Ảnh đại diện chưa có; hiển thị chữ viết tắt">{getInitials(player.name)}</div>
        <div className="profile-identity"><p className="profile-eyebrow">Premier League · {SEASONS[season]} · Cầu thủ #{player.id}</p>
          <div className="profile-title"><h1>{player.name}</h1><span className="profile-availability">Tình trạng: Chưa cập nhật</span></div>
          <p className="profile-club">{player.club} <span>· {player.position}</span></p>
          {season === 2026 && <p className="profile-source">Lịch và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>. Danh sách cầu thủ được nhập thủ công.</p>}
        </div>
      </div>
      <div className="profile-content">
        <div className="profile-tabs" role="tablist" aria-label="Thông tin cầu thủ">
          <button type="button" role="tab" id="tab-matches" aria-controls="panel-matches" aria-selected={tab === 'matches'} onClick={() => setTab('matches')}>Matches</button>
          <button type="button" role="tab" id="tab-season" aria-controls="panel-season" aria-selected={tab === 'season'} onClick={() => setTab('season')}>Season</button>
        </div>
        {tab === 'matches' ? <div role="tabpanel" id="panel-matches" aria-labelledby="tab-matches" className="profile-panel">
          <div className="profile-panel-head"><div><h2>Trận đấu Premier League</h2><p>{SEASONS[season]} · {rows.length} trận trong lịch sử đã lưu</p></div></div>
          {!rows.some((row) => row.stats) && <p className="profile-empty-inline">Chưa có thống kê cầu thủ–trận cho mùa này. Lịch thi đấu bên dưới chỉ cho biết trận của CLB, không xác nhận cầu thủ đã ra sân.</p>}
          {rows.length ? <ul className="profile-match-list">{rows.map((row) => <MatchRow key={row.match.id} row={row} />)}</ul>
            : <div className="profile-empty"><h3>Chưa có trận đấu</h3><p>Chưa có fixture Premier League thuộc thời gian cầu thủ ở CLB trong mùa này.</p></div>}
        </div> : <div role="tabpanel" id="panel-season" aria-labelledby="tab-season" className="profile-panel"><SeasonTab rows={rows} season={season} /></div>}
      </div>
    </>}
  </div></section>
}

export default PlayerDetailPage
