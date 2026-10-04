import { getInitials } from '../utils/initials.js'

const CLUB_COLORS = ['#b44955', '#425caa', '#3c987e', '#a26c35', '#705aaa', '#497d9b']
export function clubColor(id) { return CLUB_COLORS[Math.abs(id) % CLUB_COLORS.length] }
export function ratingTier(overall) { return overall >= 85 ? 'elite' : overall >= 78 ? 'strong' : overall >= 70 ? 'good' : 'basic' }

export default function PlayerAvatar({ player, group, small = false, rating }) {
  const matchRating = rating !== undefined
  const tier = matchRating ? rating >= 9 ? 'elite' : rating >= 7 ? 'strong' : rating >= 5 ? 'good' : 'basic' : ratingTier(player?.fc27Overall)
  return <span className={`fantasy-avatar${small ? ' fantasy-avatar-small' : ''}${player ? '' : ' fantasy-avatar-empty'}`}
    style={player ? { '--club-color': clubColor(player.clubId) } : undefined} aria-hidden="true">
    {player ? getInitials(player.name) : '+'}
    {player && !small && <span className={`fantasy-rating fantasy-rating-${tier}`}>{matchRating ? rating.toFixed(2) : player.fc27Overall}</span>}
    {small && group && <span className={`fantasy-position fantasy-position-${group}`}>{group === 'GOALKEEPER' ? 'GK' : group === 'DEFENDER' ? 'DEF' : group === 'MIDFIELDER' ? 'MID' : 'FWD'}</span>}
  </span>
}
