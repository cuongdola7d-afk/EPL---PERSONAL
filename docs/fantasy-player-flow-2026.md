# Luồng người chơi: lưu đội, khóa deadline, kết quả và BXH — 05/10/2026

Luồng cuối đã chốt là **Lưu đội hình** cập nhật trực tiếp đội tham gia, không có bước bắt buộc lưu nháp rồi chốt riêng. Đã phát hành và mở GW6 production; người dùng đã kiểm chứng lưu → sửa → lưu lại → reload. Xem [checkpoint production](fantasy-production-2026-10-05.md). Lượt quy trình 05/10 chỉ sửa tài liệu/đọc BXH; các điều chỉnh giao diện/OVR 06/10 đã được người dùng push, backend và MySQL production đã đồng bộ 910 sau migration được cho phép riêng.

## Hành vi

1. Đăng nhập mở tab Đội hình của bạn, sân 11 ô vẫn là màn hình chính. Tải đội từ server của đúng tài khoản/GW; không tự lấy lựa chọn localStorage hoặc đội vòng trước.
2. GW OPEN: chọn/đổi cầu thủ và sơ đồ theo luật hiện có; bấm Lưu đội hình khi đủ 11 người hợp lệ. Có thể sửa và lưu lại nhiều lần trước deadline. Chỉnh trên màn hình chưa thay đội dự thi cho tới khi lưu thành công.
3. Backend lưu đội có hiệu lực và snapshot trong transaction hiện có, dùng expectedVersion, session owner, CSRF, rosterAsOf và Clock server sau khóa/trước-sau SQL. Lưu mới lỗi giữ đội đã lưu trước. Nút duy nhất dùng endpoint `/submit` đã có; không thêm cơ chế tự chốt hoặc phụ thuộc cron.
4. Từ đúng deadline: server từ chối lưu, sân giữ đội đã lưu gần nhất ở trạng thái khóa. Hiển thị “Đã hết deadline. Đội hình đã khóa; kết quả sẽ được công bố sau.” Người chưa lưu đội hợp lệ không tự tham gia và không được cấp đội/điểm 0.
5. Công bố kết quả vẫn theo readiness và ADMIN + CSRF của bước 4; nhập rating đơn thuần chưa phải công bố. Điểm GW từng người ở góc trên bên phải avatar trên sân, giữ OVR ở vị trí cũ; ô kết quả dưới sân chỉ hiển thị tổng điểm. Khi chưa công bố không hiện điểm tạm. Không thay rating NULL thô hay cách xác nhận không chấm.
6. Tab BXH người chơi có Gameweek/Cả mùa. Điểm cao xếp trên, bằng điểm đồng hạng 1,1,3; account ID chỉ ổn định thứ tự trong nhóm bằng điểm, không tách hạng. Highlight tài khoản hiện tại bằng ID.

API nháp cũ vẫn giữ để tương thích dữ liệu/client cũ, không còn là thao tác bắt buộc trong giao diện. Mỗi lần Lưu đội hình cập nhật draft/snapshot đồng bộ bằng service đã có; các sửa chưa lưu chỉ nằm trong bộ nhớ giao diện. Giữ các luật 11 người duy nhất, vị trí hợp lệ, tối đa 3/CLB; giới hạn OVR mới là 910 theo yêu cầu ngày 06/10/2026. Nút Lưu đội hình nằm tại thanh chọn sơ đồ, thay nút Kiểm tra đội hình; bỏ khung metadata đội đã lưu. Thông báo “Đã lưu đội hình thành công.” tự mất sau 2,5 giây, lỗi vẫn giữ để người chơi xử lý.

## Điều chỉnh ngày 06/10/2026 — đã đồng bộ production

Frontend, validator backend, kiểm tra snapshot khi chấm điểm và schema H2 dùng cùng giới hạn 910. Snapshot đã lưu trước đây giữ nguyên. Migration bổ sung [2026-10-06-fantasy-ovr-limit-mysql.sql](../backend/sql/2026-10-06-fantasy-ovr-limit-mysql.sql) thay riêng CHECK OVR, giữ toàn bộ kiểm tra NULL/sơ đồ/version và dữ liệu hiện có; chạy lại không thay đổi khi đã là 910. Không sửa migration 860 đã áp dụng trước đây.

