import { useCallback, useRef } from 'react'
import { fetchStandings } from '../api/standings.js'
import { fetchPlayers } from '../api/players.js'
import { fetchClubs, fetchClubStatistics } from '../api/clubs.js'
import { fetchMatches } from '../api/matches.js'
import { useApiList } from '../hooks/useApiList.js'
import { clubDetailHash } from '../utils/clubRoute.js'
import { playerDetailHash } from '../utils/playerRoute.js'
import { standingBand, ratingClass } from '../utils/clubView.js'
import { recentClubForm, playerLeaders, mergePlayerRatings } from '../utils/standingsView.js'
import { clubVisual } from '../utils/matchView.js'
import { getInitials } from '../utils/initials.js'
import { SEASONS } from '../utils/seasons.js'
import './StandingsPage.css'

const RESULTS = { W: 'Thắng', D: 'Hòa', L: 'Thua' }

function DataState({ state, emptyMessage }) {
  if (state.status === 'loading') return <div className="st-loading" role="status" aria-label="Đang tải dữ liệu">
    {[0, 1, 2, 3].map(key => <div className="st-skeleton" key={key} />)}</div>
  if (state.status === 'error') return <div className="st-empty" role="alert"><strong>Không tải được dữ liệu</strong>
    <p>{state.error}</p><button type="button" onClick={state.reload}>Thử lại</button></div>
  if (!state.data.length) return <div className="st-empty"><strong>Chưa có dữ liệu</strong><p>{emptyMessage}</p></div>
  return null
}

function ClubsTable({ season }) {
  const request = useCallback(signal => fetchStandings(signal, season), [season])
  const state = useApiList(request)
  const requestMatches = useCallback(signal => fetchMatches({ season, status: 'FINISHED' }, signal), [season])
  const matches = useApiList(requestMatches)
  return <>
    <div className="st-panel-heading"><div><h2>Bảng xếp hạng câu lạc bộ</h2><p>Premier League {SEASONS[season]}</p></div>
      <button className="st-refresh" type="button" disabled={state.status === 'loading' || matches.status === 'loading'}
        onClick={() => { state.reload(); matches.reload() }}>Làm mới</button></div>
    <DataState state={state} emptyMessage="Bảng xếp hạng sẽ có khi dữ liệu mùa giải được cập nhật." />
    {state.status === 'success' && state.data.length > 0 && <>
      <div className="st-table-wrap"><table className="st-table"><caption className="sr-only">Bảng xếp hạng Premier League {SEASONS[season]}</caption>
        <thead><tr><th scope="col" className="st-left">#</th><th scope="col" className="st-left">Đội</th><th scope="col">Trận</th>
          <th scope="col" className="st-hide-small" title="Thắng">T</th><th scope="col" className="st-hide-small" title="Hòa">H</th><th scope="col" className="st-hide-small" title="Thua">B</th>
          <th scope="col" className="st-hide-small" title="Bàn thắng : bàn thua">BT:BB</th><th scope="col">Hiệu số</th><th scope="col" className="st-center">5 trận gần nhất</th><th scope="col">Điểm</th></tr></thead>
        <tbody>{state.data.map(row => {
          const visual = clubVisual(row.clubName)
          const form = recentClubForm(matches.data, row.clubId)
          return <tr key={row.clubId} className={standingBand(row.position, season, state.data.length)}>
            <td className="st-rank">{row.position}</td><th scope="row" className="st-left"><a className="st-team" href={clubDetailHash(row.clubId, { season })}>
              <span className="st-crest" style={{ background: visual.color }} aria-hidden="true">{visual.code}</span>
              <span className="st-team-name">{row.clubName}</span><abbr className="st-team-code" title={row.clubName}>{visual.code}</abbr></a></th>
            <td>{row.played}</td><td className="st-hide-small">{row.won}</td><td className="st-hide-small">{row.drawn}</td><td className="st-hide-small">{row.lost}</td>
            <td className="st-hide-small">{row.goalsFor}:{row.goalsAgainst}</td><td>{row.goalDifference > 0 ? '+' : ''}{row.goalDifference}</td>
            <td className="st-center"><span className="st-form">{matches.status === 'success' && form.length ? form.map(({ match, result }) =>
              <span className={`st-result st-result-${result}`} key={match.id} title={`GW${match.matchweek}: ${RESULTS[result]}`} aria-label={`Vòng ${match.matchweek}: ${RESULTS[result]}`}>{result}</span>) :
              <span title={matches.status === 'loading' ? 'Đang tải phong độ' : 'Chưa có dữ liệu phong độ'}>—</span>}</span></td><td className="st-points">{row.points}</td>
          </tr>
        })}</tbody></table></div>
      {matches.status === 'error' && <p className="st-note" role="alert">Không tải được phong độ. <button type="button" onClick={matches.reload}>Thử lại</button></p>}
      <div className="st-legend">{season === 2026 && <><span><i className="st-zone-blue" />Vị trí 1–5</span><span><i className="st-zone-orange" />Vị trí 6–7</span><span><i className="st-zone-red" />3 đội cuối</span></>}
        <span><b className="st-result st-result-W">W</b><b className="st-result st-result-D">D</b><b className="st-result st-result-L">L</b>Thắng, hòa, thua (cũ đến mới)</span></div>
    </>}
  </>
}

