import { ESTIMATED_OVR_PLAYER_IDS } from '../fantasy/lineup.js'
import { getInitials } from '../utils/initials.js'
import { playerDetailHash } from '../utils/playerRoute.js'

const POSITION = {
  GOALKEEPER: ['GK', 'Thủ môn'], DEFENDER: ['DEF', 'Hậu vệ'],
  MIDFIELDER: ['MID', 'Tiền vệ'], FORWARD: ['FWD', 'Tiền đạo'],
}
const POSITION_COLORS = { GOALKEEPER: '#f5c15c', DEFENDER: '#7db4f7', MIDFIELDER: '#5cd697', FORWARD: '#ff8b8b' }

const COLORS = [
  ['arsenal', '#e0413a'], ['liverpool', '#c8102e'], ['manchester city', '#5ba3d9'],
  ['manchester united', '#da291c'], ['chelsea', '#1f4fa3'], ['newcastle', '#4a545b'],
  ['aston villa', '#7b1d4a'], ['everton', '#1c5bb0'], ['bournemouth', '#ad252f'],
  ['brentford', '#c92834'], ['brighton', '#2362b0'], ['crystal palace', '#2d5eaa'],
  ['fulham', '#48535e'], ['leeds', '#e0b52c'], ['sunderland', '#c72d37'],
  ['nottingham forest', '#d42329'], ['tottenham', '#29436b'], ['ipswich', '#315ba5'],
  ['hull', '#d89720'], ['coventry', '#58a5d0'],
]

function clubColor(name) {
  return COLORS.find(([part]) => name.toLowerCase().includes(part))?.[1] ?? '#487783'
}

function PlayerCard({ player, season, view }) {
  const [positionCode, positionLabel] = POSITION[player.position] ?? [player.position, player.position]
  const estimated = ESTIMATED_OVR_PLAYER_IDS.has(player.id)
  const tier = player.fc27Overall == null ? 'na' : player.fc27Overall >= 85 ? 'gold' :
    player.fc27Overall >= 78 ? 'green' : player.fc27Overall >= 70 ? 'blue' : 'silver'
  const color = clubColor(player.club)
  const overall = <div className="pp-overall-wrap"><span className={`pp-overall pp-overall-${tier}`}>{player.fc27Overall ?? '—'}</span>{view === 'grid' && <small>{estimated ? 'ước tính' : player.fc27Overall == null ? 'chưa có' : 'overall'}</small>}</div>
  const avatar = <span className="pp-avatar" style={{ '--pp-club-color': color, '--pp-position-color': POSITION_COLORS[player.position] ?? '#5cd697' }} aria-hidden="true">{getInitials(player.name)}{view === 'grid' && <span className={`pp-position pp-position-${positionCode}`}>{positionCode}</span>}</span>

  return <a className={`pp-player pp-player-${view}`} href={playerDetailHash(player.id, season)} aria-label={`Xem hồ sơ ${player.name}, ${player.club}, ${player.fc27Overall == null ? 'chưa có OVR' : `OVR ${player.fc27Overall}${estimated ? ' ước tính' : ''}`}`}>
    {view === 'grid' ? <>
      <div className="pp-player-top">{avatar}{overall}</div>
      <div className="pp-player-identity"><strong>{player.name}</strong><span><i style={{ background: color }} />{player.club}</span></div>
      <div className="pp-player-go">Xem hồ sơ <span aria-hidden="true">→</span></div>
    </> : <>
      {avatar}<div className="pp-player-identity"><strong>{player.name}</strong><span><i style={{ background: color }} />{player.club}</span></div>
      <span className={`pp-position pp-position-${positionCode}`}>{positionLabel}</span>{overall}<span className="pp-list-arrow" aria-hidden="true">→</span>
    </>}
  </a>
}

export default PlayerCard
