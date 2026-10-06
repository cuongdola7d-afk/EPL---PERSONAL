import { useEffect, useRef } from 'react'
import { MAX_OVR } from '../fantasy/lineup.js'
import HomeArtwork, { ArrowIcon } from './HomeArtwork.jsx'

export default function HomeHero({ active, onChange, fixtures, week }) {
  const trackRef = useRef(null)
  const activeRef = useRef(active)
  const drag = useRef(null)
  const moved = useRef(false)
  activeRef.current = active
  const slides = [
    { key: 'clubs', label: 'Câu lạc bộ', title: '20 câu lạc bộ.', highlight: 'Một cuộc đua.',
      description: 'Khám phá các đội bóng Premier League, đội hình và hành trình của họ qua từng vòng đấu.', cta: 'Khám phá câu lạc bộ' },
    { key: 'fantasy', label: 'Fantasy · 2026/27', title: 'Chọn XI trong mơ của bạn.', highlight: 'Chinh phục từng vòng đấu.',
      description: `Chọn 11 cầu thủ theo sơ đồ và giới hạn ${MAX_OVR} OVR. Lưu đội hình trước hạn rồi cạnh tranh điểm số qua từng Gameweek.`, cta: 'Tạo đội hình', href: '#fantasy' },
    { key: 'matches', label: 'Lịch đấu', title: 'Từng vòng đấu.', highlight: 'Đừng bỏ lỡ.',
      description: 'Xem lịch và kết quả từng vòng, bấm vào một trận để mở đội hình, điểm đánh giá và thống kê cầu thủ.', cta: 'Xem lịch đấu', href: '#matches' },
    { key: 'players', label: 'Cầu thủ', title: 'Mỗi cầu thủ.', highlight: 'Một lăng kính.',
      description: 'Tra cứu cầu thủ theo tên, xem điểm đánh giá từng trận và số liệu cả mùa theo cách dễ nhìn, dễ hiểu.', cta: 'Tìm cầu thủ', href: '#players' },
  ]
  useEffect(() => {
    const element = trackRef.current
    const observer = new ResizeObserver(() => element.scrollTo({ left: activeRef.current * element.clientWidth, behavior: 'instant' }))
    observer.observe(element)
    return () => observer.disconnect()
  }, [])
  function go(index) {
    const element = trackRef.current
    element.scrollTo({ left: Math.max(0, Math.min(slides.length - 1, index)) * element.clientWidth,
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' })
  }
  function finishDrag(event) {
    if (!drag.current) return
    drag.current = null
    if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
    go(Math.round(event.currentTarget.scrollLeft / event.currentTarget.clientWidth))
  }
  return <section className="cx-hero" aria-roledescription="carousel" aria-label="Giới thiệu nổi bật">
    <span className="cx-swipe-hint" aria-hidden="true">↔ Lướt để xem thêm</span>
    <div className="cx-track" ref={trackRef} tabIndex={0}
      aria-label="Bốn mục nổi bật, dùng phím mũi tên trái hoặc phải để chuyển"
      onScroll={event => {
        const next = Math.max(0, Math.min(3, Math.round(event.currentTarget.scrollLeft / event.currentTarget.clientWidth)))
        if (next !== activeRef.current) onChange(next)
      }}
      onKeyDown={event => {
        if (['ArrowLeft', 'ArrowRight'].includes(event.key)) { event.preventDefault(); go(active + (event.key === 'ArrowRight' ? 1 : -1)) }
      }}
      onPointerDown={event => {
        if (event.pointerType !== 'mouse' || event.button !== 0 || event.target.closest('a, button')) return
        drag.current = { x: event.clientX, scroll: event.currentTarget.scrollLeft }
        moved.current = false
        event.currentTarget.setPointerCapture(event.pointerId)
        event.currentTarget.classList.add('cx-track-drag')
      }}
      onPointerMove={event => {
        if (!drag.current) return
        const delta = event.clientX - drag.current.x
        if (Math.abs(delta) > 4) moved.current = true
        event.currentTarget.scrollLeft = drag.current.scroll - delta
      }}
      onPointerUp={event => { event.currentTarget.classList.remove('cx-track-drag'); finishDrag(event) }}
      onPointerCancel={event => { event.currentTarget.classList.remove('cx-track-drag'); finishDrag(event) }}
      onClickCapture={event => { if (moved.current) { event.preventDefault(); event.stopPropagation(); moved.current = false } }}>
      {slides.map((slide, index) => {
        const Heading = index === 0 ? 'h1' : 'h2'
        return <article className={`cx-slide${slide.key === 'fantasy' ? ' cx-slide-fantasy' : ''}`} key={slide.key} inert={index !== active} aria-hidden={index !== active}
          aria-roledescription="slide" aria-label={`${index + 1} trên ${slides.length}`}>
          <div className="cx-slide-inner"><div className="cx-slide-copy">
            <p className="cx-eyebrow"><i aria-hidden="true" />prismaXI / {slide.label}</p>
            <Heading className="cx-headline"><span>{slide.title}</span><span>{slide.highlight}</span></Heading>
            <p className="cx-lead">{slide.description}</p>
            {slide.href ? <a className="cx-cta" href={slide.href}>{slide.cta}<ArrowIcon diagonal /></a> :
              <button className="cx-cta" type="button" onClick={() => document.getElementById('clubs')?.scrollIntoView()}>{slide.cta}<ArrowIcon diagonal /></button>}
          </div><div className="cx-graphic" aria-hidden="true"><HomeArtwork kind={slide.key} fixtures={fixtures} week={week} /></div></div>
        </article>
      })}
    </div>
    <div className="cx-hero-controls cx-wrap"><div className="cx-dots" role="group" aria-label="Chọn phần giới thiệu">
      {slides.map((slide, index) => <button key={slide.key} type="button" aria-label={`Xem giới thiệu ${slide.label}`}
        aria-current={index === active ? 'true' : undefined} onClick={() => go(index)} />)}
    </div><div className="cx-slide-controls"><span className="cx-slide-count" aria-live="polite">0{active + 1} / 04</span>
      <button type="button" aria-label="Slide trước" disabled={active === 0} onClick={() => go(active - 1)}><ArrowIcon left /></button>
      <button type="button" aria-label="Slide sau" disabled={active === 3} onClick={() => go(active + 1)}><ArrowIcon /></button>
    </div></div>
  </section>
}
