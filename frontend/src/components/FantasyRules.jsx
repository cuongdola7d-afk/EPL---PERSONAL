import { useEffect, useRef } from 'react'
import { MAX_OVR } from '../fantasy/lineup.js'
import { formatDeadline } from '../fantasy/gameweek.js'
import './FantasyRules.css'

export default function FantasyRules({ gameweek, deadlineUtc }) {
  const dialog = useRef(null)
  const trigger = useRef(null)
  const previousOverflow = useRef(null)
  useEffect(() => () => {
    if (previousOverflow.current !== null) document.body.style.overflow = previousOverflow.current
  }, [])

  function open() {
    previousOverflow.current = document.body.style.overflow
    dialog.current.showModal()
    document.body.style.overflow = 'hidden'
  }
  function closed() {
    if (previousOverflow.current !== null) document.body.style.overflow = previousOverflow.current
    previousOverflow.current = null
    trigger.current?.focus({ preventScroll: true })
  }
  function backdrop(event) {
    if (event.target !== event.currentTarget) return
    const rect = event.currentTarget.getBoundingClientRect()
    if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) dialog.current.close()
  }
  function keyboard(event) {
    if (event.key !== 'Tab') return
    const controls = [...event.currentTarget.querySelectorAll('button:not([disabled]), [tabindex="0"]')]
    const target = event.shiftKey && document.activeElement === controls[0] ? controls.at(-1) :
      !event.shiftKey && document.activeElement === controls.at(-1) ? controls[0] : null
    if (target) { event.preventDefault(); target.focus() }
  }

  return <>
    <button ref={trigger} type="button" className="fantasy-rules-trigger" aria-haspopup="dialog" aria-controls="fantasy-rules-dialog" onClick={open}>
      <span aria-hidden="true">i</span>Luật chơi
    </button>
    <dialog ref={dialog} id="fantasy-rules-dialog" className="fantasy-rules-dialog" aria-labelledby="fantasy-rules-title"
      aria-describedby="fantasy-rules-intro" onClose={closed} onClick={backdrop} onKeyDown={keyboard}>
      <header className="fantasy-rules-head">
        <h2 id="fantasy-rules-title">Luật chơi Fantasy PrismaXI</h2>
        <button type="button" className="fantasy-rules-close" aria-label="Đóng luật chơi" autoFocus onClick={() => dialog.current.close()}>×</button>
      </header>
      <div className="fantasy-rules-body" tabIndex={0} aria-label="Nội dung luật chơi có thể cuộn">
        <p id="fantasy-rules-intro">Fantasy mùa 2026/27 · Chọn đội, lưu trước hạn và theo dõi kết quả qua từng Gameweek.</p>
        <section aria-labelledby="fantasy-rules-selection"><h3 id="fantasy-rules-selection">Chọn đội</h3><ul>
          <li>Mỗi tài khoản có một đội hình tham gia cho mỗi Gameweek.</li>
          <li>Đội gồm đúng 11 cầu thủ, không chọn trùng.</li>
          <li>Chọn một sơ đồ trong các sơ đồ hệ thống hiện hỗ trợ.</li>
          <li>Mỗi cầu thủ phải có vị trí hợp lệ cho ô được chọn. Vị trí phụ được xét theo dữ liệu của từng cầu thủ.</li>
          <li>Tối đa 3 cầu thủ thuộc cùng một CLB.</li>
          <li>Tổng OVR của đội không vượt quá {MAX_OVR}.</li>
          <li>Cầu thủ thiếu OVR hoặc dữ liệu vị trí cần thiết chưa đủ điều kiện chọn.</li>
        </ul></section>
        <section aria-labelledby="fantasy-rules-save"><h3 id="fantasy-rules-save">Lưu đội và deadline</h3>
          <p className="fantasy-rules-deadline">{deadlineUtc ? <>Hạn của GW{gameweek}: <strong>{formatDeadline(deadlineUtc)}</strong> · Giờ Việt Nam</> : 'Deadline của Gameweek đang chọn chưa được công bố hoặc chưa tải được.'}</p>
          <ul>
            <li>Cần đăng nhập để lưu đội tham gia.</li>
            <li>Có thể thay đổi và bấm “Lưu đội hình” nhiều lần trước deadline.</li>
            <li>Đội được lưu thành công gần nhất là đội tham gia của Gameweek.</li>
            <li>Đến deadline, đội hình vẫn hiển thị nhưng bị khóa chỉnh sửa.</li>
            <li>Deadline hiển thị theo giờ Việt Nam từ dữ liệu Gameweek. Backend quyết định khóa theo thời gian server.</li>
          </ul>
        </section>
        <section aria-labelledby="fantasy-rules-score"><h3 id="fantasy-rules-score">Điểm và kết quả</h3><ul>
          <li>Điểm đội bằng tổng điểm rating SofaScore của 11 cầu thủ theo quy tắc hiện có.</li>
          <li>Người được xác nhận không ra sân có điểm 0.</li>
          <li>Người ra sân nhưng được xác nhận không có rating SofaScore nhận 0 điểm theo quy tắc unrated hiện có.</li>
          <li>Rating chưa thu thập là dữ liệu đang chờ, khác với trường hợp đã xác nhận không được chấm; không tự tính thành 0.</li>
          <li>Nếu một cầu thủ có nhiều trận thuộc cùng Gameweek, cộng điểm các trận; mỗi khóa cầu thủ–trận chỉ tính một lần.</li>
          <li>Kết quả chỉ xuất hiện sau khi dữ liệu đủ và ADMIN công bố; nhập thống kê không tự công bố kết quả.</li>
        </ul></section>
        <section aria-labelledby="fantasy-rules-ranking"><h3 id="fantasy-rules-ranking">Bảng xếp hạng</h3><ul>
          <li>Có BXH từng Gameweek và cả mùa.</li>
          <li>Điểm cao xếp trên; bằng điểm đồng hạng.</li>
          <li>Không lưu đội hợp lệ trước deadline thì không tham gia Gameweek đó.</li>
        </ul></section>
      </div>
    </dialog>
  </>
}
