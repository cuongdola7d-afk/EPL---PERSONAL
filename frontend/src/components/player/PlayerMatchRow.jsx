import { effectiveStat, matchRating, matchResult, matchState } from '../../utils/playerDetail.js'
import { RATING_TIER, formatMatchCellValue } from '../../utils/playerProfile.js'
import { hasMatchScore } from '../../utils/seasons.js'
import { matchDetailHash } from '../../utils/matchRoute.js'

const RESULT_LABEL = { W: 'Thắng', D: 'Hòa', L: 'Thua' }

export default function PlayerMatchRow({ row, season, columns }) {
  const isProvisional = row.dataStatus === 'PROVISIONAL_FPL'
  const state = matchState(row)
  const result = matchResult(row)
  const home = row.clubId === row.match.homeClubId
  const opponent = home ? row.match.awayClub : row.match.homeClub
  const score = hasMatchScore(row.match) ? `${row.match.homeGoals}–${row.match.awayGoals}` : '—'
  const played = state === 'played'
  const minutes = played ? effectiveStat(row, 'minutes') : null
  const rating = played ? matchRating(row) : null
  const tier = RATING_TIER(rating)

  const subSuffix = state === 'did-not-play'
    ? ' · Không ra sân'
    : state === 'missing'
      ? ' · Chưa có dữ liệu'
      : ''

  return (
    <li>
      <a
        className={`pd-mrow${played ? '' : ' dnp'}`}
        href={matchDetailHash(row.match.id, { season, week: row.match.matchweek }, window.location.hash)}
      >
        {isProvisional ? (
          <span
            className="pd-res pd-res-prov"
            aria-label={`Số liệu tạm FPL · ${score}`}
            title="Số liệu tạm từ FPL"
          >
            Tạm
          </span>
        ) : (
          <span
            className={`pd-res pd-res-${result ?? 'U'}`}
            aria-label={result ? `${RESULT_LABEL[result]} ${score}` : `Tỉ số ${score}`}
          >
            {result ?? '—'}
          </span>
        )}

        <span className="pd-mmain">
          <b>{opponent}</b>
          <small>
            <span className={`pd-ha pd-ha-${home ? 'H' : 'A'}`}>{home ? 'Nhà' : 'Khách'}</span>
            <b className="pd-sc">{score}</b>
            {' · '}GW{row.match.matchweek}
            {subSuffix}
          </small>
        </span>

        <span className="pd-mright">
          {played ? (
            <>
              <div className="pd-mini">
                <span>
                  <b className={minutes == null || minutes === 0 ? 'z' : ''}>
                    {minutes == null ? '—' : `${minutes}'`}
                  </b>
                  <small>Phút</small>
                </span>
                {columns.map((col) => {
                  const cell = formatMatchCellValue(row, col)
                  return (
                    <span key={col.key}>
                      <b className={cell.muted ? 'z' : ''}>{cell.text}</b>
                      <small>{col.label}</small>
                    </span>
                  )
                })}
              </div>
              <span
                className={`pd-rt pd-rt-${tier}`}
                aria-label={rating == null ? 'Chưa được chấm điểm' : `Rating ${rating.toFixed(1)}`}
              >
                {rating == null ? '—' : rating.toFixed(1)}
              </span>
            </>
          ) : (
            <span className="pd-rt pd-rt-na">{state === 'did-not-play' ? 'DNP' : '—'}</span>
          )}
        </span>
      </a>
    </li>
  )
}
