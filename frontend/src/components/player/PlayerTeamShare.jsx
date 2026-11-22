import { buildCleanSheetCells, buildTeamShareCells } from '../../utils/playerProfile.js'
import { SectionHeader, ShieldIcon, TeamIcon } from './PlayerSectionIcons.jsx'

export default function PlayerTeamShare({ mode, clubName, playerName, teamContribution, cleanSheets }) {
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
