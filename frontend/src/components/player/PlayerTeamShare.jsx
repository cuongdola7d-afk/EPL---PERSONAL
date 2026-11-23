import {
  buildCleanSheetCells,
  buildDefensiveActionsSummary,
  buildSaveRateSummary,
  buildTeamShareCells,
} from '../../utils/playerProfile.js'
import { GloveIcon, SectionHeader, ShieldIcon, TeamIcon } from './PlayerSectionIcons.jsx'

export default function PlayerTeamShare({ mode, clubName, playerName, teamContribution, cleanSheets, totals }) {
  if (mode === 'saveRate') {
    const summary = buildSaveRateSummary(totals?.saves, totals?.goalsConceded)
    if (!summary) return null
    return (
      <section className="pd-card pd-sec">
        <SectionHeader
          icon={<GloveIcon />}
          title="Tỉ lệ cản phá"
          lead="Cứu thua trên tổng số cú sút trúng đích"
        />
        <div className="pd-tc pd-tc-stack">
          <div className="pd-tc-big">
            <b>{summary.pct == null ? '—' : `${summary.pct}%`}</b>
            <span>{summary.saves} cứu thua / {summary.shotsOnTarget} cú sút trúng đích</span>
          </div>
          {summary.shotsOnTarget > 0 && (
            <div
              className="pd-split-bar"
              role="img"
              aria-label={`${summary.saves} cứu thua và ${summary.goalsConceded} bàn thua`}
            >
              {summary.saves > 0 && (
                <span
                  className="pd-split-seg pd-split-save"
                  style={{ flexGrow: summary.saves }}
                  title={`Cứu thua: ${summary.saves}`}
                >
                  {summary.saves}
                </span>
              )}
              {summary.goalsConceded > 0 && (
                <span
                  className="pd-split-seg pd-split-gc"
                  style={{ flexGrow: summary.goalsConceded }}
                  title={`Bàn thua: ${summary.goalsConceded}`}
                >
                  {summary.goalsConceded}
                </span>
              )}
            </div>
          )}
        </div>
        <div className="pd-tc-leg">
          <span><i className="pd-c-g" />Cứu thua {summary.saves}</span>
          <span><i className="pd-c-gc" />Bàn thua {summary.goalsConceded}</span>
        </div>
      </section>
    )
  }

  if (mode === 'defensiveActions') {
    const summary = buildDefensiveActionsSummary(
      totals?.tackles,
      totals?.clearancesBlocksInterceptions,
      totals?.recoveries,
      totals?.appearances,
    )
    if (!summary) return null
    return (
      <section className="pd-card pd-sec">
        <SectionHeader
          icon={<ShieldIcon />}
          title="Hoạt động phòng ngự"
          lead="Tắc bóng, chặn/cắt và đoạt bóng mỗi trận"
        />
        <div className="pd-tc pd-tc-stack">
          <div className="pd-tc-big">
            <b>{summary.perMatch == null ? '—' : summary.perMatch}</b>
            <span>trung bình mỗi trận</span>
          </div>
          {summary.total > 0 && (
            <div
              className="pd-stack-bar"
              role="img"
              aria-label={`Tắc bóng ${summary.tackles}, Chặn/cắt ${summary.cbi}, Đoạt bóng ${summary.recoveries}`}
            >
              {summary.tackles > 0 && (
                <span
                  className="pd-stack-seg pd-def-s1"
                  style={{ flexGrow: summary.tackles }}
                  title={`Tắc bóng: ${summary.tackles}`}
                />
              )}
              {summary.cbi > 0 && (
                <span
                  className="pd-stack-seg pd-def-s2"
                  style={{ flexGrow: summary.cbi }}
                  title={`Chặn/cắt: ${summary.cbi}`}
                />
              )}
              {summary.recoveries > 0 && (
                <span
                  className="pd-stack-seg pd-def-s3"
                  style={{ flexGrow: summary.recoveries }}
                  title={`Đoạt bóng: ${summary.recoveries}`}
                />
              )}
            </div>
          )}
        </div>
        <div className="pd-tc-leg">
          <span><i className="pd-def-s1" />Tắc bóng {summary.tackles}</span>
          <span><i className="pd-def-s2" />Chặn/cắt {summary.cbi}</span>
          <span><i className="pd-def-s3" />Đoạt bóng {summary.recoveries}</span>
        </div>
      </section>
    )
  }

  if (mode === 'cleanSheets') {
    const cs = cleanSheets?.cleanSheets ?? null
    const apps = cleanSheets?.appearances ?? 0
    const { pct, other, cells } = buildCleanSheetCells(cs, apps)
    return (
      <section className="pd-card pd-sec">
        <SectionHeader
          icon={<ShieldIcon />}
          title="Giữ sạch lưới"
          lead="Số trận giữ sạch lưới trên tổng số trận ra sân"
        />
        <div className="pd-tc">
          <div className="pd-tc-big">
            <b>{pct == null ? '—' : `${pct}%`}</b>
            <span>{cs == null ? '—' : cs} / {apps} trận</span>
          </div>
          {cells.length > 0 && (
            <div
              className="pd-tc-cells"
              role="img"
              aria-label={`${cs} trận giữ sạch lưới trên ${apps} trận ra sân`}
            >
              {cells.map((type, idx) => (
                <i
                  key={idx}
                  className={type === 'cs' ? 'pd-c-g' : 'pd-c-o'}
                  title={type === 'cs' ? 'Giữ sạch lưới' : 'Trận thủng lưới'}
                />
              ))}
            </div>
          )}
        </div>
        <div className="pd-tc-leg">
          <span><i className="pd-c-g" />Giữ sạch lưới {cs == null ? '—' : cs}</span>
          <span><i className="pd-c-o" />Trận còn lại {other == null ? '—' : other}</span>
        </div>
      </section>
    )
  }

  const teamGoals = teamContribution?.teamGoals ?? 0
  const goals = teamContribution?.goals ?? null
  const assists = teamContribution?.assists ?? null
  const { pct, involved, other, cells } = buildTeamShareCells(teamGoals, goals, assists)

  return (
    <section className="pd-card pd-sec">
      <SectionHeader
        icon={<TeamIcon />}
        title="Đóng góp cho đội"
        lead={`Số bàn của ${clubName} có ${playerName} ghi hoặc kiến tạo`}
      />
      <div className="pd-tc">
        <div className="pd-tc-big">
          <b>{pct == null ? '—' : `${pct}%`}</b>
          <span>{involved == null ? '—' : involved} / {teamGoals} bàn</span>
        </div>
        {cells.length > 0 && (
          <div
            className="pd-tc-cells"
            role="img"
            aria-label={`${goals} bàn, ${assists} kiến tạo trên ${teamGoals} bàn của đội`}
          >
            {cells.map((type, idx) => (
              <i
                key={idx}
                className={type === 'goal' ? 'pd-c-g' : type === 'assist' ? 'pd-c-a' : 'pd-c-o'}
                title={type === 'goal' ? `Bàn của ${playerName}` : type === 'assist' ? `Kiến tạo của ${playerName}` : 'Bàn khác của đội'}
              />
            ))}
          </div>
        )}
      </div>
      <div className="pd-tc-leg">
        <span><i className="pd-c-g" />Ghi bàn {goals == null ? '—' : goals}</span>
        <span><i className="pd-c-a" />Kiến tạo {assists == null ? '—' : assists}</span>
        <span><i className="pd-c-o" />Bàn khác {other == null ? '—' : other}</span>
      </div>
    </section>
  )
}
