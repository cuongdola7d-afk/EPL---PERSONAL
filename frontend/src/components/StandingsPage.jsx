import { useCallback } from 'react'
import { fetchStandings } from '../api/standings.js'
import { useApiList } from '../hooks/useApiList.js'
import ResultPanel from './ResultPanel.jsx'

const STATS = [
  { key: 'played', short: 'Tr', label: 'Trận' },
  { key: 'won', short: 'T', label: 'Thắng' },
  { key: 'drawn', short: 'H', label: 'Hòa' },
  { key: 'lost', short: 'B', label: 'Thua' },
  { key: 'goalsFor', short: 'BT', label: 'Bàn thắng' },
  { key: 'goalsAgainst', short: 'BB', label: 'Bàn thua' },
  { key: 'goalDifference', short: 'HS', label: 'Hiệu số' },
]

function StandingsPage() {
  const requestStandings = useCallback((signal) => fetchStandings(signal), [])
  const { data: standings, status, error, reload } = useApiList(requestStandings)

  return (
    <section className="directory-section" id="directory" aria-labelledby="standings-heading">
      <div className="container">
        <div className="section-heading">
          <div>
            <p className="section-kicker">KHÁM PHÁ GIẢI ĐẤU <span>04 / TABLE</span></p>
            <h2 id="standings-heading">Bảng xếp hạng</h2>
            <p className="section-description">Bảng xếp hạng cuối mùa 2024/25 từ dữ liệu nhà cung cấp; đây không phải thứ hạng sau Gameweek 1.</p>
          </div>
          <button className="refresh-button" type="button" onClick={reload} disabled={status === 'loading'}>Làm mới</button>
        </div>

        <ResultPanel
          status={status}
          error={error}
          count={standings.length}
          itemName="bảng xếp hạng"
          emptyMessage="API hiện chưa có dữ liệu bảng xếp hạng."
          onRetry={reload}
        >
          <div className="standings-table-wrap">
            <table className="standings-table">
              <thead>
                <tr>
                  <th scope="col">Hạng</th>
                  <th scope="col">Câu lạc bộ</th>
                  {STATS.map((stat) => <th key={stat.key} scope="col" title={stat.label}>{stat.short}</th>)}
                  <th scope="col">Điểm</th>
                </tr>
              </thead>
              <tbody>
                {standings.map((row) => (
                  <tr key={row.clubId}>
                    <td className="standing-position">{row.position}</td>
                    <th scope="row" className="standing-club">{row.clubName}</th>
                    {STATS.map((stat) => <td key={stat.key}>{row[stat.key]}</td>)}
                    <td className="standing-points">{row.points}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="standing-cards">
            {standings.map((row) => (
              <article className="standing-card" key={row.clubId}>
                <div className="standing-card-head">
                  <span className="standing-card-rank">{row.position}</span>
                  <h3>{row.clubName}</h3>
                  <div className="standing-card-points"><strong>{row.points}</strong><span>Điểm</span></div>
                </div>
                <dl className="standing-card-stats">
                  {STATS.map((stat) => (
                    <div key={stat.key}><dt>{stat.label}</dt><dd>{row[stat.key]}</dd></div>
                  ))}
                </dl>
              </article>
            ))}
          </div>
        </ResultPanel>
      </div>
    </section>
  )
}

export default StandingsPage
