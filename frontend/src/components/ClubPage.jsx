import { useCallback, useEffect, useState } from 'react'
import { fetchClubs } from '../api/clubs.js'
import { useApiList } from '../hooks/useApiList.js'
import ClubCard from './ClubCard.jsx'
import ResultPanel from './ResultPanel.jsx'

function ClubPage() {
  const [draft, setDraft] = useState('')
  const [keyword, setKeyword] = useState('')
  const [city, setCity] = useState('')
  const [cities, setCities] = useState([])
  const [sortOrder, setSortOrder] = useState('asc')
  const requestClubs = useCallback((signal) => fetchClubs(keyword, signal), [keyword])
  const { data: clubs, status, error, reload } = useApiList(requestClubs)

  useEffect(() => {
    if (status === 'success' && !keyword) {
      setCities([...new Set(clubs.map((club) => club.city))].sort((a, b) => a.localeCompare(b)))
    }
  }, [clubs, keyword, status])

  const visibleClubs = [...clubs]
    .filter((club) => !city || club.city === city)
    .sort((a, b) => sortOrder === 'asc'
      ? a.name.localeCompare(b.name)
      : b.name.localeCompare(a.name))
  const hasFilters = Boolean(keyword || city)

  function handleSearch(event) {
    event.preventDefault()
    const nextKeyword = draft.trim()
    if (nextKeyword === keyword) {
      reload()
    } else {
      setKeyword(nextKeyword)
    }
  }

  function clearFilters() {
    setDraft('')
    setCity('')
    if (keyword) setKeyword('')
  }

  return (
    <section className="directory-section" id="directory" aria-labelledby="clubs-heading">
      <div className="container">
        <div className="section-heading">
          <div>
            <p className="section-kicker">KHÁM PHÁ GIẢI ĐẤU <span>01 / CLUBS</span></p>
            <h2 id="clubs-heading">Câu lạc bộ</h2>
            <p className="section-description">Tên đội bóng và thành phố từ API PremierHub.</p>
          </div>
          {status === 'success' && (
            <p className="result-count" aria-live="polite">
              <strong>{visibleClubs.length}</strong> {hasFilters ? 'kết quả' : 'câu lạc bộ'}
            </p>
          )}
        </div>

        <form className="search-form" role="search" onSubmit={handleSearch}>
          <label htmlFor="club-search">Tìm câu lạc bộ theo tên</label>
          <div className="search-controls">
            <div className="search-input-wrap">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
                <circle cx="10.8" cy="10.8" r="6.8" />
                <path d="m16 16 5 5" />
              </svg>
              <input
                id="club-search"
                type="search"
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Ví dụ: Arsenal, Manchester..."
                autoComplete="off"
              />
            </div>
            <button className="search-button" type="submit">Tìm kiếm <span aria-hidden="true">→</span></button>
            {(draft || hasFilters) && (
              <button className="clear-button" type="button" onClick={clearFilters}>Xóa lọc</button>
            )}
          </div>
          <div className="secondary-controls">
            <div className="filter-field">
              <label htmlFor="club-city">Thành phố</label>
              <select id="club-city" value={city} onChange={(event) => setCity(event.target.value)} disabled={status !== 'success'}>
                <option value="">Tất cả thành phố</option>
                {cities.map((option) => <option key={option} value={option}>{option}</option>)}
              </select>
            </div>
            <div className="filter-field">
              <label htmlFor="club-sort">Sắp xếp tên</label>
              <select id="club-sort" value={sortOrder} onChange={(event) => setSortOrder(event.target.value)}>
                <option value="asc">A–Z</option>
                <option value="desc">Z–A</option>
              </select>
            </div>
            <button className="refresh-button" type="button" onClick={reload} disabled={status === 'loading'}>Làm mới</button>
          </div>
        </form>

        <ResultPanel
          status={status}
          error={error}
          count={visibleClubs.length}
          itemName="câu lạc bộ"
          emptyMessage={hasFilters ? 'Không có câu lạc bộ khớp với bộ lọc hiện tại.' : 'API hiện chưa có câu lạc bộ nào.'}
          onRetry={reload}
          onClear={hasFilters ? clearFilters : undefined}
        >
          <div className="club-grid">
            {visibleClubs.map((club, index) => <ClubCard key={club.id} club={club} index={index} />)}
          </div>
        </ResultPanel>
      </div>
    </section>
  )
}

export default ClubPage
