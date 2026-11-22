import { AssistIcon, GoalIcon } from '../EventIcons.jsx'
import { RATING_TIER, formatPer90 } from '../../utils/playerProfile.js'
import { ClockIcon, PitchIcon } from './PlayerSectionIcons.jsx'

export default function PlayerHeroStats({ totals, formItems }) {
  const avg = totals?.averageRating ?? null
  const rated = totals?.ratedMatches ?? 0
  const apps = totals?.appearances ?? 0
  const totalMatches = totals?.clubFinishedMatches ?? formItems.length
  const dnpCount = Math.max(0, totalMatches - apps)
  const mins = totals?.minutes ?? null
  const goals = totals?.goals ?? null
  const assists = totals?.assists ?? null
  const avgMins = mins != null && apps > 0 ? Math.round(mins / apps) : null
  const goalsPer90 = formatPer90(goals, mins)
  const assistsPer90 = formatPer90(assists, mins)
  const tier = RATING_TIER(avg)

  return (
    <div className="pd-strip">
      <div className={`pd-kpi pd-kpi-rating pd-kpi-rating-${tier}`}>
        <div>
          <b>{avg == null ? '—' : avg.toFixed(1)}</b>
          <div className="pd-k-l">Rating trung bình</div>
          <div className="pd-k-s">{rated > 0 ? `${rated} trận được chấm` : 'Chưa có trận được chấm'}</div>
        </div>
        {formItems.length > 0 && (
          <div className="pd-kpi-spark-wrap">
            <div className="pd-kpi-spark" aria-hidden="true">
              {formItems.map((item) => (
                item.state === 'played' && item.rating != null
                  ? <i key={item.id} style={{ height: `${Math.max(12, Math.min(100, item.rating * 10))}%` }} />
                  : <i key={item.id} className="hole" />
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="pd-kpi">
        <span className="pd-k-ic"><PitchIcon /></span>
        <b>{apps}{totalMatches > 0 && <em>/{totalMatches}</em>}</b>
        <span className="pd-k-l">Trận ra sân</span>
        <span className="pd-k-s">{totalMatches > 0 ? `${dnpCount} trận không ra sân` : 'Chưa có trận'}</span>
      </div>

      <div className="pd-kpi">
        <span className="pd-k-ic"><ClockIcon /></span>
        <b>{mins == null ? '—' : <>{mins}<em>&apos;</em></>}</b>
        <span className="pd-k-l">Số phút</span>
        <span className="pd-k-s">{avgMins == null ? '—' : `${avgMins} phút / trận`}</span>
      </div>

      <div className="pd-kpi">
        <span className="pd-k-ic"><GoalIcon /></span>
        <b>{goals == null ? '—' : goals}</b>
        <span className="pd-k-l">Bàn thắng</span>
        <span className="pd-k-s">{goalsPer90 == null ? '—' : `${goalsPer90} / 90 phút`}</span>
      </div>

      <div className="pd-kpi">
        <span className="pd-k-ic"><AssistIcon /></span>
        <b>{assists == null ? '—' : assists}</b>
        <span className="pd-k-l">Kiến tạo</span>
        <span className="pd-k-s">{assistsPer90 == null ? '—' : `${assistsPer90} / 90 phút`}</span>
      </div>
    </div>
  )
}
