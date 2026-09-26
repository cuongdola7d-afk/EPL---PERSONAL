import { SEASONS } from '../utils/seasons.js'

function SeasonPicker({ season, onChange }) {
  return (
    <div className="season-controls">
      <div className="filter-field">
        <label htmlFor="season-select">Mùa giải</label>
        <select id="season-select" value={season} onChange={(event) => onChange(Number(event.target.value))}>
          {Object.entries(SEASONS).map(([year, label]) => <option key={year} value={year}>{label}</option>)}
        </select>
      </div>
      {season === 2026 && (
        <p className="data-attribution">Data provided by <a href="https://www.football-data.org/" target="_blank" rel="noreferrer">football-data.org</a>.
          {' '}Dữ liệu theo lần đồng bộ gần nhất.</p>
      )}
    </div>
  )
}

export default SeasonPicker
