# Luồng người chơi: lưu đội, khóa deadline, kết quả và BXH — 05/10/2026

Luồng cuối đã chốt là **Lưu đội hình** cập nhật trực tiếp đội tham gia, không có bước bắt buộc lưu nháp rồi chốt riêng. Đã phát hành và mở GW6 production; người dùng đã kiểm chứng lưu → sửa → lưu lại → reload. Xem [checkpoint production](fantasy-production-2026-10-05.md). Lượt cập nhật quy trình chỉ sửa tài liệu/đọc BXH, không nhập thống kê, công bố điểm, đổi deadline hoặc chạy lại test/build.

## Hành vi

1. Đăng nhập mở tab Đội hình của bạn, sân 11 ô vẫn là màn hình chính. Tải đội từ server của đúng tài khoản/GW; không tự lấy lựa chọn localStorage hoặc đội vòng trước.
2. GW OPEN: chọn/đổi cầu thủ và sơ đồ theo luật hiện có; bấm Lưu đội hình khi đủ 11 người hợp lệ. Có thể sửa và lưu lại nhiều lần trước deadline. Chỉnh trên màn hình chưa thay đội dự thi cho tới khi lưu thành công.
3. Backend lưu đội có hiệu lực và snapshot trong transaction hiện có, dùng expectedVersion, session owner, CSRF, rosterAsOf và Clock server sau khóa/trước-sau SQL. Lưu mới lỗi giữ đội đã lưu trước. Nút duy nhất dùng endpoint `/submit` đã có; không thêm cơ chế tự chốt hoặc phụ thuộc cron.
4. Từ đúng deadline: server từ chối lưu, sân giữ đội đã lưu gần nhất ở trạng thái khóa. Hiển thị “Đã hết deadline. Đội hình đã khóa; kết quả sẽ được công bố sau.” Người chưa lưu đội hợp lệ không tự tham gia và không được cấp đội/điểm 0.
5. Công bố kết quả vẫn theo readiness và ADMIN + CSRF của bước 4; nhập rating đơn thuần chưa phải công bố. Dưới sân hiển thị thông báo kết quả, tổng, 11 dòng điểm, thời gian và version. Không thay rating NULL thô hay cách xác nhận không chấm.
6. Tab BXH người chơi có Gameweek/Cả mùa. Điểm cao xếp trên, bằng điểm đồng hạng 1,1,3; account ID chỉ ổn định thứ tự trong nhóm bằng điểm, không tách hạng. Highlight tài khoản hiện tại bằng ID.

API nháp cũ vẫn giữ để tương thích dữ liệu/client cũ, không còn là thao tác bắt buộc trong giao diện. Dữ liệu submitted cũ được trình bày là “Đội đã lưu”. Mỗi lần Lưu đội hình cập nhật draft/snapshot đồng bộ bằng service đã có; các sửa chưa lưu chỉ nằm trong bộ nhớ giao diện. Giữ các luật 11 người duy nhất, vị trí hợp lệ, tối đa 3/CLB và OVR không quá 860.

## Thông báo và cập nhật

Khi đang xem đội của GW đã khóa và còn chờ, frontend đọc lại kết quả mỗi 60 giây nếu tab trình duyệt đang hiển thị; trở lại tab cũng tải lại. Sau khi nhận PUBLISHED, có thông báo trong trang bằng role=status, tải lại trạng thái GW và dừng polling chờ kết quả. Có nút Cập nhật kết quả/thử lại. Đây là thông báo trong website, chưa có email/push/notification hệ điều hành; không hứa cập nhật tức thời qua websocket.

BXH chỉ tải/poll khi tab BXH được mở; cập nhật 60 giây lúc trình duyệt hiển thị và khi trở lại tab. Đổi GW/scope hủy request cũ và bỏ dữ liệu vòng trước. Ba tab hỗ trợ bàn phím Home/End/Arrow và không tràn ở 390px.

## API BXH và tính nhất quán

- GET `/api/fantasy/2026/leaderboard?gameweek=6`: điểm GW6 đã công bố; GW chính thức chỉ 6–38. GW1–5 được giao diện giải thích là Replay, không gửi request cuộc thi chính thức.
- GET `/api/fantasy/2026/leaderboard`: tổng mùa 2026/27 của các GW đã công bố.
- Response: season, gameweek (NULL cho mùa), status AWAITING_RESULTS/PUBLISHED, version (GW), publishedGameweeks và players gồm rank/accountId/displayName/totalPoints/gameweeksPlayed.
- Chưa công bố: danh sách rỗng, không trả điểm tạm. Không trả email, password hash, session, draft hoặc đội người khác. Đây là API công khai GET, no-store, frontend không gửi cookie cho request BXH.
- Query lấy `fantasy_team_results` của MAX(version) mỗi GW, chỉ khi fantasy_gameweeks PUBLISHED cùng results_published_at, rồi SUM/COUNT. Đọc trong transaction REPEATABLE_READ; tái tính nguồn ở bước 4 vẫn thay phiên bản cả GW atomic. Không cộng lại kết quả của version cũ.
- Điểm lấy BigDecimal/DECIMAL đã lưu, không dùng OVR hoặc tính điểm lại trên frontend. Danh sách giới hạn 200 người điểm cao nhất; chưa có phân trang/xem đội người khác hoặc vị trí cá nhân ngoài Top 200.
- Không tạo bảng hoặc migration mới cho BXH, dùng cấu trúc kết quả bước 4.

