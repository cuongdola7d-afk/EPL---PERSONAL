import { useEffect, useMemo, useState } from 'react'
import { fetchPlayer, fetchPlayerMatches, fetchPlayerSeasonProfile } from '../api/players.js'
import { ESTIMATED_OVR_PLAYER_IDS, GROUP_LABEL } from '../fantasy/lineup.js'
import { getInitials } from '../utils/initials.js'
import { seasonSummary } from '../utils/playerDetail.js'
import { buildFormSeries, clubGradient, normalizePlayerTab, roleMatchColumns } from '../utils/playerProfile.js'
import { SEASONS } from '../utils/seasons.js'
import { compareMatchSchedule } from '../utils/matchTime.js'
import MatchKickoff from './MatchKickoff.jsx'
import PlayerHeroStats from './player/PlayerHeroStats.jsx'
import PlayerFormChart from './player/PlayerFormChart.jsx'
import PlayerPercentiles from './player/PlayerPercentiles.jsx'
import PlayerExpected from './player/PlayerExpected.jsx'
import PlayerTeamShare from './player/PlayerTeamShare.jsx'
import PlayerSeasonGroups from './player/PlayerSeasonGroups.jsx'
import PlayerMatchRow from './player/PlayerMatchRow.jsx'
import { PitchIcon, SectionHeader } from './player/PlayerSectionIcons.jsx'
import './PlayerDetailPage.css'

const dateLabel = (date) => new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC',
}).format(new Date(`${date}T00:00:00Z`))
const numberLabel = (value) => value == null ? '—' : String(value)
const isHome = (row) => row.clubId === row.match.homeClubId
const footLabel = { LEFT: 'Chân trái', RIGHT: 'Chân phải', BOTH: 'Hai chân' }
const pendingLabel = { SCHEDULED: 'Chưa diễn ra', POSTPONED: 'Hoãn', SUSPENDED: 'Tạm dừng', LIVE: 'Đang diễn ra' }

function initialTabFromHash() {
  const query = window.location.hash.split('?')[1] ?? ''
  return normalizePlayerTab(new URLSearchParams(query).get('tab'))
}

function Empty({ title, children }) {
  return <div className="pd-empty"><strong>{title}</strong><p>{children}</p></div>
}

function MatchesTab({ rows, season, roleGroup }) {
  if (!rows.length) {
    return (
      <section className="pd-card pd-sec">
        <Empty title="Chưa có trận đã hoàn thành">
          Chưa có trận Premier League đã hoàn thành thuộc thời gian cầu thủ ở CLB trong mùa {SEASONS[season]}.
        </Empty>
      </section>
    )
  }
  const columns = roleMatchColumns(roleGroup, season)
  return (
    <section className="pd-card pd-sec">
      <SectionHeader icon={<PitchIcon />} title="Trận đã đấu" lead="Mới nhất ở trên" />
      <ul className="pd-mlist">
        {rows.map((row) => (
          <PlayerMatchRow key={row.match.id} row={row} season={season} columns={columns} />
        ))}
      </ul>
    </section>
  )
}

function FixtureCard({ row, season, featured = false }) {
  const home = isHome(row)
  const opponent = home ? row.match.awayClub : row.match.homeClub
  return (
    <div className={featured ? 'pd-next-featured' : 'pd-next-row'}>
      {featured && <span className="pd-next-tag">TRẬN TIẾP THEO</span>}
      <div className="pd-next-content">
        <span className="pd-club-initials" aria-hidden="true">{getInitials(opponent)}</span>
        <div>
          <strong>{opponent}</strong>
          <span>{home ? 'Sân nhà' : 'Sân khách'} · GW{row.match.matchweek}</span>
        </div>
        <div className="pd-next-date">
          <strong>
            <MatchKickoff
              match={row.match}
              season={season}
              legacyLabel={season === 2024 ? dateLabel(row.match.date) : undefined}
            />
          </strong>
          <small>{pendingLabel[row.match.status] ?? row.match.status}</small>
        </div>
      </div>
    </div>
  )
}

function NextTab({ rows, season }) {
  if (!rows.length) {
    return (
      <section className="pd-card pd-sec">
        <Empty title="Chưa có lịch thi đấu sắp tới">
          Hiện chưa có fixture sắp tới thuộc thời gian cầu thủ ở CLB trong mùa {SEASONS[season]}.
        </Empty>
      </section>
    )
  }
  return (
    <section className="pd-card pd-sec">
      <div className="pd-panel-head">
        <div>
          <h2>Trận sắp tới</h2>
          <p>Premier League · {SEASONS[season]} · lịch thi đấu có thể thay đổi</p>
        </div>
      </div>
      <FixtureCard row={rows[0]} season={season} featured />
      {rows.slice(1).map((row) => <FixtureCard key={row.match.id} row={row} season={season} />)}
      <p className="pd-note">Đây là lịch của CLB trong thời gian membership đã lưu, không phải xác nhận cầu thủ sẽ ra sân.</p>
    </section>
  )
}

