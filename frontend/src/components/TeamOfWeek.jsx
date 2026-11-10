import { useCallback, useEffect } from 'react'
import { fetchTeamOfWeek } from '../api/teamOfWeek.js'
import { useApiResource } from '../hooks/useApiResource.js'
import { useCompletedGameweeks } from '../hooks/useCompletedGameweeks.js'
import { teamOfWeekGameweek } from '../fantasy/teamOfWeek.js'
import { formationSlots } from '../fantasy/lineup.js'
import { playerDetailHash } from '../utils/playerRoute.js'
import { clubVisual } from '../utils/matchView.js'
import Pitch from './FantasyPitch.jsx'
import PlayerAvatar from './FantasyPlayerAvatar.jsx'
import './TeamOfWeek.css'

const slots = formationSlots('4-3-3')

export default function TeamOfWeek({ selection, onGameweekChange }) {
  const { weeks, status: calendarStatus, error: calendarError, reload: reloadCalendar } = useCompletedGameweeks()
  const gameweek = teamOfWeekGameweek(weeks, selection)
  useEffect(() => {
    if (calendarStatus === 'success' && selection !== gameweek) onGameweekChange(gameweek)
  }, [calendarStatus, selection, gameweek, onGameweekChange])
  const request = useCallback(signal => gameweek === null ? Promise.resolve(null) : fetchTeamOfWeek(gameweek, signal), [gameweek])
  const { data: response, status, error, reload: reloadTeam } = useApiResource(request)
  const reload = useCallback(() => { reloadCalendar(); reloadTeam() }, [reloadCalendar, reloadTeam])
  // Never render an earlier request's team under a newly selected round.
  const data = response?.gameweek === gameweek ? response : null
  useEffect(() => {
    const refresh = () => { if (document.visibilityState === 'visible') reload() }
    const timer = setInterval(refresh, 60000)
    document.addEventListener('visibilitychange', refresh)
    window.addEventListener('prismaxi-results-published', refresh)
    return () => {
      clearInterval(timer)
      document.removeEventListener('visibilitychange', refresh)
      window.removeEventListener('prismaxi-results-published', refresh)
    }
  }, [reload])
  const bySlot = new Map(data?.picks.map(pick => [pick.slot, pick.player]) ?? [])
  const filled = data?.picks.filter(pick => pick.player !== null).length ?? 0
  return <div className="team-of-week">
    <div className="tow-controls"><label htmlFor="tow-gameweek">Gameweek <select id="tow-gameweek" value={gameweek ?? ''}
      disabled={weeks.length === 0 || calendarStatus === 'error'} onChange={event => onGameweekChange(Number(event.target.value))}>
      {weeks.length === 0 && <option value="">Chưa có vòng kết thúc</option>}
      {weeks.map(week => <option key={week} value={week}>GW{week}</option>)}</select></label>
      <span>4-3-3 · SofaScore</span><button type="button" className="fantasy-clear" onClick={reload} disabled={status === 'loading' || calendarStatus === 'loading'}>Làm mới</button></div>
    {calendarStatus === 'loading' && weeks.length === 0 && <div className="fantasy-card tow-state" role="status">Đang kiểm tra lịch Premier League 2026/27…</div>}
    {calendarStatus === 'success' && gameweek === null && <div className="fantasy-card tow-state" role="status">Chưa có Gameweek nào kết thúc toàn bộ 10 trận.</div>}
    {gameweek !== null && calendarStatus !== 'error' && (status === 'loading' || status === 'success' && !data) && <div className="fantasy-card tow-state" role="status"><span className="spinner" aria-hidden="true" /><strong>Đang tải đội hình GW{gameweek}…</strong></div>}
    {(calendarStatus === 'error' || gameweek !== null && status === 'error') && <div className="fantasy-card tow-state" role="alert"><strong>{calendarStatus === 'error' ? 'Không tải được lịch vòng đấu' : 'Không tải được đội hình'}</strong><p>{calendarError || error}</p><button className="fantasy-primary" type="button" onClick={reload}>Thử lại</button></div>}
    {calendarStatus !== 'error' && status === 'success' && data && <>
      <div className="fantasy-card tow-summary"><div><h2>Đội hình tiêu biểu · GW{data.gameweek}</h2><p>{filled}/11 cầu thủ · {data.candidateCount} ứng viên · {data.recordedFixtures}/{data.completedFixtures} trận có thống kê</p></div>
        <div className="tow-total"><span>Tổng rating</span><strong>{data.totalRating === null ? '—' : data.totalRating.toFixed(2)}</strong></div></div>
      {data.status === 'INSUFFICIENT_DATA' && <div className="fantasy-message tow-missing" role="status"><strong>Chưa đủ dữ liệu để xếp 11 người đúng vị trí.</strong>
        <p>Các ô chưa xếp được: {data.missingSlots.join(', ')}.</p></div>}
      {data.status === 'MULTIPLE_MATCHES' && <div className="fantasy-message" role="alert"><strong>Cần chốt cách tính rating cho cầu thủ có nhiều trận trong vòng.</strong>
        {data.conflicts.map(conflict => <p key={conflict.playerId}>{conflict.name} (#{conflict.playerId}): trận {conflict.fixtureIds.join(', ')}.</p>)}</div>}
      {data.recordedFixtures < 10 && <p className="tow-note">Vòng đấu chưa có đủ 10 trận được ghi nhận.</p>}
      {data.status !== 'MULTIPLE_MATCHES' && <div className="fantasy-main">
        <div className="fantasy-pitch tow-pitch" aria-label={`Đội hình tiêu biểu GW${data.gameweek}, sơ đồ 4-3-3`}><Pitch />
          {slots.map(slot => {
            const player = bySlot.get(slot.position)
            const content = <><span className={`fantasy-position fantasy-position-${slot.group}`}>{slot.position}</span>
              <PlayerAvatar player={player} rating={player?.rating} /><span className="fantasy-slot-name">{player?.name ?? 'Thiếu dữ liệu'}</span>
              {player && <span className="tow-pitch-club" title={player.club}>{clubVisual(player.club).code}</span>}</>
            const style = { left: `${slot.x}%`, top: `${slot.y}%` }
            return player ? <a className="fantasy-slot" key={slot.key} style={style} href={playerDetailHash(player.playerId, 2026, '#fantasy')}
              aria-label={`${slot.position}: ${player.name}, ${player.club}, rating ${player.rating.toFixed(2)}`}>{content}</a> :
              <span className="fantasy-slot tow-empty-slot" key={slot.key} style={style} aria-label={`${slot.position}: chưa có cầu thủ phù hợp`}>{content}</span>
          })}</div>
        <aside className="fantasy-card fantasy-side"><div className="fantasy-side-head"><h2>Cầu thủ tiêu biểu</h2><span>{filled}/11</span></div>
          <div className="fantasy-side-list">{data.picks.map(({ slot, player }) => player ? <a className="fantasy-side-pick tow-player-row" key={slot}
            href={playerDetailHash(player.playerId, 2026, '#fantasy')}><PlayerAvatar player={player} small />
            <span className="fantasy-side-name"><strong>{player.name}</strong><small>{slot} · {player.club}</small></span><b className="tow-player-rating">{player.rating.toFixed(2)}</b></a> :
            <div className="tow-unfilled" key={slot}><b>{slot}</b><span>Chưa có cầu thủ phù hợp</span></div>)}</div>
        </aside>
      </div>}
    </>}
  </div>
}
