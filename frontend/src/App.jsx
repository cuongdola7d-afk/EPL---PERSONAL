import { useEffect, useState } from 'react'
import ClubPage from './components/ClubPage.jsx'
import PlayerPage from './components/PlayerPage.jsx'
import MatchPage from './components/MatchPage.jsx'
import StandingsPage from './components/StandingsPage.jsx'
import './App.css'

const PAGES = {
  clubs: {
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
    navLabel: 'Lịch đấu', label: 'Lịch đấu & Kết quả', title: 'Từng vòng đấu.', highlight: 'Từng khoảnh khắc.',
    description: 'Theo dõi lịch đấu và kết quả, lọc theo đội bóng, vòng đấu hoặc trạng thái từ PremierHub API.',
    component: MatchPage,
  },
  standings: {
    navLabel: 'Bảng xếp hạng', label: 'Bảng xếp hạng', title: 'Mỗi điểm số.', highlight: 'Một vị trí.',
    description: 'Xem thứ hạng và các chỉ số được tính từ những trận đã kết thúc trong PremierHub API.',
    component: StandingsPage,
  },
}

function pageFromHash() {
  const name = window.location.hash.slice(1)
  return PAGES[name] ? name : 'clubs'
}

function App() {
  const [page, setPage] = useState(pageFromHash)

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

        <CurrentPage />
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
