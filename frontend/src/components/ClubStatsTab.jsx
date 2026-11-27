import { useEffect, useMemo, useRef, useState } from 'react'
import {
  buildContributionPieSlices,
  buildXAxisTicks,
  cumulativePointsSeries,
  formatSignedDelta,
  normalizeMatchesFromFixtures,
  normalizeMatchesFromTrends,
  shortSurname,
  splitHomeAway,
  summarizeResults,
  summarizeXgTotals,
} from '../utils/clubStats.js'
import { squadRatingBand } from '../utils/clubView.js'
import './ClubStatsTab.css'

const PIE_COLORS = ['var(--cx-accent)', 'var(--cst-line2)', '#f2b84b', '#b48cf2', '#7d8d93']

function ratingDotColor(v) {
  if (v >= 7.2) return '#2fc58e'
  if (v >= 6.9) return '#9bd36a'
  if (v >= 6.6) return '#e7c84d'
  return '#ee8a4f'
}

function useChartWidth(ref, active) {
  const [width, setWidth] = useState(480)
  useEffect(() => {
    if (!active) return undefined
    const el = ref.current
    if (!el) return undefined
    const measure = () => {
      const next = Math.round(el.clientWidth)
      if (next > 0) setWidth(prev => (prev === next ? prev : next))
    }
    measure()
    const rafId = typeof requestAnimationFrame === 'function' ? requestAnimationFrame(measure) : null
    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver(measure)
      observer.observe(el)
      return () => {
        if (rafId != null) cancelAnimationFrame(rafId)
        observer.disconnect()
      }
    }
    window.addEventListener('resize', measure)
    return () => {
      if (rafId != null) cancelAnimationFrame(rafId)
      window.removeEventListener('resize', measure)
    }
  }, [ref, active])
  return width
}

function MatchChart({
  items,
  height = 186,
  yMin,
  yMax,
  yGrid,
  formatY = v => String(v),
  leftPad = 28,
  ariaLabel,
  renderPlot,
  formatTooltip,
}) {
  const wrapRef = useRef(null)
  const hasItems = Boolean(items && items.length > 0)
  const width = useChartWidth(wrapRef, hasItems)
  const [hovered, setHovered] = useState(null)
  const [pinned, setPinned] = useState(null)

  useEffect(() => {
    if (pinned == null) return undefined
    const onOutside = event => {
      if (wrapRef.current && !wrapRef.current.contains(event.target)) {
        setPinned(null)
        setHovered(null)
      }
    }
    document.addEventListener('pointerdown', onOutside)
    return () => document.removeEventListener('pointerdown', onOutside)
  }, [pinned])

  if (!items || items.length === 0) {
    return <p className="cst-empty">Chưa có dữ liệu trận đấu.</p>
  }

  const rightPad = 8
  const topPad = 18
  const plotWidth = Math.max(120, width - leftPad - rightPad)
  const axis = buildXAxisTicks(items, plotWidth)
  const bottomPad = axis.showOpponent ? 34 : 22
  const slotWidth = plotWidth / items.length
  const yScale = v => topPad + (height - bottomPad - topPad) * (1 - (v - yMin) / (yMax - yMin || 1))

  const activeIndex = pinned ?? hovered
  const activeItem = activeIndex != null ? items[activeIndex] : null
  const activeTip = activeItem ? formatTooltip(activeItem, activeIndex) : null
  const activeCenterX = activeIndex != null ? leftPad + slotWidth * (activeIndex + 0.5) : 0
  const clampedTipX = Math.min(Math.max(activeCenterX, 102), Math.max(102, width - 102))

  return (
    <div className="cst-ch" ref={wrapRef}>
      <svg
        viewBox={`0 0 ${width} ${height}`}
        height={height}
        role="img"
        aria-label={ariaLabel}
        onMouseLeave={() => setHovered(null)}
      >
        {yGrid.map(tick => {
          const y = yScale(tick)
          return (
            <g key={tick}>
              <line className="cst-gl" x1={leftPad} x2={width - rightPad} y1={y} y2={y} />
              <text className="cst-ax" x={leftPad - 6} y={y + 3} textAnchor="end">
                {formatY(tick)}
              </text>
            </g>
          )
        })}

        {activeIndex != null && (
          <rect
            x={leftPad + slotWidth * activeIndex}
            y={topPad}
            width={slotWidth}
            height={height - bottomPad - topPad}
            rx="4"
            className="cst-col-hover"
          />
        )}

        {renderPlot({ yScale, slotWidth, leftPad, rightPad, plotWidth, topPad, bottomPad, height, axis })}

        {axis.ticks.map(tick => (
          <g key={tick.index}>
            <text
              className="cst-ax"
              x={leftPad + tick.centerOffset}
              y={axis.showOpponent ? height - 18 : height - 7}
              textAnchor="middle"
            >
              {tick.label}
            </text>
            {axis.showOpponent && tick.sublabel && (
              <text
                className="cst-ax cst-ax-opp"
                x={leftPad + tick.centerOffset}
                y={height - 6}
                textAnchor="middle"
              >
                {tick.sublabel}
              </text>
            )}
          </g>
        ))}

        {items.map((item, idx) => (
          <rect
            key={item.fixtureId ?? item.matchweek ?? idx}
            x={leftPad + slotWidth * idx}
            y={0}
            width={slotWidth}
            height={height - bottomPad}
            fill="transparent"
            className="cst-hit"
            onMouseEnter={() => { if (pinned == null) setHovered(idx) }}
            onClick={() => setPinned(prev => (prev === idx ? null : idx))}
          />
        ))}
      </svg>

      {activeTip && (
        <div
          className="cst-tip"
          role="tooltip"
          style={{ left: `${clampedTipX}px` }}
        >
          <b>{activeTip.title}</b>
          <span>{activeTip.detail}</span>
        </div>
      )}
    </div>
  )
}

