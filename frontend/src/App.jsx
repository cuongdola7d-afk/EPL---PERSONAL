import { useEffect, useState } from 'react'
import ClubPage from './components/ClubPage.jsx'
import PlayerPage from './components/PlayerPage.jsx'
import MatchPage from './components/MatchPage.jsx'
import StandingsPage from './components/StandingsPage.jsx'
import FantasyPage from './components/FantasyPage.jsx'
import PlayerDetailPage from './components/PlayerDetailPage.jsx'
import ClubDetailPage from './components/ClubDetailPage.jsx'
import SiteHeader from './components/SiteHeader.jsx'
import MinigamePage from './components/MinigamePage.jsx'
import { parseMinigameRoute } from './minigame/playerGuess.js'
import { parseClubRoute, clubDetailHash } from './utils/clubRoute.js'
import { parsePlayerDetailHash, parsePlayerListHash, playerDetailHash, playerListHash } from './utils/playerRoute.js'
import { parseMatchRoute, matchListHash } from './utils/matchRoute.js'
import './App.css'
import './DarkSite.css'
import './components/StandingsBands.css'

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
    component: StandingsPage, fullPage: true,
  },
  fantasy: {
    navLabel: 'Fantasy', label: 'Fantasy · 2026/27', title: 'Đội hình của bạn.', highlight: 'Theo OVR FC 27.',
    description: 'Chọn 11 cầu thủ từ roster mùa 2026/27.', component: FantasyPage, fullPage: true,
  },
  minigame: {
    navLabel: 'Minigame', label: 'Minigame · 2026/27', component: MinigamePage, fullPage: true,
  },
}

function pageFromHash(hash) {
  const name = hash.slice(1)
  if (parseMinigameRoute(hash)) return 'minigame'
  if (parsePlayerDetailHash(hash) || parsePlayerListHash(hash)) return 'players'
  if (parseMatchRoute(hash)) return 'matches'
  return PAGES[name] ? name : 'clubs'
}

function App() {
  const [session, setSession] = useState({ account: null, loading: true, error: '' })
  const [page, setPage] = useState(() => pageFromHash(window.location.hash))
  const [season, setSeason] = useState(() => parsePlayerDetailHash(window.location.hash)?.season ??
    parsePlayerListHash(window.location.hash)?.season ?? parseMatchRoute(window.location.hash)?.season ?? parseClubRoute(window.location.hash)?.season ?? 2026)
  const [playerDetail, setPlayerDetail] = useState(() => parsePlayerDetailHash(window.location.hash))
  const [matchRoute, setMatchRoute] = useState(() => parseMatchRoute(window.location.hash))
  const [clubRoute, setClubRoute] = useState(() => parseClubRoute(window.location.hash))
  const [minigameRoute, setMinigameRoute] = useState(() => parseMinigameRoute(window.location.hash))
  const [homeState, setHomeState] = useState({ query: '', slide: 0 })
  const [homeTheme, setHomeTheme] = useState('dark')
  const [standingsTab, setStandingsTab] = useState('clubs')

  useEffect(() => {
    function handleHashChange() {
      const detail = parsePlayerDetailHash(window.location.hash)
      const playerList = parsePlayerListHash(window.location.hash)
      const match = parseMatchRoute(window.location.hash)
      const club = parseClubRoute(window.location.hash)
      setPlayerDetail(detail)
      setMatchRoute(match)
      setClubRoute(club)
      setMinigameRoute(parseMinigameRoute(window.location.hash))
      if (detail?.season || playerList?.season || match?.season || club?.season) {
        setSeason(detail?.season ?? playerList?.season ?? match?.season ?? club?.season)
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
    document.title = `${playerDetail ? 'Hồ sơ cầu thủ' : clubRoute ? 'Chi tiết câu lạc bộ' : page === 'clubs' ? 'Trang chủ' : current.label} | prismaXI`
  }, [current.label, playerDetail, clubRoute, page])

  return (
    <div className={`app-shell${page === 'clubs' ? ' club-home-shell' : page === 'standings' ? ' standings-shell' : ''}`} data-home-theme={homeTheme}>
      <SiteHeader pages={PAGES} page={page} season={season} theme={homeTheme}
        onThemeChange={setHomeTheme} onSessionChange={setSession} onSeasonChange={next => {
          setSeason(next)
          if (clubRoute) window.location.hash = clubDetailHash(clubRoute.clubId, { ...clubRoute, season: next })
          else if (playerDetail) window.location.hash = playerDetailHash(playerDetail.playerId, next, playerDetail.backHash)
          else if (page === 'players') window.location.hash = playerListHash({ ...parsePlayerListHash(window.location.hash), season: next })
          else if (page === 'matches') window.location.hash = matchListHash({ season: next })
          else if (page === 'clubs') setHomeState({ query: '', slide: homeState.slide })
        }} />

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
          theme={homeTheme} playerId={playerDetail.playerId} season={playerDetail.season} backHash={playerDetail.backHash} /> :
          clubRoute ? <ClubDetailPage key={`${clubRoute.clubId}-${season}`} route={clubRoute} season={season} /> :
          <CurrentPage theme={homeTheme} key={['fantasy', 'minigame'].includes(page) ? `${page}:${session.loading}:${session.account?.id ?? 'guest'}` : current.hasSeasons ? `${page}-${season}` : page}
            {...(['fantasy', 'minigame'].includes(page) ? { account: session.account, authLoading: session.loading, authError: session.error } : {})}
            {...(page === 'minigame' ? { route: minigameRoute } : {})}
            {...(current.hasSeasons ? { season } : {})}
            {...(page === 'clubs' ? { homeState, onHomeStateChange: setHomeState } : {})}
            {...(page === 'standings' ? { tab: standingsTab, onTabChange: setStandingsTab } : {})}
            {...(page === 'matches' ? { matchRoute } : {})} />}
      </main>

      <footer className="site-footer">
        <div className="container footer-inner">
          <span className="footer-brand"><img src="/prismaxi-logo.svg" alt="" width="37" height="33" />prismaXI</span>
          {page === 'clubs' && <><p className="cx-footer-attribution">Lịch và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>.</p>
            <nav className="cx-footer-links" aria-label="Điều hướng cuối trang">{Object.entries(PAGES).map(([name, item]) =>
              <a key={name} href={`#${name}`}>{item.navLabel}</a>)}</nav></>}
        </div>
      </footer>
    </div>
  )
}

export default App
