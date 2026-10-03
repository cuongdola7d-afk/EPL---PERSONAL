import { useEffect, useState } from 'react'
import ClubPage from './components/ClubPage.jsx'
import PlayerPage from './components/PlayerPage.jsx'
import MatchPage from './components/MatchPage.jsx'
import StandingsPage from './components/StandingsPage.jsx'
import FantasyPage from './components/FantasyPage.jsx'
import PlayerDetailPage from './components/PlayerDetailPage.jsx'
import { parsePlayerDetailHash, parsePlayerListHash } from './utils/playerRoute.js'
import { parseMatchRoute } from './utils/matchRoute.js'
import { SEASONS } from './utils/seasons.js'
import './App.css'
import './DarkSite.css'

const PAGES = {
  clubs: {
    hasSeasons: true,
    navLabel: 'Câu lạc bộ', label: 'Câu lạc bộ', title: 'Một giải đấu.', highlight: 'Nhiều câu chuyện.',
    description: 'Bắt đầu khám phá Premier League qua danh sách câu lạc bộ. Tìm tên đội bạn quan tâm từ dữ liệu của prismaXI.',
    component: ClubPage, fullPage: true,
  },
  players: {
    hasSeasons: true,
    navLabel: 'Cầu thủ', label: 'Cầu thủ', title: 'Những gương mặt.', highlight: 'Tạo nên trận đấu.',
    description: 'Khám phá cầu thủ theo câu lạc bộ và vị trí, cùng hồ sơ và chỉ số mùa giải từ prismaXI.',
    component: PlayerPage, fullPage: true,
  },
  matches: {
    hasSeasons: true,
    navLabel: 'Lịch đấu', label: 'Lịch đấu & Kết quả', title: 'Từng vòng đấu.', highlight: 'Từng khoảnh khắc.',
    description: 'Tra cứu lịch đấu và kết quả mùa 2024/25 hoặc 2026/27, lọc theo đội bóng, Gameweek và trạng thái trận.',
    component: MatchPage, fullPage: true,
  },
  standings: {
    hasSeasons: true,
    navLabel: 'Bảng xếp hạng', label: 'Bảng xếp hạng', title: 'Mỗi điểm số.', highlight: 'Một vị trí.',
    description: 'Tra cứu bảng xếp hạng theo mùa giải từ dữ liệu đã lưu tại prismaXI.',
    component: StandingsPage,
  },
  fantasy: {
    navLabel: 'Fantasy', label: 'Fantasy · 2026/27', title: 'Đội hình của bạn.', highlight: 'Theo OVR FC 27.',
    description: 'Chọn 11 cầu thủ từ roster mùa 2026/27.', component: FantasyPage, fullPage: true,
  },
}

function pageFromHash(hash) {
  const name = hash.slice(1)
  if (parsePlayerDetailHash(hash) || parsePlayerListHash(hash)) return 'players'
  if (parseMatchRoute(hash)) return 'matches'
  return PAGES[name] ? name : 'clubs'
}

