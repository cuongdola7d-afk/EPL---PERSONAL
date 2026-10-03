import { useCallback } from 'react'
import { fetchClubs } from '../api/clubs.js'
import { fetchMatches } from '../api/matches.js'
import { fetchStandings } from '../api/standings.js'
import { useApiList } from '../hooks/useApiList.js'
import { matchesPlayerSearch } from '../utils/playerSort.js'
import { matchDetailHash, matchListHash } from '../utils/matchRoute.js'
import { homeFixturePreview, vietnamToday, fixtureDateLabel } from '../utils/homeView.js'
import { SEASONS, hasMatchScore } from '../utils/seasons.js'
import { MAX_OVR } from '../fantasy/lineup.js'
import ClubCard, { HomeCrest } from './ClubCard.jsx'
import HomeHero from './HomeHero.jsx'
import { ArrowIcon, SearchIcon, FantasyArtwork } from './HomeArtwork.jsx'
import './ClubPage.css'

function HomeDataState({ status, empty, title, error, reload }) {
  if (status === 'loading') return <div className="cx-preview-loading" role="status" aria-label={`Đang tải ${title}`}>
    {[0, 1, 2].map(index => <div className="cx-skeleton" key={index} />)}</div>
  if (status === 'error') return <div className="cx-empty" role="alert"><strong>Không tải được {title}</strong>
    <p>{error}</p><button type="button" onClick={reload}>Thử lại</button></div>
  if (empty) return <div className="cx-empty"><strong>Chưa có {title}</strong><p>Dữ liệu mùa này sẽ được cập nhật sau.</p></div>
  return null
}

