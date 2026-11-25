import { useEffect, useState } from 'react'
import { fetchLatestFantasyResult } from '../api/fantasyLatestResult.js'
import {
  dismissLatestResult,
  formatCardPoints,
  formatOpenDeadlineFriendly,
  formatPublishedDayMonth,
  formatSeasonRankChange,
  isLatestResultDismissed,
  shouldShowLatestResultCard,
} from '../fantasy/latestResult.js'
import { shortPlayerName } from '../utils/matchView.js'
import './FantasyLatestResultCard.css'

export default function FantasyLatestResultCard({
  account = null,
  authLoading = false,
  viewingGameweek = null,
  openGameweek = null,
  onViewTeamAndScore,
  onViewLeaderboard,
  onOpenLogin,
}) {
  const [summary, setSummary] = useState(null)
  const [dismissed, setDismissed] = useState(false)
  const [reloadNonce, setReloadNonce] = useState(0)

  useEffect(() => {
    const refresh = () => setReloadNonce(n => n + 1)
    window.addEventListener('prismaxi-results-published', refresh)
    return () => window.removeEventListener('prismaxi-results-published', refresh)
  }, [])

  useEffect(() => {
    if (authLoading) return undefined
    const controller = new AbortController()
    fetchLatestFantasyResult(controller.signal)
      .then(data => {
        if (controller.signal.aborted) return
        setSummary(data)
        setDismissed(data?.gameweek ? isLatestResultDismissed(data.gameweek) : false)
      })
      .catch(error => {
        if (!controller.signal.aborted) {
          console.error('Failed to load latest Fantasy result summary:', error)
          setSummary(null)
        }
      })
    return () => controller.abort()
  }, [account?.id, authLoading, reloadNonce])

  const isGuest = !account
  if (authLoading || !shouldShowLatestResultCard({ summary, viewingGameweek, isGuest, dismissed })) {
    return null
  }

  const gw = summary.gameweek
  const publishedLabel = formatPublishedDayMonth(summary.publishedAt)
  const me = account ? summary.me : null
  const leader = summary.leader
  const rankChange = me ? formatSeasonRankChange(me.seasonRankChange) : null

  function handleDismiss() {
    dismissLatestResult(gw)
    setDismissed(true)
  }

  return (
    <section className="fantasy-latest-res" aria-label="Kết quả vòng vừa rồi">
      {!isGuest && (
        <button
          type="button"
          className="fantasy-latest-res-close"
          aria-label={`Ẩn kết quả GW${gw}`}
          onClick={handleDismiss}
        >
          ×
        </button>
      )}

      {me ? (
        <>
          <div className="fantasy-latest-res-pts">
            <b>{formatCardPoints(me.points)}</b>
            <small>điểm GW{gw}</small>
          </div>
          <div className="fantasy-latest-res-mid">
            <h3>
              Kết quả GW{gw}
              {publishedLabel && <span>Đã công bố {publishedLabel}</span>}
            </h3>
            <p>
              Bạn xếp <b>hạng {me.rank} / {me.participants}</b> người chơi
              {summary.averagePoints !== null && (
                <> · điểm trung bình vòng <b>{formatCardPoints(summary.averagePoints)}</b></>
              )}
            </p>
            <div className="fantasy-latest-res-chips">
              {me.topPlayer && (
                <span className="gold">
                  ★ Cao nhất đội bạn: {shortPlayerName(me.topPlayer.name)} {formatCardPoints(me.topPlayer.points)}
                </span>
              )}
              {me.seasonRank !== null && me.seasonRank !== undefined && (
                <span>
                  Hạng mùa: {me.seasonRank}{rankChange ? ` (${rankChange})` : ''}
                </span>
              )}
            </div>
          </div>
          <div className="fantasy-latest-res-acts">
            <button type="button" className="fantasy-latest-res-btn" onClick={() => onViewTeamAndScore?.(gw)}>
              Xem đội &amp; điểm
            </button>
            <button type="button" className="fantasy-latest-res-ghost" onClick={() => onViewLeaderboard?.(gw)}>
              Bảng xếp hạng
            </button>
          </div>
        </>
      ) : !isGuest ? (
        <>
          <div className="fantasy-latest-res-pts">
            <b className="is-muted">–</b>
            <small>bạn không chơi</small>
          </div>
          <div className="fantasy-latest-res-mid">
            <h3>
              Kết quả GW{gw}
              {publishedLabel && <span>Đã công bố {publishedLabel}</span>}
            </h3>
            <p>
              {leader ? (
                <>
                  Dẫn đầu vòng: <b>{leader.displayName} · {formatCardPoints(leader.points)} điểm</b> · {summary.participants} người tham gia
                </>
              ) : (
                <>{summary.participants} người tham gia</>
              )}
            </p>
            {openGameweek?.gameweek && openGameweek?.deadlineUtc && (
              <div className="fantasy-latest-res-chips">
                <span>
                  Đừng quên chọn đội GW{openGameweek.gameweek} trước {formatOpenDeadlineFriendly(openGameweek.deadlineUtc)}
                </span>
              </div>
            )}
          </div>
          <div className="fantasy-latest-res-acts">
            <button type="button" className="fantasy-latest-res-ghost" onClick={() => onViewLeaderboard?.(gw)}>
              Bảng xếp hạng
            </button>
          </div>
        </>
      ) : (
        <>
          <div className="fantasy-latest-res-pts">
            <b>{leader ? formatCardPoints(leader.points) : '–'}</b>
            <small>cao nhất GW{gw}</small>
          </div>
          <div className="fantasy-latest-res-mid">
            <h3>
              Kết quả GW{gw}
              {publishedLabel && <span>Đã công bố {publishedLabel}</span>}
            </h3>
            <p>
              {leader ? (
                <>Dẫn đầu vòng: <b>{leader.displayName}</b> · {summary.participants} người tham gia</>
              ) : (
                <>{summary.participants} người tham gia</>
              )}
            </p>
            <div className="fantasy-latest-res-chips">
              <span>Đăng nhập để chơi Fantasy và xem điểm của bạn</span>
            </div>
          </div>
          <div className="fantasy-latest-res-acts">
            <button type="button" className="fantasy-latest-res-btn" onClick={() => onOpenLogin?.()}>
              Đăng nhập
            </button>
            <button type="button" className="fantasy-latest-res-ghost" onClick={() => onViewLeaderboard?.(gw)}>
              Bảng xếp hạng
            </button>
          </div>
        </>
      )}
    </section>
  )
}
