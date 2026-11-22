import { expectedVerdict } from '../../utils/playerProfile.js'
import { SectionHeader, TargetIcon } from './PlayerSectionIcons.jsx'

function ExpectedItem({ title, actual, expected }) {
  const verdict = expectedVerdict(actual, expected)
  const hasBoth = actual != null && expected != null
  const maxVal = hasBoth ? Math.max(Number(actual), Number(expected), 0.5) * 1.15 : 1
  const realWidth = hasBoth ? Math.min(100, (Number(actual) / maxVal) * 100) : 0
  const expWidth = hasBoth ? Math.min(100, (Number(expected) / maxVal) * 100) : 0

  return (
    <div className="pd-xv">
      <div className="pd-xv-head">
        <b>{title}</b>
        <span className={`pd-verdict pd-v-${verdict.tone}`}>{verdict.label}</span>
      </div>
      <div className="pd-xv-bar">
        <span>Thực tế</span>
        <span className="pd-xv-tr">
          {actual != null && <i className="pd-xv-real" style={{ width: `${realWidth}%` }} />}
        </span>
        <b>{actual == null ? '—' : actual}</b>
      </div>
      <div className="pd-xv-bar">
        <span>Kỳ vọng</span>
        <span className="pd-xv-tr">
          {expected != null && <i className="pd-xv-exp" style={{ width: `${expWidth}%` }} />}
        </span>
        <b>{expected == null ? '—' : Number(expected).toFixed(1)}</b>
      </div>
    </div>
  )
}

export default function PlayerExpected({ totals }) {
  return (
    <section className="pd-card pd-sec">
      <SectionHeader
        icon={<TargetIcon />}
        title="Thực tế so với kỳ vọng"
        lead="Số thật so với số đáng lẽ có (xG, xA)"
      />
      <ExpectedItem title="Bàn thắng" actual={totals?.goals ?? null} expected={totals?.expectedGoals ?? null} />
      <ExpectedItem title="Kiến tạo" actual={totals?.assists ?? null} expected={totals?.expectedAssists ?? null} />
    </section>
  )
}