function ResultsDonut({ summary, points }) {
  const total = summary.played
  const r = 15.9
  const circ = 2 * Math.PI * r
  const segments = [
    { key: 'W', label: 'Thắng', count: summary.won, color: 'var(--cst-w)' },
    { key: 'D', label: 'Hòa', count: summary.drawn, color: 'var(--cst-d)' },
    { key: 'L', label: 'Thua', count: summary.lost, color: 'var(--cst-l)' },
  ]
  const nonZeroCount = segments.filter(s => s.count > 0).length
  const gap = nonZeroCount > 1 ? 0.8 : 0

  let offset = 0
  return (
    <div className="cst-donut">
      <svg width="132" height="132" viewBox="0 0 42 42" role="img" aria-label={`Kết quả: ${summary.won} thắng, ${summary.drawn} hòa, ${summary.lost} thua`}>
        {total === 0 ? (
          <circle r={r} cx="21" cy="21" fill="none" stroke="var(--cx-chip)" strokeWidth="5" />
        ) : (
          segments.map(seg => {
            if (seg.count === 0) return null
            const len = (seg.count / total) * circ
            const visible = Math.max(0.2, len - gap)
            const curOffset = offset
            offset += len
            return (
              <circle
                key={seg.key}
                r={r}
                cx="21"
                cy="21"
                fill="none"
                stroke={seg.color}
                strokeWidth="5"
                strokeDasharray={`${visible} ${circ - visible}`}
                strokeDashoffset={-curOffset}
                transform="rotate(-90 21 21)"
              />
            )
          })
        )}
        <text x="21" y="21" textAnchor="middle" fontSize="8" fontWeight="800" fill="currentColor">
          {points}
        </text>
        <text x="21" y="27" textAnchor="middle" fontSize="3.4" fill="var(--cx-muted)">
          điểm
        </text>
      </svg>
      <div className="cst-lg">
        {segments.map(seg => (
          <span key={seg.key}>
            <i style={{ background: seg.color }} />
            {seg.label}
            <b>{seg.count}</b>
          </span>
        ))}
      </div>
    </div>
  )
}