## Kiểm chứng local

- 17 test backend đạt: 13 test chấm điểm/công bố/BXH (bao gồm 3 kịch bản mới: chưa công bố/quyền public/no email, đồng hạng và latest version, tổng mùa bỏ GW chưa PUBLISHED) và 4 test entry liên quan lưu lại/luật/snapshot/deadline. Không chạy lại toàn bộ auth.
- 13 test frontend liên quan entry/gameweek/results/leaderboard và Vite build đạt; Maven package đạt bằng POM/classifier kiểm tra dưới target, không sửa framework/dependency/POM nguồn.
- Chrome gọi Spring/H2 cô lập thật: đăng nhập → chọn 11 → Lưu đội hình → reload → thay Player 1 bằng Player 12 → lưu lần 2, snapshot 11 và version 2. Tại đúng deadline POST bị 409, sân giữ Player 12 và bị khóa. ADMIN riêng công bố có CSRF; cùng phiên người chơi nhận thông báo khi tải lại trạng thái, điểm 74.27 và 11 dòng; BXH xếp người đó trước người 66.77, tổng mùa đúng. Chưa công bố không có BXH điểm tạm. Desktop 1440px/390px không tràn và không có lỗi JavaScript.
- MySQL 9.6 local loopback/database fantasy_results_20261005: service đọc BXH GW/mùa đúng latest version 2, hai người cùng 67.77 đồng hạng 1,1; tổng mùa chỉ có một GW. Probe chỉ đọc kết quả cũ đã kiểm chứng, không ghi lại rating/đội/kết quả. Không coi đây là kiểm chứng Railway production.
- Seed/helper/ảnh/report được ignore dưới backend/target/fantasy-flow-check. Toàn bộ dữ liệu thi đấu/tài khoản là giả lập local, không phải cuộc thi GW6 thật.

## Production và giới hạn

Production đã có chín bảng Fantasy và bản mã hiện hành; GW6 OPEN, deadline 09/10/2026 00:00 Việt Nam, rosterAsOf 05/10/2026. Lưu/lưu lại/reload bằng tài khoản thật đã đạt, không kiểm tra lại. Kết quả GW6 còn chờ thu thập/nhập/xác nhận nguồn đầy đủ và thao tác ADMIN. Quy trình command, readiness, quyền còn thiếu, công bố và tái tính ở [chấm điểm](fantasy-results-2026.md). Nhập dữ liệu trận không tự công bố kết quả.

Kiểm tra chỉ đọc production ngày 05/10/2026: BXH GW6 và cả mùa đều HTTP 200/no-store, AWAITING_RESULTS, version null, publishedGameweeks=0, players=[]; không có điểm giả hoặc lỗi 500. Theo checkpoint production, hai tài khoản đều USER; UI/API ADMIN đã có nhưng cần chủ dự án cho phép cấp quyền cho tài khoản cụ thể riêng, không tự nâng quyền.

Bước 5 nay đã có BXH GW/mùa và kết quả của mình; xem đội người khác sau deadline, phân trang và thông báo ngoài website vẫn chưa triển khai. Cuộc thi thật chưa được công bố trong lượt này.

File của triển khai luồng/BXH trước đây:

- backend/src/main/java/com/premierhub/config/SecurityConfiguration.java
- backend/src/main/java/com/premierhub/fantasy/FantasyResultRepository.java, FantasyResultService.java, FantasyResultController.java
- backend/src/test/java/com/premierhub/fantasy/FantasyResultIntegrationTest.java
- frontend/src/hooks/useFantasyEntry.js
- frontend/src/components/FantasyPage.jsx, FantasyGameweek.jsx, FantasyResults.jsx, FantasyLeaderboard.jsx, FantasyLeaderboard.css
- frontend/src/api/fantasyLeaderboard.js
- frontend/src/fantasy/leaderboard.test.js
- docs/fantasy-player-flow-2026.md, fantasy-multiplayer-2026-plan.md

Commit message: `feat(fantasy): simplify lineup saving and add player leaderboards`.