Production hiện đã đồng bộ 910 sau lần chủ dự án cho phép migration riêng ngày 06/10, xem biên bản bên dưới. Không đổi deadline, rosterAsOf, đội thật hoặc công bố kết quả. Lệnh mysql cho đích mới, sau khi được giao/xác nhận đích/backup: `source backend/sql/2026-10-06-fantasy-ovr-limit-mysql.sql;` từ client chạy ở gốc repo, đã chọn đúng database. Với client MySQL 9.6 cần bật `--commands=ON` để dùng `source` (mặc định tắt); không thêm vào cron hoặc Pre-deploy Command. Tài khoản chạy migration cần quyền ALTER và CREATE/EXECUTE/DROP ROUTINE cho procedure kiểm tra tạm; procedure được xóa khi hoàn tất, không tạo hệ thống migration mới.

### Lỗi lưu trên production sau khi push — 06/10/2026

Người dùng đã push `a94343a4e857754200b4e2f13cbd61973c0d82b8`; Railway deployment `a835f711-4c0e-425b-8eb2-af7a99a15c3d` SUCCESS chạy mã giới hạn 910. Kiểm tra chỉ đọc đúng backend/MySQL đích (database railway, UUID đã đối chiếu checkpoint) xác nhận CHECK `fantasy_entries_chk_3` vẫn có `submitted_total_ovr BETWEEN 11 AND 860`. Log lúc 09:30–09:31 UTC ghi CHECK này bị vi phạm khi POST `/api/fantasy/2026/me/gameweeks/6/submit`, dẫn tới HTTP 500 và “An unexpected error occurred”. Đây là thiếu migration production sau phát hành mã, không cần đổi thông báo thành công hoặc chạy lại build để sửa nguyên nhân.

Đội đã lưu ID 1/GW6 còn version/submitted_version 4/4 và tổng OVR 850 tại lần đọc; các lần lưu thất bại không thay thế đội đó. Bước cần được cho phép tiếp theo: tạo/kiểm tra backup SQL mới ngoài Git, áp dụng migration 910 đã kiểm chứng local, đọc lại CHECK và snapshot để xác nhận dữ liệu giữ nguyên; người dùng tự bấm lưu lại rồi đối chiếu API/database đúng tài khoản/GW. Chưa ghi production, chạy migration, redeploy hoặc tạo đội thử trong lượt chẩn đoán; không chạy lại test/build vì không sửa mã.

### Migration 910 production đã hoàn tất — 06/10/2026

Sau lần cho phép rõ ràng riêng migration 910, đối chiếu biến JDBC backend/MySQL bằng bộ nhớ và xác nhận database `railway`, MySQL 9.7.2, UUID `8835db23-b8ca-11f1-89a0-a2aa18198d9d`. Backend deployment `a835f711-4c0e-425b-8eb2-af7a99a15c3d` SUCCESS/commit `a94343a4e857754200b4e2f13cbd61973c0d82b8`; không cần redeploy.

Backup mới ngoài Git trước khi ghi: `backend/local-backups/fantasy-ovr910-production-20261006-164254/railway-before-ovr910-20261006-164254.sql`, 863.675 byte, SHA-256 `bd5160839334c6617c0ec881cd6bf4cf57b92ec9c6a63c5b4b378a22f143eeea`. Dump single-transaction exit 0, đủ CREATE TABLE của 35 bảng và footer hoàn tất; đối chiếu hash lần nữa trước migration. Chưa thử restore. Backup chứa dữ liệu riêng tư, không đưa vào Git.

Áp dụng đúng `backend/sql/2026-10-06-fantasy-ovr-limit-mysql.sql`, SHA-256 `1ce33655edbbfede554aa65802208f0702cd24c7de48ddb63dce9eaea6d14e4b`. Kết quả lần đầu nâng 860 → 910; lần hai trả already 910/unchanged. CHECK submission giữ nguyên điều kiện NULL/sơ đồ/version, chỉ nâng cap; các CHECK khác không đổi, procedure tạm đã được xóa. Dấu đối chiếu dữ liệu của cả chín bảng Fantasy trước/sau giống nhau, gồm metadata đội, draft, snapshot, deadline/audit, bằng chứng và kết quả. Không chạy lại bốn migration cũ, sửa bảng bóng đá hay công bố điểm.

Kiểm tra database bằng UPDATE tổng OVR của một entry đã tồn tại trong transaction rồi ROLLBACK: 910 được chấp nhận; 911 bị CHECK từ chối (MySQL 3819). Đối chiếu lại cả chín bảng xác nhận đội thật/deadline không đổi; không INSERT hoặc commit tổng giả. Qua Vercel, gọi riêng API `/api/fantasy/2026/validate?gameweek=6` chỉ SELECT: mẫu đủ 11 người đúng vị trí/không trùng/tối đa 3 CLB/tổng 910 trả HTTP 200/valid=true/issues=[]; mẫu 911 trả HTTP 200/valid=false và duy nhất OVR_LIMIT. Endpoint validator không ghi đội; service lưu dùng cùng validator, không gọi /submit hoặc /draft trong kiểm tra này.

