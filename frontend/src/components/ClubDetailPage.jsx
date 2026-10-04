import { useCallback } from 'react'
import { fetchClub, fetchClubStatistics } from '../api/clubs.js'
import { fetchPlayers } from '../api/players.js'
import { fetchMatches } from '../api/matches.js'
import { fetchStandings } from '../api/standings.js'
import { useApiList } from '../hooks/useApiList.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { clubDetailHash } from '../utils/clubRoute.js'
import { clubMatches, clubMatchResult, clubRanking, standingBand, FOOT_LABEL, valueLabel, ratingClass } from '../utils/clubView.js'
import { playerDetailHash } from '../utils/playerRoute.js'
import { matchDetailHash } from '../utils/matchRoute.js'
import { SEASONS, hasMatchScore } from '../utils/seasons.js'
import { vietnamToday } from '../utils/homeView.js'
import { clubVisual } from '../utils/matchView.js'
import { getInitials } from '../utils/initials.js'
import { HomeCrest } from './ClubCard.jsx'
import './ClubDetailPage.css'

const dateLabel = value => value ? new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${value}T00:00:00Z`)) : '—'
const groups = [['GOALKEEPER', 'Thủ môn', 'GK'], ['DEFENDER', 'Hậu vệ', 'DEF'],
  ['MIDFIELDER', 'Tiền vệ', 'MID'], ['FORWARD', 'Tiền đạo', 'FWD']]
const resultLabel = { W: 'Thắng', D: 'Hòa', L: 'Thua' }
const pendingLabel = { SCHEDULED: 'Chưa diễn ra', LIVE: 'Đang diễn ra', POSTPONED: 'Hoãn', SUSPENDED: 'Tạm dừng' }

function DataState({ status, error, reload, empty, title }) {
  if (status === 'loading') return <div className="cd-loading" role="status" aria-label={`Đang tải ${title}`}>
    {[0,1,2].map(key => <div className="cx-skeleton" key={key} />)}</div>
  if (status === 'error') return <div className="cd-empty" role="alert"><strong>Không tải được {title}</strong>
    <p>{error}</p><button type="button" onClick={reload}>Thử lại</button></div>
  if (empty) return <div className="cd-empty"><strong>Chưa có {title}</strong><p>Dữ liệu sẽ được cập nhật sau.</p></div>
  return null
}

function MatchTeams({ match }) {
  return <div className="cd-match-teams"><span className="cd-team cd-team-home"><span className="cd-team-name">{match.homeClub}</span><HomeCrest name={match.homeClub} /></span>
    <b className={`cd-score${hasMatchScore(match) ? '' : ' cd-vs'}`}>{hasMatchScore(match) ? `${match.homeGoals} – ${match.awayGoals}` : 'vs'}</b>
    <span className="cd-team"><HomeCrest name={match.awayClub} /><span className="cd-team-name">{match.awayClub}</span></span></div>
}

function FeaturedMatch({ label, match, season, fromHash }) {
  return match ? <a className="cd-match-box" href={matchDetailHash(match.id, {season, week:match.matchweek}, fromHash)}>
    <div className="cd-box-label"><span>{label}</span><span>GW{match.matchweek}</span></div>
    <MatchTeams match={match} /><span className="cd-match-date">{dateLabel(match.date)}</span>
  </a> : <div className="cd-match-box"><div className="cd-box-label">{label}</div><p className="cd-muted">Chưa có dữ liệu</p></div>
}

function Position({ player }) {
  const group = groups.find(([code]) => code === player.position)
  return <span className={`cd-position cd-position-${player.position}`}>{player.primaryPosition ?? group?.[2] ?? 'Chưa xác định'}</span>
}

function PlayerRows({ players, season, fromHash }) {
  const known = new Set(groups.map(([code]) => code))
  const sections = [...groups, ...players.some(player => !known.has(player.position)) ? [['UNKNOWN','Chưa xác định','?']] : []]
  return <><div className="cd-player-row cd-player-columns" aria-hidden="true"><span>Cầu thủ</span><span>Quốc tịch</span><span>Chiều cao</span><span>Ngày sinh</span><span>Chân thuận</span><span>OVR</span></div>
    {sections.map(([code,label]) => {
      const members = players.filter(player => code === 'UNKNOWN' ? !known.has(player.position) : player.position === code)
      if (!members.length) return null
      return <section className="cd-player-group" key={code}><h3>{label}<span>{members.length}</span></h3>
        {members.map(player => <a key={player.id} className="cd-player-row" href={playerDetailHash(player.id,season,fromHash)} aria-label={`Xem hồ sơ ${player.name}`}>
          <div className="cd-player-who"><span className="cd-avatar" aria-hidden="true">{getInitials(player.name)}</span><div><strong>{player.name}</strong><Position player={player} />
            <small className="cd-foot-mobile">Chân thuận: {FOOT_LABEL[player.preferredFoot] ?? 'Chưa có dữ liệu'}</small></div></div>
          <span className="cd-profile-cell" data-label="Quốc tịch">{player.nationality ?? '—'}</span>
          <span className="cd-profile-cell" data-label="Chiều cao">{player.heightCm == null ? '—' : `${player.heightCm} cm`}</span>
          <span className="cd-profile-cell" data-label="Ngày sinh">{dateLabel(player.birthDate)}</span>
          <span className="cd-profile-cell" data-label="Chân thuận">{FOOT_LABEL[player.preferredFoot] ?? '—'}</span>
          <span className={`cd-ovr cd-ovr-${player.fc27Overall == null ? 'na' : player.fc27Overall >= 85 ? 'gold' : player.fc27Overall >= 78 ? 'green' : player.fc27Overall >= 70 ? 'blue' : 'silver'}`}>{valueLabel(player.fc27Overall)}</span>
        </a>)}
      </section>
    })}</>
}

function Ranking({ players, stats, state, update, season, fromHash }) {
  const ranked = clubRanking(players,stats,state.metric)
  const highest = ranked[0]?.value ?? 0
  return <><div className="cd-chips" role="group" aria-label="Xếp hạng cầu thủ">
    {[['rating','Điểm đánh giá'],['goals','Bàn thắng'],['assists','Kiến tạo']].map(([key,label]) => <button type="button" key={key}
      aria-pressed={state.metric === key} onClick={() => update({metric:key,count:5})}>{label}</button>)}</div>
    {!ranked.length && <DataState status="success" empty title="chỉ số cầu thủ cho mục này" />}
    {ranked.slice(0,state.count).map(({player,stats:row,value},index) => <a className="cd-ranking-row" key={player.id} href={playerDetailHash(player.id,season,fromHash)}>
      <span className="cd-ranking-number">{index+1}</span><span className="cd-avatar" aria-hidden="true">{getInitials(player.name)}</span>
      <div><strong>{player.name}</strong><Position player={player} />
        {state.metric === 'rating' ? <small>{row.ratedAppearances} lượt được chấm</small> : <div className="cd-ranking-bar"><i style={{width:`${highest ? value/highest*100 : 0}%`}} /></div>}</div>
      <b className={state.metric === 'rating' ? `cd-rating cd-rating-${ratingClass(value)}` : 'cd-ranking-value'}>{state.metric === 'rating' ? value.toFixed(1) : value}</b>
    </a>)}
    {ranked.length > state.count && <button className="cd-more" type="button" onClick={() => update({count:state.count+5})}>Xem thêm {Math.min(5,ranked.length-state.count)} cầu thủ</button>}
    <p className="cd-note">Chỉ số tính trong những trận đã nhập khi cầu thủ thi đấu cho CLB này. Chỉ hiển thị người thuộc roster hiện hành; trường thiếu không được tính thành 0.</p>
  </>
}

function Statistics({ standing, resource, done, clubId }) {
  const stats = resource.data
  const chart = [...done].reverse().filter(hasMatchScore)
  const maximum = Math.max(1,...chart.flatMap(match => [match.homeGoals,match.awayGoals]))
  const metrics = [['Điểm đánh giá TB',stats?.averageRating == null ? '—' : stats.averageRating.toFixed(2)],
    ['Số trận',valueLabel(standing?.played)], ['Bàn thắng',valueLabel(standing?.goalsFor)], ['Bàn thua',valueLabel(standing?.goalsAgainst)]]
  return <><div className="cd-metrics">{metrics.map(([label,value],index) => <div className="cd-metric" key={label}><span>{label}</span>
    <strong className={index === 0 ? `cd-rating cd-rating-${ratingClass(stats?.averageRating)}` : ''}>{value}</strong></div>)}</div>
    <DataState {...resource} title="điểm đánh giá" />
    <div className="cd-chart-box"><h3>Bàn thắng và bàn thua từng trận</h3><div className="cd-chart-legend"><span><i />Ghi bàn</span><span><i />Thủng lưới</span></div>
      {chart.length ? <div className="cd-chart" role="list" aria-label="Bàn thắng và bàn thua theo trận">{chart.map(match => {
        const home=match.homeClubId === clubId, goals=home?match.homeGoals:match.awayGoals, conceded=home?match.awayGoals:match.homeGoals
        const opponent=home?match.awayClub:match.homeClub
        return <div className="cd-chart-group" role="listitem" key={match.id} title={`GW${match.matchweek} · ${opponent}: ${goals} bàn thắng, ${conceded} bàn thua`}>
          <div className="cd-chart-bar" style={{height:`${Math.max(2,goals/maximum*85)}%`}}><span>{goals}</span></div>
          <div className="cd-chart-bar cd-chart-against" style={{height:`${Math.max(2,conceded/maximum*85)}%`}}><span>{conceded}</span></div>
          <small>GW{match.matchweek}<br />{clubVisual(opponent).code}</small>
        </div>
      })}</div> : <DataState status="success" empty title="kết quả trận để vẽ biểu đồ" />}
    </div><p className="cd-note">Bàn thắng/thua lấy từ BXH. {stats ? <>Rating trung bình tính trên {stats.ratedAppearances} lượt được chấm trong {stats.recordedMatches} trận có thống kê; bỏ qua DNP và rating trống.</> : 'Chưa có dữ liệu tổng hợp rating.'}</p>
  </>
}

export default function ClubDetailPage({ route, season }) {
  const requestClub=useCallback(signal=>fetchClub(route.clubId,season,signal),[route.clubId,season])
  const club=useApiResource(requestClub)
  const requestStandings=useCallback(signal=>fetchStandings(signal,season),[season])
  const standings=useApiList(requestStandings)
  const requestMatches=useCallback(signal=>club.data ? fetchMatches({season,club:club.data.name},signal) : Promise.resolve([]),[season,club.data])
  const matches=useApiList(requestMatches)
  const requestPlayers=useCallback(signal=>club.data ? fetchPlayers({club:club.data.name},signal,season) : Promise.resolve([]),[season,club.data])
  const players=useApiList(requestPlayers)
  const requestStats=useCallback(signal=>season === 2026 ? fetchClubStatistics(route.clubId,season,signal) : Promise.resolve(null),[route.clubId,season])
  const stats=useApiResource(requestStats)
  const update=changes=> {
    const hash=clubDetailHash(route.clubId,{...route,...changes,season})
    window.history.replaceState(window.history.state,'',hash)
    window.dispatchEvent(new HashChangeEvent('hashchange'))
  }
  const fromHash=clubDetailHash(route.clubId,{...route,season})
  const standing=standings.data.find(row=>row.clubId === route.clubId)
  const {done,soon,pending}=clubMatches(matches.data,route.clubId,vietnamToday())
  const matchList=route.matches === 'done' ? done : pending
  const tabs=[['standings','Bảng xếp hạng'],['matches','Trận đấu'],['players','Cầu thủ'],['stats','Thống kê']]
  const visual=clubVisual(club.data?.name ?? '')

  return <section className="cd-page"><div className="cd-wrap">
    <a className="cd-back" href="#clubs">‹ Câu lạc bộ</a>
    <DataState {...club} title="câu lạc bộ" />
    {club.data && <>
      <header className="cd-card cd-header"><div className="cd-identity"><span className="cd-big-crest" style={{background:visual.color}} aria-hidden="true">{visual.code}</span>
        <div><h1>{club.data.name}</h1><p>Premier League · {SEASONS[season]}</p>
          {season === 2026 && <div className="cd-club-information">
            <p><span>HLV</span><strong>{club.data.managerName ?? 'Chưa cập nhật'}{club.data.managerName && club.data.managerStatus === 'INTERIM' ? ' (tạm quyền)' : ''}</strong></p>
            <p><span>Sân nhà</span><strong>{club.data.stadiumName ?? 'Chưa cập nhật'}</strong></p>
          </div>}
          <div className="cd-pills">
          {standing && <><span className="cd-pill cd-pill-accent">Hạng {standing.position}</span><span className="cd-pill">{standing.points} điểm</span></>}
          <div className="cd-form" aria-label="5 trận gần nhất">{done.slice(0,5).reverse().map(match=> {
            const result=clubMatchResult(match,route.clubId)
            return <span className={`cd-form-${result ?? 'unknown'}`} key={match.id} title={`GW${match.matchweek}: ${resultLabel[result] ?? 'Chưa có tỉ số'}`}>{result ?? '—'}</span>
          })}</div></div></div></div>
        {matches.status === 'success' ? <><FeaturedMatch label="Trận trước" match={done[0]} season={season} fromHash={fromHash} />
          <FeaturedMatch label="Trận kế tiếp" match={soon[0]} season={season} fromHash={fromHash} /></> : <div className="cd-header-state"><DataState {...matches} title="lịch đấu" /></div>}
      </header>
      <nav className="cd-tabs" role="tablist" aria-label="Thông tin câu lạc bộ">{tabs.map(([key,label])=><button type="button" role="tab" key={key}
        id={`cd-tab-${key}`} aria-selected={route.tab === key} aria-controls={`cd-panel-${key}`} onClick={()=>update({tab:key})}>{label}</button>)}</nav>
      <div className="cd-card cd-panel" role="tabpanel" id={`cd-panel-${route.tab}`} aria-labelledby={`cd-tab-${route.tab}`}>
        <div className="cd-panel-heading"><div><h2>{tabs.find(([key])=>key===route.tab)?.[1]}</h2><p>Premier League · {SEASONS[season]}{route.tab === 'players' && players.status === 'success' ? ` · ${players.data.length} cầu thủ` : ''}</p></div>
          {route.tab === 'players' && <div className="cd-segment" role="group" aria-label="Chế độ danh sách cầu thủ">{[['squad','Đội hình'],['top','Top cầu thủ']].map(([key,label])=><button key={key} type="button" aria-pressed={route.players===key} onClick={()=>update({players:key})}>{label}</button>)}</div>}
        </div>
        {route.tab === 'standings' && <><DataState {...standings} empty={!standings.data.length} title="bảng xếp hạng" />
          {standings.status === 'success' && standings.data.length>0 && <><div className="cd-table-wrap"><table className="cd-table"><thead><tr><th scope="col">#</th><th scope="col">Đội</th>
            {['Trận','T','H','B','BT:BB','Hiệu số','Điểm'].map(label=><th scope="col" key={label}>{label}</th>)}</tr></thead>
            <tbody>{[...standings.data].sort((a,b)=>a.position-b.position).map(row=><tr key={row.clubId} className={`${standingBand(row.position,season)} ${row.clubId===route.clubId?'cd-my-club':''}`} aria-current={row.clubId===route.clubId?'true':undefined}>
              <td>{row.position}</td><th scope="row"><a href={clubDetailHash(row.clubId,{season})}><HomeCrest name={row.clubName} /><span>{row.clubName}</span></a></th>
              <td>{row.played}</td><td>{row.won}</td><td>{row.drawn}</td><td>{row.lost}</td><td>{row.goalsFor}:{row.goalsAgainst}</td>
              <td>{row.goalDifference>0?'+':''}{row.goalDifference}</td><td>{row.points}</td></tr>)}</tbody></table></div>
            {season===2026 && <div className="cd-standing-legend"><span><i className="rank-champions" />1–5 · C1</span><span><i className="rank-europa" />6–7 · C2</span><span><i className="rank-relegation" />18–20 · Xuống hạng</span></div>}</>}
        </>}
        {route.tab === 'matches' && <><div className="cd-chips" role="group" aria-label="Lọc trận đấu">{[['done','Đã kết thúc',done.length],['soon','Sắp diễn ra',pending.length]].map(([key,label,count])=><button type="button" key={key} aria-pressed={route.matches===key} onClick={()=>update({matches:key})}>{label}<span>{count}</span></button>)}</div>
          <DataState {...matches} empty={!matchList.length} title="trận đấu cho mục này" />
          {matches.status==='success' && matchList.map(match=><a className="cd-match-card" key={match.id} href={matchDetailHash(match.id,{season,week:match.matchweek},fromHash)}>
            <div className="cd-match-meta"><span><b>GW{match.matchweek}</b> {dateLabel(match.date)}</span><span className={`cd-result cd-result-${clubMatchResult(match,route.clubId) ?? 'unknown'}`}>{resultLabel[clubMatchResult(match,route.clubId)] ?? pendingLabel[match.status] ?? 'Chưa rõ'}</span></div><MatchTeams match={match} /></a>)}
        </>}
        {route.tab === 'players' && <><DataState {...players} empty={!players.data.length} title="cầu thủ" />
          {players.status==='success' && (route.players==='squad' ? <PlayerRows players={players.data} season={season} fromHash={fromHash} /> : <>
            <DataState {...stats} title="thống kê cầu thủ" />{stats.status==='success' && <Ranking players={players.data} stats={stats.data} state={route} update={update} season={season} fromHash={fromHash} />}</>)}
        </>}
        {route.tab === 'stats' && <><DataState {...standings} title="chỉ số BXH" /><DataState {...matches} title="kết quả trận đấu" />
          <Statistics standing={standing} resource={stats} done={done} clubId={route.clubId} /></>}
      </div>
    </>}
  </div></section>
}