function ContributionDonut({ title, pie }) {
  const wrapRef = useRef(null)
  const [activeIdx, setActiveIdx] = useState(null)

  useEffect(() => {
    if (activeIdx == null) return undefined
    const onOutside = event => {
      if (wrapRef.current && !wrapRef.current.contains(event.target)) {
        setActiveIdx(null)
      }
    }
    document.addEventListener('pointerdown', onOutside)
    return () => document.removeEventListener('pointerdown', onOutside)
  }, [activeIdx])

  if (!pie || pie.total === 0 || pie.slices.length === 0) {
    return (
      <div className="cst-pie-group">
        <h4>{title} · 0</h4>
        <p className="cst-empty">Chưa có dữ liệu.</p>
      </div>
    )
  }

  const R = 52
  const r0 = 32
  const cx = 60
  const cy = 60
  const leader = pie.slices[0]
  const activeSlice = activeIdx != null ? pie.slices[activeIdx] : null

  let angle = -Math.PI / 2
  const paths = pie.slices.map((slice, idx) => {
    const color = slice.isOther ? PIE_COLORS[4] : PIE_COLORS[idx % 4]
    const span = (slice.value / pie.total) * 2 * Math.PI
    const a0 = angle
    const a1 = angle + span
    angle = a1

    if (pie.slices.length === 1) {
      return { slice, color, fullRing: true, d: null }
    }
    const g = 0.025
    const s0 = a0 + g
    const s1 = Math.max(s0 + 0.01, a1 - g)
    const largeArc = s1 - s0 > Math.PI ? 1 : 0
    const pt = (a, rad) => [cx + rad * Math.cos(a), cy + rad * Math.sin(a)]
    const [x1, y1] = pt(s0, R)
    const [x2, y2] = pt(s1, R)
    const [x3, y3] = pt(s1, r0)
    const [x4, y4] = pt(s0, r0)
    const d = `M${x1} ${y1} A${R} ${R} 0 ${largeArc} 1 ${x2} ${y2} L${x3} ${y3} A${r0} ${r0} 0 ${largeArc} 0 ${x4} ${y4}Z`
    return { slice, color, fullRing: false, d }
  })

  const sliceTitle = slice => (
    slice.tooltipMembers.length > 0
      ? `${slice.label}: ${slice.value} (${slice.tooltipMembers.join(', ')})`
      : `${slice.fullName}: ${slice.value} (${slice.percent}%)`
  )

  return (
    <div className="cst-pie-group" ref={wrapRef}>
      <h4>{title} · {pie.total}</h4>
      <div className="cst-pie">
        <svg width="120" height="120" viewBox="0 0 120 120" role="img" aria-label={`${title}: ${pie.total}`}>
          {paths.map((item, idx) => (
            item.fullRing ? (
              <circle
                key={item.slice.label}
                cx={cx}
                cy={cy}
                r={(R + r0) / 2}
                fill="none"
                stroke={item.color}
                strokeWidth={R - r0}
                className="cst-pie-slice"
                onMouseEnter={() => setActiveIdx(idx)}
                onMouseLeave={() => setActiveIdx(null)}
                onClick={() => setActiveIdx(prev => (prev === idx ? null : idx))}
              >
                <title>{sliceTitle(item.slice)}</title>
              </circle>
            ) : (
              <path
                key={item.slice.label}
                d={item.d}
                fill={item.color}
                className={`cst-pie-slice${activeIdx != null && activeIdx !== idx ? ' cst-pie-dim' : ''}`}
                onMouseEnter={() => setActiveIdx(idx)}
                onMouseLeave={() => setActiveIdx(null)}
                onClick={() => setActiveIdx(prev => (prev === idx ? null : idx))}
              >
                <title>{sliceTitle(item.slice)}</title>
              </path>
            )
          ))}
          <text x="60" y="57" textAnchor="middle" fontSize="20" fontWeight="800" fill="var(--cx-text)">
            {leader.percent}%
          </text>
          <text x="60" y="73" textAnchor="middle" fontSize="9.5" fill="var(--cx-muted)">
            {leader.label}
          </text>
        </svg>

        <div className="cst-pl">
          {paths.map((item, idx) => (
            <button
              type="button"
              key={item.slice.label}
              className={`cst-pl-row${activeIdx === idx ? ' cst-pl-active' : ''}`}
              title={sliceTitle(item.slice)}
              onMouseEnter={() => setActiveIdx(idx)}
              onMouseLeave={() => setActiveIdx(null)}
              onClick={() => setActiveIdx(prev => (prev === idx ? null : idx))}
            >
              <i style={{ background: item.color }} />
              <span className="cst-pl-name">{item.slice.label}</span>
              <b>{item.slice.value}</b>
              <em>{item.slice.percent}%</em>
            </button>
          ))}
        </div>

        {activeSlice && (
          <div className="cst-pie-tip" role="tooltip">
            <b>{activeSlice.label}: {activeSlice.value} ({activeSlice.percent}%)</b>
            {activeSlice.tooltipMembers.length > 0 && (
              <span>{activeSlice.tooltipMembers.join(', ')}</span>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function nicePointsAxis(maxPoints) {
  if (maxPoints <= 24) return { max: 24, grid: [0, 6, 12, 18, 24] }
  const step = Math.ceil(maxPoints / 4 / 3) * 3
  const max = step * 4
  return { max, grid: [0, step, step * 2, step * 3, max] }
}

function niceGoalsAxis(maxGoals) {
  const max = Math.max(4, Math.ceil(maxGoals / 2) * 2)
  const half = max / 2
  return { min: -max, max, grid: [-max, -half, 0, half, max] }
}

function niceXgAxis(maxVal) {
  const max = Math.max(5, Math.ceil(maxVal))
  const step = max <= 6 ? 1 : Math.ceil(max / 5)
  const top = step * Math.ceil(max / step)
  const grid = []
  for (let v = 0; v <= top; v += step) grid.push(v)
  return { max: top, grid }
}

function niceRatingAxis(values) {
  let min = 6
  let max = 8
  for (const v of values) {
    if (v < min) min = Math.floor(v * 2) / 2
    if (v > max) max = Math.ceil(v * 2) / 2
  }
  const grid = []
  for (let v = min; v <= max + 0.001; v += 0.5) {
    grid.push(Math.round(v * 10) / 10)
  }
  return { min, max, grid }
}

export default function ClubStatsTab({
  season,
  clubId,
  standing,
  doneMatches,
  statsResource,
  trendsResource,
}) {
  const [contribView, setContribView] = useState('bar')
  const is2026 = season === 2026
  const stats = statsResource?.data ?? null
  const trends = trendsResource?.data ?? null

  const matches = useMemo(() => {
    if (is2026 && trends?.matches?.length) {
      return normalizeMatchesFromTrends(trends.matches)
    }
    return normalizeMatchesFromFixtures(doneMatches, clubId)
  }, [is2026, trends, doneMatches, clubId])

  const summary = useMemo(() => summarizeResults(matches), [matches])
  const homeAway = useMemo(() => splitHomeAway(matches), [matches])
  const cumSeries = useMemo(() => cumulativePointsSeries(matches), [matches])

  const totalPlayed = standing?.played ?? summary.played
  const totalPoints = standing?.points ?? summary.points
  const totalGoalsFor = standing?.goalsFor ?? summary.goalsFor
  const totalGoalsAgainst = standing?.goalsAgainst ?? summary.goalsAgainst

  const ratingBand = squadRatingBand(stats?.averageRating)
  const ptsAxis = nicePointsAxis(Math.max(0, ...cumSeries.map(p => p.cumulativePoints)))
  const goalsAxis = niceGoalsAxis(Math.max(0, ...matches.flatMap(m => [m.goalsFor, m.goalsAgainst])))

  const ratedValues = matches.filter(m => m.teamRating != null).map(m => m.teamRating)
  const ratingAxis = niceRatingAxis(
    stats?.averageRating != null ? [...ratedValues, stats.averageRating] : ratedValues,
  )

  const xgForSummary = useMemo(() => summarizeXgTotals(matches, 'xgFor', 'goalsFor'), [matches])
  const xgAgainstSummary = useMemo(() => summarizeXgTotals(matches, 'xgAgainst', 'goalsAgainst'), [matches])
  const xgForAxis = niceXgAxis(Math.max(0, ...matches.flatMap(m => [m.goalsFor, m.xgFor ?? 0])))
  const xgAgainstAxis = niceXgAxis(Math.max(0, ...matches.flatMap(m => [m.goalsAgainst, m.xgAgainst ?? 0])))

  const goalBars = useMemo(() => {
    const list = (trends?.scorers ?? []).map(s => ({
      key: `p-${s.playerId}`,
      label: shortSurname(s.name),
      fullName: s.name,
      value: s.goals,
    }))
    if ((trends?.ownGoalsFor ?? 0) > 0) {
      list.push({
        key: 'own-goals',
        label: 'Phản lưới nhà',
        fullName: 'Phản lưới nhà',
        value: trends.ownGoalsFor,
      })
    }
    return list
  }, [trends])

  const assistBars = useMemo(() => (trends?.assisters ?? []).map(a => ({
    key: `p-${a.playerId}`,
    label: shortSurname(a.name),
    fullName: a.name,
    value: a.assists,
  })), [trends])

  const goalsPie = useMemo(() => buildContributionPieSlices(
    (trends?.scorers ?? []).map(s => ({ name: s.name, value: s.goals })),
    { ownGoals: trends?.ownGoalsFor ?? 0 },
  ), [trends])

  const assistsPie = useMemo(() => buildContributionPieSlices(
    (trends?.assisters ?? []).map(a => ({ name: a.name, value: a.assists })),
  ), [trends])

  const rankItems = useMemo(() => {
    const matchByGw = new Map(matches.map(m => [m.matchweek, m]))
    return (trends?.rankHistory ?? []).map(r => {
      const m = matchByGw.get(r.matchweek)
      return {
        matchweek: r.matchweek,
        position: r.position,
        home: m?.home ?? null,
        opponentClean: m?.opponentClean ?? null,
        opponentCode: m?.opponentCode ?? null,
      }
    })
  }, [trends, matches])

  const bestRank = rankItems.length > 0 ? Math.min(...rankItems.map(r => r.position)) : null

  const homeAwayRows = [
    { label: 'Điểm', h: homeAway.home.points, a: homeAway.away.points },
    { label: 'Thắng', h: homeAway.home.won, a: homeAway.away.won },
    { label: 'Bàn thắng', h: homeAway.home.goalsFor, a: homeAway.away.goalsFor },
    { label: 'Bàn thua', h: homeAway.home.goalsAgainst, a: homeAway.away.goalsAgainst },
  ]

  const trendsLoading = is2026 && trendsResource?.status === 'loading'
  const trendsError = is2026 && trendsResource?.status === 'error'

  return (
    <div className="cst-root">
      {/* 1. Hàng KPI */}
      <div className={is2026 ? 'cst-kp' : 'cst-kp cst-kp-4'}>
        {is2026 && (
          <div className="cst-k">
            <small>Điểm đánh giá TB</small>
            <b>
              {stats?.averageRating != null ? (
                <span className={`cst-rt cst-rt-${ratingBand}`}>
                  {stats.averageRating.toFixed(2)}
                </span>
              ) : (
                <span className="cst-rt-na">—</span>
              )}
            </b>
          </div>
        )}
        <div className="cst-k">
          <small>Điểm</small>
          <b>{totalPoints}</b>
          <em>{standing ? `Hạng ${standing.position}` : '—'}</em>
        </div>
        <div className="cst-k">
          <small>Bàn thắng</small>
          <b>{totalGoalsFor}</b>
          <em>{totalPlayed > 0 ? `${(totalGoalsFor / totalPlayed).toFixed(2)} / trận` : '—'}</em>
        </div>
        <div className="cst-k">
          <small>Bàn thua</small>
          <b>{totalGoalsAgainst}</b>
          <em>{totalPlayed > 0 ? `${(totalGoalsAgainst / totalPlayed).toFixed(2)} / trận` : '—'}</em>
        </div>
        <div className="cst-k">
          <small>Giữ sạch lưới</small>
          <b>{summary.cleanSheets}</b>
          <em>/ {summary.played} trận</em>
        </div>
      </div>

      {/* 2. Kết quả + Sân nhà / Sân khách */}
      <div className="cst-g2">
        <div className="cst-box">
          <h3>Kết quả</h3>
          <ResultsDonut summary={summary} points={totalPoints} />
        </div>
        <div className="cst-box">
          <h3>Sân nhà · Sân khách</h3>
          <div className="cst-hl">
            <span className="cst-hl-a">Sân nhà · {homeAway.home.played} trận</span>
            <span className="cst-hl-b">Sân khách · {homeAway.away.played} trận</span>
          </div>
          <div className="cst-ha">
            {homeAwayRows.map(row => {
              const rowMax = Math.max(1, row.h, row.a)
              return (
                <div className="cst-har" key={row.label}>
                  <span className="cst-har-side">
                    <span className="cst-har-v">{row.h}</span>
                    <span className="cst-bar cst-bar-l">
                      <i style={{ width: `${(row.h / rowMax) * 100}%` }} />
                    </span>
                  </span>
                  <span className="cst-har-lbl">{row.label}</span>
                  <span className="cst-har-side">
                    <span className="cst-bar cst-bar-r">
                      <i style={{ width: `${(row.a / rowMax) * 100}%` }} />
                    </span>
                    <span className="cst-har-v">{row.a}</span>
                  </span>
                </div>
              )
            })}
          </div>
        </div>
      </div>

      {/* 3. Điểm tích lũy + Điểm đánh giá TB từng trận */}
      <div className={is2026 ? 'cst-g3' : 'cst-g3 cst-g3-single'}>
        <div className="cst-box">
          <h3>Điểm tích lũy</h3>
          <MatchChart
            items={cumSeries}
            height={186}
            yMin={0}
            yMax={ptsAxis.max}
            yGrid={ptsAxis.grid}
            ariaLabel="Điểm tích lũy qua từng trận"
            formatTooltip={m => ({
              title: `GW${m.matchweek} · ${m.home ? 'Nhà' : 'Khách'} · ${m.opponentClean}`,
              detail: `Tỉ số ${m.goalsFor}–${m.goalsAgainst} · tổng ${m.cumulativePoints} điểm`,
            })}
            renderPlot={({ yScale, slotWidth, leftPad }) => {
              const pts = cumSeries.map((p, i) => [leftPad + slotWidth * (i + 0.5), yScale(p.cumulativePoints)])
              const last = pts.at(-1)
              const lastVal = cumSeries.at(-1)?.cumulativePoints ?? 0
              const areaPath = `M${pts[0][0]} ${yScale(0)} L${pts.map(q => q.join(' ')).join(' L')} L${last[0]} ${yScale(0)}Z`
              return (
                <>
                  <path d={areaPath} fill="var(--cx-accent)" opacity="0.13" />
                  <polyline
                    points={pts.map(q => q.join(',')).join(' ')}
                    fill="none"
                    stroke="var(--cx-accent)"
                    strokeWidth="2"
                  />
                  {pts.map((q, i) => (
                    <circle
                      key={cumSeries[i].fixtureId ?? i}
                      cx={q[0]}
                      cy={q[1]}
                      r={i === pts.length - 1 ? 4.5 : 3}
                      fill="var(--cx-accent)"
                      stroke="var(--cx-card)"
                      strokeWidth="1.5"
                    />
                  ))}
                  <text
                    x={last[0]}
                    y={last[1] - 9}
                    textAnchor="middle"
                    fontSize="11"
                    fontWeight="800"
                    fill="var(--cx-text)"
                  >
                    {lastVal}
                  </text>
                </>
              )
            }}
          />
        </div>

        {is2026 && (
          <div className="cst-box">
            <h3>Điểm đánh giá TB từng trận</h3>
            {trendsLoading ? (
              <div className="cx-skeleton cst-skel" />
            ) : trendsError ? (
              <p className="cst-empty">
                {trendsResource.error}{' '}
                <button type="button" className="cst-retry" onClick={trendsResource.reload}>Thử lại</button>
              </p>
            ) : (
              <MatchChart
                items={matches}
                height={186}
                yMin={ratingAxis.min}
                yMax={ratingAxis.max}
                yGrid={ratingAxis.grid}
                ariaLabel="Điểm đánh giá trung bình từng trận"
                formatTooltip={m => ({
                  title: `GW${m.matchweek} · ${m.home ? 'Nhà' : 'Khách'} · ${m.opponentClean}`,
                  detail: m.teamRating != null
                    ? `Điểm TB cả đội: ${m.teamRating.toFixed(2)}`
                    : 'Chưa có điểm đánh giá',
                })}
                renderPlot={({ yScale, slotWidth, leftPad, width: _, plotWidth }) => {
                  const ratedPts = matches
                    .map((m, i) => (m.teamRating != null
                      ? { x: leftPad + slotWidth * (i + 0.5), y: yScale(m.teamRating), val: m.teamRating, key: m.fixtureId ?? i }
                      : null))
                    .filter(Boolean)
                  return (
                    <>
                      {stats?.averageRating != null && (
                        <line
                          x1={leftPad}
                          x2={leftPad + plotWidth}
                          y1={yScale(stats.averageRating)}
                          y2={yScale(stats.averageRating)}
                          stroke="var(--cx-muted)"
                          strokeDasharray="4 4"
                          opacity="0.6"
                        />
                      )}
                      {ratedPts.length > 1 && (
                        <polyline
                          points={ratedPts.map(p => `${p.x},${p.y}`).join(' ')}
                          fill="none"
                          stroke="var(--cx-muted)"
                          strokeWidth="1.2"
                          opacity="0.5"
                        />
                      )}
                      {ratedPts.map(p => (
                        <circle
                          key={p.key}
                          cx={p.x}
                          cy={p.y}
                          r={matches.length > 24 ? 3.5 : 5.5}
                          fill={ratingDotColor(p.val)}
                          stroke="var(--cx-card)"
                          strokeWidth="1.5"
                        />
                      ))}
                    </>
                  )
                }}
              />
            )}
          </div>
        )}
      </div>

      {/* 4. Bàn thắng · bàn thua từng trận */}
      <div className="cst-box cst-full">
        <h3>Bàn thắng · bàn thua từng trận</h3>
        <MatchChart
          items={matches}
          height={224}
          yMin={goalsAxis.min}
          yMax={goalsAxis.max}
          yGrid={goalsAxis.grid}
          ariaLabel="Bàn thắng và bàn thua từng trận"
          formatTooltip={m => ({
            title: `GW${m.matchweek} · ${m.home ? 'Nhà' : 'Khách'} · ${m.opponentClean}`,
            detail: `Ghi ${m.goalsFor} · thủng lưới ${m.goalsAgainst}`,
          })}
          renderPlot={({ yScale, slotWidth, leftPad, plotWidth }) => {
            const zeroY = yScale(0)
            const showNumbers = slotWidth >= 18
            return (
              <>
                {matches.map((m, i) => {
                  const w = Math.max(4, Math.min(28, slotWidth * 0.5))
                  const x = leftPad + slotWidth * (i + 0.5) - w / 2
                  return (
                    <g key={m.fixtureId ?? i}>
                      {m.goalsFor > 0 && (
                        <>
                          <rect
                            x={x}
                            y={yScale(m.goalsFor)}
                            width={w}
                            height={Math.max(2, zeroY - yScale(m.goalsFor) - 1)}
                            rx="4"
                            fill="var(--cst-gf)"
                          />
                          {showNumbers && (
                            <text
                              x={x + w / 2}
                              y={yScale(m.goalsFor) - 4}
                              textAnchor="middle"
                              fontSize="10"
                              fontWeight="800"
                              fill="var(--cx-text)"
                            >
                              {m.goalsFor}
                            </text>
                          )}
                        </>
                      )}
                      {m.goalsAgainst > 0 && (
                        <>
                          <rect
                            x={x}
                            y={zeroY + 1}
                            width={w}
                            height={Math.max(2, yScale(-m.goalsAgainst) - zeroY - 1)}
                            rx="4"
                            fill="var(--cst-ga)"
                          />
                          {showNumbers && (
                            <text
                              x={x + w / 2}
                              y={yScale(-m.goalsAgainst) + 12}
                              textAnchor="middle"
                              fontSize="10"
                              fontWeight="800"
                              fill="var(--cx-text)"
                            >
                              {m.goalsAgainst}
                            </text>
                          )}
                        </>
                      )}
                    </g>
                  )
                })}
                <line x1={leftPad} x2={leftPad + plotWidth} y1={zeroY} y2={zeroY} stroke="var(--cx-muted)" />
              </>
            )
          }}
        />
      </div>

      {/* 5. Hai khối xG (2026 only) */}
      {is2026 && (
        <div className="cst-g3 cst-mt">
          <div className="cst-box">
            <div className="cst-bh">
              <h3>Bàn thắng · xG</h3>
              {xgForSummary.count > 0 && (
                <span className="cst-sm">
                  <b>{xgForSummary.goals}</b> bàn · xG <b>{xgForSummary.xg.toFixed(1)}</b> ·{' '}
                  <span className={xgForSummary.delta < 0 ? 'cst-neg' : xgForSummary.delta > 0 ? 'cst-pos' : ''}>
                    {formatSignedDelta(xgForSummary.delta)}
                  </span>
                </span>
              )}
            </div>
            <div className="cst-lgd">
              <span><i style={{ background: 'var(--cst-gf)' }} />Bàn thắng</span>
              <span><i className="cst-lgd-xg-gf" />xG</span>
            </div>
            {trendsLoading ? (
              <div className="cx-skeleton cst-skel" />
            ) : trendsError ? (
              <p className="cst-empty">{trendsResource.error}</p>
            ) : (
              <MatchChart
                items={matches}
                height={200}
                yMin={0}
                yMax={xgForAxis.max}
                yGrid={xgForAxis.grid}
                ariaLabel="Bàn thắng so với xG từng trận"
                formatTooltip={m => ({
                  title: `GW${m.matchweek} · ${m.home ? 'Nhà' : 'Khách'} · ${m.opponentClean}`,
                  detail: m.xgFor != null
                    ? `Ghi ${m.goalsFor} bàn · xG ${m.xgFor.toFixed(2)}`
                    : `Ghi ${m.goalsFor} bàn · chưa có xG`,
                })}
                renderPlot={({ yScale, slotWidth, leftPad }) => matches.map((m, i) => {
                  const w = Math.max(3, Math.min(18, slotWidth * 0.3))
                  const gap = Math.min(2, slotWidth * 0.06)
                  const x1 = leftPad + slotWidth * (i + 0.5) - w - gap / 2
                  const x2 = leftPad + slotWidth * (i + 0.5) + gap / 2
                  return (
                    <g key={m.fixtureId ?? i}>
                      {m.goalsFor > 0 && (
                        <rect
                          x={x1}
                          y={yScale(m.goalsFor)}
                          width={w}
                          height={Math.max(1, yScale(0) - yScale(m.goalsFor))}
                          rx="3"
                          fill="var(--cst-gf)"
                        />
                      )}
                      {m.xgFor != null && m.xgFor > 0 && (
                        <rect
                          x={x2}
                          y={yScale(m.xgFor)}
                          width={w}
                          height={Math.max(1, yScale(0) - yScale(m.xgFor))}
                          rx="3"
                          className="cst-xg-bar-gf"
                        />
                      )}
                    </g>
                  )
                })}
              />
            )}
          </div>

          <div className="cst-box">
            <div className="cst-bh">
              <h3>Bàn thua · xG đối thủ</h3>
              {xgAgainstSummary.count > 0 && (
                <span className="cst-sm">
                  <b>{xgAgainstSummary.goals}</b> bàn · xG <b>{xgAgainstSummary.xg.toFixed(1)}</b> ·{' '}
                  <span className={xgAgainstSummary.delta < 0 ? 'cst-pos' : xgAgainstSummary.delta > 0 ? 'cst-neg' : ''}>
                    {formatSignedDelta(xgAgainstSummary.delta)}
                  </span>
                </span>
              )}
            </div>
            <div className="cst-lgd">
              <span><i style={{ background: 'var(--cst-ga)' }} />Bàn thua</span>
              <span><i className="cst-lgd-xg-ga" />xG đối thủ</span>
            </div>
            {trendsLoading ? (
              <div className="cx-skeleton cst-skel" />
            ) : trendsError ? (
              <p className="cst-empty">{trendsResource.error}</p>
            ) : (
              <MatchChart
                items={matches}
                height={200}
                yMin={0}
                yMax={xgAgainstAxis.max}
                yGrid={xgAgainstAxis.grid}
                ariaLabel="Bàn thua so với xG đối thủ từng trận"
                formatTooltip={m => ({
                  title: `GW${m.matchweek} · ${m.home ? 'Nhà' : 'Khách'} · ${m.opponentClean}`,
                  detail: m.xgAgainst != null
                    ? `Thủng lưới ${m.goalsAgainst} · xG đối thủ ${m.xgAgainst.toFixed(2)}`
                    : `Thủng lưới ${m.goalsAgainst} · chưa có xG`,
                })}
                renderPlot={({ yScale, slotWidth, leftPad }) => matches.map((m, i) => {
                  const w = Math.max(3, Math.min(18, slotWidth * 0.3))
                  const gap = Math.min(2, slotWidth * 0.06)
                  const x1 = leftPad + slotWidth * (i + 0.5) - w - gap / 2
                  const x2 = leftPad + slotWidth * (i + 0.5) + gap / 2
                  return (
                    <g key={m.fixtureId ?? i}>
                      {m.goalsAgainst > 0 && (
                        <rect
                          x={x1}
                          y={yScale(m.goalsAgainst)}
                          width={w}
                          height={Math.max(1, yScale(0) - yScale(m.goalsAgainst))}
                          rx="3"
                          fill="var(--cst-ga)"
                        />
                      )}
                      {m.xgAgainst != null && m.xgAgainst > 0 && (
                        <rect
                          x={x2}
                          y={yScale(m.xgAgainst)}
                          width={w}
                          height={Math.max(1, yScale(0) - yScale(m.xgAgainst))}
                          rx="3"
                          className="cst-xg-bar-ga"
                        />
                      )}
                    </g>
                  )
                })}
              />
            )}
          </div>
        </div>
      )}

      {/* 6. Ghi bàn · kiến tạo + Thứ hạng qua từng vòng (2026 only) */}
      {is2026 && (
        <div className="cst-g3 cst-mt">
          <div className="cst-box">
            <div className="cst-bh">
              <h3>Ghi bàn · kiến tạo</h3>
              <span className="cst-seg" role="group" aria-label="Kiểu hiển thị ghi bàn và kiến tạo">
                <button
                  type="button"
                  aria-pressed={contribView === 'bar'}
                  onClick={() => setContribView('bar')}
                >
                  Thanh
                </button>
                <button
                  type="button"
                  aria-pressed={contribView === 'pie'}
                  onClick={() => setContribView('pie')}
                >
                  Tròn
                </button>
              </span>
            </div>

            {trendsLoading ? (
              <div className="cx-skeleton cst-skel" />
            ) : trendsError ? (
              <p className="cst-empty">{trendsResource.error}</p>
            ) : contribView === 'bar' ? (
              <div className="cst-ctb">
                <div>
                  <h4>Bàn thắng</h4>
                  {goalBars.length === 0 ? (
                    <p className="cst-empty">Chưa có dữ liệu.</p>
                  ) : (
                    <div className="cst-hb">
                      {goalBars.map(row => {
                        const maxG = Math.max(1, ...goalBars.map(r => r.value))
                        return (
                          <div className="cst-hbr" key={row.key} title={`${row.fullName}: ${row.value}`}>
                            <span className="cst-hbr-n">{row.label}</span>
                            <span className="cst-hbr-t">
                              <i style={{ width: `${(row.value / maxG) * 100}%`, background: 'var(--cst-gf)' }} />
                            </span>
                            <b>{row.value}</b>
                          </div>
                        )
                      })}
                    </div>
                  )}
                </div>
                <div>
                  <h4>Kiến tạo</h4>
                  {assistBars.length === 0 ? (
                    <p className="cst-empty">Chưa có dữ liệu.</p>
                  ) : (
                    <div className="cst-hb">
                      {assistBars.map(row => {
                        const maxA = Math.max(1, ...assistBars.map(r => r.value))
                        return (
                          <div className="cst-hbr" key={row.key} title={`${row.fullName}: ${row.value}`}>
                            <span className="cst-hbr-n">{row.label}</span>
                            <span className="cst-hbr-t">
                              <i style={{ width: `${(row.value / maxA) * 100}%`, background: 'var(--cst-line2)' }} />
                            </span>
                            <b>{row.value}</b>
                          </div>
                        )
                      })}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="cst-ctb cst-ctb-pie">
                <ContributionDonut title="Bàn thắng" pie={goalsPie} />
                <ContributionDonut title="Kiến tạo" pie={assistsPie} />
              </div>
            )}
          </div>

          <div className="cst-box">
            <div className="cst-bh">
              <h3>Thứ hạng qua từng vòng</h3>
              {bestRank != null && (
                <span className="cst-sm">
                  Cao nhất <b>hạng {bestRank}</b>
                </span>
              )}
            </div>

            {trendsLoading ? (
              <div className="cx-skeleton cst-skel" />
            ) : trendsError ? (
              <p className="cst-empty">{trendsResource.error}</p>
            ) : (
              <MatchChart
                items={rankItems}
                height={204}
                yMin={-20}
                yMax={-1}
                yGrid={[-20, -15, -10, -5, -1]}
                formatY={v => String(Math.abs(v))}
                ariaLabel="Thứ hạng qua từng vòng đấu"
                formatTooltip={item => ({
                  title: item.opponentClean
                    ? `GW${item.matchweek} · ${item.home ? 'Nhà' : 'Khách'} · ${item.opponentClean}`
                    : `Vòng ${item.matchweek}`,
                  detail: `Hạng ${item.position} sau vòng ${item.matchweek}`,
                })}
                renderPlot={({ yScale, slotWidth, leftPad, plotWidth, axis }) => {
                  const pts = rankItems.map((r, i) => ({
                    x: leftPad + slotWidth * (i + 0.5),
                    y: yScale(-r.position),
                    pos: r.position,
                    gw: r.matchweek,
                  }))
                  const showAllLabels = slotWidth >= 18
                  return (
                    <>
                      <rect
                        x={leftPad}
                        y={yScale(-1)}
                        width={plotWidth}
                        height={yScale(-4.5) - yScale(-1)}
                        fill="var(--cst-w)"
                        opacity="0.09"
                      />
                      {pts.length > 1 && (
                        <polyline
                          points={pts.map(p => `${p.x},${p.y}`).join(' ')}
                          fill="none"
                          stroke="var(--cx-accent)"
                          strokeWidth="2"
                        />
                      )}
                      {pts.map((p, i) => (
                        <g key={p.gw}>
                          <circle
                            cx={p.x}
                            cy={p.y}
                            r={rankItems.length > 24 ? 3 : 4}
                            fill="var(--cx-accent)"
                            stroke="var(--cx-card)"
                            strokeWidth="1.5"
                          />
                          {(showAllLabels || i % axis.step === 0 || i === pts.length - 1) && (
                            <text
                              x={p.x}
                              y={p.y - 8}
                              textAnchor="middle"
                              fontSize="10"
                              fontWeight="800"
                              fill="var(--cx-text)"
                            >
                              {p.pos}
                            </text>
                          )}
                        </g>
                      ))}
                    </>
                  )
                }}
              />
            )}
          </div>
        </div>
      )}
    </div>
  )
}