function InfoTab({ player, season }) {
  const estimated = ESTIMATED_OVR_PLAYER_IDS.has(player.id)
  const details = [
    [season === 2026 ? 'OVR FC 27' : 'OVR', player.fc27Overall,
      estimated ? 'Ước tính prismaXI · chưa đủ điều kiện Fantasy'
        : player.fc27Overall == null ? 'Chưa có OVR' : 'Chỉ số đang lưu'],
    ['Quốc tịch', player.nationality],
    ['Ngày sinh', player.birthDate ? dateLabel(player.birthDate) : null],
    ['Chân thuận', footLabel[player.preferredFoot] ?? null],
    ['Chiều cao', player.heightCm == null ? null : `${player.heightCm} cm`],
    ['Số áo', player.shirtNumber == null ? null : `#${player.shirtNumber}`],
  ]
  return (
    <section className="pd-card pd-sec">
      <div className="pd-panel-head">
        <div>
          <h2>Thông tin cầu thủ</h2>
          <p>Hồ sơ theo mùa {SEASONS[season]}</p>
        </div>
      </div>
      <div className="pd-info-grid">
        {details.map(([label, value, note]) => (
          <div className="pd-info-item" key={label}>
            <span>{label}</span>
            <strong className={value == null ? 'pd-unknown' : ''}>{value == null ? 'Chưa có dữ liệu' : value}</strong>
            {note && <small>{note}</small>}
          </div>
        ))}
      </div>
    </section>
  )
}

function OverviewTab({ player, season, profile, formSeries, effectiveTotals }) {
  const is2026 = season === 2026
  const roleGroup = profile?.roleGroup ?? 'MIXED'
  const isAttackOrMixed = roleGroup === 'ATTACK' || roleGroup === 'MIXED'

  return (
    <div className="pd-overview">
      <PlayerFormChart formSeries={formSeries} averageRating={effectiveTotals?.averageRating ?? null} />

      {is2026 && profile && (
        <div className="pd-grid2">
          <PlayerPercentiles
            percentiles={profile.percentiles}
            comparisonGroupLabel={profile.comparisonGroupLabel}
          />
          <div className="pd-col">
            {isAttackOrMixed ? (
              <>
                <PlayerExpected totals={effectiveTotals} />
                <PlayerTeamShare
                  mode="team"
                  clubName={player.club}
                  playerName={player.name}
                  teamContribution={profile.teamContribution}
                />
              </>
            ) : (
              <>
                <PlayerTeamShare
                  mode="cleanSheets"
                  clubName={player.club}
                  playerName={player.name}
                  cleanSheets={profile.cleanSheets}
                />
                <PlayerTeamShare
                  mode={roleGroup === 'GK' ? 'saveRate' : 'defensiveActions'}
                  totals={effectiveTotals}
                />
              </>
            )}
          </div>
        </div>
      )}

      <PlayerSeasonGroups totals={effectiveTotals} roleGroup={roleGroup} season={season} />
    </div>
  )
}

