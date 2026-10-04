# Sân Fantasy sau đăng nhập — 05/10/2026

Người dùng báo đăng nhập làm mất sân. Đã kiểm tra đúng phạm vi, chỉ đọc production; không nhập SQL, commit/push/deploy hoặc chạy lại auth/backend test.

## Nguyên nhân đã đối chiếu

- GET `/api/fantasy/2026/gameweeks` trả HTTP 500 cả khi gọi trực tiếp Railway và qua Vercel, nên không phải lỗi proxy/Google login.
- Đọc information_schema của MySQL Railway xác nhận cả chín bảng Fantasy bước 2–4 chưa tồn tại: fantasy_gameweeks, fantasy_deadline_changes, fantasy_entries, fantasy_draft_picks, fantasy_submitted_picks, fantasy_unrated_confirmations, fantasy_fixture_confirmations, fantasy_result_publications, fantasy_team_results. Cột roster_as_of cũng chưa có vì bảng GW chưa có. Không đọc email/session hoặc dữ liệu bóng đá.
- Frontend chỉ hiển thị editor khi entry.ready; ResultPanel lại ẩn nội dung lúc loading/error/roster rỗng. Khi overview lỗi, selected còn NULL nhưng select hiển thị GW6; so sánh NULL < 6 làm xuất hiện thông báo Replay sai.

## Sửa local

- GW mặc định đang hiển thị là GW6 được truyền cho màn đội ngay cả khi overview chưa tải. Sau khi API trả recommendation hợp lệ vẫn dùng recommendation như trước; không tự mở cuộc thi hoặc đoán roster/deadline.
- Sân luôn có 11 ô cho tài khoản khi đội/roster tải lỗi. ResultPanel có keepContent tùy chọn, chỉ dùng cho sân Fantasy; màn tra cứu khác giữ hành vi cũ. Nút retry nằm ngoài fieldset bị khóa nên vẫn bấm được.
- Thiếu entry/roster hoặc vòng khóa thì không được chọn/lưu/chốt; không tải roster khách thay cho rosterAsOf của GW.
- Snapshot đội đã chốt vẫn hiển thị khi vòng khóa và tải roster lỗi. Chỉ dùng snapshot khi entry.ready khớp chủ/GW; trong lúc tải chủ/GW mới dùng sân trống, không lộ đội cũ.
- Không báo Replay nếu chưa xác định GW. Có thông báo chưa tải đội/mốc danh sách và thao tác thử lại. Không nhập/ghi đè lựa chọn localStorage.

## Kiểm chứng

7 test frontend liên quan entry/gameweek và Vite build đạt. Trình duyệt Chrome riêng, frontend build thật, API giả lập loopback tái hiện production 500: đăng nhập có 11 ô nhưng khóa, không báo Replay sai, giữ localStorage, phục hồi API + retry cho phép chọn, vòng khóa giữ 11 snapshot khi roster lỗi, đổi tài khoản xóa snapshot cũ. Desktop 1440px/390px, không tràn ngang/lỗi JavaScript; không gửi thao tác ghi hoặc fallback roster khách. Helper/ảnh/report chỉ nằm backend/target/fantasy-pitch-check được ignore.

## Production còn chặn

Sửa giao diện đã hoàn thiện local; website thật chưa nhận bản sửa và API Fantasy vẫn lỗi vì thiếu migration. Không xử lý bằng tắt CSRF, bỏ khóa deadline, dùng roster ngày cố định hoặc tạo cuộc thi giả.

Khi được giao nhập/phát hành: xác nhận MySQL đích, tạo và kiểm tra backup mới ngoài Git, áp dụng migration theo thứ tự dưới đây, giữ SQL init production tắt; không nhập seed/tài khoản H2 hoặc tự mở GW đã hết hạn:

1. backend/sql/2026-10-05-fantasy-gameweeks-mysql.sql
2. backend/sql/2026-10-05-fantasy-entries-mysql.sql
3. backend/sql/2026-10-05-fantasy-roster-reference-mysql.sql
4. backend/sql/2026-10-05-fantasy-results-mysql.sql

Sau migration, API overview phải trả 200 và GW chưa công bố vẫn canEdit=false. Quản trị mở cuộc thi còn hạn bằng thao tác công bố deadline/rosterAsOf đã có, không coi migration là đã mở vòng. Phát hành bản sửa frontend bằng luồng được giao, kiểm tra lại sân và API thật.

File cần commit: frontend/src/components/FantasyPage.jsx, FantasyGameweek.jsx, ResultPanel.jsx; docs/fantasy-pitch-recovery-2026.md, fantasy-multiplayer-2026-plan.md.

Commit message: `fix(fantasy): keep the lineup pitch visible when gameweek data fails`.
