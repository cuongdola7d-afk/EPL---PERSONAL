import { percentileBand, percentileLabel, percentileMeta } from '../../utils/playerProfile.js'
import { CompareIcon, SectionHeader } from './PlayerSectionIcons.jsx'

export default function PlayerPercentiles({ percentiles, comparisonGroupLabel }) {
  const groupLabel = comparisonGroupLabel || 'cầu thủ'
  return (
    <section className="pd-card pd-sec">
      <SectionHeader
        icon={<CompareIcon />}
        title={`So với các ${groupLabel} khác`}
        lead={`Hơn bao nhiêu % ${groupLabel} khác mùa này`}
      />

      {percentiles === null ? (
        <div className="pd-pc-empty">Cần đá ít nhất 450 phút để so sánh</div>
      ) : (
        <div className="pd-pc-list">
          {percentiles.map((item) => {
            const meta = percentileMeta(item.key)
            const hasValue = item.value != null && Number.isFinite(item.value)
            const hasPct = item.percentile != null && Number.isFinite(item.percentile)
            const band = percentileBand(item.percentile)
            const pctText = hasPct ? `${item.percentile}%` : '—'
            const valText = hasValue ? `${meta.format(item.value)} ${meta.unit}` : '—'

            return (
              <div className="pd-pc-row" key={item.key}>
                <div className="pd-pc-name">
                  <b>{meta.label}</b>
                  <small>{valText}</small>
                </div>
                <div
                  className="pd-pc-track"
                  role="img"
                  aria-label={hasPct ? `${meta.label}: hơn ${item.percentile}% ${groupLabel}` : `${meta.label}: chưa đủ dữ liệu`}
                >
                  {hasPct && <i className={`pd-i-${band}`} style={{ width: `${Math.max(0, Math.min(100, item.percentile))}%` }} />}
                  <span className="pd-pc-mid" aria-hidden="true" />
                </div>
                <div className="pd-pc-val">
                  <b>{pctText}</b>
                  <small className={`pd-t-${band}`}>{hasPct ? percentileLabel(item.percentile) : '—'}</small>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}
