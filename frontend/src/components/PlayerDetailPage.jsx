import { useEffect, useState } from 'react'
import { fetchPlayer, fetchPlayerMatches } from '../api/players.js'
import { ESTIMATED_OVR_PLAYER_IDS, GROUP_LABEL } from '../fantasy/lineup.js'
import { getInitials } from '../utils/initials.js'
import { effectiveStat, matchRating, matchResult, matchState, seasonSummary } from '../utils/playerDetail.js'
import { SEASONS, hasMatchScore } from '../utils/seasons.js'
import './PlayerDetailPage.css'

const dateLabel = (date) => new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${date}T00:00:00Z`))
const numberLabel = (value) => value == null ? '—' : String(value)
const ratingTier = (rating) => rating == null ? 'na' : rating >= 9 ? 'blue' : rating >= 7 ? 'green' : rating >= 5 ? 'orange' : 'red'
const isHome = (row) => row.clubId === row.match.homeClubId
const resultLabel = { W: 'Thắng', D: 'Hòa', L: 'Thua' }
const footLabel = { LEFT: 'Chân trái', RIGHT: 'Chân phải', BOTH: 'Hai chân' }
const pendingLabel = { SCHEDULED: 'Chưa diễn ra', POSTPONED: 'Hoãn', SUSPENDED: 'Tạm dừng', LIVE: 'Đang diễn ra' }

function Empty({ title, children }) {
  return <div className="pd-empty"><strong>{title}</strong><p>{children}</p></div>
}

function MatchCard({ row }) {
  const state = matchState(row)
  const result = matchResult(row)
  const rating = state === 'played' ? matchRating(row) : null
  const score = hasMatchScore(row.match) ? `${row.match.homeGoals} – ${row.match.awayGoals}` : '—'
  const stats = [
    ['Phút', 'minutes', '′'], ['Bàn', 'goals', '⚽'], ['Kiến tạo', 'assists', '↗'],
    ['Thẻ vàng', 'yellowCards', '▪'], ['Thẻ đỏ', 'redCards', '▪'],
  ]
  return <li className="pd-match-card">
    <div className="pd-match-main">
      <div className="pd-match-meta"><span className="pd-gw">GW{row.match.matchweek}</span><span>{dateLabel(row.match.date)}</span>
        <span className={`pd-result pd-result-${result ?? 'unknown'}`}>{result ? resultLabel[result] : 'Chưa rõ'}</span></div>
      <div className="pd-teams"><span className={isHome(row) ? 'pd-my-club' : ''}>{row.match.homeClub}</span><b className="pd-score">{score}</b><span className={!isHome(row) ? 'pd-my-club' : ''}>{row.match.awayClub}</span></div>
      <span className="pd-match-note">{state === 'missing' ? 'Chưa có thống kê cầu thủ cho trận này' : state === 'did-not-play' ? 'Không ra sân' : rating === null ? 'Đã ra sân · chưa được chấm điểm' : 'Đã ra sân'}</span>
    </div>
    {state === 'played' ? <div className="pd-match-right">
      <span className={`pd-rating pd-rating-${ratingTier(rating)}`} aria-label={`Đánh giá: ${rating == null ? 'chưa có' : rating.toFixed(1)}`}>{rating == null ? '—' : rating.toFixed(1)}</span>
      <div className="pd-stat-pills">{stats.map(([label, field, symbol]) => <span key={field} className={`pd-stat-pill pd-stat-pill-${field}`} title={label} aria-label={`${label}: ${numberLabel(effectiveStat(row, field))}`}><span aria-hidden="true">{symbol}</span> {numberLabel(effectiveStat(row, field))}<small>{label}</small></span>)}</div>
    </div> : <span className="pd-dnp">{state === 'did-not-play' ? 'DNP' : 'Chưa có dữ liệu'}</span>}
  </li>
}

function MatchesTab({ rows, season }) {
  if (!rows.length) return <Empty title="Chưa có trận đã hoàn thành">Chưa có trận Premier League đã hoàn thành thuộc thời gian cầu thủ ở CLB trong mùa {SEASONS[season]}.</Empty>
  return <>
    <div className="pd-panel-head"><div><h2>Trận đã đấu</h2><p>Premier League · {SEASONS[season]} · {rows.length} trận của CLB</p></div></div>
    <div className="pd-legend"><span>Đánh giá</span><span><i className="pd-legend-dot pd-rating-blue" /> 9+</span><span><i className="pd-legend-dot pd-rating-green" /> 7–8.9</span><span><i className="pd-legend-dot pd-rating-orange" /> 5–6.9</span><span><i className="pd-legend-dot pd-rating-red" /> dưới 5</span><span>— chưa được chấm</span></div>
    <ul className="pd-match-list">{rows.map((row) => <MatchCard key={row.match.id} row={row} />)}</ul>
    <p className="pd-note">Một trận của CLB không xác nhận cầu thủ đã ra sân. DNP là không ra sân; cầu thủ đã chơi nhưng chưa được chấm vẫn có rating trống.</p>
  </>
}

function FixtureCard({ row, featured = false }) {
  const home = isHome(row)
  const opponent = home ? row.match.awayClub : row.match.homeClub
  return <div className={featured ? 'pd-next-featured' : 'pd-next-row'}>
    {featured && <span className="pd-next-tag">TRẬN TIẾP THEO</span>}
    <div className="pd-next-content"><span className="pd-club-initials" aria-hidden="true">{getInitials(opponent)}</span><div><strong>{opponent}</strong><span>{home ? 'Sân nhà' : 'Sân khách'} · GW{row.match.matchweek}</span></div><div className="pd-next-date"><strong>{dateLabel(row.match.date)}</strong><small>{pendingLabel[row.match.status] ?? row.match.status}</small></div></div>
  </div>
}

function NextTab({ rows, season }) {
  if (!rows.length) return <Empty title="Chưa có lịch thi đấu sắp tới">Hiện chưa có fixture sắp tới thuộc thời gian cầu thủ ở CLB trong mùa {SEASONS[season]}.</Empty>
  return <><div className="pd-panel-head"><div><h2>Trận sắp tới</h2><p>Premier League · {SEASONS[season]} · lịch thi đấu có thể thay đổi</p></div></div>
    <FixtureCard row={rows[0]} featured />
    {rows.slice(1).map((row) => <FixtureCard key={row.match.id} row={row} />)}
    <p className="pd-note">Đây là lịch của CLB trong thời gian membership đã lưu, không phải xác nhận cầu thủ sẽ ra sân.</p>
  </>
}

function SeasonTab({ rows, season }) {
  const summary = seasonSummary(rows)
  if (!rows.some((row) => row.stats)) return <Empty title="Chưa có thống kê mùa">Chưa có thống kê cầu thủ theo trận cho mùa {SEASONS[season]}.</Empty>
  const metrics = [
    ['Số trận ra sân', summary.appearances], ['Tổng số phút', summary.totalMinutes],
    ['Bàn thắng', summary.goals], ['Kiến tạo', summary.assists],
    ['Thẻ vàng', summary.yellowCards], ['Thẻ đỏ', summary.redCards],
  ]
  const rated = [...summary.ratedMatches].sort((a, b) => a.row.match.date.localeCompare(b.row.match.date))
  return <><div className="pd-panel-head"><div><h2>Tổng kết mùa</h2><p>Premier League · {SEASONS[season]}</p></div></div>
    <div className="pd-season-top"><span className={`pd-rating pd-season-rating pd-rating-${ratingTier(summary.averageRating)}`}>{summary.averageRating == null ? '—' : summary.averageRating.toFixed(1)}</span><div><strong>Đánh giá trung bình</strong><p>{rated.length} trận có điểm đánh giá</p>
      {rated.length > 0 && <div className="pd-spark" role="img" aria-label={`Đánh giá theo trận: ${rated.map(({ row, rating }) => `GW${row.match.matchweek} ${rating.toFixed(1)}`).join(', ')}`}>{rated.map(({ row, rating }) => <span key={row.match.id} className={`pd-rating-${ratingTier(rating)}`} style={{ height: `${Math.max(8, rating * 10)}%` }} title={`GW${row.match.matchweek}: ${rating.toFixed(1)}`} />)}</div>}</div></div>
    <div className="pd-metric-grid">{metrics.map(([label, value]) => <div className="pd-metric" key={label}><span>{label}</span><strong>{numberLabel(value)}</strong></div>)}</div>
    <p className="pd-note">Tổng chỉ số để trống nếu một trận đã ra sân còn thiếu chỉ số đó. Đánh giá trận là rating đã lưu, không phải điểm Fantasy.</p>
  </>
}

function InfoTab({ player, season }) {
  const estimated = ESTIMATED_OVR_PLAYER_IDS.has(player.id)
  const details = [
    [season === 2026 ? 'OVR FC 27' : 'OVR', player.fc27Overall, estimated ? 'Ước tính PremierHub · chưa đủ điều kiện Fantasy' : player.fc27Overall == null ? 'Chưa có OVR' : 'Chỉ số đang lưu'],
    ['Quốc tịch', player.nationality], ['Ngày sinh', player.birthDate ? dateLabel(player.birthDate) : null],
    ['Chân thuận', footLabel[player.preferredFoot] ?? null], ['Chiều cao', player.heightCm == null ? null : `${player.heightCm} cm`],
    ['Số áo', player.shirtNumber == null ? null : `#${player.shirtNumber}`],
  ]
  return <><div className="pd-panel-head"><div><h2>Thông tin cầu thủ</h2><p>Hồ sơ theo mùa {SEASONS[season]}</p></div></div>
    <div className="pd-info-grid">{details.map(([label, value, note]) => <div className="pd-info-item" key={label}><span>{label}</span><strong className={value == null ? 'pd-unknown' : ''}>{value == null ? 'Chưa có dữ liệu' : value}</strong>{note && <small>{note}</small>}</div>)}</div>
  </>
}

