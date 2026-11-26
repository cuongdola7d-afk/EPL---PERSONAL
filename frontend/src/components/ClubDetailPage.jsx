import { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
import { fetchClub, fetchClubOverview, fetchClubStatistics } from '../api/clubs.js'
import { fetchPlayers } from '../api/players.js'
import { fetchMatches } from '../api/matches.js'
import { fetchStandings } from '../api/standings.js'
import { useApiList } from '../hooks/useApiList.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { clubDetailHash } from '../utils/clubRoute.js'
import {
  calculateAge,
  clampTooltipPosition,
  clubMatches,
  clubMatchResult,
  clubPerspectiveScore,
  clubShortName,
  formatFormTooltip,
  formatShortKickoffDate,
  formatSquadStat,
  groupMatchesByVietnamMonth,
  isSquadColumnLeader,
  ovrTier,
  playerPositionChips,
  ratingClass,
  sortSquadRows,
  SQUAD_POSITION_GROUPS,
  squadColumnLeaders,
  squadColumnsForGroup,
  squadRatingBand,
  standingBand,
  stripClubSuffix,
  valueLabel,
} from '../utils/clubView.js'
import { playerDetailHash } from '../utils/playerRoute.js'
import { matchDetailHash } from '../utils/matchRoute.js'
import { SEASONS, hasMatchScore } from '../utils/seasons.js'
import { vietnamToday } from '../utils/homeView.js'
import { clubVisual } from '../utils/matchView.js'
import { getInitials } from '../utils/initials.js'
import { HomeCrest } from './ClubCard.jsx'
import MatchKickoff from './MatchKickoff.jsx'
import './ClubDetailPage.css'

const dateLabel = value => value ? new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${value}T00:00:00Z`)) : '—'
const resultLabel = { W: 'Thắng', D: 'Hòa', L: 'Thua' }

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
    <MatchTeams match={match} /><span className="cd-match-date"><MatchKickoff match={match} season={season} legacyLabel={season === 2024 ? dateLabel(match.date) : undefined} /></span>
  </a> : <div className="cd-match-box"><div className="cd-box-label">{label}</div><p className="cd-muted">Chưa có dữ liệu</p></div>
}

function SquadTables({ players, stats, statsResource, season, fromHash }) {
  const today = vietnamToday()
  const hasStats = season === 2026
  const statsById = new Map((stats?.players ?? []).map(row => [row.playerId, row]))
  const knownCodes = new Set(SQUAD_POSITION_GROUPS.map(group => group.code))
  const sections = [
    ...SQUAD_POSITION_GROUPS,
    ...(players.some(player => !knownCodes.has(player.position))
      ? [{ code: 'UNKNOWN', label: 'Chưa xác định', fallbackTag: '?' }]
      : []),
  ]

  return (
    <>
      {hasStats && statsResource.status === 'error' && (
        <DataState status="error" error={statsResource.error} reload={statsResource.reload} empty={false} title="thống kê cầu thủ" />
      )}
      {sections.map(group => {
        const members = players.filter(player =>
          group.code === 'UNKNOWN' ? !knownCodes.has(player.position) : player.position === group.code,
        )
        if (!members.length) return null
        const columns = hasStats ? squadColumnsForGroup(group.code) : []
        const rows = sortSquadRows(
          members.map(player => {
            const rowStats = hasStats ? (statsById.get(player.id) ?? null) : null
            const played = Boolean(rowStats && rowStats.appearances > 0)
            return { player, stats: rowStats, played }
          }),
          hasStats,
        )
        const leaders = hasStats ? squadColumnLeaders(rows, columns, 270) : {}

        return (
          <section
            className={hasStats ? 'cd-sq' : 'cd-sq cd-sq-basic'}
            key={group.code}
            aria-label={group.label}
          >
            <div className="cd-sqh">
              <span className="cd-sqh-gt">
                {group.label}
                <span>{rows.length}</span>
              </span>
              <span className="cd-sqh-ovr">OVR</span>
              {hasStats && columns.map((col, idx) => (
                <span key={col.key} className={`cd-col-${idx + 1}`}>{col.label}</span>
              ))}
              {hasStats && <span className="cd-col-rt">Điểm TB</span>}
            </div>
            {rows.map(row => {
              const { player, stats: rowStats, played } = row
              const age = calculateAge(player.birthDate, today)
              const metaParts = [player.nationality, age != null ? `${age} tuổi` : null].filter(Boolean)
              const chips = playerPositionChips(player, 2)
              const ratingBand = played ? squadRatingBand(rowStats?.averageRating) : null
              const isIdle = hasStats && !played

              return (
                <a
                  key={player.id}
                  className={isIdle ? 'cd-pr cd-pr-idle' : 'cd-pr'}
                  href={playerDetailHash(player.id, season, fromHash)}
                  aria-label={`Xem hồ sơ ${player.name}`}
                >
                  <span
                    className={player.shirtNumber != null ? 'cd-pr-no' : 'cd-pr-no cd-pr-no-init'}
                    aria-hidden="true"
                  >
                    {player.shirtNumber != null ? player.shirtNumber : getInitials(player.name)}
                  </span>
                  <span className="cd-pr-nm">
                    <b>{player.name}</b>
                    {(metaParts.length > 0 || chips.length > 0) && (
                      <span className="cd-pr-meta">
                        {metaParts.length > 0 && <small>{metaParts.join(' · ')}</small>}
                        {chips.map(code => <em key={code}>{code}</em>)}
                      </span>
                    )}
                  </span>
                  <span className={`cd-ovr cd-ovr-${ovrTier(player.fc27Overall)}`}>
                    {valueLabel(player.fc27Overall)}
                  </span>
                  {hasStats && columns.map((col, idx) => {
                    const isTop = isSquadColumnLeader(row, col, leaders, 270)
                    return (
                      <span
                        key={col.key}
                        className={`cd-pr-c cd-col-${idx + 1}${isTop ? ' cd-pr-top' : ''}`}
                      >
                        {played ? formatSquadStat(rowStats?.[col.key], col.format) : '–'}
                      </span>
                    )
                  })}
                  {hasStats && (
                    <span className="cd-pr-c cd-col-rt">
                      {played && rowStats?.averageRating != null ? (
                        <span className={`cd-rb cd-rb-${ratingBand}`}>
                          {rowStats.averageRating.toFixed(2)}
                        </span>
                      ) : '–'}
                    </span>
                  )}
                </a>
              )
            })}
          </section>
        )
      })}
    </>
  )
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

function FormStrip({ matches, clubId, season }) {
  const [activeId, setActiveId] = useState(null)
  const [pinnedId, setPinnedId] = useState(null)
  const [pos, setPos] = useState({ left: 0, arrowLeft: 110 })
  const containerRef = useRef(null)
  const buttonRefs = useRef(new Map())
  const tipRef = useRef(null)
  const tipId = useId()

  const shownId = pinnedId ?? activeId
  const activeMatch = matches.find(m => m.id === shownId) ?? null
  const activeTip = activeMatch ? formatFormTooltip(activeMatch, clubId, season) : null

  useEffect(() => {
    if (pinnedId == null) return undefined
    const handleOutside = event => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setPinnedId(null)
        setActiveId(null)
      }
    }
    const handleKey = event => {
      if (event.key === 'Escape') {
        setPinnedId(null)
        setActiveId(null)
      }
    }
    document.addEventListener('pointerdown', handleOutside)
    document.addEventListener('keydown', handleKey)
    return () => {
      document.removeEventListener('pointerdown', handleOutside)
      document.removeEventListener('keydown', handleKey)
    }
  }, [pinnedId])

  useLayoutEffect(() => {
    if (shownId == null || !containerRef.current || !tipRef.current) return
    const btn = buttonRefs.current.get(shownId)
    if (!btn) return
    const containerRect = containerRef.current.getBoundingClientRect()
    const btnRect = btn.getBoundingClientRect()
    const tipRect = tipRef.current.getBoundingClientRect()
    const anchorCenterX = btnRect.left - containerRect.left + btnRect.width / 2
    const next = clampTooltipPosition({
      anchorCenterX,
      containerLeft: containerRect.left,
      tooltipWidth: tipRect.width || 228,
      viewportWidth: window.innerWidth,
      margin: 12,
    })
    setPos(next)
  }, [shownId, matches])

  return (
    <div className="cd-form" ref={containerRef} aria-label="Phong độ 5 trận gần nhất">
      {matches.map(match => {
        const info = formatFormTooltip(match, clubId, season)
        const result = info?.result
        const isCurrent = shownId === match.id
        return (
          <button
            type="button"
            key={match.id}
            ref={el => {
              if (el) buttonRefs.current.set(match.id, el)
              else buttonRefs.current.delete(match.id)
            }}
            className={`cd-form-btn cd-form-${result ?? 'unknown'}`}
            aria-label={info?.ariaLabel ?? `GW${match.matchweek}`}
            aria-describedby={isCurrent ? tipId : undefined}
            aria-expanded={isCurrent}
            onMouseEnter={() => { if (pinnedId == null) setActiveId(match.id) }}
            onMouseLeave={() => { if (pinnedId == null) setActiveId(prev => prev === match.id ? null : prev) }}
            onFocus={() => setActiveId(match.id)}
            onBlur={() => {
              setActiveId(prev => prev === match.id ? null : prev)
              setPinnedId(prev => prev === match.id ? null : prev)
            }}
            onClick={() => {
              setPinnedId(prev => prev === match.id ? null : match.id)
              setActiveId(match.id)
            }}
          >
            {result ?? '—'}
          </button>
        )
      })}
      {activeTip && (
        <div
          id={tipId}
          ref={tipRef}
          className="cd-form-tip"
          role="tooltip"
          style={{ left: `${pos.left}px`, '--cd-tip-arrow': `${pos.arrowLeft}px` }}
        >
          <small>{activeTip.header}</small>
          <b>{activeTip.scoreline}</b>
          <span>{activeTip.timeLabel}</span>
        </div>
      )}
    </div>
  )
}

function LeaderCard({ title, hint, items, season, fromHash, formatValue }) {
  return (
    <div className="cd-top">
      <h3>
        {title}
        {hint && <span className="cd-top-hint">{hint}</span>}
      </h3>
      {items.length === 0 ? (
        <p className="cd-top-empty">Chưa có dữ liệu</p>
      ) : (
        items.map(item => (
          <a
            key={item.playerId}
            className="cd-tr"
            href={playerDetailHash(item.playerId, season, fromHash)}
          >
            <span className="cd-tr-rank">{item.rank}</span>
            <span className="cd-tr-who">
              <strong>{item.name}</strong>
              <small>{item.appearances} trận</small>
            </span>
            <b className={item.rank === 1 ? 'cd-tr-val cd-tr-lead' : 'cd-tr-val'}>
              {formatValue(item.value)}
            </b>
          </a>
        ))
      )}
    </div>
  )
}

function OverviewTab({ resource, season, clubColor, fromHash }) {
  const data = resource.data
  const hasLineup = Boolean(data?.formation && Array.isArray(data.xi) && data.xi.length > 0)

  return (
    <div className="cd-ov-grid" role="tabpanel" id="cd-panel-overview" aria-labelledby="cd-tab-overview">
      <section className="cd-card cd-ov-card">
        <h2 className="cd-ov-title">Đội hình hay dùng nhất</h2>
        <DataState status={resource.status} error={resource.error} reload={resource.reload} empty={false} title="đội hình hay dùng nhất" />
        {resource.status === 'success' && (
          hasLineup ? (
            <>
              <div className="cd-pitch" style={{ '--cd-club-color': clubColor }}>
                <svg viewBox="0 0 68 80" preserveAspectRatio="none" aria-hidden="true">
                  <rect x="2" y="2" width="64" height="76" rx="1" />
                  <path d="M2 40h64" />
                  <circle cx="34" cy="40" r="7" />
                  <circle className="cd-pitch-spot" cx="34" cy="40" r=".7" />
                  <circle className="cd-pitch-spot" cx="34" cy="10" r=".6" />
                  <circle className="cd-pitch-spot" cx="34" cy="70" r=".6" />
                  <path d="M27.5 14a7 7 0 0 0 13 0" />
                  <path d="M27.5 66a7 7 0 0 1 13 0" />
                  <rect x="18" y="2" width="32" height="12" />
                  <rect x="26" y="2" width="16" height="5" />
                  <rect x="18" y="66" width="32" height="12" />
                  <rect x="26" y="73" width="16" height="5" />
                </svg>
                {data.xi.map(player => (
                  <a
                    key={`${player.rowIndex}-${player.slotIndex}-${player.playerId}`}
                    className="cd-pl"
                    style={{ left: `${player.x}%`, top: `${player.y}%` }}
                    href={playerDetailHash(player.playerId, season, fromHash)}
                    title={`${player.name} · ${player.position} · ${player.startsAtPosition} trận đá chính`}
                    aria-label={`${player.name}, vị trí ${player.position}, ${player.startsAtPosition} trận đá chính`}
                  >
                    <span className="cd-pl-circle">
                      {player.position}
                      <i>{player.startsAtPosition}</i>
                    </span>
                    <span className="cd-pl-name">{player.shortName}</span>
                  </a>
                ))}
              </div>
              <div className="cd-fm">
                {data.formationCounts.map((entry, idx) => (
                  <span className="cd-fm-chip" key={entry.formation}>
                    {idx === 0 ? (
                      <>Sơ đồ <b>{entry.formation}</b> · {entry.matches}/{data.lineupMatches} trận</>
                    ) : (
                      <>{entry.formation} · {entry.matches}/{data.lineupMatches} trận</>
                    )}
                  </span>
                ))}
              </div>
            </>
          ) : (
            <div className="cd-ov-empty">Chưa có dữ liệu đội hình mùa này</div>
          )
        )}
      </section>

      <section className="cd-card cd-ov-card">
        <h2 className="cd-ov-title">Cầu thủ nổi bật</h2>
        <p className="cd-ov-sub">Mùa {SEASONS[season]} · Premier League</p>
        <DataState status={resource.status} error={resource.error} reload={resource.reload} empty={false} title="cầu thủ nổi bật" />
        {resource.status === 'success' && data && (
          <div className="cd-tops">
            <LeaderCard
              title="Bàn thắng"
              items={data.topScorers}
              season={season}
              fromHash={fromHash}
              formatValue={v => String(v)}
            />
            <LeaderCard
              title="Kiến tạo"
              items={data.topAssists}
              season={season}
              fromHash={fromHash}
              formatValue={v => String(v)}
            />
            <LeaderCard
              title="Điểm đánh giá TB"
              hint=" (từ 5 trận)"
              items={data.topRatings}
              season={season}
              fromHash={fromHash}
              formatValue={v => Number(v).toFixed(2)}
            />
          </div>
        )}
      </section>
    </div>
  )
}

function UpcomingCard({ match, clubId, season, fromHash, isFirst }) {
  const { date, time } = formatShortKickoffDate(match, season)
  const isHomeOwn = match.homeClubId === clubId
  const isAwayOwn = match.awayClubId === clubId
  const isLive = match.status === 'LIVE'
  const statusText = isLive
    ? 'Đang diễn ra'
    : match.status === 'POSTPONED'
      ? 'Hoãn'
      : match.status === 'SUSPENDED'
        ? 'Tạm dừng'
        : null
  const primaryTime = statusText ?? time ?? (season === 2026 ? 'Chưa rõ giờ' : date)
  const secondaryMeta = statusText || time || season === 2026
    ? `${date ? `${date} · ` : ''}GW${match.matchweek}`
    : `GW${match.matchweek}`

  return (
    <a
      className={`cd-nx${isFirst ? ' cd-nx-first' : ''}`}
      href={matchDetailHash(match.id, { season, week: match.matchweek }, fromHash)}
    >
      <div className="cd-nx-top">
        <b className={isLive ? 'cd-nx-live' : statusText ? 'cd-nx-status' : undefined}>{primaryTime}</b>
        <span>{secondaryMeta}</span>
      </div>
      <div className="cd-nx-vs">
        <span className={`cd-tm cd-tm-h${isHomeOwn ? ' cd-tm-me' : ''}`}>
          <span className="cd-tm-n">{stripClubSuffix(match.homeClub)}</span>
          <span className="cd-tm-s">{clubShortName(match.homeClub)}</span>
          <HomeCrest name={match.homeClub} />
        </span>
        <i aria-hidden="true">vs</i>
        <span className={`cd-tm cd-tm-a${isAwayOwn ? ' cd-tm-me' : ''}`}>
          <HomeCrest name={match.awayClub} />
          <span className="cd-tm-n">{stripClubSuffix(match.awayClub)}</span>
          <span className="cd-tm-s">{clubShortName(match.awayClub)}</span>
        </span>
      </div>
    </a>
  )
}

function MatchRow({ match, clubId, season, fromHash, upcoming = false }) {
  const isHome = match.homeClubId === clubId
  const opponent = isHome ? match.awayClub : match.homeClub
  const { date, time } = formatShortKickoffDate(match, season)

  if (upcoming) {
    const isLive = match.status === 'LIVE'
    const statusLabel = isLive
      ? 'Đang diễn ra'
      : match.status === 'POSTPONED'
        ? 'Hoãn'
        : match.status === 'SUSPENDED'
          ? 'Tạm dừng'
          : null
    return (
      <a
        className="cd-rw cd-rw-up"
        href={matchDetailHash(match.id, { season, week: match.matchweek }, fromHash)}
      >
        <span className="cd-rw-gw">GW{match.matchweek}</span>
        <span className="cd-rw-dt">
          <b>{date}</b>
          {time ?? ''}
        </span>
        <span className="cd-rw-op">
          <HomeCrest name={opponent} />
          <span className="cd-rw-n">{stripClubSuffix(opponent)}</span>
          <span className="cd-rw-n cd-rw-s">{clubShortName(opponent)}</span>
          <span className="cd-rw-tag">{isHome ? 'Nhà' : 'Khách'}</span>
        </span>
        <span className="cd-rw-sc">{isLive ? 'Đang đá' : (time ?? 'vs')}</span>
        {statusLabel ? (
          <span className={`cd-rp ${isLive ? 'cd-rp-live' : 'cd-rp-D'}`}>{statusLabel}</span>
        ) : (
          <span className="cd-rp-empty" aria-hidden="true" />
        )}
      </a>
    )
  }

  const result = clubMatchResult(match, clubId)
  const score = clubPerspectiveScore(match, clubId) ?? '—'
  return (
    <a
      className={`cd-rw cd-rw-${result ?? 'unknown'}`}
      href={matchDetailHash(match.id, { season, week: match.matchweek }, fromHash)}
    >
      <span className="cd-rw-gw">GW{match.matchweek}</span>
      <span className="cd-rw-dt">
        <b>{date}</b>
        {time ?? ''}
      </span>
      <span className="cd-rw-op">
        <HomeCrest name={opponent} />
        <span className="cd-rw-n">{stripClubSuffix(opponent)}</span>
        <span className="cd-rw-n cd-rw-s">{clubShortName(opponent)}</span>
        <span className="cd-rw-tag">{isHome ? 'Nhà' : 'Khách'}</span>
      </span>
      <span className="cd-rw-sc">{score}</span>
      <span className={`cd-rp cd-rp-${result ?? 'unknown'}`}>{resultLabel[result] ?? '—'}</span>
    </a>
  )
}

function MatchesTab({ matchesResource, done, pending, clubId, season, expanded, onToggleExpanded, fromHash }) {
  const isEmpty = matchesResource.status === 'success' && done.length === 0 && pending.length === 0
  const monthGroups = groupMatchesByVietnamMonth(done, season)
  const topUpcoming = pending.slice(0, 3)
  const extraUpcoming = expanded ? pending.slice(3) : []

  return (
    <>
      <DataState {...matchesResource} empty={isEmpty} title="trận đấu" />
      {matchesResource.status === 'success' && !isEmpty && (
        <>
          {pending.length > 0 && (
            <section className="cd-matches-upcoming" aria-label="Trận sắp tới">
              <div className="cd-mh">Sắp tới</div>
              <div className="cd-next">
                {topUpcoming.map((match, idx) => (
                  <UpcomingCard
                    key={match.id}
                    match={match}
                    clubId={clubId}
                    season={season}
                    fromHash={fromHash}
                    isFirst={idx === 0}
                  />
                ))}
              </div>
              {extraUpcoming.length > 0 && (
                <div className="cd-up-more">
                  {extraUpcoming.map(match => (
                    <MatchRow
                      key={match.id}
                      match={match}
                      clubId={clubId}
                      season={season}
                      fromHash={fromHash}
                      upcoming
                    />
                  ))}
                </div>
              )}
              {pending.length > 3 && (
                <button
                  type="button"
                  className="cd-more-link"
                  onClick={() => onToggleExpanded(!expanded)}
                >
                  {expanded ? 'Thu gọn ‹' : `Xem tất cả ${pending.length} trận sắp tới ›`}
                </button>
              )}
            </section>
          )}

          {done.length > 0 && (
            <section className="cd-matches-past" aria-label="Trận đã đá">
              <div className={`cd-mh${pending.length > 0 ? ' cd-mh-past' : ''}`}>Đã đá</div>
              {monthGroups.map(group => (
                <div key={group.key} className="cd-month-group">
                  <div className="cd-mh">{group.label}</div>
                  {group.matches.map(match => (
                    <MatchRow
                      key={match.id}
                      match={match}
                      clubId={clubId}
                      season={season}
                      fromHash={fromHash}
                    />
                  ))}
                </div>
              ))}
            </section>
          )}
        </>
      )}
    </>
  )
}

export default function ClubDetailPage({ route, season }) {
  const requestClub=useCallback(signal=>fetchClub(route.clubId,season,signal),[route.clubId,season])
  const club=useApiResource(requestClub)
  const requestOverview=useCallback(signal=>fetchClubOverview(route.clubId,season,signal),[route.clubId,season])
  const overview=useApiResource(requestOverview)
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
  const {done,soon,pending}=clubMatches(matches.data,route.clubId,vietnamToday(),season)
  const playedPlayerIds=new Set((stats.data?.players ?? []).filter(row=>row.appearances > 0).map(row=>row.playerId))
  const playedCount=players.data.filter(player=>playedPlayerIds.has(player.id)).length
  const tabs=[['overview','Tổng quan'],['standings','Bảng xếp hạng'],['matches','Trận đấu'],['players','Cầu thủ'],['stats','Thống kê']]
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
          <FormStrip matches={done.slice(0,5).reverse()} clubId={route.clubId} season={season} /></div></div></div>
        {matches.status === 'success' ? <><FeaturedMatch label="Trận trước" match={done[0]} season={season} fromHash={fromHash} />
          <FeaturedMatch label="Trận kế tiếp" match={soon[0]} season={season} fromHash={fromHash} /></> : <div className="cd-header-state"><DataState {...matches} title="lịch đấu" /></div>}
      </header>
      <nav className="cd-tabs" role="tablist" aria-label="Thông tin câu lạc bộ">{tabs.map(([key,label])=><button type="button" role="tab" key={key}
        id={`cd-tab-${key}`} aria-selected={route.tab === key} aria-controls={`cd-panel-${key}`} onClick={()=>update({tab:key})}>{label}</button>)}</nav>
      {route.tab === 'overview' ? (
        <OverviewTab resource={overview} season={season} clubColor={visual.color} fromHash={fromHash} />
      ) : (
      <div className="cd-card cd-panel" role="tabpanel" id={`cd-panel-${route.tab}`} aria-labelledby={`cd-tab-${route.tab}`}>
        <div className="cd-panel-heading"><div><h2>{tabs.find(([key])=>key===route.tab)?.[1]}</h2><p>{route.tab === 'matches' && matches.status === 'success'
          ? `${done.length} trận đã đá${pending.length > 0 ? ` · ${pending.length} trận còn lại` : ''}${season === 2026 ? ' · giờ Việt Nam' : ''}`
          : `Premier League · ${SEASONS[season]}${route.tab === 'players' && players.status === 'success' ? ` · ${players.data.length} cầu thủ${season === 2026 && stats.status === 'success' ? ` · ${playedCount} người đã ra sân` : ''}` : ''}`}</p></div>
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
        {route.tab === 'matches' && (
          <MatchesTab
            matchesResource={matches}
            done={done}
            pending={pending}
            clubId={route.clubId}
            season={season}
            expanded={route.matches === 'soon'}
            onToggleExpanded={next => update({ matches: next ? 'soon' : 'done' })}
            fromHash={fromHash}
          />
        )}
        {route.tab === 'players' && <><DataState {...players} empty={!players.data.length} title="cầu thủ" />
          {players.status==='success' && players.data.length > 0 && (
            <SquadTables players={players.data} stats={stats.data} statsResource={stats} season={season} fromHash={fromHash} />
          )}
        </>}
        {route.tab === 'stats' && <><DataState {...standings} title="chỉ số BXH" /><DataState {...matches} title="kết quả trận đấu" />
          <Statistics standing={standing} resource={stats} done={done} clubId={route.clubId} /></>}
      </div>
      )}
    </>}
  </div></section>
}
