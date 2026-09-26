import { useEffect, useState } from 'react'
import ClubPage from './components/ClubPage.jsx'
import PlayerPage from './components/PlayerPage.jsx'
import MatchPage from './components/MatchPage.jsx'
import StandingsPage from './components/StandingsPage.jsx'
import FantasyReplayPage from './components/FantasyReplayPage.jsx'
import './App.css'

const PAGES = {
  clubs: {
    hasSeasons: true,
    navLabel: 'Câu lạc bộ', label: 'Câu lạc bộ', title: 'Một giải đấu.', highlight: 'Nhiều câu chuyện.',
    description: 'Bắt đầu khám phá Premier League qua danh sách câu lạc bộ. Tìm tên đội bạn quan tâm từ dữ liệu của PremierHub API.',
    component: ClubPage,
  },
  players: {
    navLabel: 'Cầu thủ', label: 'Cầu thủ', title: 'Những gương mặt.', highlight: 'Tạo nên trận đấu.',
    description: 'Khám phá cầu thủ theo câu lạc bộ và vị trí, cùng số bàn thắng và kiến tạo từ PremierHub API.',
    component: PlayerPage,
  },
  matches: {
    hasSeasons: true,
    navLabel: 'Lịch đấu', label: 'Lịch đấu & Kết quả', title: 'Từng vòng đấu.', highlight: 'Từng khoảnh khắc.',
    description: 'Tra cứu lịch đấu và kết quả mùa 2024/25 hoặc 2026/27, lọc theo đội bóng, Gameweek và trạng thái trận.',
    component: MatchPage,
  },
  standings: {
    hasSeasons: true,
    navLabel: 'Bảng xếp hạng', label: 'Bảng xếp hạng', title: 'Mỗi điểm số.', highlight: 'Một vị trí.',
    description: 'Tra cứu bảng xếp hạng theo mùa giải từ dữ liệu đã lưu tại PremierHub.',
    component: StandingsPage,
  },
  replay: {
    navLabel: 'Fantasy Replay', label: 'Fantasy Replay · GW1 2024/25', title: 'Chọn 11 cầu thủ.', highlight: 'Xem lại GW1.',
    description: 'Chơi lại Gameweek 1 mùa 2024/25 đã kết thúc với điểm v1 đã xác minh từ 10 trận. Đội hình được lưu trên trình duyệt của bạn.',
    component: FantasyReplayPage,
  },
}

function pageFromHash() {
  const name = window.location.hash.slice(1)
  return PAGES[name] ? name : 'clubs'
}

function App() {
  const [page, setPage] = useState(pageFromHash)
  const [season, setSeason] = useState(2024)

  useEffect(() => {
    function handleHashChange() {
      setPage(pageFromHash())
      window.scrollTo(0, 0)
    }

    window.addEventListener('hashchange', handleHashChange)
    return () => window.removeEventListener('hashchange', handleHashChange)
  }, [])

  const current = PAGES[page]
  const CurrentPage = current.component

  return (
    <div className="app-shell">
      <header className="site-header">
        <div className="container header-inner">
          <a className="brand" href="#clubs" aria-label="PremierHub, trang câu lạc bộ">
            <span className="brand-mark" aria-hidden="true">P</span>
            <span>Premier<span className="brand-accent">Hub</span></span>
          </a>

          <nav className="site-nav" aria-label="Điều hướng chính">
            {Object.entries(PAGES).map(([name, item]) => (
              <a key={name} className={`nav-link${page === name ? ' active' : ''}`} href={`#${name}`} aria-current={page === name ? 'page' : undefined}>
                {item.navLabel}
              </a>
            ))}
          </nav>
        </div>
      </header>

      <main>
        <section className="hero" aria-labelledby="hero-title">
          <div className="container hero-inner">
            <div className="hero-copy">
              <p className="eyebrow"><span className="eyebrow-line" /> PremierHub / {current.label}</p>
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
              <span className="pitch-center">PH</span>
              <span className="hero-art-caption">THE BEAUTIFUL GAME</span>
            </div>
          </div>
        </section>

        <CurrentPage key={current.hasSeasons ? `${page}-${season}` : page}
          {...(current.hasSeasons ? { season, onSeasonChange: setSeason } : {})} />
      </main>

      <footer className="site-footer">
        <div className="container footer-inner">
          <span>PremierHub</span>
          <span>Dự án học Full-stack với dữ liệu bóng đá.</span>
        </div>
      </footer>
    </div>
  )
}

export default App
