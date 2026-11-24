import { useEffect, useState } from 'react'
import { clubVisual, matchPlayerState, matchRating, shortPlayerName } from '../utils/matchView.js'
import { hasAnyExtraStats, playerStatValue, sortPlayers, teamTotals, topValues } from '../utils/matchStats.js'

const POSITIONS = { G: 'GK', D: 'DEF', M: 'MID', F: 'FWD' }

const COMPARE_ROWS = [
  { key: 'averageRating', label: 'Rating trung bình', decimals: 1, lowBetter: false },
  { key: 'expectedGoals', label: 'Bàn thắng kỳ vọng (xG)', decimals: 2, lowBetter: false },
  { key: 'expectedAssists', label: 'Kiến tạo kỳ vọng (xA)', decimals: 2, lowBetter: false },
  { key: 'saves', label: 'Cứu thua', decimals: 0, lowBetter: false },
  { key: 'tackles', label: 'Tắc bóng', decimals: 0, lowBetter: false },
  { key: 'clearancesBlocksInterceptions', label: 'Phá bóng, chặn, cắt bóng', decimals: 0, lowBetter: false },
  { key: 'recoveries', label: 'Đoạt lại bóng', decimals: 0, lowBetter: false },
  { key: 'yellowCards', label: 'Thẻ vàng', decimals: 0, lowBetter: true },
]

const TABLE_COLS = [
  { key: 'minutes', label: 'Phút', decimals: 0, highlightTop: false },
  { key: 'rating', label: 'Rating', decimals: 1, highlightTop: false },
  { key: 'goals', label: 'Bàn', decimals: 0, highlightTop: true },
  { key: 'assists', label: 'KT', decimals: 0, highlightTop: true },
  { key: 'expectedGoals', label: 'xG', decimals: 2, highlightTop: true },
  { key: 'expectedAssists', label: 'xA', decimals: 2, highlightTop: true },
  { key: 'tackles', label: 'Tắc', decimals: 0, highlightTop: true },
  { key: 'clearancesBlocksInterceptions', label: 'PCC', decimals: 0, highlightTop: true },
  { key: 'recoveries', label: 'Đoạt', decimals: 0, highlightTop: true },
  { key: 'saves', label: 'Cứu', decimals: 0, highlightTop: true },
  { key: 'bps', label: 'BPS', decimals: 0, highlightTop: true },
]

const TOP_KEYS = TABLE_COLS.filter((col) => col.highlightTop).map((col) => col.key)

function ratingTier(value) {
  return value == null ? 'na' : value >= 9 ? 'blue' : value >= 7 ? 'green' : value >= 5 ? 'orange' : 'red'
}

function formatNum(value, decimals = 0) {
  if (value == null) return '—'
  return decimals > 0 ? Number(value).toFixed(decimals) : String(value)
}

function initialsOf(name) {
  const parts = (name ?? '').replace(/\./g, '').split(/\s+/).filter(Boolean)
  if (parts.length < 2) return (name ?? '').slice(0, 2).toUpperCase()
  return `${parts[0][0]}${parts.at(-1)[0]}`.toUpperCase()
}

function subtitleText(player) {
  const pos = player.matchPosition ?? POSITIONS[player.position] ?? player.position ?? '—'
  if (player.role === 'SUB_USED' && player.substitutionInMinute != null) {
    return `${pos} · vào ${player.substitutionInMinute}′`
  }
  if (player.substitutionOutMinute != null) {
    return `${pos} · ra ${player.substitutionOutMinute}′`
  }
  return pos
}

