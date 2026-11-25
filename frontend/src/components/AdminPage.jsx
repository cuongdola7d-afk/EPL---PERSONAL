import { useCallback, useEffect, useState } from 'react'
import { fetchAdminOverview } from '../api/admin.js'
import { PAGE_LABELS } from '../utils/analytics.js'
import './AdminPage.css'

const VN_TZ = 'Asia/Ho_Chi_Minh'

function formatTimeVn(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TZ,
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(d)
}

function formatDateShortVn(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  const parts = new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TZ,
    day: '2-digit',
    month: '2-digit',
  }).format(d)
  return parts.replaceAll('-', '/')
}

function formatDateTimeVn(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  return `${formatTimeVn(iso)} · ${formatDateShortVn(iso)}`
}

function formatDeadlineVn(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '—'
  const weekday = new Intl.DateTimeFormat('vi-VN', {
    timeZone: VN_TZ,
    weekday: 'long',
  }).format(d)
  const capWeekday = weekday.charAt(0).toUpperCase() + weekday.slice(1)
  return `${formatTimeVn(iso)} ${capWeekday} ${formatDateShortVn(iso)}`
}

function formatDayLabel(dayStr) {
  if (!dayStr) return ''
  const [, m, d] = dayStr.split('-')
  return `${d}/${m}`
}

function formatRelativeVnDay(dayStr, nowIso) {
  if (!dayStr) return '—'
  const now = nowIso ? new Date(nowIso) : new Date()
  const todayStr = new Intl.DateTimeFormat('en-CA', { timeZone: VN_TZ }).format(now)
  const todayDate = new Date(`${todayStr}T00:00:00Z`)
  const targetDate = new Date(`${dayStr}T00:00:00Z`)
  const diffDays = Math.round((todayDate - targetDate) / 86_400_000)
  if (diffDays === 0) return 'hôm nay'
  if (diffDays === 1) return 'hôm qua'
  if (diffDays > 1 && diffDays <= 7) return `${diffDays} ngày trước`
  return formatDayLabel(dayStr)
}

function formatActionLabel(item) {
  switch (item.actionType) {
    case 'PUBLISH_DEADLINE':
      return `GW${item.gameweek}: mở chọn đội`
    case 'ADJUST_DEADLINE':
      return `GW${item.gameweek}: điều chỉnh deadline`
    case 'PUBLISH_RESULTS':
      return `GW${item.gameweek}: công bố kết quả`
    case 'RECALCULATE_RESULTS':
      return `GW${item.gameweek}: tái tính kết quả`
    default:
      return `GW${item.gameweek}: thao tác Fantasy`
  }
}

function TrafficChart({ series }) {
  const [hoverIdx, setHoverIdx] = useState(null)
  const hasAnyData = series.some(point => point.views > 0 || point.visitors > 0)

  if (!hasAnyData) {
    return (
      <div className="adm-empty">
        Chưa có dữ liệu, bắt đầu đếm từ ngày triển khai
      </div>
    )
  }

  const W = 620
  const H = 200
  const padLeft = 36
  const padBottom = 24
  const padTop = 8
  const rawMax = Math.max(...series.map(p => Math.max(p.views, p.visitors)), 1)
  const step = rawMax <= 10 ? 2 : rawMax <= 40 ? 10 : Math.ceil(rawMax / 4 / 10) * 10
  const maxY = Math.max(step * 4, 4)
  const plotW = W - padLeft
  const plotH = H - padBottom - padTop
  const bw = plotW / series.length
  const yOf = v => padTop + plotH - (v / maxY) * plotH
  const ticks = [0, maxY / 4, maxY / 2, (maxY * 3) / 4, maxY]
  const labelEvery = Math.max(1, Math.ceil(series.length / 6))

  const active = hoverIdx != null ? series[hoverIdx] : null

  return (
    <div className="adm-chart" onMouseLeave={() => setHoverIdx(null)}>
      <svg viewBox={`0 0 ${W} ${H}`} preserveAspectRatio="none" role="img" aria-label="Biểu đồ khách và lượt xem theo ngày">
        {ticks.map(g => (
          <g key={g}>
            <line className="adm-gl" x1={padLeft} x2={W} y1={yOf(g)} y2={yOf(g)} />
            <text className="adm-ax" x={padLeft - 6} y={yOf(g) + 3} textAnchor="end">
              {Math.round(g)}
            </text>
          </g>
        ))}

        {series.map((point, idx) => {
          const x = padLeft + idx * bw
          const w = Math.max(3, bw - 3)
          const viewsH = Math.max(0, H - padBottom - yOf(point.views))
          const visH = Math.max(0, H - padBottom - yOf(point.visitors))
          const showLabel = idx % labelEvery === 0 || idx === series.length - 1
          return (
            <g key={point.day}>
              <rect
                x={x + 1.5}
                y={yOf(point.views)}
                width={w}
                height={viewsH}
                rx="3"
                fill="var(--adm-bar2)"
              />
              <rect
                x={x + 1.5 + w * 0.2}
                y={yOf(point.visitors)}
                width={Math.max(2, w * 0.6)}
                height={visH}
                rx="3"
                fill="var(--adm-bar)"
              />
              {showLabel && (
                <text className="adm-ax" x={x + bw / 2} y={H - 6} textAnchor="middle">
                  {formatDayLabel(point.day)}
                </text>
              )}
              <rect
                x={x}
                y={0}
                width={bw}
                height={H - padBottom}
                fill="transparent"
                onMouseEnter={() => setHoverIdx(idx)}
                onFocus={() => setHoverIdx(idx)}
              />
            </g>
          )
        })}
      </svg>

      {active && (
        <div
          className="adm-tip"
          style={{
            left: `${((padLeft + (hoverIdx + 0.5) * bw) / W) * 100}%`,
            top: `${(yOf(Math.max(active.views, active.visitors)) / H) * 100}%`,
          }}
        >
          {formatDayLabel(active.day)} · {active.visitors} khách · {active.views} lượt xem
        </div>
      )}
    </div>
  )
}

