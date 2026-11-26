export const DATA_HEALTH_META = {
  playedWithoutMembership: {
    title: 'Ra sân nhưng không có trong đội hình CLB',
    subtitle: 'Có tên trong trận 2 vòng gần nhất nhưng không có membership đang mở ở CLB đó',
    columns: ['Cầu thủ', 'CLB', 'Trận gần nhất', 'Membership'],
  },
  rosterNotSelectable: {
    title: 'Trong đội hình nhưng chưa chọn được trong Fantasy',
    subtitle: 'Thiếu vị trí chi tiết hoặc thiếu OVR FC 27',
    columns: ['Cầu thủ', 'CLB', 'Thiếu'],
  },
  fplUnmapped: {
    title: 'Cầu thủ FPL chưa khớp ID',
    subtitle: 'Từ job số liệu tạm FPL',
    columns: ['Tên FPL', 'CLB', 'Trận'],
  },
  multipleOpenMemberships: {
    title: 'Cầu thủ có từ 2 membership đang mở',
    subtitle: 'Trùng CLB cùng lúc',
    columns: ['Cầu thủ', 'Số dòng mở', 'Các CLB đang mở'],
  },
  playedMissingCore: {
    title: 'Ra sân nhưng thiếu điểm hoặc ô trống',
    subtitle: 'Đá trên 10 phút mà thiếu rating, hoặc phút/bàn/kiến tạo trống (GW6 trở đi)',
    columns: ['Cầu thủ', 'CLB', 'Trận', 'Thiếu'],
  },
}

const MISSING_FIELD_LABELS = {
  POSITION: 'Vị trí',
  OVR: 'OVR',
  OVR_RANGE: 'OVR ngoài 1–99',
  MINUTES: 'Phút',
  GOALS: 'Bàn thắng',
  ASSISTS: 'Kiến tạo',
  RATING: 'Rating',
}

export function formatShortIsoDay(isoDay) {
  if (!isoDay || typeof isoDay !== 'string') return ''
  const parts = isoDay.split('-')
  if (parts.length !== 3) return isoDay
  return `${parts[2]}/${parts[1]}`
}

export function formatMissingFields(codes) {
  if (!Array.isArray(codes) || codes.length === 0) return '—'
  return codes.map(code => MISSING_FIELD_LABELS[code] ?? code).join(', ')
}

export function formatLatestMatchStatus(item) {
  if (!item) return '—'
  const gw = item.gameweek != null ? `GW${item.gameweek}` : '—'
  if (item.participationStatus === 'PLAYED') {
    return item.minutes != null ? `${gw} · ${item.minutes}'` : `${gw} · ra sân`
  }
  return `${gw} · dự bị`
}

export function formatClosedMembership(isoDay) {
  if (!isoDay) return 'chưa có'
  return `đóng ${formatShortIsoDay(isoDay)}`
}

export function initialOpenCheckKeys(checks) {
  const open = {}
  if (!Array.isArray(checks)) return open
  for (const check of checks) {
    if (check && check.key && check.count > 0) {
      open[check.key] = true
    }
  }
  return open
}

export function summarizeDataHealth(data) {
  const checks = Array.isArray(data?.checks) ? data.checks : []
  const warnCount = checks.filter(c => (c?.count ?? 0) > 0 || c?.status === 'WARN').length
  if (warnCount > 0) {
    return {
      status: 'WARN',
      warnCount,
      label: `${warnCount} mục cần xem`,
    }
  }
  return {
    status: 'OK',
    warnCount: 0,
    label: 'Tất cả ổn',
  }
}

export function formatDataHealthCells(key, item) {
  switch (key) {
    case 'playedWithoutMembership':
      return [
        item.playerName ?? '—',
        item.clubName ?? '—',
        formatLatestMatchStatus(item),
        formatClosedMembership(item.lastClosedEndDate),
      ]
    case 'rosterNotSelectable':
      return [
        item.playerName ?? '—',
        item.clubName ?? '—',
        formatMissingFields(item.missing),
      ]
    case 'fplUnmapped':
      return [
        item.fplName ?? '—',
        item.clubName ?? '—',
        item.gameweek != null
          ? `GW${item.gameweek} · ${item.fixtureLabel ?? '—'}${item.minutes > 0 ? ` (${item.minutes}')` : ''}`
          : (item.fixtureLabel ?? '—'),
      ]
    case 'multipleOpenMemberships':
      return [
        item.playerName ?? '—',
        String(item.openCount ?? 0),
        Array.isArray(item.clubs) && item.clubs.length > 0 ? item.clubs.join(', ') : '—',
      ]
    case 'playedMissingCore':
      return [
        item.playerName ?? '—',
        item.clubName ?? '—',
        item.gameweek != null
          ? `GW${item.gameweek} · ${item.fixtureLabel ?? '—'}${item.minutes != null ? ` (${item.minutes}')` : ''}`
          : (item.fixtureLabel ?? '—'),
        formatMissingFields(item.missing),
      ]
    default:
      return [JSON.stringify(item)]
  }
}