function LeaderCard({ title, unit, players, metric, season, state, emptyMessage }) {
  const leaders = playerLeaders(players, metric)
  return <section className="st-leader-card" aria-label={title}><div className="st-leader-heading"><h3>{title}</h3><span>{unit}</span></div>
    <DataState state={{ ...state, data: leaders }} emptyMessage={emptyMessage ?? 'Chưa có thống kê đã xác minh cho mục này.'} />
    {state.status === 'success' && leaders.map((player, index) => {
      const visual = clubVisual(player.club)
      return <a className="st-player-row" key={player.id} href={playerDetailHash(player.id, season, '#standings')}>
        <span className={`st-player-rank st-medal-${index}`}>{index + 1}</span><span className="st-avatar" style={{ background: visual.color }} aria-hidden="true">{getInitials(player.name)}</span>
        <div className="st-player-info"><span className="st-player-name">{player.name}</span><span className="st-player-club"><i style={{ background: visual.color }} />{player.club}</span></div>
        <strong className={metric === 'averageRating' ? `st-rating st-rating-${ratingClass(player[metric])}` : 'st-player-value'}>{metric === 'averageRating' ? player[metric].toFixed(2) : player[metric]}</strong>
      </a>
    })}</section>
}

function PlayersTable({ season }) {
  const request = useCallback(signal => fetchPlayers({}, signal, season), [season])
  const players = useApiList(request)
  const requestRatings = useCallback(async signal => {
    if (season !== 2026) return []
    const clubs = await fetchClubs('', signal, season)
    return Promise.all(clubs.map(club => fetchClubStatistics(club.id, season, signal)))
  }, [season])
  const ratings = useApiList(requestRatings)
  const ratedPlayers = mergePlayerRatings(players.data, ratings.data)
  const ratingState = players.status !== 'success' ? players : ratings
  return <>
    <div className="st-panel-heading"><div><h2>Bảng xếp hạng cầu thủ</h2><p>Premier League {SEASONS[season]}</p></div>
      <button className="st-refresh" type="button" disabled={players.status === 'loading' || ratings.status === 'loading'} onClick={() => { players.reload(); ratings.reload() }}>Làm mới</button></div>
    <div className="st-leader-grid">
      <LeaderCard title="Vua phá lưới" unit="Bàn thắng" players={players.data} metric="goals" season={season} state={players} />
      <LeaderCard title="Kiến tạo" unit="Đường kiến tạo" players={players.data} metric="assists" season={season} state={players} />
      <LeaderCard title="Điểm SofaScore" unit="Trung bình" players={ratedPlayers} metric="averageRating" season={season} state={ratingState}
        emptyMessage={season === 2024 ? 'Chưa có điểm đánh giá cho mùa 2024/25.' : 'Chưa có trận được chấm điểm.'} />
    </div><p className="st-note">Điểm SofaScore trung bình chỉ tính các trận cầu thủ ra sân và có điểm đánh giá.</p>
  </>
}

function StandingsPage({ season, tab, onTabChange }) {
  const tabRefs = useRef([])
  function handleTabKey(event, index) {
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? 1 :
      ['ArrowLeft', 'ArrowRight'].includes(event.key) ? 1 - index : null
    if (next === null) return
    event.preventDefault()
    onTabChange(next === 0 ? 'clubs' : 'players')
    tabRefs.current[next]?.focus()
  }
  return <section className="standings-page" id="directory" aria-labelledby="standings-heading"><div className="st-wrap">
    <header className="st-hero"><svg className="st-hero-art" viewBox="0 0 200 200" fill="none" stroke="currentColor" strokeWidth="3" aria-hidden="true">
      <circle cx="100" cy="100" r="80" /><circle cx="100" cy="100" r="46" /><circle cx="100" cy="100" r="6" fill="currentColor" /><path d="M100 20v160" /></svg>
      <h1 id="standings-heading">Bảng xếp hạng</h1><p>Câu lạc bộ và cầu thủ xuất sắc nhất mùa {SEASONS[season]}</p></header>
    <div className="st-tabs" role="tablist" aria-label="Loại bảng xếp hạng">{[['clubs', 'Câu lạc bộ'], ['players', 'Cầu thủ']].map(([key, label], index) =>
      <button type="button" role="tab" key={key} id={`st-tab-${key}`} aria-controls={`st-panel-${key}`} aria-selected={tab === key} tabIndex={tab === key ? 0 : -1}
        ref={element => { tabRefs.current[index] = element }} onKeyDown={event => handleTabKey(event, index)} onClick={() => onTabChange(key)}>{label}</button>)}</div>
    <section className="st-panel" role="tabpanel" id={`st-panel-${tab}`} aria-labelledby={`st-tab-${tab}`} tabIndex={0}>
      {tab === 'clubs' ? <ClubsTable season={season} /> : <PlayersTable season={season} />}</section>
    <p className="st-attribution">Lịch và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>.</p>
  </div></section>
}

export default StandingsPage