function App() {
  const [page, setPage] = useState(() => pageFromHash(window.location.hash))
  const [season, setSeason] = useState(() => parsePlayerDetailHash(window.location.hash)?.season ??
    parsePlayerListHash(window.location.hash)?.season ?? parseMatchRoute(window.location.hash)?.season ?? 2026)
  const [playerDetail, setPlayerDetail] = useState(() => parsePlayerDetailHash(window.location.hash))
  const [matchRoute, setMatchRoute] = useState(() => parseMatchRoute(window.location.hash))
  const [homeState, setHomeState] = useState({ query: '', slide: 0 })
  const [homeTheme, setHomeTheme] = useState('dark')

  useEffect(() => {
    function handleHashChange() {
      const detail = parsePlayerDetailHash(window.location.hash)
      const playerList = parsePlayerListHash(window.location.hash)
      const match = parseMatchRoute(window.location.hash)
      setPlayerDetail(detail)
      setMatchRoute(match)
      if (detail?.season || playerList?.season || match?.season) {
        setSeason(detail?.season ?? playerList?.season ?? match?.season)
      }
      setPage(pageFromHash(window.location.hash))
      window.scrollTo(0, 0)
    }

    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  const current = PAGES[page]
  const CurrentPage = current.component

  useEffect(() => {
    document.title = `${playerDetail ? 'Hồ sơ cầu thủ' : page === 'clubs' ? 'Trang chủ' : current.label} | prismaXI`
  }, [current.label, playerDetail, page])

  return (
    <div className={`app-shell${page === 'clubs' ? ' club-home-shell' : ''}`} data-home-theme={homeTheme}>
      <header className="site-header">
        <div className="container header-inner">
          <a className="brand" href="#clubs" aria-label="prismaXI, trang chủ">
            <img className="brand-logo" src="/prismaxi-logo.svg" alt="" width="52" height="47" />
            <span>prisma<span className="brand-accent">XI</span></span>
          </a>

          <nav className="site-nav" aria-label="Điều hướng chính">
            {Object.entries(PAGES).map(([name, item]) => (
              <a key={name} className={`nav-link${page === name ? ' active' : ''}`} href={`#${name}`} aria-current={page === name ? 'page' : undefined}>
                {item.navLabel}
              </a>
            ))}
          </nav>
          {page === 'clubs' && <div className="cx-header-controls">
            <label className="cx-season"><span className="sr-only">Mùa giải</span><select aria-label="Mùa giải" value={season}
              onChange={event => { setSeason(Number(event.target.value)); setHomeState({ query: '', slide: homeState.slide }) }}>
              {[2026, 2024].map(year => <option value={year} key={year}>{SEASONS[year]}</option>)}
            </select><span aria-hidden="true">⌄</span></label>
            <button className="cx-theme-toggle" type="button" aria-label={`Chuyển sang chế độ ${homeTheme === 'dark' ? 'sáng' : 'tối'}`}
              onClick={() => setHomeTheme(homeTheme === 'dark' ? 'light' : 'dark')}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <circle cx="12" cy="12" r="9" /><path d="M12 3a9 9 0 0 0 0 18z" fill="currentColor" />
              </svg>
            </button>
          </div>}
        </div>
      </header>

      <main>
        {!playerDetail && !current.fullPage && <section className="hero" aria-labelledby="hero-title">
          <div className="container hero-inner">
            <div className="hero-copy">
              <p className="eyebrow"><span className="eyebrow-line" /> prismaXI / {current.label}</p>
              <h1 id="hero-title">{current.title}<br /><em>{current.highlight}</em></h1>
              <p className="hero-description">{current.description}</p>
              <button
                className="hero-link"
                type="button"
                onClick={() => document.getElementById('directory')?.scrollIntoView()}
              >
                Xem {current.label.toLocaleLowerCase('vi')} <span aria-hidden="true">↗</span>
              </button>
            </div>
            <div className="hero-art" aria-hidden="true">
              <span className="pitch-ring pitch-ring-one" />
              <span className="pitch-ring pitch-ring-two" />
              <span className="pitch-center"><img src="/prismaxi-logo.svg" alt="" /></span>
              <span className="hero-art-caption">THE BEAUTIFUL GAME</span>
            </div>
          </div>
        </section>}

        {playerDetail ? <PlayerDetailPage key={`${playerDetail.playerId}-${playerDetail.season}`}
          playerId={playerDetail.playerId} season={playerDetail.season} backHash={playerDetail.backHash} /> :
          <CurrentPage key={current.hasSeasons ? `${page}-${season}` : page}
            {...(current.hasSeasons ? { season, onSeasonChange: setSeason } : {})}
            {...(page === 'clubs' ? { homeState, onHomeStateChange: setHomeState } : {})}
            {...(page === 'matches' ? { matchRoute } : {})} />}
      </main>

      <footer className="site-footer">
        <div className="container footer-inner">
          <span className="footer-brand"><img src="/prismaxi-logo.svg" alt="" width="37" height="33" />prismaXI</span>
          {page === 'clubs' && <><p className="cx-footer-attribution">Lịch và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>. Danh sách cầu thủ được nhập thủ công.</p>
            <nav className="cx-footer-links" aria-label="Điều hướng cuối trang">{Object.entries(PAGES).map(([name, item]) =>
              <a key={name} href={`#${name}`}>{item.navLabel}</a>)}</nav></>}
        </div>
      </footer>
    </div>
  )
}

export default App