export default function ClubPage({ season, homeState, onHomeStateChange }) {
  const requestClubs = useCallback(signal => fetchClubs('', signal, season), [season])
  const requestMatches = useCallback(signal => fetchMatches({ season }, signal), [season])
  const requestStandings = useCallback(signal => fetchStandings(signal, season), [season])
  const clubs = useApiList(requestClubs)
  const matches = useApiList(requestMatches)
  const standings = useApiList(requestStandings)
  const query = homeState.query
  const visible = [...clubs.data].filter(club => matchesPlayerSearch(club.name, query))
    .sort((a, b) => a.name.localeCompare(b.name, 'en', { sensitivity: 'base' }))
  const preview = homeFixturePreview(matches.data, vietnamToday())
  const table = [...standings.data].sort((a, b) => a.position - b.position).slice(0, 6)
  const fixtureTitle = preview.week == null ? 'Lịch đấu & kết quả' :
    `Vòng ${preview.week}${preview.mode === 'upcoming' ? ' sắp diễn ra' : preview.mode === 'recent' ? ' · Kết quả' : ' · Lịch đấu'}`
  const scheduleHash = matchListHash({ season, week: preview.week })

  return <div className="cx-home">
    <HomeHero active={homeState.slide} onChange={slide => onHomeStateChange({ ...homeState, slide })}
      fixtures={preview.matches} week={preview.week} />
    <div className="cx-wrap">
      <section className="cx-section" id="clubs" aria-labelledby="clubs-heading">
        <div className="cx-section-heading"><div><h2 id="clubs-heading">Câu lạc bộ</h2>
          <p aria-live="polite">Premier League {SEASONS[season]}{clubs.status === 'success' ?
            ` · ${visible.length}${query.trim() ? ` / ${clubs.data.length}` : ''} đội` : ''}</p></div>
          <div className="cx-search" role="search"><SearchIcon /><input type="search" aria-label="Tìm câu lạc bộ"
            placeholder="Tìm câu lạc bộ" autoComplete="off" value={query}
            onChange={event => onHomeStateChange({ ...homeState, query: event.target.value })} />
            {query && <button type="button" aria-label="Xóa tìm kiếm" onClick={() => onHomeStateChange({ ...homeState, query: '' })}>×</button>}
          </div>
        </div>
        <div className="cx-club-grid" aria-busy={clubs.status === 'loading'}>
          {clubs.status === 'loading' && <div className="cx-club-loading" role="status" aria-label="Đang tải câu lạc bộ">
            {Array.from({ length: 12 }, (_, index) => <div className="cx-skeleton" key={index} />)}</div>}
          {clubs.status === 'error' && <div className="cx-empty" role="alert"><strong>Không tải được danh sách câu lạc bộ</strong>
            <p>{clubs.error}</p><button type="button" onClick={clubs.reload}>Thử lại</button></div>}
          {clubs.status === 'success' && (visible.length ? visible.map(club =>
            <ClubCard key={club.id} club={club} season={season} query={query} />) :
            <div className="cx-empty"><strong>{query.trim() ? `Không tìm thấy câu lạc bộ “${query.trim()}”` : 'Chưa có câu lạc bộ nào'}</strong>
              <p>{query.trim() ? 'Thử gõ ít chữ hơn.' : 'Dữ liệu mùa này sẽ được cập nhật sau.'}</p>
              {query && <button type="button" onClick={() => onHomeStateChange({ ...homeState, query: '' })}>Xóa tìm kiếm</button>}
            </div>)}
        </div>
      </section>

      <section className="cx-section cx-overview" aria-label="Lịch đấu và bảng xếp hạng rút gọn">
        <div className="cx-panel"><header className="cx-panel-heading"><h2>{fixtureTitle}</h2>
          <a className="cx-more" href={scheduleHash}>Xem lịch đấu<ArrowIcon /></a></header>
          <HomeDataState {...matches} empty={!preview.matches.length} title="lịch đấu" />
          {matches.status === 'success' && preview.matches.map((match, index) => <div key={match.id}>
            {(index === 0 || preview.matches[index - 1].date !== match.date) && <h3 className="cx-fixture-day">{fixtureDateLabel(match.date)}</h3>}
            <a className="cx-fixture" href={matchDetailHash(match.id, { season, week: match.matchweek })}
              aria-label={`Xem trận ${match.homeClub} – ${match.awayClub}`}>
              <span className="cx-team cx-team-home"><span className="cx-team-name">{match.homeClub}</span><HomeCrest name={match.homeClub} /></span>
              <span className="cx-fixture-score">{hasMatchScore(match) ? `${match.homeGoals} – ${match.awayGoals}` : 'vs'}</span>
              <span className="cx-team"><HomeCrest name={match.awayClub} /><span className="cx-team-name">{match.awayClub}</span></span>
            </a>
          </div>)}
        </div>
        <div className="cx-panel"><header className="cx-panel-heading"><h2>Bảng xếp hạng</h2>
          <a className="cx-more" href="#standings">Xem đầy đủ<ArrowIcon /></a></header>
          <HomeDataState {...standings} empty={!table.length} title="bảng xếp hạng" />
          {standings.status === 'success' && table.length > 0 && <div className="cx-table-wrap"><table className="cx-table">
            <caption className="sr-only">Sáu vị trí đầu Premier League {SEASONS[season]}</caption>
            <thead><tr><th scope="col">#</th><th scope="col">Đội</th><th scope="col">Trận</th>
              <th scope="col" title="Hiệu số bàn thắng">Hiệu số</th><th scope="col">Điểm</th></tr></thead>
            <tbody>{table.map(row => <tr key={row.clubId}><td>{row.position}</td>
              <th scope="row"><HomeCrest name={row.clubName} /><span>{row.clubName}</span></th>
              <td>{row.played}</td><td>{row.goalDifference > 0 ? '+' : ''}{row.goalDifference}</td><td>{row.points}</td></tr>)}</tbody>
          </table></div>}
        </div>
      </section>
      <section className="cx-section" aria-labelledby="home-fantasy-heading"><div className="cx-fantasy-band">
        <div><div className="cx-chips"><span>11 cầu thủ</span><span>Tối đa {MAX_OVR} OVR</span><span>Tối đa 3 người mỗi CLB</span></div>
          <h2 id="home-fantasy-heading">Xếp đội hình 11 người của riêng bạn</h2>
          <p>Chọn từng vị trí trên sân, tìm cầu thủ theo tên, CLB hay vị trí, và xem tổng điểm của đội hình mùa 2026/27.</p>
          <a className="cx-button" href="#fantasy">Bắt đầu xếp đội hình<ArrowIcon /></a>
        </div><div className="cx-band-art cx-bob"><FantasyArtwork /></div>
      </div></section>
    </div>
  </div>
}
