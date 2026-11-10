import { useEffect, useRef } from 'react'
import AccountMenu from './AccountMenu.jsx'
import { SEASONS } from '../utils/seasons.js'
import './SiteHeader.css'

export default function SiteHeader({ pages, page, season, onSeasonChange, theme, onThemeChange, onSessionChange }) {
  const navigation = useRef(null)
  useEffect(() => {
    navigation.current?.querySelector('[aria-current="page"]')?.scrollIntoView({ block: 'nearest', inline: 'nearest' })
  }, [page])
  const supportsSeasons = pages[page].hasSeasons
  return <header className="site-header">
    <div className="container header-inner">
      <a className="brand" href="#clubs" aria-label="prismaXI, trang chủ">
        <img className="brand-logo" src="/prismaxi-logo.svg" alt="" width="34" height="34" />
        <span>prisma<span className="brand-accent">XI</span></span>
      </a>
      <nav ref={navigation} className="site-nav" aria-label="Điều hướng chính">
        {Object.entries(pages).map(([name, item]) => <a key={name} href={`#${name}`}
          className={`nav-link${page === name ? ' active' : ''}`} aria-current={page === name ? 'page' : undefined}>{item.navLabel}</a>)}
      </nav>
      <div className="cx-header-controls">
        <label className="cx-season"><span className="sr-only">Mùa giải</span>
          <select aria-label="Mùa giải" value={supportsSeasons ? season : 2026} disabled={!supportsSeasons}
            onChange={event => onSeasonChange(Number(event.target.value))}>
            {(supportsSeasons ? [2026, 2024] : [2026]).map(year => <option value={year} key={year}>{SEASONS[year]}</option>)}
          </select><span aria-hidden="true">⌄</span>
        </label>
        <button className="cx-theme-toggle" type="button" aria-label={`Chuyển sang chế độ ${theme === 'dark' ? 'sáng' : 'tối'}`}
          onClick={() => onThemeChange(theme === 'dark' ? 'light' : 'dark')}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <circle cx="12" cy="12" r="9" /><path d="M12 3a9 9 0 0 0 0 18z" fill="currentColor" />
          </svg>
        </button>
      </div>
      <AccountMenu onSessionChange={onSessionChange} />
    </div>
  </header>
}
