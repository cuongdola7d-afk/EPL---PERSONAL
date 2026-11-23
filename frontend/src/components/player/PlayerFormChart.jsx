import { useEffect, useState } from 'react'
import { PlayerEventIcons } from '../EventIcons.jsx'
import { formatFormDetail } from '../../utils/playerProfile.js'
import { FormIcon, SectionHeader } from './PlayerSectionIcons.jsx'

export default function PlayerFormChart({ formSeries, averageRating }) {
  const { items, bestId, defaultActiveId } = formSeries
  const [activeId, setActiveId] = useState(defaultActiveId)

  useEffect(() => {
    setActiveId(defaultActiveId)
  }, [defaultActiveId])

  if (!items.length) {
    return (
      <section className="pd-card pd-sec">
        <SectionHeader icon={<FormIcon />} title="Phong độ từng trận" lead="Chưa có trận đã hoàn thành trong mùa này" />
      </section>
    )
  }

  const activeItem = items.find((item) => item.id === activeId) ?? items.find((item) => item.id === defaultActiveId) ?? items[0]
  const detail = formatFormDetail(activeItem)
  const isShowingBest = activeItem && bestId != null && activeItem.id === bestId
  const lead = averageRating != null
    ? `Rating mỗi trận · đường đứt nét: trung bình ${averageRating.toFixed(1)}`
    : 'Rating mỗi trận'

  return (
    <section className="pd-card pd-sec">
      <SectionHeader icon={<FormIcon />} title="Phong độ từng trận" lead={lead} />

      <div className="pd-form-scroll">
        <div className="pd-form" style={{ minWidth: items.length > 14 ? `${items.length * 46}px` : undefined }}>
          {items.map((item) => {
            const isSelected = activeItem?.id === item.id
            if (item.state !== 'played') {
              const isDnp = item.state === 'did-not-play'
              const tag = isDnp ? 'DNP' : '—'
              const statusText = isDnp ? 'không ra sân' : 'Chưa có số liệu'
              const aria = `GW${item.matchweek} gặp ${item.opponent}, ${statusText}`
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`pd-fbar${isSelected ? ' on' : ''}`}
                  aria-label={aria}
                  title={isDnp ? 'Không ra sân' : 'Chưa có số liệu'}
                  aria-pressed={isSelected}
                  onClick={() => setActiveId(item.id)}
                  onMouseEnter={() => setActiveId(item.id)}
                  onFocus={() => setActiveId(item.id)}
                >
                  <span className="pd-dnp-box">{tag}</span>
                </button>
              )
            }

            if (item.rating == null) {
              return (
                <button
                  key={item.id}
                  type="button"
                  className={`pd-fbar${isSelected ? ' on' : ''}`}
                  aria-label={`GW${item.matchweek} gặp ${item.opponent}, chưa được chấm điểm`}
                  aria-pressed={isSelected}
                  onClick={() => setActiveId(item.id)}
                  onMouseEnter={() => setActiveId(item.id)}
                  onFocus={() => setActiveId(item.id)}
                >
                  <span className="pd-dnp-box">—</span>
                </button>
              )
            }

            const h = Math.max(10, Math.min(100, (item.rating / 10) * 100))
            return (
              <button
                key={item.id}
                type="button"
                className={`pd-fbar${isSelected ? ' on' : ''}`}
                aria-label={`GW${item.matchweek} gặp ${item.opponent}, rating ${item.rating.toFixed(1)}`}
                aria-pressed={isSelected}
                onClick={() => setActiveId(item.id)}
                onMouseEnter={() => setActiveId(item.id)}
                onFocus={() => setActiveId(item.id)}
              >
                <span className="pd-fval" style={{ bottom: `calc(${h}% + 4px)` }}>{item.rating.toFixed(1)}</span>
                <span className={`pd-bar pd-bar-${item.tier}`} style={{ height: `${h}%` }} />
              </button>
            )
          })}
          {averageRating != null && (
            <span
              className="pd-avg-line"
              style={{ bottom: `calc((100% - 22px) * ${(Math.min(10, Math.max(0, averageRating)) / 10).toFixed(3)})` }}
              aria-hidden="true"
            />
          )}
        </div>

        <div className="pd-fx" style={{ minWidth: items.length > 14 ? `${items.length * 46}px` : undefined }}>
          {items.map((item) => (
            <span key={item.id} className="pd-fx-item">
              <span>GW{item.matchweek}</span>
              <span className="pd-fx-ev">
                {item.state === 'played' && item.row.stats && (
                  <PlayerEventIcons player={item.row.stats} evidenceStatus={item.row.evidenceStatus} />
                )}
              </span>
            </span>
          ))}
        </div>
      </div>

      {detail && (
        <div className="pd-tip" role="status">
          {isShowingBest ? 'Trận hay nhất: ' : ''}
          <b>{detail.headline}</b>
          {detail.suffix}
        </div>
      )}
    </section>
  )
}
