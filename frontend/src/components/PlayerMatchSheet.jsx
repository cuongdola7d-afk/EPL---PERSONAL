import { useEffect, useRef } from 'react'
import { playerDetailHash } from '../utils/playerRoute.js'
import { clubVisual, matchPlayerValue, matchRating } from '../utils/matchView.js'
import { AssistIcon, CardIcon, GoalIcon, PlayerEventIcons } from './EventIcons.jsx'

const POSITIONS = { G: 'GK', D: 'DEF', M: 'MID', F: 'FWD' }

function ratingTier(value) {
  return value == null ? 'na' : value >= 9 ? 'blue' : value >= 7 ? 'green' : value >= 5 ? 'orange' : 'red'
}

function formatNum(value, decimals = 0) {
  if (value == null) return '—'
  const num = Number(value)
  if (!Number.isFinite(num)) return '—'
  return decimals > 0 ? num.toFixed(decimals) : String(num)
}

function initialsOf(name) {
  const parts = (name ?? '').replace(/\./g, '').split(/\s+/).filter(Boolean)
  if (parts.length < 2) return (name ?? '').slice(0, 2).toUpperCase()
  return `${parts[0][0]}${parts.at(-1)[0]}`.toUpperCase()
}

function playTimeLabel(player) {
  if (player.role === 'SUB_USED') {
    return player.substitutionInMinute != null ? `Vào sân phút ${player.substitutionInMinute}` : 'Vào sân từ ghế dự bị'
  }
  if (player.role === 'STARTER') {
    return player.substitutionOutMinute != null ? `Đá chính · rời sân phút ${player.substitutionOutMinute}` : 'Đá chính · cả trận'
  }
  if (player.substitutionInMinute != null) return `Vào sân phút ${player.substitutionInMinute}`
  if (player.substitutionOutMinute != null) return `Đá chính · rời sân phút ${player.substitutionOutMinute}`
  return 'Đã ra sân'
}

function SheetRow({ label, value }) {
  return (
    <div className="mx-sh-row">
      <span>{label}</span>
      <b>{value}</b>
    </div>
  )
}

function PlayerMatchSheet({ player, club, season, evidenceStatus, isProvisional = false, onClose, returnFocusRef }) {
  const closeRef = useRef(null)

  useEffect(() => {
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    closeRef.current?.focus()

    function handleKeyDown(event) {
      if (event.key === 'Escape') {
        event.preventDefault()
        onClose()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = prevOverflow
      const trigger = returnFocusRef?.current
      if (trigger && typeof trigger.focus === 'function') {
        trigger.focus()
      }
    }
  }, [onClose, returnFocusRef])

  if (!player) return null

  const rating = matchRating(player)
  const minutes = matchPlayerValue(player, 'minutes', evidenceStatus)
  const goals = matchPlayerValue(player, 'goals', evidenceStatus)
  const assists = matchPlayerValue(player, 'assists', evidenceStatus)
  const yellowCards = matchPlayerValue(player, 'yellowCards', evidenceStatus)
  const redCards = matchPlayerValue(player, 'redCards', evidenceStatus)
  const extra = player.extra ?? null
  const posLabel = player.matchPosition ?? POSITIONS[player.position] ?? player.position ?? '—'
  const isGk = player.matchPosition === 'GK' || player.position === 'G'
  const clubColor = clubVisual(club).color
  const profileUrl = playerDetailHash(player.playerId, season, window.location.hash)

  return (
    <div
      className="mx-sh-ov"
      role="dialog"
      aria-modal="true"
      aria-labelledby="mx-sheet-player-name"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div className="mx-sheet">
        <div className="mx-sh-top">
          <span className="mx-sh-av" style={{ background: clubColor }} aria-hidden="true">
            {initialsOf(player.playerName)}
          </span>
          <div className="mx-sh-who">
            <h3 id="mx-sheet-player-name">
              <span>{player.playerName}</span>
              <PlayerEventIcons player={player} evidenceStatus={evidenceStatus} className="mx-inline-ev" />
            </h3>
            <small>{club} · {posLabel} · {playTimeLabel(player)}</small>
          </div>
          <button
            ref={closeRef}
            type="button"
            className="mx-sh-close"
            onClick={onClose}
            aria-label="Đóng"
          >
            ✕
          </button>
        </div>

        <div className="mx-sh-sum">
          <div>
            <span className={`mx-tbl-rt mx-rating-${ratingTier(rating)}`}>
              {rating == null ? '—' : rating.toFixed(1)}
            </span>
            <small>Rating</small>
          </div>
          <div>
            <b>{minutes == null ? '—' : `${minutes}′`}</b>
            <small>Số phút</small>
          </div>
          <div>
            <b>{formatNum(extra?.bps)}</b>
            <small>BPS</small>
          </div>
        </div>

        {isGk && (
          <div className="mx-sh-grp">
            <h4>Thủ môn</h4>
            <SheetRow label="Cứu thua" value={formatNum(extra?.saves)} />
            <SheetRow label="Bàn thua" value={formatNum(extra?.goalsConceded)} />
            <SheetRow label="Phá bóng, chặn, cắt" value={formatNum(extra?.clearancesBlocksInterceptions)} />
            <SheetRow label="Đoạt lại bóng" value={formatNum(extra?.recoveries)} />
          </div>
        )}

        <div className="mx-sh-grp">
          <h4>Tấn công</h4>
          <SheetRow
            label={<span className="mx-sh-ic-lbl"><GoalIcon />Bàn thắng</span>}
            value={formatNum(goals)}
          />
          <SheetRow
            label={<span className="mx-sh-ic-lbl"><AssistIcon />Kiến tạo</span>}
            value={formatNum(assists)}
          />
          <SheetRow label="Bàn thắng kỳ vọng (xG)" value={formatNum(extra?.expectedGoals, 2)} />
          <SheetRow label="Kiến tạo kỳ vọng (xA)" value={formatNum(extra?.expectedAssists, 2)} />
        </div>

        {!isGk && (
          <div className="mx-sh-grp">
            <h4>Phòng ngự</h4>
            <SheetRow label="Tắc bóng" value={formatNum(extra?.tackles)} />
            <SheetRow label="Phá bóng, chặn, cắt" value={formatNum(extra?.clearancesBlocksInterceptions)} />
            <SheetRow label="Đoạt lại bóng" value={formatNum(extra?.recoveries)} />
            <SheetRow label="Đóng góp phòng ngự" value={formatNum(extra?.defensiveContribution)} />
          </div>
        )}

        <div className="mx-sh-grp">
          <h4>Kỷ luật</h4>
          <SheetRow
            label={<span className="mx-sh-ic-lbl"><CardIcon color="yellow" />Thẻ vàng</span>}
            value={formatNum(yellowCards)}
          />
          <SheetRow
            label={<span className="mx-sh-ic-lbl"><CardIcon color="red" />Thẻ đỏ</span>}
            value={formatNum(redCards)}
          />
        </div>

        <div className="mx-sh-foot">
          <a className="mx-sh-profile-link" href={profileUrl} onClick={onClose}>
            Xem hồ sơ cầu thủ ›
          </a>
          <p className="mx-sh-src">
            {isProvisional
              ? 'Số liệu tạm: Fantasy Premier League · Chờ xác nhận rating SofaScore'
              : 'Rating: Sofascore · Chỉ số mở rộng: Fantasy Premier League'}
          </p>
        </div>
      </div>
    </div>
  )
}

export default PlayerMatchSheet
