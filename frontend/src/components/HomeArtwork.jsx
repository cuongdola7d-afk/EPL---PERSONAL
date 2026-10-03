import { MAX_OVR } from '../fantasy/lineup.js'
import { HomeCrest } from './ClubCard.jsx'
import { hasMatchScore } from '../utils/seasons.js'

export function ArrowIcon({ diagonal = false, left = false }) {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4"
    strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d={diagonal ? 'M7 17L17 7M8 7h9v9' : left ? 'M15 6l-6 6 6 6' : 'M9 6l6 6-6 6'} />
  </svg>
}
export function SearchIcon() {
  return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
    strokeLinecap="round" aria-hidden="true"><circle cx="11" cy="11" r="7" /><path d="M20 20l-3.5-3.5" /></svg>
}
export function FantasyArtwork() {
  const slots = [[150,300,'GK'], [52,238,'LB'], [108,238,'CB'], [192,238,'CB'], [248,238,'RB'],
    [82,170,'CM'], [150,170,'CM'], [218,170,'CM'], [98,102,'LW'], [150,102,'ST'], [202,102,'RW']]
  const colors = ['#c8102e', '#5ba3d9', '#e0413a', '#315ba5', '#58a5d0', '#1f4fa3', '#7b1d4a', '#da291c', '#29436b', '#4a545b', '#c92834']
  return <svg className="cx-fantasy-art" viewBox="0 0 300 340" aria-hidden="true">
    <polygon points="64,8 236,8 292,334 8,334" fill="#2f8f4a" />
    <polygon points="64,8 236,8 242,42 58,42" fill="#fff" opacity=".07" />
    <polygon points="52,112 248,112 260,176 40,176" fill="#fff" opacity=".07" />
    <g fill="none" stroke="#ffffff99" strokeWidth="2" strokeLinejoin="round">
      <polygon points="64,8 236,8 292,334 8,334" /><path d="M118 8a32 20 0 0 0 64 0" />
      <path d="M40 176h220" /><ellipse cx="150" cy="176" rx="35" ry="25" />
      <polyline points="68,334 74,278 226,278 232,334" />
    </g>
    {slots.map(([x,y,position], index) => <g key={index}>
      <circle cx={x} cy={y} r="17" fill={colors[index]} stroke="#fff" strokeWidth="2.5" />
      <text x={x} y={y + 4} textAnchor="middle" fontSize="10" fontWeight="700" fill="#fff">{position}</text>
    </g>)}
  </svg>
}
export default function HomeArtwork({ kind, fixtures = [], week }) {
  if (kind === 'clubs') return <svg viewBox="0 0 400 400" aria-hidden="true">
    <circle cx="200" cy="200" r="190" fill="var(--cx-ring0)" stroke="var(--cx-ring-line)" />
    <circle cx="200" cy="200" r="132" fill="var(--cx-ring1)" stroke="var(--cx-ring-line)" />
    <circle cx="200" cy="200" r="86" fill="none" stroke="var(--cx-accent)" opacity=".5" strokeDasharray="3 6" />
    <g className="cx-orbit"><circle cx="283" cy="97" r="17" fill="var(--cx-accent)" opacity=".18" /><circle cx="283" cy="97" r="9" fill="var(--cx-accent)" /></g>
    <g className="cx-orbit cx-orbit-reverse"><circle cx="105" cy="338" r="15" fill="var(--cx-accent)" opacity=".18" /><circle cx="105" cy="338" r="8" fill="var(--cx-accent)" /></g>
    <circle cx="200" cy="200" r="64" fill="#0f2a33" stroke="var(--cx-accent)" strokeWidth="3" />
    <image href="/prismaxi-logo.svg" x="154" y="157" width="92" height="86" />
    <text x="200" y="337" textAnchor="middle" fontSize="11" fontWeight="700" letterSpacing="3" fill="var(--cx-muted)">THE BEAUTIFUL GAME</text>
  </svg>
  if (kind === 'fantasy') return <><div className="cx-bob"><FantasyArtwork /></div>
    <span className="cx-art-chip cx-art-chip-top">11 cầu thủ · tối đa {MAX_OVR} OVR</span>
    <span className="cx-art-chip cx-art-chip-bottom">Sơ đồ 4-3-3</span></>
  if (kind === 'matches') return <div className="cx-fixture-art" aria-hidden="true">
    <span className="cx-gw-tag">{week ? `Gameweek ${week}` : 'Lịch đấu & kết quả'}</span>
    {fixtures.slice(0, 3).map((match, index) => <div className="cx-art-fixture cx-bob" key={match.id}
      style={{ '--fixture-y': `${17 + index * 24}%`, animationDelay: `${index * .6}s` }}>
      <HomeCrest name={match.homeClub} />
      <span className="cx-art-vs">{hasMatchScore(match) ? `${match.homeGoals} – ${match.awayGoals}` : 'vs'}</span>
      <HomeCrest name={match.awayClub} />
    </div>)}
    {!fixtures.length && <div className="cx-art-calendar"><span>GW</span><strong>Premier League</strong><span>Lịch đấu & kết quả</span></div>}
  </div>
  return <div className="cx-player-art" aria-hidden="true">
    {['GK', 'CM', 'ST', 'RW'].map((position, index) => <div className={`cx-art-player cx-art-player-${index} cx-bob`} key={position}>
      <span>{position}</span><small>Hồ sơ cầu thủ</small></div>)}
    <div className="cx-art-search"><SearchIcon />Tìm cầu thủ theo tên</div>
  </div>
}
