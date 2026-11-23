import { CardIcon, GoalIcon } from '../EventIcons.jsx'
import { buildSaveRateSummary, formatPer90, formatPerMatch } from '../../utils/playerProfile.js'
import { GloveIcon, ListIcon, SectionHeader, ShieldIcon } from './PlayerSectionIcons.jsx'

function StatRow({ label, value, sub }) {
  return (
    <div className="pd-sg-row">
      <span>{label}</span>
      <b>
        {value == null ? '—' : value}
        {value != null && sub && <small>{sub}</small>}
      </b>
    </div>
  )
}

export default function PlayerSeasonGroups({ totals, roleGroup, season }) {
  const apps = totals?.appearances ?? 0
  const mins = totals?.minutes ?? null
  const isExtended = season === 2026
  const isGkExtended = isExtended && roleGroup === 'GK'
  const lead = isExtended
    ? `${apps} trận · ${mins == null ? '—' : `${mins} phút`} · số nhỏ là trung bình mỗi 90 phút`
    : `${apps} trận · ${mins == null ? '—' : `${mins} phút`}`

  const p90 = (v) => formatPer90(v, mins)
  const fmtDec = (v) => (v == null ? null : Number(v).toFixed(2))
  const bpsPerMatch = formatPerMatch(totals?.bps, apps, 1)
  const csPct = totals?.cleanSheets != null && apps > 0
    ? `${Math.round((totals.cleanSheets / apps) * 100)}%`
    : null
  const saveRate = isGkExtended ? buildSaveRateSummary(totals?.saves, totals?.goalsConceded) : null

  return (
    <section className="pd-card pd-sec">
      <SectionHeader icon={<ListIcon />} title="Số liệu mùa giải" lead={lead} />
      <div className={`pd-sgs${isExtended ? '' : ' pd-sgs-basic'}`}>
        {isGkExtended ? (
          <div className="pd-sg pd-sg-att">
            <div className="pd-sg-h">
              <span className="pd-sg-ic"><GloveIcon /></span>
              Thủ môn
            </div>
            <StatRow label="Cứu thua" value={totals?.saves} sub={p90(totals?.saves)} />
            <StatRow label="Bàn thua" value={totals?.goalsConceded} sub={p90(totals?.goalsConceded)} />
            <StatRow label="Giữ sạch lưới" value={totals?.cleanSheets} sub={csPct} />
            <StatRow
              label="Tỉ lệ cản phá"
              value={saveRate?.pct != null ? `${saveRate.pct}%` : null}
              sub={saveRate && saveRate.shotsOnTarget > 0 ? saveRate.subtitle : null}
            />
          </div>
        ) : (
          <div className="pd-sg pd-sg-att">
            <div className="pd-sg-h">
              <span className="pd-sg-ic"><GoalIcon /></span>
              Tấn công
            </div>
            <StatRow label="Bàn thắng" value={totals?.goals} sub={p90(totals?.goals)} />
            <StatRow label="Kiến tạo" value={totals?.assists} sub={p90(totals?.assists)} />
            {isExtended && (
              <>
                <StatRow label="xG" value={fmtDec(totals?.expectedGoals)} sub={p90(totals?.expectedGoals)} />
                <StatRow label="xA" value={fmtDec(totals?.expectedAssists)} sub={p90(totals?.expectedAssists)} />
              </>
            )}
          </div>
        )}

        {isExtended && (
          <div className="pd-sg pd-sg-def">
            <div className="pd-sg-h">
              <span className="pd-sg-ic"><ShieldIcon /></span>
              Phòng ngự
            </div>
            <StatRow label="Tắc bóng" value={totals?.tackles} sub={p90(totals?.tackles)} />
            <StatRow label="Chặn/cắt bóng" value={totals?.clearancesBlocksInterceptions} sub={p90(totals?.clearancesBlocksInterceptions)} />
            <StatRow label="Đoạt lại bóng" value={totals?.recoveries} sub={p90(totals?.recoveries)} />
          </div>
        )}

        <div className="pd-sg pd-sg-dis">
          <div className="pd-sg-h">
            <span className="pd-sg-ic"><CardIcon color="yellow" /></span>
            {isExtended ? 'Kỷ luật & FPL' : 'Kỷ luật'}
          </div>
          <StatRow label="Thẻ vàng" value={totals?.yellowCards} />
          <StatRow label="Thẻ đỏ" value={totals?.redCards} />
          {isExtended && (
            <>
              <StatRow label="BPS" value={totals?.bps} sub={bpsPerMatch ? `${bpsPerMatch}/trận` : null} />
              <StatRow label="Điểm thưởng" value={totals?.bonus} />
            </>
          )}
        </div>
      </div>
    </section>
  )
}