GW6 vẫn OPEN, deadline `2026-10-08T17:00:00Z` = 09/10/2026 00:00 Việt Nam, rosterAsOf `2026-10-05`. Không tạo tài khoản/đội thử production, đọc/in cookie/token/password, commit/push/deploy hoặc chạy test/build vì không sửa mã. Audit chi tiết ngoài Git cùng thư mục backup: prepare-audit.json, migration-audit.json, backend-validation-audit.json. Còn chờ người dùng tự lưu đội bằng tài khoản thật rồi phản hồi để đối chiếu ID/GW/version/tổng/snapshot; chưa gọi đó là đã kiểm chứng lưu thành công thực tế sau migration.

File cần commit riêng lượt migration: docs/fantasy-player-flow-2026.md, docs/fantasy-multiplayer-2026-plan.md, docs/fantasy-user-lineups-2026.md, docs/fantasy-results-2026.md. Commit message đề xuất: `docs(fantasy): record production OVR 910 migration`. Không commit backup, audit riêng hoặc helper trong target; danh sách mã dưới đây thuộc lượt triển khai trước đã được người dùng push.

## Kiểm chứng và file cần commit — 06/10/2026

Kiểm chứng điều chỉnh 06/10: 10 test backend scoped trên H2 và Maven package đạt; 17 test frontend entry/lineup/results đạt. Sau khi sửa trạng thái ban đầu chưa có phiên/kết quả, chạy lại riêng 5 test results và Vite build; sau tinh chỉnh badge mobile chỉ build lại frontend. MySQL 9.6 local loopback 33027 chạy migration hai lần: dữ liệu cũ nguyên vẹn, 910 hợp lệ, 911 và submission NULL một phần bị CHECK chặn. Chrome với API giả lập cô lập: lưu 910 → thay người/lưu 909 → reload đúng đội; thông báo thành công tồn tại khoảng 2,5 giây; desktop/390px không tràn, điểm 0 vẫn hiển thị, OVR giữ nguyên, khóa đội sau công bố, tổng 71,10 và đổi GW xóa điểm cũ. Không coi kiểm tra giao diện giả lập là kiểm chứng production hoặc chạy lại Google thật.

File cần commit cho điều chỉnh này (không gồm helper/build/ảnh trong `backend/target`):

```text
backend/sql/2026-10-06-fantasy-ovr-limit-mysql.sql
backend/src/main/java/com/premierhub/fantasy/FantasyResultService.java
backend/src/main/java/com/premierhub/service/FantasyLineupService.java
backend/src/main/resources/fantasy-entry-schema.sql
backend/src/test/java/com/premierhub/fantasy/FantasyEntryIntegrationTest.java
backend/src/test/java/com/premierhub/fantasy/FantasyResultIntegrationTest.java
backend/src/test/java/com/premierhub/service/FantasyLineupServiceTest.java
frontend/src/components/FantasyEntry.css
frontend/src/components/FantasyPage.css
frontend/src/components/FantasyPage.jsx
frontend/src/components/FantasyPlayerAvatar.jsx
frontend/src/components/FantasyResults.jsx
frontend/src/fantasy/lineup.js
frontend/src/fantasy/lineup.test.js
frontend/src/fantasy/results.js
frontend/src/fantasy/results.test.js
frontend/src/hooks/useFantasyEntry.js
frontend/src/hooks/useFantasyResult.js
docs/fantasy-multiplayer-2026-plan.md
docs/fantasy-player-flow-2026.md
docs/fantasy-results-2026.md
docs/fantasy-user-lineups-2026.md
```

Commit message đề xuất: `feat(fantasy): streamline lineup saving and raise OVR cap to 910`.

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

Kiểm tra chỉ đọc production ngày 05/10/2026: BXH GW6 và cả mùa đều HTTP 200/no-store, AWAITING_RESULTS, version null, publishedGameweeks=0, players=[]; không có điểm giả hoặc lỗi 500. Sau lượt quy trình, chủ dự án đã cho phép cấp ADMIN riêng ID 2; ID 1 giữ USER. Đã đối chiếu phiên ID 2 mới: /me ADMIN và readiness 200/ready=false với blocker đúng; ID 1 đã bị readiness 403. UI/API quản trị giữ phân quyền/CSRF/proxy, không tự công bố điểm. Xem [checkpoint cấp quyền](fantasy-production-2026-10-05.md).

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