function MatchStats({ match, homePlayers, awayPlayers, evidenceStatus, isProvisional = false, onSelectPlayer }) {
  const [side, setSide] = useState('home')
  const [sortKey, setSortKey] = useState(isProvisional ? 'bps' : 'rating')
  const [sortDir, setSortDir] = useState('desc')

  useEffect(() => {
    setSortKey(isProvisional ? 'bps' : 'rating')
    setSortDir('desc')
  }, [match.id, isProvisional])

  const homeColor = clubVisual(match.homeClub).color
  const awayColor = clubVisual(match.awayClub).color
  const homeTotals = teamTotals(homePlayers, evidenceStatus)
  const awayTotals = teamTotals(awayPlayers, evidenceStatus)
  const hasExtra = hasAnyExtraStats(homePlayers) || hasAnyExtraStats(awayPlayers)

  const visibleCompareRows = COMPARE_ROWS.filter(
    (row) => !(isProvisional && row.key === 'averageRating') &&
      (homeTotals[row.key] != null || awayTotals[row.key] != null),
  )

  const activeClub = side === 'home' ? match.homeClub : match.awayClub
  const activeColor = side === 'home' ? homeColor : awayColor
  const activePlayers = side === 'home' ? homePlayers : awayPlayers
  const played = sortPlayers(
    activePlayers.filter((player) => matchPlayerState(player) === 'played'),
    sortKey,
    sortDir,
    evidenceStatus,
  )
  const dnp = activePlayers.filter((player) => matchPlayerState(player) !== 'played')
  const tops = topValues(played, TOP_KEYS, evidenceStatus)

  function handleSort(key) {
    if (sortKey === key) {
      setSortDir((prev) => (prev === 'desc' ? 'asc' : 'desc'))
    } else {
      setSortKey(key)
      setSortDir('desc')
    }
  }

  return (
    <div className="mx-stats">
      <section className="mx-block">
        <h3>So sánh hai đội</h3>
        <div className="mx-cmp-head">
          <span>
            <i className="mx-cmp-dot" style={{ background: homeColor }} aria-hidden="true" />
            {match.homeClub}
          </span>
          <span aria-hidden="true" />
          <span>
            {match.awayClub}
            <i className="mx-cmp-dot mx-cmp-dot-away" style={{ background: awayColor }} aria-hidden="true" />
          </span>
        </div>

        {!hasExtra && (
          <p className="mx-stats-empty-note" role="status">
            Chưa có dữ liệu thống kê mở rộng cho trận này
          </p>
        )}

        {visibleCompareRows.map(({ key, label, decimals, lowBetter }) => {
          const h = homeTotals[key]
          const a = awayTotals[key]
          const hNum = h ?? 0
          const aNum = a ?? 0
          const total = hNum + aNum
          const hPct = total > 0 ? (hNum / total) * 100 : 0
          const aPct = total > 0 ? (aNum / total) * 100 : 0
          const bothComparable = h != null && a != null && h !== a
          const homeWins = bothComparable && (lowBetter ? h < a : h > a)
          const awayWins = bothComparable && (lowBetter ? a < h : a > h)
          return (
            <div className="mx-cmp-row" key={key}>
              <span className={`mx-cmp-val${homeWins ? ' mx-cmp-win' : ''}`}>
                {formatNum(h, decimals)}
              </span>
              <div className="mx-cmp-mid">
                <div className="mx-cmp-lbl">{label}</div>
                <div className="mx-cmp-bars" aria-hidden="true">
                  <span className="mx-cmp-bar mx-cmp-bar-l">
                    <i style={{ width: `${hPct}%`, background: homeColor }} />
                  </span>
                  <span className="mx-cmp-bar mx-cmp-bar-r">
                    <i style={{ width: `${aPct}%`, background: awayColor }} />
                  </span>
                </div>
              </div>
              <span className={`mx-cmp-val mx-cmp-val-r${awayWins ? ' mx-cmp-win' : ''}`}>
                {formatNum(a, decimals)}
              </span>
            </div>
          )
        })}

        <p className="mx-stats-sub">
          Tổng từ các cầu thủ đã ra sân. xG/xA: khả năng một cú sút thành bàn / một đường chuyền thành kiến tạo.
        </p>
      </section>

      <section className="mx-block">
        <h3>Thống kê cầu thủ</h3>
        <div className="mx-tbl-top">
          <div className="mx-seg" role="group" aria-label="Chọn đội xem thống kê cầu thủ">
            <button type="button" aria-pressed={side === 'home'} onClick={() => setSide('home')}>
              {match.homeClub}
            </button>
            <button type="button" aria-pressed={side === 'away'} onClick={() => setSide('away')}>
              {match.awayClub}
            </button>
          </div>
          <span className="mx-tbl-legend">
            KT: kiến tạo · PCC: phá bóng, chặn, cắt · Đoạt: đoạt lại bóng · BPS: điểm hiệu suất FPL
          </span>
        </div>

        <div className="mx-tbl-scroll">
          <table className="mx-stats-table">
            <thead>
              <tr>
                <th scope="col">Cầu thủ</th>
                {TABLE_COLS.map((col) => {
                  const active = sortKey === col.key
                  const ariaSort = active ? (sortDir === 'desc' ? 'descending' : 'ascending') : 'none'
                  return (
                    <th key={col.key} scope="col" aria-sort={ariaSort} className="mx-th-sortable">
                      <button type="button" className="mx-sort-btn" onClick={() => handleSort(col.key)}>
                        {col.label}
                        {active && <span aria-hidden="true">{sortDir === 'desc' ? ' ↓' : ' ↑'}</span>}
                      </button>
                    </th>
                  )
                })}
              </tr>
            </thead>
            <tbody>
              {played.map((player) => {
                const rating = matchRating(player)
                return (
                  <tr
                    key={player.playerId}
                    className="mx-stats-row"
                    tabIndex={0}
                    onClick={(event) => onSelectPlayer?.(player, activeClub, event.currentTarget)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        onSelectPlayer?.(player, activeClub, event.currentTarget)
                      }
                    }}
                  >
                    <td>
                      <span className="mx-tbl-player">
                        <span className="mx-tbl-av" style={{ background: activeColor }} aria-hidden="true">
                          {initialsOf(player.playerName)}
                        </span>
                        <span>
                          <b title={player.playerName}>{shortPlayerName(player.playerName)}</b>
                          <small>{subtitleText(player)}</small>
                        </span>
                      </span>
                    </td>
                    {TABLE_COLS.map((col) => {
                      if (col.key === 'rating') {
                        return (
                          <td key={col.key}>
                            <span className={`mx-tbl-rt mx-rating-${ratingTier(rating)}`}>
                              {rating == null ? '—' : rating.toFixed(1)}
                            </span>
                          </td>
                        )
                      }
                      const value = playerStatValue(player, col.key, evidenceStatus)
                      const isTop = col.highlightTop && value != null && tops[col.key] != null && value === tops[col.key]
                      const isZero = value === 0
                      return (
                        <td key={col.key} className={isTop ? 'mx-td-top' : isZero ? 'mx-td-zero' : undefined}>
                          {formatNum(value, col.decimals)}
                        </td>
                      )
                    })}
                  </tr>
                )
              })}
              {dnp.length > 0 && (
                <tr className="mx-stats-dnp-row">
                  <td colSpan={TABLE_COLS.length + 1}>
                    Không ra sân: {dnp.map((player) => shortPlayerName(player.playerName)).join(', ')}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <p className="mx-stats-sub">
          Bấm tiêu đề cột để sắp xếp · bấm một cầu thủ để xem chi tiết. Số đậm màu nhấn là cao nhất đội.
        </p>
      </section>
    </div>
  )
}

export default MatchStats
