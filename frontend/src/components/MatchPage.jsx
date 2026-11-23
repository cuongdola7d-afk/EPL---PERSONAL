import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchMatches } from '../api/matches.js'
import { useApiList } from '../hooks/useApiList.js'
import { buildScheduleHeroChips, isMatchDone, preferredMatchweek } from '../utils/matchView.js'
import { matchDetailHash, matchListHash } from '../utils/matchRoute.js'
import { SEASONS } from '../utils/seasons.js'
import MatchCard from './MatchCard.jsx'
import MatchDetail from './MatchDetail.jsx'
import MatchweekStrip from './MatchweekStrip.jsx'
import { useLiveScores } from '../hooks/useLiveScores.js'
import { compareMatchSchedule, formatDayGroupHeading, matchDayKey } from '../utils/matchTime.js'
import './MatchPage.css'

const dateLabel = (date) => new Intl.DateTimeFormat('vi-VN', {
  weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${date}T00:00:00Z`))

function MatchPage({ season, theme = 'dark', matchRoute }) {
  const [week, setWeek] = useState(matchRoute?.week ?? null)
  const [filter, setFilter] = useState(matchRoute?.filter ?? 'all')
  const [club, setClub] = useState(matchRoute?.club ?? '')
  const selectedMatchId = matchRoute?.matchId ?? null
  const { liveByFixture, flashingIds } = useLiveScores(season === 2026)
  const seasonLive = season === 2026 ? liveByFixture : null
  const requestMatches = useCallback((signal) => fetchMatches({ season }, signal), [season])
  const { data: matches, status, error, reload } = useApiList(requestMatches)

  const weeks = useMemo(() => [...new Set(matches.map((match) => match.matchweek))].sort((a, b) => a - b), [matches])
  const activeWeek = useMemo(
    () => preferredMatchweek(matches, new Date(), seasonLive),
    [matches, seasonLive],
  )
  const currentWeek = week ?? activeWeek
  const heroChips = useMemo(
    () => buildScheduleHeroChips(matches, { season, liveByFixture: seasonLive }),
    [matches, season, seasonLive],
  )
  const clubs = useMemo(() => [...new Set(matches.flatMap((match) => [match.homeClub, match.awayClub]))].sort(), [matches])
  const weekMatches = matches.filter((match) => match.matchweek === currentWeek &&
    (!club || match.homeClub === club || match.awayClub === club))
  const finished = weekMatches.filter((match) => isMatchDone(match, seasonLive)).length
  const visible = weekMatches.filter((match) => filter === 'all' ||
    (filter === 'finished' ? isMatchDone(match, seasonLive) : !isMatchDone(match, seasonLive)))
    .sort((a, b) => compareMatchSchedule(a, b, season))
  const dayGroups = useMemo(() => {
    const groups = []
    for (const match of visible) {
      const key = matchDayKey(match, season) ?? ''
      const last = groups.at(-1)
      if (!last || last.dayKey !== key) {
        groups.push({ dayKey: key, matches: [match] })
      } else {
        last.matches.push(match)
      }
    }
    return groups
  }, [visible, season])
  const selectedMatch = matches.find((match) => match.id === selectedMatchId)

  useEffect(() => {
    if (!matchRoute) return
    setWeek(matchRoute.week)
    setFilter(matchRoute.filter)
    setClub(matchRoute.club)
  }, [matchRoute?.week, matchRoute?.filter, matchRoute?.club])

  function changeWeek(next) {
    setWeek(next)
  }

  function openMatch(id) {
    const state = { season, week: currentWeek, filter, club }
    window.history.replaceState(window.history.state, '', matchListHash(state))
    window.location.hash = matchDetailHash(id, state)
  }

  function closeMatch() {
    window.location.hash = matchRoute?.backHash ?? matchListHash({ season, week: currentWeek, filter, club })
  }

  return (
    <section data-theme={theme} className="mx-page" id="directory" aria-label="Lịch đấu và kết quả">
      <div className={`mx-wrap${selectedMatchId !== null ? ' mx-wrap-detail' : ''}`}>
        {selectedMatchId !== null ? (
          <MatchDetail
            key={selectedMatchId}
            matchId={selectedMatchId}
            summary={selectedMatch}
            season={season}
            onClose={closeMatch}
            liveState={season === 2026 ? (liveByFixture[selectedMatchId] ?? null) : null}
            isFlashing={season === 2026 && Boolean(flashingIds[selectedMatchId])}
            backLabel={matchRoute?.backHash?.startsWith('#players/') ? 'Cầu thủ' : matchRoute?.backHash ? 'Câu lạc bộ' : 'Lịch đấu'}
          />
        ) : (
          <>
            <header className="mx-hero">
              <svg className="mx-hero-pitch" viewBox="0 0 300 190" aria-hidden="true">
                <rect x="6" y="6" width="288" height="178" rx="6" />
                <path d="M150 6v178" />
                <circle cx="150" cy="95" r="34" />
                <rect x="6" y="50" width="46" height="90" />
                <rect x="248" y="50" width="46" height="90" />
                <rect x="6" y="75" width="16" height="40" />
                <rect x="278" y="75" width="16" height="40" />
              </svg>
              <h1>Lịch <span>đấu</span></h1>
              <p>Premier League {SEASONS[season]} · kết quả và các trận sắp tới</p>
              {heroChips.length > 0 && (
                <div className="mx-hero-chips">
                  {heroChips.map((chip) => (
                    <span key={chip.kind} className="mx-hero-chip">
                      <i className={`mx-chip-dot mx-chip-dot-${chip.tone}`} aria-hidden="true" />
                      {chip.text}
                    </span>
                  ))}
                </div>
              )}
            </header>

            <MatchweekStrip
              weeks={weeks}
              currentWeek={currentWeek}
              activeWeek={activeWeek}
              matches={matches}
              season={season}
              liveByFixture={seasonLive}
              onChangeWeek={changeWeek}
            />

            <div className="mx-filters">
              <div className="mx-status-filters" role="group" aria-label="Lọc trạng thái trận">
                {[
                  ['all', 'Tất cả', weekMatches.length],
                  ['finished', 'Đã kết thúc', finished],
                  ['upcoming', 'Chưa kết thúc', weekMatches.length - finished],
                ].map(([value, label, count]) => (
                  <button
                    key={value}
                    type="button"
                    aria-pressed={filter === value}
                    onClick={() => setFilter(value)}
                  >
                    {label}
                    <span>{count}</span>
                  </button>
                ))}
              </div>
              <select
                className="mx-club-filter"
                value={club}
                aria-label="Lọc theo câu lạc bộ"
                onChange={(event) => setClub(event.target.value)}
              >
                <option value="">Tất cả CLB</option>
                {clubs.map((name) => <option key={name} value={name}>{name}</option>)}
              </select>
            </div>

            {status === 'loading' && (
              <div className="mx-skeletons" role="status" aria-label="Đang tải lịch đấu">
                {Array.from({ length: 4 }, (_, index) => <div key={index} />)}
              </div>
            )}
            {status === 'error' && (
              <div className="mx-empty" role="alert">
                <strong>Không tải được lịch đấu</strong>
                <p>{error}</p>
                <button type="button" onClick={reload}>Thử lại</button>
              </div>
            )}
            {status === 'success' && (
              <>
                <div className="mx-count">Gameweek {currentWeek} · <strong>{visible.length}</strong> trận</div>
                {visible.length === 0 ? (
                  <div className="mx-empty">
                    <strong>Không có trận nào</strong>
                    <p>Thử đổi vòng đấu hoặc bộ lọc.</p>
                    <button type="button" onClick={() => { setFilter('all'); setClub('') }}>Xem tất cả</button>
                  </div>
                ) : (
                  <div className="mx-match-list">
                    {dayGroups.map((group) => {
                      const heading = season === 2026
                        ? formatDayGroupHeading(group.dayKey)
                        : { label: dateLabel(group.dayKey), relative: '' }
                      return (
                        <section key={group.dayKey} className="mx-day">
                          <div className="mx-day-h">
                            <h2>{heading.label}</h2>
                            {heading.relative && <span className="mx-day-rel">{heading.relative}</span>}
                            <small>{group.matches.length} trận</small>
                          </div>
                          <div className="mx-day-list">
                            {group.matches.map((match) => (
                              <MatchCard
                                key={match.id}
                                match={match}
                                season={season}
                                onOpen={openMatch}
                                liveState={season === 2026 ? (liveByFixture[match.id] ?? null) : null}
                                isFlashing={season === 2026 && Boolean(flashingIds[match.id])}
                              />
                            ))}
                          </div>
                        </section>
                      )
                    })}
                  </div>
                )}
                <p className="mx-attribution">
                  Lịch và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>.
                </p>
              </>
            )}
          </>
        )}
      </div>
    </section>
  )
}

export default MatchPage