function PlayerDetailPage({ playerId, season }) {
  const [tab, setTab] = useState('matches')
  const [theme, setTheme] = useState('light')
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
  const finished = rows.filter((row) => row.match.status === 'FINISHED').sort((a, b) => b.match.date.localeCompare(a.match.date))
  const next = rows.filter((row) => ['SCHEDULED', 'POSTPONED', 'SUSPENDED', 'LIVE'].includes(row.match.status)).sort((a, b) => a.match.date.localeCompare(b.match.date))
  const tabs = [['matches', 'Trận đã đấu', finished.length], ['next', 'Trận sắp tới', next.length], ['season', 'Mùa giải', null], ['info', 'Thông tin', null]]
  const estimated = player && ESTIMATED_OVR_PLAYER_IDS.has(player.id)

  return <section className="player-detail-page" data-theme={theme}><div className="pd-wrap">
    <div className="pd-topbar"><a className="pd-back" href="#players">← Danh sách cầu thủ</a><button className="pd-theme" type="button" onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')} aria-label={theme === 'light' ? 'Đổi sang giao diện tối' : 'Đổi sang giao diện sáng'}>◐</button></div>
    {status === 'loading' && <div className="pd-card pd-loading" role="status">Đang tải chi tiết cầu thủ...</div>}
    {status === 'error' && <div className="pd-card pd-empty" role="alert"><strong>Không thể tải cầu thủ</strong><p>{error}</p><button type="button" onClick={() => { setData({ status: 'loading', player: null, rows: [], error: '' }); setReloadKey((value) => value + 1) }}>Thử lại</button></div>}
    {status === 'success' && <>
      <header className="pd-card pd-hero"><div className="pd-avatar" aria-label="Chữ viết tắt tên cầu thủ">{getInitials(player.name)}</div><div className="pd-identity"><p>PREMIER LEAGUE · {SEASONS[season]} · #{player.id}</p><h1>{player.name}</h1><div className="pd-hero-chips"><span className="pd-chip">{player.club}</span><span className={`pd-chip pd-position pd-position-${player.position}`}>{GROUP_LABEL[player.position] ?? player.position}</span></div></div><div className="pd-hero-ovr"><span>{numberLabel(player.fc27Overall)}</span><small>{estimated ? 'OVR ước tính' : season === 2026 ? 'OVR FC 27' : 'OVR'}</small></div></header>
      <nav className="pd-tabs" role="tablist" aria-label="Thông tin cầu thủ">{tabs.map(([key, label, count]) => <button key={key} type="button" role="tab" id={`pd-tab-${key}`} aria-controls={`pd-panel-${key}`} aria-selected={tab === key} onClick={() => setTab(key)}>{label}{count !== null && <span className="pd-tab-count">{count}</span>}</button>)}</nav>
      <div className="pd-card pd-panel" role="tabpanel" id={`pd-panel-${tab}`} aria-labelledby={`pd-tab-${tab}`}>
        {tab === 'matches' && <MatchesTab rows={finished} season={season} />}
        {tab === 'next' && <NextTab rows={next} season={season} />}
        {tab === 'season' && <SeasonTab rows={finished} season={season} />}
        {tab === 'info' && <InfoTab player={player} season={season} />}
      </div>
      <p className="pd-footer">Lịch thi đấu và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>. Hồ sơ và thống kê hiển thị theo dữ liệu PremierHub đã lưu.</p>
    </>}
  </div></section>
}

export default PlayerDetailPage