function PlayerDetailPage({ playerId, season, backHash, theme = 'dark' }) {
  const [tab, setTab] = useState(initialTabFromHash)
  const [reloadKey, setReloadKey] = useState(0)
  const [data, setData] = useState({ status: 'loading', player: null, rows: [], profile: null, error: '' })

  useEffect(() => {
    const controller = new AbortController()
    Promise.all([
      fetchPlayer(playerId, season, controller.signal),
      fetchPlayerMatches(playerId, season, controller.signal),
      fetchPlayerSeasonProfile(playerId, season, controller.signal),
    ])
      .then(([player, rows, profile]) => {
        if (!controller.signal.aborted) {
          setData({ status: 'success', player, rows, profile, error: '' })
        }
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setData({ status: 'error', player: null, rows: [], profile: null, error: error.message })
        }
      })
    return () => controller.abort()
  }, [playerId, season, reloadKey])

  const { status, player, rows, profile, error } = data

  const finished = useMemo(() => rows
    .filter((row) => row.match.status === 'FINISHED')
    .sort((a, b) => season === 2026
      ? compareMatchSchedule(b.match, a.match, season)
      : b.match.date.localeCompare(a.match.date)), [rows, season])

  const next = useMemo(() => rows
    .filter((row) => ['SCHEDULED', 'POSTPONED', 'SUSPENDED', 'LIVE'].includes(row.match.status))
    .sort((a, b) => season === 2026
      ? compareMatchSchedule(a.match, b.match, season)
      : a.match.date.localeCompare(b.match.date)), [rows, season])

  const formSeries = useMemo(() => buildFormSeries(finished, season), [finished, season])

  const effectiveTotals = useMemo(() => {
    if (profile?.totals) return profile.totals
    const fallback = seasonSummary(finished)
    const verifiedFinished = finished.filter(
      (row) => row.dataStatus !== 'PROVISIONAL_FPL' &&
        (row.stats != null || row.match?.hasManualStats || row.evidenceStatus === 'VERIFIED'),
    )
    return {
      appearances: fallback.appearances,
      clubFinishedMatches: verifiedFinished.length,
      minutes: fallback.totalMinutes,
      goals: fallback.goals,
      assists: fallback.assists,
      yellowCards: fallback.yellowCards,
      redCards: fallback.redCards,
      averageRating: fallback.averageRating,
      ratedMatches: fallback.ratedMatches.length,
    }
  }, [profile, finished])

  const tabs = [
    ['overview', 'Tổng quan', null],
    ['matches', 'Trận đã đấu', finished.length],
    ['next', 'Sắp tới', next.length],
    ['info', 'Thông tin', null],
  ]

  const estimated = player && ESTIMATED_OVR_PLAYER_IDS.has(player.id)
  const backLabel = backHash?.startsWith('#matches/') ? 'Trở lại trận đấu'
    : backHash?.startsWith('#clubs/') ? 'Trở lại câu lạc bộ'
      : backHash === '#fantasy' ? 'Trở lại Fantasy' : 'Danh sách cầu thủ'

  const clubColors = clubGradient(player?.club)
  const groupPosLabel = player ? (GROUP_LABEL[player.position] ?? player.position) : ''
  const posChipLabel = player?.primaryPosition
    ? `${groupPosLabel} · ${player.primaryPosition}`
    : groupPosLabel

  const eyebrow = player
    ? `PREMIER LEAGUE · ${SEASONS[season]}${player.shirtNumber != null ? ` · SỐ ÁO ${player.shirtNumber}` : ''}`
    : ''

  return (
    <section
      className="player-detail-page"
      data-theme={theme}
      data-mg-theme={theme}
      style={{ '--pd-club': clubColors.primary, '--pd-club-dark': clubColors.secondary }}
    >
      <div className="pd-wrap">
        <div className="pd-topbar">
          <a className="pd-back" href={backHash ?? '#players'}>← {backLabel}</a>
        </div>

        {status === 'loading' && (
          <div className="pd-card pd-loading" role="status">Đang tải chi tiết cầu thủ...</div>
        )}

        {status === 'error' && (
          <div className="pd-card pd-empty" role="alert">
            <strong>Không thể tải cầu thủ</strong>
            <p>{error}</p>
            <button
              type="button"
              onClick={() => {
                setData({ status: 'loading', player: null, rows: [], profile: null, error: '' })
                setReloadKey((value) => value + 1)
              }}
            >
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && player && (
          <>
            <header className="pd-card pd-hero">
              <div className="pd-avatar" aria-label="Chữ viết tắt tên cầu thủ">
                {getInitials(player.name)}
              </div>
              <div className="pd-identity">
                <p>{eyebrow}</p>
                <h1>{player.name}</h1>
                <div className="pd-hero-chips">
                  <span className="pd-chip">{player.club}</span>
                  <span className="pd-chip pd-chip-pos">{posChipLabel}</span>
                </div>
              </div>
              <div className="pd-hero-ovr">
                <span>{numberLabel(player.fc27Overall)}</span>
                <small>{estimated ? 'OVR ước tính' : season === 2026 ? 'OVR FC 27' : 'OVR'}</small>
              </div>
              <PlayerHeroStats totals={effectiveTotals} formItems={formSeries.items} />
            </header>

            <nav className="pd-tabs" role="tablist" aria-label="Thông tin cầu thủ">
              {tabs.map(([key, label, count]) => (
                <button
                  key={key}
                  type="button"
                  role="tab"
                  id={`pd-tab-${key}`}
                  aria-controls={`pd-panel-${key}`}
                  aria-selected={tab === key}
                  onClick={() => setTab(key)}
                >
                  {label}
                  {count !== null && <span className="pd-tab-count">{count}</span>}
                </button>
              ))}
            </nav>

            <div role="tabpanel" id={`pd-panel-${tab}`} aria-labelledby={`pd-tab-${tab}`}>
              {tab === 'overview' && (
                <OverviewTab
                  player={player}
                  season={season}
                  profile={profile}
                  formSeries={formSeries}
                  effectiveTotals={effectiveTotals}
                />
              )}
              {tab === 'matches' && (
                <MatchesTab rows={finished} season={season} roleGroup={profile?.roleGroup ?? 'MIXED'} />
              )}
              {tab === 'next' && <NextTab rows={next} season={season} />}
              {tab === 'info' && <InfoTab player={player} season={season} />}
            </div>

            <p className="pd-footer">
              Lịch thi đấu và kết quả: <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>. Hồ sơ và thống kê hiển thị theo dữ liệu prismaXI đã lưu.
            </p>
          </>
        )}
      </div>
    </section>
  )
}

export default PlayerDetailPage