export default function AdminPage({ theme = 'dark', account, authLoading }) {
  const [range, setRange] = useState(30)
  const [state, setState] = useState({ loading: true, error: '', data: null })

  const load = useCallback((selectedRange, signal) => {
    setState(prev => ({ ...prev, loading: true, error: '' }))
    fetchAdminOverview(selectedRange, signal)
      .then(data => setState({ loading: false, error: '', data }))
      .catch(error => {
        if (error.name === 'AbortError') return
        setState({ loading: false, error: error.message || 'Không tải được dữ liệu quản trị.', data: null })
      })
  }, [])

  useEffect(() => {
    if (authLoading) return
    if (account?.role !== 'ADMIN') {
      setState({ loading: false, error: '', data: null })
      return
    }
    const controller = new AbortController()
    load(range, controller.signal)
    return () => controller.abort()
  }, [account?.id, account?.role, authLoading, range, load])

  if (authLoading) {
    return (
      <div className="adm-page" data-theme={theme}>
        <div className="adm-wrap">
          <div className="adm-state">
            <h2>Đang kiểm tra quyền truy cập…</h2>
            <p>Vui lòng chờ trong giây lát.</p>
          </div>
        </div>
      </div>
    )
  }

  if (account?.role !== 'ADMIN') {
    return (
      <div className="adm-page" data-theme={theme}>
        <div className="adm-wrap">
          <div className="adm-state" role="alert">
            <h2>Không có quyền truy cập</h2>
            <p>Trang Quản trị chỉ dành cho tài khoản có vai trò ADMIN.</p>
          </div>
        </div>
      </div>
    )
  }

  const data = state.data
  const topPages = data?.traffic?.topPages ?? []
  const maxPageViews = topPages.length > 0 ? Math.max(...topPages.map(p => p.views), 1) : 1

  const automationBadge = !data
    ? null
    : !data.fantasy.automationEnabled
      ? { cls: 'adm-auto-off', text: '● Đang tắt' }
      : data.fantasy.automationDryRun
        ? { cls: 'adm-auto-dry', text: '● Chạy thử (dry-run)' }
        : { cls: 'adm-auto-on', text: '● Đang bật' }

  const delta = data?.traffic?.deltaPercent

  return (
    <div className="adm-page" data-theme={theme}>
      <div className="adm-wrap">
        <section className="adm-hero">
          <div>
            <h1>Quản <span>trị</span></h1>
            <p>
              Người dùng, lượt truy cập và trạng thái Fantasy
              {data?.generatedAtUtc ? ` · cập nhật lúc ${formatTimeVn(data.generatedAtUtc)}` : ''}
            </p>
          </div>
          <span className="adm-seg" role="group" aria-label="Khoảng thời gian">
            <button type="button" aria-pressed={range === 7} onClick={() => setRange(7)}>7 ngày</button>
            <button type="button" aria-pressed={range === 30} onClick={() => setRange(30)}>30 ngày</button>
          </span>
        </section>

        {state.loading && !data && (
          <div className="adm-state" role="status">
            <h2>Đang tải số liệu quản trị…</h2>
            <p>Đang tổng hợp người dùng, lượt truy cập và trạng thái Fantasy.</p>
          </div>
        )}

        {state.error && (
          <div className="adm-state" role="alert">
            <h2>Không tải được dữ liệu quản trị</h2>
            <p>{state.error}</p>
            <button type="button" className="adm-retry" onClick={() => load(range)}>Thử lại</button>
          </div>
        )}

        {data && (
          <>
            <h2 className="adm-section-title">Tổng quan</h2>
            <div className="adm-kpis">
              <div className="adm-kpi">
                <small>Tổng tài khoản</small>
                <b>{data.accounts.total}</b>
                <em>+{data.accounts.newLast7Days}</em>
                <span className="adm-kpi-meta">tài khoản mới trong 7 ngày</span>
              </div>
              <div className="adm-kpi">
                <small>Hoạt động hôm nay</small>
                <b>{data.accounts.activeToday}</b>
                <span className="adm-kpi-meta">đã đăng nhập và vào web</span>
              </div>
              <div className="adm-kpi">
                <small>Hoạt động 7 / 30 ngày</small>
                <b>
                  {data.accounts.active7Days}{' '}
                  <span className="adm-kpi-sub-num">/ {data.accounts.active30Days}</span>
                </b>
                <span className="adm-kpi-meta">tài khoản khác nhau</span>
              </div>
              <div className="adm-kpi">
                <small>Khách hôm nay</small>
                <b>{data.traffic.visitorsToday}</b>
                {delta != null && (
                  <em className={delta < 0 ? 'dn' : undefined}>
                    {delta >= 0 ? `+${delta}%` : `${delta}%`}
                  </em>
                )}
                <span className="adm-kpi-meta">
                  {delta != null
                    ? 'so với cùng ngày tuần trước'
                    : 'chưa có dữ liệu cùng ngày tuần trước'}
                </span>
              </div>
            </div>

            <div className="adm-grid2 adm-grid2-top">
              <div className="adm-card">
                <h3>Lượt truy cập theo ngày</h3>
                <p className="adm-sub">Khách khác nhau và lượt xem trang, kể cả người chưa đăng nhập</p>
                <div className="adm-legend">
                  <span><i style={{ background: 'var(--adm-bar)' }} />Khách</span>
                  <span><i style={{ background: 'var(--adm-bar2)' }} />Lượt xem</span>
                </div>
                <TrafficChart series={data.traffic.dailySeries} />
              </div>

              <div className="adm-card">
                <h3>Trang được xem nhiều</h3>
                <p className="adm-sub">Theo lượt xem trong {data.rangeDays} ngày qua</p>
                {topPages.length === 0 ? (
                  <div className="adm-empty">
                    Chưa có dữ liệu, bắt đầu đếm từ ngày triển khai
                  </div>
                ) : (
                  <ul className="adm-list">
                    {topPages.map(item => (
                      <li key={item.pageKey}>
                        <span>{PAGE_LABELS[item.pageKey] ?? item.pageKey}</span>
                        <b>{item.views}</b>
                        <span className="adm-bar">
                          <i style={{ width: `${Math.max(2, (item.views / maxPageViews) * 100)}%` }} />
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>

            <h2 className="adm-section-title">Fantasy</h2>
            <div className="adm-grid2">
              <div className="adm-card">
                <div className="adm-auto">
                  <span>Tự động mở vòng và công bố kết quả</span>
                  {automationBadge && <span className={automationBadge.cls}>{automationBadge.text}</span>}
                </div>

                {data.fantasy.openGameweek ? (
                  <div className="adm-gw">
                    <span className="adm-gw-num">GW{data.fantasy.openGameweek.gameweek}</span>
                    <div>
                      <span className="adm-st adm-st-open">Đang mở chọn đội</span>
                      <div className="adm-gw-meta">
                        Deadline {formatDeadlineVn(data.fantasy.openGameweek.deadlineUtc)} ·{' '}
                        {data.fantasy.openGameweek.bySystem ? 'tự mở' : 'mở tay'} lúc{' '}
                        {formatDateTimeVn(data.fantasy.openGameweek.actionAtUtc)}
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="adm-gw">
                    <span className="adm-gw-num">—</span>
                    <div>
                      <span className="adm-st adm-st-wait">Chưa có vòng đang mở</span>
                      <div className="adm-gw-meta">Chưa mở chọn đội cho vòng tiếp theo</div>
                    </div>
                  </div>
                )}

                {data.fantasy.latestPublishedGameweek && (
                  <div className="adm-gw">
                    <span className="adm-gw-num">GW{data.fantasy.latestPublishedGameweek.gameweek}</span>
                    <div>
                      <span className="adm-st adm-st-pub">Đã công bố</span>
                      <div className="adm-gw-meta">
                        {data.fantasy.latestPublishedGameweek.finishedFixtures ?? 0}/
                        {data.fantasy.latestPublishedGameweek.totalFixtures ?? 10} trận ·{' '}
                        {data.fantasy.latestPublishedGameweek.bySystem ? 'tự công bố' : 'công bố tay'} lúc{' '}
                        {formatDateTimeVn(data.fantasy.latestPublishedGameweek.actionAtUtc)}
                      </div>
                    </div>
                  </div>
                )}

                <h3 className="adm-players-title">Người chơi</h3>
                <div className="adm-fp">
                  <div>
                    <b>{data.fantasy.players.everSubmitted}</b>
                    <small>đã từng chơi</small>
                  </div>
                  <div>
                    <b>{data.fantasy.players.previousSubmitted}</b>
                    <small>
                      {data.fantasy.players.previousGameweek != null
                        ? `nộp đội GW${data.fantasy.players.previousGameweek}`
                        : 'vòng trước'}
                    </small>
                  </div>
                  <div>
                    <b>{data.fantasy.players.currentSubmitted}</b>
                    <small>
                      {data.fantasy.players.currentGameweek != null
                        ? `nộp đội GW${data.fantasy.players.currentGameweek}`
                        : 'vòng hiện tại'}
                    </small>
                  </div>
                  <div>
                    <b>{data.fantasy.players.currentDrafts}</b>
                    <small>
                      {data.fantasy.players.currentGameweek != null
                        ? `đang lưu nháp GW${data.fantasy.players.currentGameweek}`
                        : 'đang lưu nháp'}
                    </small>
                  </div>
                </div>
              </div>

              <div className="adm-card">
                <h3>Lịch sử thao tác</h3>
                <p className="adm-sub">Việc máy tự làm và việc admin làm tay</p>
                {data.fantasy.history.length === 0 ? (
                  <div className="adm-empty">Chưa có lịch sử thao tác Fantasy</div>
                ) : (
                  <ul className="adm-tl">
                    {data.fantasy.history.map((item, idx) => (
                      <li key={`${item.actionType}-${item.gameweek}-${item.occurredAtUtc}-${idx}`} className={item.actorType === 'SYSTEM' ? 'sys' : undefined}>
                        {formatActionLabel(item)}
                        <span className="adm-who">{item.actorName}</span>
                        <small>
                          {formatDateTimeVn(item.occurredAtUtc)}
                          {item.reason ? ` · ${item.reason}` : ''}
                        </small>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>

            <h2 className="adm-section-title">Tài khoản mới</h2>
            <div className="adm-card">
              {data.recentAccounts.length === 0 ? (
                <div className="adm-empty">Chưa có tài khoản người dùng nào</div>
              ) : (
                <div className="adm-table-wrap">
                  <table className="adm-table">
                    <thead>
                      <tr>
                        <th>Tên hiển thị</th>
                        <th>Cách đăng ký</th>
                        <th>Ngày tạo</th>
                        <th className="adm-num">Lần cuối vào web</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.recentAccounts.map((acc, idx) => (
                        <tr key={`${acc.displayName}-${acc.createdAtUtc}-${idx}`}>
                          <td>{acc.displayName}</td>
                          <td><span className="adm-tag">{acc.authMethod}</span></td>
                          <td>{formatDateShortVn(acc.createdAtUtc)}</td>
                          <td className="adm-num">{formatRelativeVnDay(acc.lastActiveDay, data.generatedAtUtc)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <p className="adm-sub" style={{ margin: '10px 0 0' }}>
                Không hiện email. Chỉ hiện tên hiển thị, cách đăng ký và ngày.
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
